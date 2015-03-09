package com.samsung.multiscreen.msf20.casteroids.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the ConfigType to Enabled/Disabled flag mappings.<br>
 * <br>
 * These ConfigTypes mappings are used to enabled/disable different features of the game. This was added to assist in
 * the performance tuning of the TV Application.
 * 
 * @author Dan McCafferty
 * 
 */
public class ConfigTypeMap {
	private final Map<ConfigType, Boolean> map = new HashMap<ConfigType, Boolean>();

	/**
	 * Constructor.
	 */
	public ConfigTypeMap() {
		super();

		// Initialize the list and map with the default values.
		init();
	}

	/**
	 * Initializes the list and map with the default values.
	 */
	private void init() {
		for (ConfigType type : ConfigType.values()) {
			map.put(type, type.getDefaultValue());
		}
	}

	/**
	 * Returns a sorted list of ConfigTypes.
	 * 
	 * @return
	 */
	public ConfigType[] getConfigTypes() {
		return ConfigType.values();
	}

	/**
	 * Returns a flag indicating whether or not the given ConfigType is enabled.
	 * 
	 * @param type
	 * @return
	 */
	public boolean isEnabled(ConfigType type) {
		return map.containsKey(type) ? map.get(type) : false;
	}

	/**
	 * Sets the flag indicating whether or not the given ConfigType is enabled.
	 * 
	 * @param type
	 * @param isEnabled
	 */
	public void setIsEnabled(ConfigType type, boolean isEnabled) {
		map.put(type, isEnabled);
	}

	@Override
	public String toString() {
		return "ConfigTypeMap [map=" + map + "]";
	}
}
