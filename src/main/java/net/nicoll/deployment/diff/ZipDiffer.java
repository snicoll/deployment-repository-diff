package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.nicoll.deployment.diff.DiffUtils.Diff;

import static org.assertj.core.api.Assertions.assertThat;

class ZipDiffer {

	private final Path leftZip;

	private final Path rightZip;

	ZipDiffer(Path leftZip, Path rightZip) {
		this.leftZip = leftZip;
		this.rightZip = rightZip;
	}

	Diff<String> diff(MismatchFilter<String> filter) throws IOException {
		List<String> leftEntries = entries(this.leftZip);
		List<String> rightEntries = entries(this.rightZip);
		return DiffUtils.diff(leftEntries, rightEntries, filter);
	}

	private static List<String> entries(Path file) throws IOException {
		assertThat(file).exists().isRegularFile();
		List<String> names = new ArrayList<>();
		try (ZipFile jarFile = new ZipFile(file.toFile())) {
			Enumeration<? extends ZipEntry> zipEntries = jarFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry jarEntry = zipEntries.nextElement();
				names.add(jarEntry.getName());
			}
		}
		return names;
	}
}
