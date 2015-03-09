package com.samsung.multiscreen.msf20.casteroids.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Helper class that encodes/decodes message data transmitted between the client and TV applications.
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
	 * Returns the JSON encoded JOIN_REQUEST message data in the TV application defined JSON format:<br>
	 * <code>
	 *     { "name": "Buck Rogers", "color": "blue" }
	 * </code>
	 * 
	 * @param name
	 * @param color
	 * @return
	 */
	public static String encodeJoinRequestData(String name, Color color) {
		JSONObject jsonObject = new JSONObject();

		try {
			jsonObject.put("name", name);
			jsonObject.put("color", color.getName());
		} catch (JSONException e) {
			Log.e(TAG, "Faled to JSON encode JOIN_REQUEST message data. name=" + name + ", color=" + color, e);
			return null;
		}

		return jsonObject.toString();
	}

	/**
	 * Returns the JSON encoded ROTATE message data in the TV application defined JSON format:<br>
	 * <code>
	 *     { "rotate": "left", strength: 50 }
	 * </code>
	 * 
	 * @param rotate
	 *            Which direction to rotate.
	 * @param strength
	 *            The strength of the rotate from 0 to 20.
	 * @return
	 */
	public static String encodeRotateData(Rotate rotate, int strength) {
		JSONObject jsonObject = new JSONObject();

		try {
			jsonObject.put("rotate", rotate.getName());
			jsonObject.put("strength", (rotate != Rotate.NONE ? strength : 0));
		} catch (JSONException e) {
			Log.e(TAG, "Faled to JSON encode ROTATE message data. rotate=" + rotate + ", strength=" + strength, e);
			return null;
		}

		return jsonObject.toString();
	}

	/******************************************************************************************************************
	 * Decode Methods
	 */

	/**
	 * Returns the decoded and sorted SlotData list from the SLOT_UPDATE message data sent by the TV application. The
	 * data is The list is sorted by the order of the color. The data is expected to be in the TV application defined
	 * JSON format:<br>
	 * <code>
	 * [ { "available": false, "color": "red" }, { "available": true, "color": "blue" } ]
	 * </code>
	 * 
	 * @param data
	 *            The string data from the SLOT_UPDATE message.
	 * @return
	 */
	public static List<SlotData> decodeSlotUpdateSlotData(String data) {
		List<SlotData> slotDataList = new ArrayList<SlotData>();

		try {
			// Initialize a JSON Array with the given data object
			JSONArray jsonArray = new JSONArray(data);

			// Loop through the length of the JSON Array...
			for (int i = 0; i < jsonArray.length(); i++) {
				// Get the JSON object at the current index.
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				// Get the score data from the current JSON object.
				boolean available = jsonObject.getBoolean("available");
				Color color = Color.getByName(jsonObject.getString("color"));

				// Create and add a ScoreData object to the score data list.
				slotDataList.add(new SlotData(available, color));
			}
		} catch (JSONException e) {
			Log.e(TAG, "Failed to decode the SlotData list. data=" + data, e);
			slotDataList.clear();
		}

		// Sort the slot data.
		Collections.sort(slotDataList);

		// Return the sorted slot data list.
		return slotDataList;
	}

	/**
	 * Returns the decoded JoinResponseData from the JOIN_RESPONSE message data sent by the TV application. The data is
	 * expected to be in the TV application defined JSON format:<br>
	 * <code>
	 * { "response_code": 0, "name": "Buck", "color": "red" }
	 * </code>
	 * 
	 * @param data
	 *            The string data from the JOIN_RESPONSE message.
	 * @return
	 */
	public static JoinResponseData decodeJoinResponseData(String data) {
		JoinResponseData joinResponseData = null;

		try {
			// Get the JSON object from the data string.
			JSONObject jsonObject = new JSONObject(data);

			int responseCode = jsonObject.getInt("response_code");
			String name = jsonObject.getString("name");
			Color color = Color.getByName(jsonObject.getString("color"));

			// Create and add a ScoreData object to the score data list.
			joinResponseData = new JoinResponseData(responseCode, name, color);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to decode the JoinResponseData list. data=" + data, e);
		}

		// Return the sorted slot data list.
		return joinResponseData;
	}

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
				Color color = Color.getByName(jsonObject.getString("color"));
				int score = jsonObject.getInt("score");

				// Create and add a ScoreData object to the score data list.
				scoreDataList.add(new ScoreData(name, color, score));
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
