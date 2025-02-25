package net.nicoll.deployment.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

		public String diffDescription(String title, Supplier<String> onlyInLeftHeader,
				Supplier<String> onlyInRightHeader) {
			if (hasSameEntries()) {
				throw new IllegalStateException("Could not provide a diff description for " + this);
			}
			StringBuilder message = new StringBuilder("%s:".formatted(title));
			if (!onlyInRight().isEmpty()) {
				message.append("%n\t%s:%n\t\t".formatted(onlyInRightHeader.get()));
				message.append(String.join("%n\t\t".formatted(), toString(onlyInRight())));
			}
			if (!onlyInLeft().isEmpty()) {
				message.append("%n\t%s:%n\t\t".formatted(onlyInLeftHeader.get()));
				message.append(String.join("%n\t\t".formatted(), toString(onlyInLeft())));
			}
			return message.toString();
		}

		private List<String> toString(List<T> list) {
			return list.stream().map(Object::toString).collect(Collectors.toList());
		}

	}

}
