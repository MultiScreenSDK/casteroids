package com.samsung.multiscreen.msf20.casteroids.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Helper class that JSON encodes/decodes message data transmitted between the client and TV applications.
 * 
 * @author Dan McCafferty
 * 
 */
public class MessageDataHelper {
    // Used to identify the source of a log message.
    private static final String TAG = MessageDataHelper.class.getSimpleName();

    /******************************************************************************************************************
     * Encode Methods
     */

    /**
     * Returns the JSON encoded JOIN message data in the TV application defined format:<br>
     * <code>
     *     { "name": "Buck", "color": "blue" }
     * </code>
     * 
     * @param name
     * @param color
     * @return
     */
    public static String encodeJoinData(String name, Color color) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("name", name);
            jsonObject.put("color", color.getName());
        } catch (JSONException e) {
            Log.e(TAG, "Faled to JSON encode GAME_JOIN message data. name=" + name + ", color=" + color, e);
            return null;
        }

        return jsonObject.toString();
    }

    /******************************************************************************************************************
     * Decode Methods
     */

    /**
     * Returns the decoded game start count down time in seconds from the GAME_START message data sent by the TV
     * application.
     * 
     * @param data
     * @return
     */
    public static int decodeStartCountDownSeconds(String data) {
        return getIntFromString(data, 0);
    }

    /**
     * Returns the decoded player out count down time in seconds from the PLAYER_OUT message data sent by the TV
     * application.
     * 
     * @param data
     * @return
     */
    public static int decodePlayerOutCountDownSeconds(String data) {
        return getIntFromString(data, 0);
    }

    /******************************************************************************************************************
     * Internal helper methods
     */

    /**
     * Internal helper method that parses an integer from the given string value. If the string is null or not an
     * integer the given default int value is returned.
     * 
     * @param stringVal
     *            The string that contains the integer.
     * @param defaultIntVal
     *            The value to return if the string val is null or not an integer.
     * @return
     */
    private static int getIntFromString(String stringVal, int defaultIntVal) {
        int intVal = defaultIntVal;

        try {
            intVal = Integer.parseInt(stringVal);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Failed to convert '" + stringVal + "' to int.", nfe);
            intVal = defaultIntVal;
        }

        return intVal;
    }
}
