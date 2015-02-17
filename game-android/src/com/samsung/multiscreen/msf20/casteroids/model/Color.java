package com.samsung.multiscreen.msf20.casteroids.model;

/**
 * Enumeration of all the application defined data types for the COLORs. A Color is sent as a part of the GAME_JOIN
 * message event.
 * 
 * @author Dan McCafferty
 * 
 */
public enum Color {
	RED("left", "#FF0000"),
	ORANGE("orange", "#FF8000"),
	GREEN("right", "#00FF00"),
	BLUE("none", "#0000FF");

    // The application defined name for the message data type.
    private final String name;

    // The color-int representing the color. This is intended to be used by the UI layer to display the color.
    private final int colorInt;

    /**
     * Constructor.
     * 
     * @param name
     *            The application defined name for the message data type
     * @param colorHex
     *            The hex code representing the color. This is intended to be used by the UI layer to display the color.
     */
    Color(String name, String colorHex) {
        this.name = name;
        this.colorInt = android.graphics.Color.parseColor(colorHex);
    }

    /**
     * Returns the application defined name for the message data type.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the color-int representing this Color. This is intended to be used by the UI layer to display the color.
     * 
     * @return
     */
    public int getColorInt() {
        return colorInt;
    }
    
    /**
     * Returns an Color with the given TV application defined name or NULL if no match.
     * 
     * @param name
     *            The TV application defined name for the message event
     * @return
     */
    public static Color getByName(String name) {
        Color color = null;

        for (Color currentColor : Color.values()) {
            if (currentColor.name.equalsIgnoreCase(name)) {
                color = currentColor;
                break;
            }
        }

        return color;
    }    
}
