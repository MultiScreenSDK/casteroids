package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the TV application defined configuration types.<br>
 * <br>
 * These configuration types are used to enabled/disable different features of the game. This was added to assist in the
 * performance tuning of the TV Application.
 * 
 * @author Dan McCafferty
 * 
 */
public enum ConfigType {

	COLLISION_DETECTION("isCollisionDetectionEnabled", "Collision Detection", true),
	OPTIMIZED_COLLISION_DETECTION("isOptimizedCollisionDetectionEnabled", "Optimized Collision Detection", true),
	OPTIMIZED_RESPAWN("isOptimizedCollisionDetectionEnabled", "Optimized Respawn", true),
	ALIEN("isAlienEnabled", "Alien", true),
	SOUND("isSoundEnabled", "Sound", true),
	SPACESHIP_TINTING("isSpaceshipTintingEnabled", "Spaceship Tinting", true),
	BULLET_TINTING("isBulletTintingEnabled", "Bullet Tinting", true),
	GAME_TEXT("isGameTextEnabled", "Game Text", true),
	POINTS_TEXT("isPointsTextEnabled", "Points Text", true),
	BACKGROUND_IMAGE("isBackgroundImageEnabled", "Background Image", true),
	FPS("isBackgroundImageEnabled", "FPS", true);
	
	// The TV application defined name for the ConfigType
	private final String name;

	// A String description of the ConfigType.
	private final String description;
	
	// The default value for the ConfigType.
	private final boolean defaultValue;

	ConfigType(String name, String description, boolean defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
	}

	/**
	 * Returns the TV application defined name for the ConfigType.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a String description of the ConfigType. s
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
