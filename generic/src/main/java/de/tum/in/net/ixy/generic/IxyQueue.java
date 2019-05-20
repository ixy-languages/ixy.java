package de.tum.in.net.ixy.generic;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract class used as starting point for {@code Rx} and {@code Tx} queues.
 * 
 * @author Esaú García Sánchez-Torija
 */
@Slf4j
public abstract class IxyQueue {

	/** The number of entries this queue has. */
	protected int numberOfEntries;
	
	/** The index that points to the current head. */
	protected int index;

	/** Instantiates a queue of a specific capacity denoted by {@code entries}. */
	public IxyQueue(final int entries) {
		if (BuildConstants.DEBUG) log.trace("Instantiating IxyQueue with {} entries", entries);
		numberOfEntries = entries;
	}

}
