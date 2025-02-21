package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.nicoll.deployment.diff.DiffUtils.Diff;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JarDiffer {

	private static final Log logger = LogFactory.getLog(JarDiffer.class);

	private final Deployment deployment;

	private final Path left;

	private final Path right;

	private final String classifier;

	JarDiffer(Deployment deployment, Path left, Path right, String classifier) {
		this.deployment = deployment;
		this.left = left;
		this.right = right;
		this.classifier = classifier;
	}

	public void diff(MismatchFilter<String> entriesFilter) throws IOException {
		String jarType = this.classifier.isEmpty() ? "main" : "'%s'".formatted(this.classifier);
		logger.debug("Checking %s JARs".formatted(jarType));
		Diff<String> entriesDiff = DiffUtils.diff(entries(this.left), entries(this.right), entriesFilter);
		if (!entriesDiff.hasSameEntries()) {
			StringBuilder message = new StringBuilder("Mismatch between %s JARs:".formatted(jarType));
			if (!entriesDiff.onlyInRight().isEmpty()) {
				message.append("%n\tOnly in %s JAR (%s):%n\t\t".formatted(this.deployment.rightName(),
						this.deployment.rightDirectory().relativize(this.right)));
				message.append(String.join("%n\t\t".formatted(), entriesDiff.onlyInRight()));
			}
			if (!entriesDiff.onlyInLeft().isEmpty()) {
				message.append("%n\tOnly in %s JAR (%s):%n\t\t".formatted(this.deployment.leftName(),
						this.deployment.leftDirectory().relativize(this.left)));
				message.append(String.join("%n\t\t".formatted(), entriesDiff.onlyInLeft()));
			}
			logger.error(message.toString());
		}
		else {
			logger.debug("Identical entries for %s JARs".formatted(jarType));
		}
		ManifestDiff manifestDiff = diffManifest();
		if (!manifestDiff.hasSameEntries()) {
			StringBuilder message = new StringBuilder("Mismatch between manifest of %s JARs:".formatted(jarType));
			if (!manifestDiff.mismatches().isEmpty()) {
				message.append("%n\tValues mismatches:%n\t\t".formatted());
				message.append(String.join("%n\t\t".formatted(), manifestDiff.mismatches()));
			}
			if (!manifestDiff.onlyInRight().isEmpty()) {
				message.append("%n\tOnly in %s manifest (%s):%n\t\t".formatted(this.deployment.rightName(),
						this.deployment.rightDirectory().relativize(this.right)));
				message.append(String.join("%n\t\t".formatted(), manifestDiff.onlyInRight()));
			}
			if (!manifestDiff.onlyInLeft().isEmpty()) {
				message.append("%n\tOnly in %s manifest (%s):%n\t\t".formatted(this.deployment.leftName(),
						this.deployment.leftDirectory().relativize(this.left)));
				message.append(String.join("%n\t\t".formatted(), manifestDiff.onlyInLeft()));
			}
			logger.error(message.toString());
		}
		else {
			logger.debug("Identical manifest entries for %s JARs".formatted(jarType));
		}
	}

	private ManifestDiff diffManifest() throws IOException {
		Manifest leftManifest = JarUtils.readManifest(this.left);
		Manifest rightManifest = JarUtils.readManifest(this.right);
		Map<Object, Object> leftEntries = new HashMap<>(leftManifest.getMainAttributes());
		Map<Object, Object> rightEntries = new HashMap<>(rightManifest.getMainAttributes());
		List<String> onlyInLeft = new ArrayList<>();
		List<String> mismatches = new ArrayList<>();
		Iterator<Entry<Object, Object>> it = leftEntries.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> entry = it.next();
			Object key = entry.getKey();
			Object value = rightEntries.get(key);
			if (value == null) {
				onlyInLeft.add(key.toString());
			}
			else {
				if (!value.equals(entry.getValue())) {
					mismatches.add("Value mismatch for '%s': '%s' (%s) vs. '%s' (%s)".formatted(key, entry.getValue(),
							this.deployment.leftName(), value, this.deployment.rightName()));
				}
				it.remove();
				rightEntries.remove(key);
			}
		}
		return new ManifestDiff(onlyInLeft, rightEntries.keySet().stream().map(Object::toString).toList(), mismatches);

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

	record ManifestDiff(List<String> onlyInLeft, List<String> onlyInRight, List<String> mismatches) {

		public boolean hasSameEntries() {
			return onlyInLeft.isEmpty() && onlyInRight.isEmpty() && mismatches.isEmpty();
		}
	}

}
