package com.samsung.multiscreen.msf20.casteroids.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
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
     * Returns the JSON encoded JOIN message data in the TV application defined JSON format:<br>
     * <code>
     *     { "name": "Buck Rogers", "color": "blue" }
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
     * application. The data is expected to be the TV application defined format of an integer stored in a string value.
     * 
     * @param data
     *            The string data from the GAME_START message.
     * @return
     */
    public static int decodeGameStartCountDownSeconds(String data) {
        return getIntFromString(data, 0);
    }

    /**
     * Returns the decoded player out count down time in seconds from the PLAYER_OUT message data sent by the TV
     * application. The data is expected to be the TV application defined format of an integer stored in a string value.
     * 
     * @param data
     *            The string data from the PLAYER_OUT message.
     * @return
     */
    public static int decodePlayerOutCountDownSeconds(String data) {
        return getIntFromString(data, 0);
    }

    /**
     * Returns the decoded and sorted ScoreData list from the GAME_OVER message data sent by the TV application. The
     * data is The list is sorted by the best score to the worst score. The data is expected to be in the TV application
     * defined JSON format:<br>
     * <code>
     * [ { "name": "Buck Rogers", "score": 9700 }, { "name": "Captain Kirk", "score": 3370 } ]
     * </code>
     * 
     * @param data
     *            The string data from the GAME_OVER message.
     * @return
     */
    public static List<ScoreData> decodeGameOverScoreData(String data) {
        List<ScoreData> scoreDataList = new ArrayList<ScoreData>();

        try {
            // Initialize a JSON Array with the given data object
            JSONArray jsonArray = new JSONArray(data);

            // Loop through the length of the JSON Array...
            for (int i = 0; i < jsonArray.length(); i++) {
                // Get the JSON object at the current index.
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Get the score data from the current JSON object.
                String name = jsonObject.getString("name");
                int score = jsonObject.getInt("score");

                // Create and add a ScoreData object to the score data list.
                scoreDataList.add(new ScoreData(name, score));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to decode the ScoreData list. data=" + data, e);
            scoreDataList.clear();
        }

        // Sort the score data.
        Collections.sort(scoreDataList);

        // Return the sorted score data list.
        return scoreDataList;
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
