package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the application defined data types for the ROTATE message event.
 * 
 * @author Dan McCafferty
 * 
 */
public enum Rotate {
	LEFT("left"), 
	RIGHT("right"),
	NONE("none");
	
	// The application defined name for the message data type
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The application defined name for the message data type
	 */
	Rotate(String name) {
		this.name = name;
	}

	/**
	 * Returns the application defined name for the message data type.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
}
