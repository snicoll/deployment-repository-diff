package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.nicoll.deployment.diff.DiffUtils.Diff;
import net.nicoll.deployment.diff.JarDiffUtils.ManifestDiff;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JarDiffer {

	private static final Log logger = LogFactory.getLog(JarDiffer.class);

	private final GroupDeployment groupDeployment;

	private final Path left;

	private final Path right;

	private final String classifier;

	JarDiffer(GroupDeployment groupDeployment, Path left, Path right, String classifier) {
		this.groupDeployment = groupDeployment;
		this.left = left;
		this.right = right;
		this.classifier = classifier;
	}

	public void diff(MismatchFilter<String> entriesFilter) throws IOException {
		String jarType = this.classifier.isEmpty() ? "main" : "'%s'".formatted(this.classifier);
		logger.debug("Checking %s JARs".formatted(jarType));
		Diff<String> entriesDiff = DiffUtils.diff(entries(this.left), entries(this.right), entriesFilter);
		if (!entriesDiff.hasSameEntries()) {
			logger.error(entriesDiff.diffDescription("Mismatch between %s JARs".formatted(jarType),
					() -> "Only in %s JAR (%s)".formatted(this.groupDeployment.leftName(),
							this.groupDeployment.leftDirectory().relativize(this.left)),
					() -> "Only in %s JAR (%s)".formatted(this.groupDeployment.leftName(),
							this.groupDeployment.leftDirectory().relativize(this.left))));
		}
		else {
			logger.debug("Identical entries for %s JARs".formatted(jarType));
		}
		ManifestDiff manifestDiff = JarDiffUtils.diffManifest(this.left, this.right);
		if (!manifestDiff.hasSameEntries()) {
			StringBuilder message = new StringBuilder("Mismatch between manifest of %s JARs:".formatted(jarType));
			if (!manifestDiff.valueMismatches().isEmpty()) {
				message.append("%n\tValues mismatches:%n\t\t".formatted());
				message.append(String.join("%n\t\t".formatted(),
						manifestDiff.valueMismatches()
							.stream()
							.map(valueMismatch -> valueMismatch.toDescription(this.groupDeployment.leftName(),
									this.groupDeployment.rightName()))
							.toList()));
			}
			if (!manifestDiff.onlyInRight().isEmpty()) {
				message.append("%n\tOnly in %s manifest (%s):%n\t\t".formatted(this.groupDeployment.rightName(),
						this.groupDeployment.rightDirectory().relativize(this.right)));
				message.append(String.join("%n\t\t".formatted(), manifestDiff.onlyInRight()));
			}
			if (!manifestDiff.onlyInLeft().isEmpty()) {
				message.append("%n\tOnly in %s manifest (%s):%n\t\t".formatted(this.groupDeployment.leftName(),
						this.groupDeployment.leftDirectory().relativize(this.left)));
				message.append(String.join("%n\t\t".formatted(), manifestDiff.onlyInLeft()));
			}
			logger.error(message.toString());
		}
		else {
			logger.debug("Identical manifest entries for %s JARs".formatted(jarType));
		}
	}

	private static List<String> entries(Path file) throws IOException {
		assertThat(file).exists().isRegularFile();
		List<String> names = new ArrayList<>();
		try (JarFile jarFile = new JarFile(file.toFile())) {
			Enumeration<JarEntry> jarEntries = jarFile.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry jarEntry = jarEntries.nextElement();
				names.add(jarEntry.getName());
			}
		}
		return names;
	}

}
