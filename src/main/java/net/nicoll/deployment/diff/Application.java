package net.nicoll.deployment.diff;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import net.nicoll.deployment.diff.DiffUtils.Diff;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.aether.graph.Dependency;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	private static final Log logger = LogFactory.getLog(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	ApplicationRunner run() {
		return args -> {
			List<String> options = args.getNonOptionArgs();
			if (options.size() != 3) {
				throw new IllegalArgumentException(
						"Usage: <leftDirectory> <rightDirectory> <version>, got " + options.size());
			}
			Path leftDirectory = Paths.get(options.get(0));
			Path rightDirectory = Paths.get(options.get(1));
			String version = options.get(2);
			GroupDeployment groupDeployment = new Deployment("Maven", leftDirectory, "Gradle", rightDirectory, version)
				.registerJarMismatchFilter("", new MainJarMismatchFilter())
				.registerJarMismatchFilter("javadoc", new JavadocJarMismatchFilter())
				.setModuleMismatchFilter(new ModuleMismatchFilter())
				.setPomMismatchFilter(new PomMismatchFilter())
				.resolveGroupId(true, "org.springframework.ws");
			new DeploymentDiffer(groupDeployment).diff();

			logger.info("Handling special case, docs to spring-ws-docs");
			Path leftZip = groupDeployment.leftDirectory()
				.resolve("spring-ws")
				.resolve(groupDeployment.version())
				.resolve("spring-ws-%s-docs.zip".formatted(groupDeployment.version()));
			Path rightZip = groupDeployment.rightDirectory()
				.resolve("spring-ws-docs")
				.resolve(groupDeployment.version())
				.resolve("spring-ws-docs-%s.zip".formatted(groupDeployment.version()));
			Diff<String> diff = new ZipDiffer(leftZip, rightZip).diff(MismatchFilter.noop());
			if (!diff.hasSameEntries()) {
				logger.error(diff.diffDescription("Mismatch between docs distribution",
						() -> "Only in %s docs (%s)".formatted(groupDeployment.leftName(),
								groupDeployment.leftDirectory().relativize(leftZip)),
						() -> "Only in %s docs (%s)".formatted(groupDeployment.rightName(),
								groupDeployment.rightDirectory().relativize(rightZip))));
			}
		};
	}

	static class MainJarMismatchFilter implements MismatchFilter<String> {

		@Override
		public boolean ignoreInLeft(String key) {
			return key.endsWith("package-info.class");
		}

		@Override
		public boolean ignoreInRight(String key) {
			return false;
		}

	}

	static class JavadocJarMismatchFilter implements MismatchFilter<String> {

		@Override
		public boolean ignoreInLeft(String key) {
			boolean classOrPackageUse = key.contains("/class-use/") || key.endsWith("/package-use.html");
			if (classOrPackageUse) {
				logger.trace("Ignoring '%s".formatted(key));
			}
			return classOrPackageUse;
		}

		@Override
		public boolean ignoreInRight(String key) {
			return false;
		}

	}

	static class ModuleMismatchFilter implements MismatchFilter<String> {

		private static final List<String> suffixesToIgnoreInGradle = List.of(".sha256", ".sha512", ".md5", ".module",
				".module.sha1");

		@Override
		public boolean ignoreInLeft(String key) {
			return false;
		}

		@Override
		public boolean ignoreInRight(String key) {
			for (String suffix : suffixesToIgnoreInGradle) {
				if (key.endsWith(suffix)) {
					logger.trace("Ignoring '%s' in Gradle as it matches a suffix to ignore".formatted(key));
					return true;
				}
			}
			return false;
		}

	}

	static class PomMismatchFilter implements MismatchFilter<Dependency> {

		@Override
		public boolean ignoreInLeft(Dependency dependency) {
			if (Boolean.TRUE.equals(dependency.getOptional())) {
				return true;
			}
			return false;
		}

		@Override
		public boolean ignoreInRight(Dependency dependency) {
			return false;
		}

	}

}
