package com.samsung.multiscreen.msf20.game.model;

/**
 * Enumeration of all the application defined message events.
 * 
 * @author Dan McCafferty
 * 
 */
public enum Event {
	ROTATE("rotate"), THRUST("thrust"), FIRE("fire");

	// The application defined name for the message event
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The application defined name for the message event
	 */
	Event(String name) {
		this.name = name;
	}

	/**
	 * Returns the application defined name for the message event.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
}
