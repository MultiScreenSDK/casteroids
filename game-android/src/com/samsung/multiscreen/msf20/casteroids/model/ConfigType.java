package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the TV application defined configuration types. Adding an additional enum will automatically
 * display it in the UI and send it to the client.<br>
 * <br>
 * These configuration types are used to enabled/disable different features of the game. This was added to assist in the
 * performance tuning of the TV Application.
 * 
 * @author Dan McCafferty
 * 
 */
public enum ConfigType {

	SOUND("isSoundEnabled", "Sound", false),
	FPS("isFpsEnabled", "FPS", false),
	SHORT_GAME("isShortGameEnabled", "Short Game", false),
	BACKGROUND_IMAGE("isBackgroundImageEnabled", "Background Image", true),
	COLLISION_DETECTION("isCollisionDetectionEnabled", "Collision Detection", true),
	ALIEN("isAlienEnabled", "Alien", true),
	GAME_TEXT("isGameTextEnabled", "Game Text", true),
	POINTS_TEXT("isPointsTextEnabled", "Points Text", false),
	AGGRESSIVE_UPDATE_CYCLE("isAggressiveUpdateCycle", "Sacrifice Responsiveness for Performance", true);

	// The TV application defined name for the ConfigType. This field is as the key when sending the data to the client.
	private final String name;

	// A String description of the ConfigType. This field is used by the UI layer when displaying the options to the user.
	private final String description;
	
	// The default value for the ConfigType.
	private final boolean defaultValue;

	ConfigType(String name, String description, boolean defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the TV application defined name for the ConfigType. This field is as the key when sending the data to the
	 * client.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a String description of the ConfigType. This field is used by the UI layer when displaying the options to
	 * the user.
	 * 
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the default value for the ConfigType.
	 * 
	 * @return
	 */
	public boolean getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * Returns an ConfigType with the given TV application defined name or NULL if no match.
	 * 
	 * @param name
	 *            The TV application defined name for the ConfigType
	 * @return
	 */
	public static ConfigType getByName(String name) {
		ConfigType type = null;

		for (ConfigType currentType : ConfigType.values()) {
			if (currentType.name.equalsIgnoreCase(name)) {
				type = currentType;
				break;
			}
		}

		return type;
	}
}
