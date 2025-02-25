package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;

record Deployment(String leftName, Path leftDirectory, String rightName, Path rightDirectory, String version,
		Map<String, MismatchFilter<String>> jarMismatchFilters, MismatchFilter<String> moduleMismatchFilter,
		MismatchFilter<Dependency> pomMismatchFilter) {

	Deployment(String leftName, Path leftDirectory, String rightName, Path rightDirectory, String version) {
		this(leftName, leftDirectory, rightName, rightDirectory, version, new HashMap<>(), MismatchFilter.noop(),
				MismatchFilter.noop());
	}

	MismatchFilter<String> jarMismatchFilter(String classifier) {
		MismatchFilter<String> filter = this.jarMismatchFilters.get(classifier);
		return (filter != null) ? filter : MismatchFilter.noop();
	}

	Deployment registerJarMismatchFilter(String classifier, MismatchFilter<String> filter) {
		HashMap<String, MismatchFilter<String>> map = new HashMap<>(this.jarMismatchFilters);
		map.put(classifier, filter);
		return new Deployment(this.leftName, this.leftDirectory, this.rightName, this.rightDirectory, this.version, map,
				this.moduleMismatchFilter, this.pomMismatchFilter);
	}

	Deployment setModuleMismatchFilter(MismatchFilter<String> filter) {
		return new Deployment(this.leftName, this.leftDirectory, this.rightName, this.rightDirectory, this.version,
				this.jarMismatchFilters, filter, this.pomMismatchFilter);
	}

	Deployment setPomMismatchFilter(MismatchFilter<Dependency> filter) {
		return new Deployment(this.leftName, this.leftDirectory, this.rightName, this.rightDirectory, this.version,
				this.jarMismatchFilters, this.moduleMismatchFilter, filter);
	}

	GroupDeployment resolveGroupId(boolean unique, String groupId) throws IOException {
		String[] parts = groupId.split("\\.");
		Path targetLeft = this.leftDirectory;
		for (String part : parts) {
			targetLeft = resolveDirectory(unique, targetLeft, part);
		}
		Path targetRight = this.rightDirectory;
		for (String part : parts) {
			targetRight = resolveDirectory(unique, targetRight, part);
		}
		return new GroupDeployment(this, targetLeft, targetRight, groupId);
	}

	private static Path resolveDirectory(boolean unique, Path directory, String name) throws IOException {
		List<Path> candidates = PathUtils.listFilesAndDirectoriesIn(directory);
		Path result = candidates.stream()
			.filter(candidate -> candidate.getFileName().toString().equals(name))
			.findFirst()
			.orElse(null);
		if (result == null) {
			throw new IllegalStateException("Directory with name '%s' not found in '%s'".formatted(name, directory));
		}
		if (unique && candidates.size() != 1) {
			throw new IllegalStateException(
					"Invalid '%s', expected only '%s', but got %s".formatted(directory, name, candidates));
		}
		return result;
	}

}
