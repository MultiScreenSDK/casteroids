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

    /**
     * Returns the JOIN message data in the TV application defined JSON format:<br>
     * <code>
     *     { "name": "Buck", "color": "blue" }
     * </code>
     * 
     * @param name
     * @param color
     * @return
     */
    public static String jsonEncodeJoinData(String name, Color color) {
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

}
