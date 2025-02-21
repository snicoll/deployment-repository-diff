package net.nicoll.deployment.diff;

import java.util.List;

record ModuleDiff(Module module, List<String> onlyInLeft, List<String> onlyInRight) {

	boolean hasSameEntries() {
		return (module.left() != null && module.right() != null) && onlyInLeft.isEmpty() && onlyInRight.isEmpty();
	}

}
