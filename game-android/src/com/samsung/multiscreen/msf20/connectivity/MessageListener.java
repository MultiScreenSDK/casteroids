package com.samsung.multiscreen.msf20.connectivity;

/**
 * This interface is implemented by components that want to receive messsage updates.<br>
 * <br>
 * This interface tries to normalize the data so that it can be used to receive messages from the Multiscreen SDK and
 * other SDKs that your application integrates with.<br>
 * <br>
 * Callbacks will not be on the UI thread. If the callback results in the UI being updated you need to run that change
 * ont he UI thread.
 * 
 * @author Dan McCafferty
 * 
 */
public interface MessageListener {

    /**
     * Called when a new message that the Message Listener registered for was received.<br>
     * <br>
     * NOTE: This callback will not be on the UI thread. If the callback results in the UI being updated you need to run
     * that change ont he UI thread.
     * 
     * @param event
     *            The application defined event name.
     * @param data
     *            The application defined data structure for the event. May be null.
     * @param payload
     *            The application defined byte array for the event. May be null.
     */
    public void onMessage(String event, String data, byte[] payload);
}
