package net.nicoll.deployment.diff;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
			Deployment deployment = new Deployment("Maven", leftDirectory, "Gradle", rightDirectory, version)
				.registerJarMismatchFilter("", new MainJarMismatchFilter())
				.registerJarMismatchFilter("javadoc", new JavadocJarMismatchFilter())
				.setModuleMismatchFilter(new ModuleMismatchFilter())
				.resolveGroupId(true, "org", "springframework", "ws");
			new DeploymentDiffer(deployment).diff();
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

}
