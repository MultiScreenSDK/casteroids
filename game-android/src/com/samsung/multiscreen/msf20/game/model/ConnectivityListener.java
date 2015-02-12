package com.samsung.multiscreen.msf20.game.model;

/**
 * This interface is implemented by components that want to receive connectivity updates.<br>
 * <br>
 * This interface tries to normalize the data so that it can be used to receive connectivity updates from the
 * Multiscreen SDK and other SDKs that your application integrates with. <br>
 * <br>
 * Callbacks will not be on the UI thread. If the callback results in the UI being updated you need to run that change
 * ont he UI thread.
 * 
 * @author Dan McCafferty
 * 
 */
public interface ConnectivityListener {

    // Connectivity update event ids
    public static final int DISCOVERY_STARTED = 1;
    public static final int DISCOVERY_STOPPED = 2;
    public static final int DISCOVERY_FOUND_SERVICE = 3;
    public static final int DISCOVERY_LOST_SERVICE = 4;
    public static final int APPLICATION_CONNECTED = 5;
    public static final int APPLICATION_DISCONNECTED = 6;
    public static final int APPLICATION_CONNECT_FAILED = 7;

    /**
     * Called any time a connectivity event occurs. <br>
     * <br>
     * NOTE: This callback will not be on the UI thread. If the callback results in the UI being updated you need to run
     * that change ont he UI thread.
     * 
     * @param eventId
     *            One of the following: DISCOVERY_STARTED, DISCOVERY_STOPPED, DISCOVERY_FOUND_SERVICE,
     *            DISCOVERY_LOST_SERVICE, APPLICATION_CONNECTED, APPLICATION_DISCONNECTED, or
     *            APPLICATION_CONNECT_FAILED.
     */
    public void onConnectivityUpdate(int eventId);
}
