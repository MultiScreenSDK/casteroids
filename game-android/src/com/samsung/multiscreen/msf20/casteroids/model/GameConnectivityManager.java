package com.samsung.multiscreen.msf20.casteroids.model;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.samsung.multiscreen.msf20.casteroids.BuildConfig;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

/**
 * This class extends the ConnectivityManager with Castroids game specific logic. It serves as a layer of abstraction
 * between the Castroids game logic and the connectivity and transport logic in a way where the the game does not know
 * anything about the underlying protocols or SDK being used.
 * 
 * @author Dan McCafferty
 * 
 */
public class GameConnectivityManager extends ConnectivityManager implements ConnectivityListener, MessageListener {

	// An singleton instance of this class
	private static GameConnectivityManager instance = null;

	// The URL where the TV application lives
	// private static final String TV_APP_URL = "http://127.0.0.1:63342/game-webapp/dist/tv/index.html";
	private static final String TV_APP_URL = "http://dev-multiscreen.samsung.com/casteroids/tv/index.html";

	// The Channel ID for the TV application
	private static final String TV_APP_CHANNEL_ID = "com.samsung.multiscreen.castroids";

	// Contains game state data collected from events received from the TV Application.
	private GameState gameState = new GameState();

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param url
	 * @param channelId
	 * @param discoveryTimeoutMillis
	 */
	private GameConnectivityManager(Context context, String url, String channelId, long discoveryTimeoutMillis) {
		super(context, url, channelId, discoveryTimeoutMillis);

		// Register for connectivity updates
		registerConnectivityListener(this);
	}

	/**
	 * Returns the instance.
	 * 
	 * @param context
	 * @return
	 */
	public static GameConnectivityManager getInstance(Context context) {
		if (instance == null) {
			synchronized (lock) {
				if (instance == null) {
					instance = new GameConnectivityManager(context, TV_APP_URL, TV_APP_CHANNEL_ID, 0);
				}
			}
		}
		return instance;
	}

	/**
	 * Returns the GameState object. It contains the most recent game state data collected from events received from the
	 * TV Application.
	 * 
	 * @return
	 */
	public GameState getGameState() {
		return gameState;
	}

	/**
	 * Sends a JOIN message to the TV application.
	 * 
	 * @param name
	 *            The name of the player.
	 * @param color
	 *            The color the player chose.
	 * @return
	 */
	public void sendJoinMessage(String name, Color color) {
		String data = MessageDataHelper.encodeJoinData(name, color);

		if (data != null) {
			sendMessage(Event.JOIN_REQUEST.getName(), data);
		} else {
			Log.e(TAG, "Failed to create data string using name='" + name + "', and color=" + color + ".");
		}

		// FIXME: Remove. Sending JOIN_RESPONSE until the TV Application is updated to do this.
		List<MessageListener> listeners = messageListenerMap.get(Event.JOIN_RESPONSE.getName());
		if (listeners != null) {
			String responseData = "{ \"response_code\": 0, \"name\": \""+name+"\", \"color\": \""+color.getName()+"\" }";
			for (MessageListener listener : listeners) {
				listener.onMessage(Event.JOIN_RESPONSE.getName(), responseData, null);
			}
		}
	}

	/**
	 * Sends a QUIT message to the TV application.
	 */
	public void sendQuitMessage() {
		sendMessage(Event.QUIT.getName(), null);
	}

	/**
	 * Sends a ROTATE message to the TV application.
	 * 
	 * @param rotate
	 */
	public void sendRotateMessage(Rotate rotate) {
		sendMessage(Event.ROTATE.getName(), rotate.getName());
	}

	/**
	 * Sends a THRUST message to the TV application.
	 * 
	 * @param thrust
	 */
	public void sendThrustMessage(Thrust thrust) {
		sendMessage(Event.THRUST.getName(), thrust.getName());
	}

	/**
	 * Sends a FIRE message to the TV application.
	 * 
	 * @param fire
	 */
	public void sendFireMessage(Fire fire) {
		sendMessage(Event.FIRE.getName(), fire.getName());
	}

	/**
	 * Registers the given listener for message updates.
	 * 
	 * @param listener
	 * @param events
	 */
	public void registerMessageListener(MessageListener listener, Event... events) {
		for (Event event : events) {
			registerMessageListener(listener, event.getName());
		}
	}

	/**
	 * Unregisters the given listener for message updates.
	 * 
	 * @param listener
	 * @param events
	 */
	public void unregisterMessageListener(MessageListener listener, Event... events) {
		for (Event event : events) {
			unregisterMessageListener(listener, event.getName());
		}
	}

	@Override
	public void onConnectivityUpdate(int eventId) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Received connectivity update '" + eventId + "'.");
		}

		switch (eventId) {
			case APPLICATION_CONNECTED:
				// Any time we connect register for Events that this class is interested in.
				this.registerMessageListener(this, Event.SLOT_UPDATE, Event.JOIN_RESPONSE, Event.GAME_START, Event.GAME_OVER,
				        Event.PLAYER_OUT);
				break;
			default:
				// Ignore.
		}
	}

	@Override
	public void onMessage(String eventName, String data, byte[] payload) {
		// Get the Event object from the Event name.
		Event event = Event.getByName(eventName);

		// If the Event object is null, log and ignore it.
		if (event == null) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG, "Ignoring unexpected event '" + eventName + "'. data=" + data);
			}
			return;
		}

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Received message '" + eventName + "'. data=" + data);
		}

		// Switch on the Event
		switch (event) {
			case SLOT_UPDATE:
				gameState.onSlotData(MessageDataHelper.decodeSlotUpdateSlotData(data));
				break;
			case JOIN_RESPONSE:
				gameState.onJoinResponse(MessageDataHelper.decodeJoinResponseData(data));
				break;
			case GAME_START:
				gameState.onGameStart(MessageDataHelper.decodeGameStartCountDownSeconds(data));
				break;
			case GAME_OVER:
				gameState.onGameOver(MessageDataHelper.decodeGameOverScoreData(data));
				break;
			case PLAYER_OUT:
				gameState.onPlayerOut(MessageDataHelper.decodePlayerOutCountDownSeconds(data));
				break;
			default:
				// Ignore.
		}
	}

	@Override
	public String toString() {
		return "GameConnectivityManager [gameState=" + gameState + "]";
	}
}
