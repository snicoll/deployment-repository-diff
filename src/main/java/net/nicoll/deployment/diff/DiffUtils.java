package net.nicoll.deployment.diff;

import java.util.ArrayList;
import java.util.List;

class DiffUtils {

	static <T> Diff<T> diff(List<T> left, List<T> right, MismatchFilter<T> filter) {
		left = new ArrayList<>(left);
		right = new ArrayList<>(right);
		List<T> onlyInLeft = new ArrayList<>();
		for (T key : left) {
			if (right.contains(key)) {
				right.remove(key);
			}
			else if (!filter.ignoreInLeft(key)) {
				onlyInLeft.add(key);
			}
		}
		List<T> onlyInRight = right.stream().filter(key -> !filter.ignoreInRight(key)).toList();
		return new Diff<>(onlyInLeft, onlyInRight);
	}

	record Diff<T>(List<T> onlyInLeft, List<T> onlyInRight) {

		public boolean hasSameEntries() {
			return this.onlyInLeft.isEmpty() && this.onlyInRight.isEmpty();
		}

	}

}
