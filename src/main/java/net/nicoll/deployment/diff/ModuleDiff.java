package net.nicoll.deployment.diff;

import java.util.List;

import net.nicoll.deployment.diff.PomDiffer.PomDiff;

record ModuleDiff(Module module, List<String> onlyInLeft, List<String> onlyInRight, PomDiff pomDiff) {

	boolean hasSameEntries() {
		return (module.left() != null && module.right() != null) && onlyInLeft.isEmpty() && onlyInRight.isEmpty()
				&& pomDiff().hasSameEntries();
	}

}
