package net.nicoll.deployment.diff;

import java.nio.file.Path;

record Module(String name, Path left, Path right) {
}
