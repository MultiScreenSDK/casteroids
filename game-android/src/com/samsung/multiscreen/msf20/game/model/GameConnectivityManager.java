package com.samsung.multiscreen.msf20.game.model;

import android.content.Context;

/**
 * Extends the ConnectivityManager with app specific logic.
 * 
 * @author Dan McCafferty
 *
 */
public class GameConnectivityManager extends ConnectivityManager {
    
    // An singleton instance of this class
    private static GameConnectivityManager instance = null;
        
    // The URL where the TV application lives
    private static final String TV_APP_URL = "http://192.168.1.105:8080/dist/tv/";
    //private static final String TV_APP_URL = "http://127.0.0.1:63342/game-webapp/dist/tv/index.html";

    // The Channel ID for the TV application
    private static final String TV_APP_CHANNEL_ID = "com.samsung.multiscreen.game";

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
                    instance = new GameConnectivityManager(context, TV_APP_URL, TV_APP_CHANNEL_ID, DEFAULT_DISCOVERY_TIMEOUT_MILLIS);
                }
            }
        }
        return instance;
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
}
