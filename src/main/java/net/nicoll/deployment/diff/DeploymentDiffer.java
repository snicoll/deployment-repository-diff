package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.nicoll.deployment.diff.PomDiffer.PomDiff;
import net.nicoll.deployment.diff.PomDiffer.PomMismatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.model.Dependency;

import org.springframework.util.function.ThrowingFunction;

class DeploymentDiffer {

	private static final Log logger = LogFactory.getLog(DeploymentDiffer.class);

	private final GroupDeployment groupDeployment;

	DeploymentDiffer(GroupDeployment groupDeployment) {
		this.groupDeployment = groupDeployment;
	}

	public void diff() throws IOException {
		logger.info("Diffing %s from '%s' against '%s'".formatted(this.groupDeployment.version(),
				this.groupDeployment.leftDirectory(), this.groupDeployment.rightDirectory()));
		List<ModuleDiff> moduleDiffs = diffModules(module -> new ModuleDiffer(this.groupDeployment, module).diff());
		moduleDiffs.forEach(this::logModuleDiff);
	}

	private void logModuleDiff(ModuleDiff moduleDiff) {
		String moduleName = moduleDiff.module().name();
		if (moduleDiff.hasSameEntries()) {
			logger.info("Module '%s' has similar entries".formatted(moduleName));
		}
		else {
			StringBuilder message = new StringBuilder("Diff result for %s:".formatted(moduleName));
			if (!moduleDiff.onlyInRight().isEmpty()) {
				message.append("%n\tOnly in %s:%n\t\t".formatted(this.groupDeployment.rightName()));
				message.append(String.join("%n\t\t".formatted(), moduleDiff.onlyInRight()));
			}
			if (!moduleDiff.onlyInLeft().isEmpty()) {
				message.append("%n\tOnly in %s:%n\t\t".formatted(this.groupDeployment.leftName()));
				message.append(String.join("%n\t\t".formatted(), moduleDiff.onlyInLeft()));
			}
			PomDiff pomDiff = moduleDiff.pomDiff();
			if (pomDiff != null && !pomDiff.hasSameEntries()) {
				if (!pomDiff.pomMismatches().isEmpty()) {
					message.append("%n\tDependencies mismatches:%n\t\t".formatted());
					message.append(String.join("%n\t\t".formatted(),
							pomDiff.pomMismatches()
								.stream()
								.map(PomMismatch::toDescription)
								.toList()));
				}
				if (!pomDiff.onlyInRight().isEmpty()) {
					message.append("%n\tDependencies only in %s:%n\t\t".formatted(this.groupDeployment.rightName()));
					message.append(String.join("%n\t\t".formatted(),
							pomDiff.onlyInRight().stream().map(this::toString).toList()));
				}
				if (!pomDiff.onlyInLeft().isEmpty()) {
					message.append("%n\tDependencies only in %s:%n\t\t".formatted(this.groupDeployment.leftName()));
					message.append(String.join("%n\t\t".formatted(),
							pomDiff.onlyInLeft().stream().map(this::toString).toList()));
				}
			}
			logger.error(message.toString());
		}
	}

	private String toString(Dependency dependency) {
		return "%s:%s:%s - %s %s".formatted(dependency.getGroupId(), dependency.getArtifactId(),
				dependency.getVersion(), dependency.getScope(), dependency.isOptional() ? "(optional)" : "");
	}

	private List<ModuleDiff> diffModules(ThrowingFunction<Module, ModuleDiff> moduleDiff) throws IOException {
		List<Path> leftModules = PathUtils.listDirectoriesIn(this.groupDeployment.leftDirectory());
		logger.debug("Found '%s' modules for %s in '%s'".formatted(leftModules.size(), this.groupDeployment.leftName(),
				this.groupDeployment.leftDirectory()));
		List<Path> rightModules = PathUtils.listDirectoriesIn(this.groupDeployment.rightDirectory());
		logger.debug("Found '%s' modules for %s in '%s'".formatted(rightModules.size(),
				this.groupDeployment.rightName(), this.groupDeployment.rightDirectory()));
		List<String> processed = new ArrayList<>();
		List<ModuleDiff> moduleDiffs = new ArrayList<>();
		for (Path leftModule : leftModules) {
			String name = leftModule.getFileName().toString();
			Path rightModule = findWithFileName(rightModules, name);
			Module module = new Module(name, leftModule, rightModule);
			if (rightModule == null) {
				logger.error("%s does not contain module '%s'".formatted(this.groupDeployment.rightName(), name));
			}
			else {
				moduleDiffs.add(moduleDiff.apply(module));
			}
			processed.add(name);
		}
		List<String> onlyInRight = PathUtils.toFileNames(rightModules)
			.stream()
			.filter(name -> !processed.contains(name))
			.toList();
		if (!onlyInRight.isEmpty()) {
			logger.error("Only in %s: %s".formatted(this.groupDeployment.rightName(), onlyInRight));
		}
		return moduleDiffs;
	}

	private static Path findWithFileName(List<Path> paths, String fileName) {
		return paths.stream().filter(p -> p.getFileName().toString().equals(fileName)).findFirst().orElse(null);
	}

}
