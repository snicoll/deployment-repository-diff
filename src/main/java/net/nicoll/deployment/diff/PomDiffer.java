package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

import org.springframework.util.FileSystemUtils;

class PomDiffer {

	private final GroupDeployment groupDeployment;

	private final List<String> scopesToInclude;

	private final MismatchFilter<Dependency> filter;

	PomDiffer(GroupDeployment groupDeployment, List<String> scopesToInclude) {
		this.groupDeployment = groupDeployment;
		this.scopesToInclude = scopesToInclude;
		this.filter = groupDeployment.deployment().pomMismatchFilter();
	}

	PomDiffer(GroupDeployment groupDeployment) {
		this(groupDeployment, List.of("compile", "compile+runtime", "runtime"));
	}

	PomDiff diff(String artifactId) throws IOException {
		List<Dependency> left = resolveDependencies(this.groupDeployment.leftRoot(), artifactId);
		List<Dependency> right = resolveDependencies(this.groupDeployment.rightRoot(), artifactId);
		left = new ArrayList<>(left);
		right = new ArrayList<>(right);
		List<Dependency> onlyInLeft = new ArrayList<>();
		for (Dependency dependency : left) {
			Dependency matchingDependency = foundIn(right, dependency);
			if (matchingDependency != null) {
				right.remove(matchingDependency);
			}
			else if (!filter.ignoreInLeft(dependency)) {
				onlyInLeft.add(dependency);
			}
		}
		List<Dependency> onlyInRight = right.stream().filter(key -> !filter.ignoreInRight(key)).toList();
		return new PomDiff(onlyInLeft, onlyInRight);
	}

	private Dependency foundIn(List<Dependency> dependencies, Dependency target) {
		for (Dependency dependency : dependencies) {
			if (isSimilarDependency(target, dependency)) {
				return dependency;
			}
		}
		return null;
	}

	private boolean isSimilarDependency(Dependency left, Dependency right) {
		if (!isSimilarArtifact(left.getArtifact(), right.getArtifact())) {
			return false;
		}
		return Objects.equals(left.getScope(), right.getScope())
				&& Objects.equals(left.isOptional(), right.isOptional());
	}

	private boolean isSimilarArtifact(Artifact left, Artifact that) {
		return Objects.equals(left.getArtifactId(), that.getArtifactId())
				&& Objects.equals(left.getGroupId(), that.getGroupId())
				&& Objects.equals(left.getVersion(), that.getVersion())
				&& Objects.equals(left.getClassifier(), that.getClassifier());
	}

	private List<Dependency> resolveDependencies(Path localRepository, String artifact) throws IOException {
		MavenDependencyResolver resolver = new MavenDependencyResolver(prepareLocalRepository(localRepository));
		List<Dependency> dependencies = resolver.resolveDependencies(this.groupDeployment.groupId(), artifact,
				this.groupDeployment.version());
		return dependencies.stream().filter(candidate -> this.scopesToInclude.contains(candidate.getScope())).toList();
	}

	private Path prepareLocalRepository(Path localRepositoryLocation) throws IOException {
		Path tempDirectory = Files.createTempDirectory("deployment-repository-diff");
		FileSystemUtils.copyRecursively(localRepositoryLocation, tempDirectory);
		return tempDirectory;
	}

	record PomDiff(List<Dependency> onlyInLeft, List<Dependency> onlyInRight) {

		public boolean hasSameEntries() {
			return this.onlyInLeft.isEmpty() && this.onlyInRight.isEmpty();
		}

	}

}
