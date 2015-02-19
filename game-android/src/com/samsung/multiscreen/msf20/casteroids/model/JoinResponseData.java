package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Contains data from the JOIN_RESPONSE event.
 * 
 * @author Dan McCafferty
 * 
 */
public class JoinResponseData {
	// The TV Application defined response codes.
	public static final int RESPONCE_CODE_SUCCESS = 0;
	public static final int RESPONSE_CODE_NO_SLOT = 1;
	public static final int RESPONSE_CODE_COLOR_TAKEN = 2;

	// The TV Application defined response code.
	private final int responseCode;
	
	// The name of the player.
	private final String name;
	
	// The color the player chose.
	private final Color color;

	/**
	 * Constructor.
	 * 
	 * @param responseCode
	 *            The TV Application defined response code.
	 * @param name
	 *            The name of the player.
	 * @param color
	 *            The color the player chose.
	 */
	public JoinResponseData(int responseCode, String name, Color color) {
		super();
		this.responseCode = responseCode;
		this.name = name;
		this.color = color;
	}

	/**
	 * Returns whether or not the response code is a successful response code.
	 * 
	 * @return
	 */
	public boolean isSuccessful() {
		return (responseCode == RESPONCE_CODE_SUCCESS);
	}

	/**
	 * Returns the TV Application defined response code.
	 * 
	 * @return
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * Returns the player's name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the color the player chose.
	 * 
	 * @return
	 */
	public Color getColor() {
		return color;
	}

	@Override
	public String toString() {
		return "JoinResponseData [responseCode=" + responseCode + ", name=" + name + ", color=" + color + "]";
	}
}
