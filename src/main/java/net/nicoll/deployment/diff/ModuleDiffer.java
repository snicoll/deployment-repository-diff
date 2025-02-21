package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import net.nicoll.deployment.diff.DiffUtils.Diff;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ModuleDiffer {

	private static final Log logger = LogFactory.getLog(ModuleDiffer.class);

	private final Deployment deployment;

	private final Module module;

	ModuleDiffer(Deployment deployment, Module module) {
		this.deployment = deployment;
		this.module = module;
	}

	ModuleDiff diff() throws IOException {
		logger.info("Diffing '%s'".formatted(module.name()));
		Path leftRoot = module.left().resolve(this.deployment.version());
		Path rightRoot = module.right().resolve(this.deployment.version());
		Map<String, Jar> leftJars = getJars(leftRoot, module.name());
		logger.debug("Found '%s' JARs for %s in '%s'".formatted(leftJars.size(), this.deployment.leftName(), leftRoot));
		Map<String, Jar> rightJars = getJars(rightRoot, module.name());
		logger.debug(
				"Found '%s' JARs for %s in '%s'".formatted(rightJars.size(), this.deployment.rightName(), rightRoot));
		for (Entry<String, Jar> entry : leftJars.entrySet()) {
			String classifier = entry.getKey();
			Jar rightJar = rightJars.get(classifier);
			if (rightJar != null) {
				new JarDiffer(this.deployment, entry.getValue().path(), rightJar.path(), classifier)
					.diff(this.deployment.jarMismatchFilter(classifier));
			}
			else {
				logger.error("No '%s' JAR found for '%s'".formatted(module.name(), classifier));
			}
		}
		List<String> leftFiles = PathUtils.toFileNames(PathUtils.listFilesAndDirectoriesIn(leftRoot));
		List<String> rightFiles = PathUtils.toFileNames(PathUtils.listFilesAndDirectoriesIn(rightRoot));
		Diff<String> filesDiff = DiffUtils.diff(leftFiles, rightFiles, this.deployment.moduleMismatchFilter());
		return new ModuleDiff(this.module, filesDiff.onlyInLeft(), filesDiff.onlyInRight());

	}

	private Map<String, Jar> getJars(Path directory, String moduleName) throws IOException {
		Predicate<Path> jarFilter = candidate -> candidate.getFileName().toString().endsWith(".jar");
		List<Path> jarFiles = PathUtils.listPaths(directory, jarFilter);
		Map<String, Jar> jars = new HashMap<>();
		jarFiles.forEach(path -> {
			String fileName = path.getFileName().toString();
			if (!fileName.startsWith(moduleName)) {
				throw new IllegalStateException("Unexpected jar for '%s': %s".formatted(moduleName, fileName));
			}
			int versionIndex = fileName.indexOf(this.deployment.version());
			int jarIndex = fileName.indexOf(".jar");
			String endOfFileName = fileName.substring(versionIndex + this.deployment.version().length(), jarIndex);
			String classifier = endOfFileName.startsWith("-") ? endOfFileName.substring(1) : "";
			jars.put(classifier, new Jar(path, moduleName));
		});
		return jars;
	}

	record Jar(Path path, String name) {
	}

}
