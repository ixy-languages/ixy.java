package de.tum.in.net.ixy.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A static class used to manipulate {@link Thread threads}.
 *
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Threads {

	/**
	 * Makes the current thread sleep for {@code milis} milliseconds.
	 *
	 * @param millis The number of milliseconds.
	 */
	public static void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			log.warn("Could NOT sleep for {} milliseconds.", millis, e);
		}
	}

}
