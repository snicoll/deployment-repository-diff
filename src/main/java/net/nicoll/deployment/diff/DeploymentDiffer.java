package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.function.ThrowingFunction;

class DeploymentDiffer {

	private static final Log logger = LogFactory.getLog(DeploymentDiffer.class);

	private final Deployment deployment;

	DeploymentDiffer(Deployment deployment) {
		this.deployment = deployment;
	}

	public void diff() throws IOException {
		logger.info("Diffing %s from '%s' against '%s'".formatted(this.deployment.version(),
				this.deployment.leftDirectory(), this.deployment.rightDirectory()));
		List<ModuleDiff> moduleDiffs = diffModules(module -> new ModuleDiffer(this.deployment, module).diff());
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
				message.append("%n\tOnly in %s:%n\t\t".formatted(this.deployment.rightName()));
				message.append(String.join("%n\t\t".formatted(), moduleDiff.onlyInRight()));
			}
			if (!moduleDiff.onlyInLeft().isEmpty()) {
				message.append("%n\tOnly in %s:%n\t\t".formatted(this.deployment.leftName()));
				message.append(String.join("%n\t\t".formatted(), moduleDiff.onlyInLeft()));
			}
			logger.error(message.toString());
		}
	}

	private List<ModuleDiff> diffModules(ThrowingFunction<Module, ModuleDiff> moduleDiff) throws IOException {
		List<Path> leftModules = PathUtils.listDirectoriesIn(this.deployment.leftDirectory());
		logger.debug("Found '%s' modules for %s in '%s'".formatted(leftModules.size(), this.deployment.leftName(),
				this.deployment.leftDirectory()));
		List<Path> rightModules = PathUtils.listDirectoriesIn(this.deployment.rightDirectory());
		logger.debug("Found '%s' modules for %s in '%s'".formatted(rightModules.size(), this.deployment.rightName(),
				this.deployment.rightDirectory()));
		List<String> processed = new ArrayList<>();
		List<ModuleDiff> moduleDiffs = new ArrayList<>();
		for (Path leftModule : leftModules) {
			String name = leftModule.getFileName().toString();
			Path rightModule = findWithFileName(rightModules, name);
			Module module = new Module(name, leftModule, rightModule);
			if (rightModule == null) {
				logger.error("%s does not contain module '%s'".formatted(this.deployment.rightName(), name));
				moduleDiffs
					.add(new ModuleDiff(module, PathUtils.toFileNames(PathUtils.listFilesAndDirectoriesIn(leftModule)),
							Collections.emptyList()));
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
			logger.error("Only in %s: %s".formatted(this.deployment.rightName(), onlyInRight));
		}
		return moduleDiffs;
	}

	private static Path findWithFileName(List<Path> paths, String fileName) {
		return paths.stream().filter(p -> p.getFileName().toString().equals(fileName)).findFirst().orElse(null);
	}

}
