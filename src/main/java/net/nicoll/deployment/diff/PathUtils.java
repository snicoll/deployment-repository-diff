package net.nicoll.deployment.diff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

abstract class PathUtils {

	static List<String> toFileNames(List<Path> paths) {
		return paths.stream().map(Path::getFileName).map(Object::toString).toList();
	}

	static List<Path> listDirectoriesIn(Path directory) throws IOException {
		return listPaths(directory, Files::isDirectory);
	}

	static List<Path> listFilesAndDirectoriesIn(Path directory) throws IOException {
		return listPaths(directory, candidate -> true);
	}

	static List<Path> listPaths(Path directory, Predicate<Path> filter) throws IOException {
		try (Stream<Path> list = Files.list(directory)) {
			return list.filter(filter).toList();
		}
	}

}
