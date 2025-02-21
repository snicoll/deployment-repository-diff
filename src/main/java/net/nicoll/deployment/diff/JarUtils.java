package net.nicoll.deployment.diff;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

abstract class JarUtils {

	static final String MANIFEST_LOCATION = "META-INF/MANIFEST.MF";

	private static final Log logger = LogFactory.getLog(JarUtils.class);

	static Manifest readManifest(Path jarFile) throws IOException {
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

}
