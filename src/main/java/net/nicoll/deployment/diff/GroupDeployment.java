package net.nicoll.deployment.diff;

import java.nio.file.Path;

record GroupDeployment(Deployment deployment, Path leftDirectory, Path rightDirectory, String groupId) {

	Path leftRoot() {
		return this.deployment.leftDirectory();
	}

	String leftName() {
		return this.deployment.leftName();
	}

	Path rightRoot() {
		return this.deployment.rightDirectory();
	}

	String rightName() {
		return this.deployment.rightName();
	}

	String version() {
		return this.deployment.version();
	}

}
