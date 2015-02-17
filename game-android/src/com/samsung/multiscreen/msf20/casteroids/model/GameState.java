package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the application defined data types for the GAME_STATE message event.
 * 
 * @author Dan McCafferty
 *
 */
public enum GameState {
    // The game has started
    ON_START("on_start"), 
            
    // The game is over
    ON_OVER("on_over");
    	
	// The application defined name for the message data type
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The application defined name for the message data type
	 */
	GameState(String name) {
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
