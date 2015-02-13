package com.samsung.multiscreen.msf20.game.model;

/**
 * Enumeration of all the application defined data types for the GAME_OPTION message event.
 * 
 * @author Dan McCafferty
 *
 */
public enum GameOption {
    // The user has joined the game
    JOIN("join");
	
	// The application defined name for the message data type
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The application defined name for the message data type
	 */
	GameOption(String name) {
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
