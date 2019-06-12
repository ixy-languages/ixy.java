package de.tum.in.net.ixy.generic;

/**
 * Ixy's queue specification.
 *
 * @author Esaú García Sánchez-Torija
 */
public interface IxyQueue {

	/**
	 * Returns the capacity of the queue.
	 *
	 * @return The capacity of the queue.
	 */
	int getCapacity();

	/**
	 * Returns the index of the queue.
	 *
	 * @return The index of the queue.
	 */
	int getIndex();

}
