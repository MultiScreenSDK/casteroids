package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the TV application defined message events.
 * 
 * @author Dan McCafferty
 * 
 */
public enum Event {
	/**
	 * Events that the Client sends to the TV Application.
	 */

	// Event sent to request to join the game. The server will send back a JOIN_RESPONSE that indicates whether or not
	// the user was allowed to join the game.
	JOIN_REQUEST("join_request", true),

	// Event sent to quit the game.
	QUIT("quit", true),

	// Event sent to start/stop rotation of the spacecraft
	ROTATE("rotate", true),

	// Event sent to enable/disable the spacecraft's thrust.
	THRUST("thrust", true),

	// Event sent to have the spacecraft fire a bullet.
	FIRE("fire", true),

	// Event sent to enabled/disable different features of the game. This was added to assist in the performance tuning of
	// the TV Application.
	CONFIG_UPDATE("config_update", true),

	/**
	 * Events that the Client receives from the TV Application.
	 */

	// Event received with the updated slot information. This will allow us to know how many users can join the game and
	// which colors are available.
	SLOT_UPDATE("slot_update", false),

	// Event received in response to a JOIN_REQUEST that indicates whether or not the user was allowed to join the game.
	JOIN_RESPONSE("join_response", false),

	// Event received with count down to the start of the game.
	GAME_START("game_start", false),

	// Event received to indicate the end of the game.
	GAME_OVER("game_over", false),

	// Event received to indicate the player's is out of the game since his/her spaceship was blown to smithereens (i.e.
	// destroyed). The payload contains the count down until the player can rejoin the game.
	PLAYER_OUT("player_out", false);

	// The TV application defined name for the message event
	private final String name;

	// A flag indicating whether or not the event is one the client sends versus
	// one the client receives.
	private final boolean send;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            The TV application defined name for the message event
	 * @param send
	 *            A flag indicating whether or not the event is one the client sends versus one the client receives.
	 */
	Event(String name, boolean send) {
		this.name = name;
		this.send = send;
	}

	/**
	 * Returns the TV application defined name for the message event.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a flag indicating whether or not the event is one the client sends this event.
	 * 
	 * @return
	 */
	public boolean doesClientSend() {
		return send;
	}

	/**
	 * Returns a flag indicating whether or not the event is one the client receives the event.
	 * 
	 * @return
	 */
	public boolean doesClientReceive() {
		return !send;
	}

	/**
	 * Returns an Event with the given TV application defined name or NULL if no match.
	 * 
	 * @param name
	 *            The TV application defined name for the message event
	 * @return
	 */
	public static Event getByName(String name) {
		Event event = null;

		for (Event currentEvent : Event.values()) {
			if (currentEvent.name.equalsIgnoreCase(name)) {
				event = currentEvent;
				break;
			}
		}

		return event;
	}
}
