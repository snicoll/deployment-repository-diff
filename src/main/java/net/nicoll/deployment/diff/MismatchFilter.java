package net.nicoll.deployment.diff;

interface MismatchFilter<T> {

	static <T> MismatchFilter<T> noop() {
		return new MismatchFilter<>() {
			@Override
			public boolean ignoreInLeft(T key) {
				return false;
			}

			@Override
			public boolean ignoreInRight(T key) {
				return false;
			}
		};
	}

	boolean ignoreInLeft(T key);

	boolean ignoreInRight(T key);

}
