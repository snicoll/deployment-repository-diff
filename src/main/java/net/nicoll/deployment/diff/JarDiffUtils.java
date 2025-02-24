package net.nicoll.deployment.diff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import net.nicoll.deployment.diff.JarDiffUtils.ManifestDiff.ValueMismatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

abstract class JarDiffUtils {

	static final String MANIFEST_LOCATION = "META-INF/MANIFEST.MF";

	private static final Log logger = LogFactory.getLog(JarDiffUtils.class);

	static ManifestDiff diffManifest(Path leftJar, Path rightJar) throws IOException {
		Manifest leftManifest = JarDiffUtils.readManifest(leftJar);
		Manifest rightManifest = JarDiffUtils.readManifest(rightJar);
		Map<Object, Object> leftEntries = new HashMap<>(leftManifest.getMainAttributes());
		Map<Object, Object> rightEntries = new HashMap<>(rightManifest.getMainAttributes());
		List<String> onlyInLeft = new ArrayList<>();
		List<ValueMismatch> valueMismatches = new ArrayList<>();
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
					valueMismatches.add(new ValueMismatch(entry.getKey().toString(), entry.getValue().toString(),
							value.toString()));
				}
				it.remove();
				rightEntries.remove(key);
			}
		}
		return new ManifestDiff(onlyInLeft, rightEntries.keySet().stream().map(Object::toString).toList(),
				valueMismatches);

	}

	private static Manifest readManifest(Path jarFile) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(jarFile, Collections.emptyMap())) {
			Path manifestPath = fs.getPath(MANIFEST_LOCATION);
			try (InputStream is = Files.newInputStream(manifestPath)) {
				Manifest manifest = new Manifest(is);
				logger.trace("""
						Reading manifest from '%s':
						%s""".formatted(manifestPath, toLog(manifest)));
				return manifest;
			}
			catch (IOException ex) {
				String type = Files.isRegularFile(manifestPath) ? "JAR file" : "JAR directory structure";
				throw new IllegalStateException(
						"Invalid %s '%s', cannot read %s".formatted(type, manifestPath, MANIFEST_LOCATION), ex);
			}
		}
	}

	private static String toLog(Manifest manifest) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		manifest.write(out);
		String content = out.toString(StandardCharsets.UTF_8);
		return content.lines().map(line -> "\t" + line).collect(Collectors.joining("\n")).stripTrailing();
	}

	record ManifestDiff(List<String> onlyInLeft, List<String> onlyInRight, List<ValueMismatch> valueMismatches) {

		public boolean hasSameEntries() {
			return onlyInLeft.isEmpty() && onlyInRight.isEmpty() && valueMismatches.isEmpty();
		}

		record ValueMismatch(String key, String leftValue, String rightValue) {

			String toDescription(String leftName, String rightValue) {
				return "'%s': '%s' (%s) vs. '%s' (%s)".formatted(this.key, this.leftValue, leftName, this.rightValue,
						rightValue);
			}
		}
	}

}
