package com.samsung.multiscreen.msf20.game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import com.samsung.multiscreen.Application;
import com.samsung.multiscreen.Channel.OnConnectListener;
import com.samsung.multiscreen.Channel.OnDisconnectListener;
import com.samsung.multiscreen.Channel.OnMessageListener;
import com.samsung.multiscreen.Client;
import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Message;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Search;
import com.samsung.multiscreen.Search.OnServiceFoundListener;
import com.samsung.multiscreen.Search.OnServiceLostListener;
import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.msf20.game.BuildConfig;

/**
 * Encapsulates the logic to Discover, Connect, and Communicate to compatible Samsung SmartTVs.<br>
 * <br>
 * This class does not contain any application specific logic.
 * 
 * @author Dan McCafferty
 * 
 */
public class ConnectivityManager implements OnConnectListener, OnDisconnectListener, OnMessageListener,
        OnServiceFoundListener, OnServiceLostListener, Result<Client> {

    // Used to identify the source of a log message.
    protected final String TAG;

    // The default time that service discovery can run. Set to 0 for no limit.
    protected static final long DEFAULT_DISCOVERY_TIMEOUT_MILLIS = (1000 * 60); // 1 minute

    // Reference to the context.
    protected final Context context;

    // The URL where the TV application lives
    private String url;

    // The URI where the TV application lives
    private Uri uri;

    // The Channel ID for the TV application
    private String channelId;

    // The maximum time that service discovery can run. Set to 0 for no limit.
    private final long discoveryTimeoutMillis;

    // An singleton instance of this class
    private static ConnectivityManager instance = null;

    // The current Search object used during service discovery or null.
    private Search search = null;

    // The current Application object or null.
    private Application application = null;

    // This clients current Client object or null;
    protected Client client = null;

    // A map of service name to Service object.
    private Map<String, Service> serviceMap = new HashMap<String, Service>();

    // A lock used to synchronize creation of this object and access to the service map
    protected static final Object lock = new Object();

    // A map that stores the registered connectivity listeners
    private List<ConnectivityListener> connectivityListenerList = new ArrayList<ConnectivityListener>();

    // A map that stores the registered message listeners
    private Map<String, List<MessageListener>> messageListenerMap = new HashMap<String, List<MessageListener>>();

    /**
     * Constructor.
     * 
     * @param context
     *            The Android application context.
     * @param url
     *            The URL where the TV application lives.
     * @param channelId
     *            The Channel ID for the TV application.
     * @param discoveryTimeoutMillis
     *            The time that service discovery can run. Set to 0 for no limit.
     */
    protected ConnectivityManager(Context context, String url, String channelId, long discoveryTimeoutMillis) {
        this.TAG = this.getClass().getSimpleName();

        this.context = context.getApplicationContext();

        this.url = url;
        this.uri = Uri.parse(url);
        this.channelId = channelId;
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

    /**
     * Returns the instance with the default discovery timeout.
     * 
     * @param context
     *            The Android application context.
     * @param url
     *            The URL where the TV application lives.
     * @param channelId
     *            The Channel ID for the TV application.
     * @return
     */
    public static ConnectivityManager getInstance(Context context, String url, String channelId) {
        return getInstance(context, url, channelId, DEFAULT_DISCOVERY_TIMEOUT_MILLIS);
    }

    /**
     * Returns the instance.
     * 
     * @param context
     *            The Android application context.
     * @param url
     *            The URL where the TV application lives.
     * @param channelId
     *            The Channel ID for the TV application.
     * @param discoveryTimeoutMillis
     *            The time that service discovery can run. Set to 0 for no limit.
     * @return
     */
    public static ConnectivityManager getInstance(Context context, String url, String channelId,
            long discoveryTimeoutMillis) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConnectivityManager(context, url, channelId, discoveryTimeoutMillis);
                }
            }
        }
        return instance;
    }

    /**
     * Call this method to notify this object that it is no longer used and is being removed. The object will clean up
     * any resources it holds (threads, registered receivers, etc) at this point. Upon return, there should be no more
     * calls in to this module object and it is effectively dead.
     */
    public void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Destroying ConnectivityManager.");
        }

        synchronized (lock) {
            // Stop any discovery actions
            stopDiscovery();

            // Disconnect from any applications
            disconnect();

            // Clear the service map to release Service objects
            serviceMap.clear();
        }
    }

    /******************************************************************************************************************
     * Discover related methods
     */

    /**
     * Start service discovery.
     */
    public void startDiscovery() {
        // If already searching, return.
        if (isDiscovering()) {
            return;
        }

        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Starting discovery.");
            }

            // Clear previous service mappings
            serviceMap.clear();

            // Get an instance of Search
            search = Service.search(context);

            // Add a listener for the service found event
            search.setOnServiceFoundListener(this);

            // Add a listener for the service lost event
            search.setOnServiceLostListener(this);

            // Start the discovery process
            search.start();
        }

        // Start the discovery timer to limit the discovery time.
        startDiscoveryTimer(discoveryTimeoutMillis);

        // Notify listeners that discovery started.
        notifyConnectivityListeners(ConnectivityListener.DISCOVERY_STARTED);
    }

    /**
     * Stop service discovery.
     */
    public void stopDiscovery() {
        // If not searching, return.
        if (!isDiscovering()) {
            return;
        }

        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Stopping discovery.");
            }

            // Stop the discovery process after some amount of time, preferably once the user has selected a service to
            // work with.
            search.stop();
            search = null;
        }

        // Notify listeners that discovery stopped.
        notifyConnectivityListeners(ConnectivityListener.DISCOVERY_STOPPED);
    }

    /**
     * Returns a flag indicating whether or not service discovery is active.
     * 
     * @return
     */
    public boolean isDiscovering() {
        return ((search != null) && search.isSearching());
    }

    /**
     * Returns a flag indicating whether or not at least one service has been discovered.
     * 
     * @return
     */
    public boolean hasDiscoveredService() {
        return (serviceMap.size() > 0);
    }
    
    /**
     * Returns a String array of discovered service names.
     * 
     * @return
     */
    public String[] getDiscoveredServiceNames() {
        Set<String> servicesNameSet = serviceMap.keySet();
        return servicesNameSet.toArray(new String[servicesNameSet.size()]);
    }

    /**
     * Returns a List of discovered Service objects.
     * 
     * @return
     */
    public List<Service> getDiscoveredServices() {
        return new ArrayList<Service>(serviceMap.values());
    }

    @Override
    public void onFound(Service service) {
        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Search.onFound() service: " + service.toString());
            }

            // Add service to a service map.
            serviceMap.put(service.getName(), service);
        }

        // Notify listeners of the found service.
        notifyConnectivityListeners(ConnectivityListener.DISCOVERY_FOUND_SERVICE);
    }

    @Override
    public void onLost(Service service) {
        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Search.onLost() service: " + service.toString());
            }

            // Remove this service from the service map.
            serviceMap.remove(service.getName());
        }

        // Notify listeners of the lost service.
        notifyConnectivityListeners(ConnectivityListener.DISCOVERY_LOST_SERVICE);
    }

    /**
     * Calls stopDiscovery() in the given number of millis.
     * 
     * @param millis
     */
    private void startDiscoveryTimer(long millis) {
        // Millis set to 0 or less is used as an indication not to limit
        // discovery.
        if (millis <= 0) {
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting discovery Timer. millis=" + millis);
        }

        new CountDownTimer(millis, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Discovery timer finished. Calling stopDiscovery().");
                }
                cancel();
                stopDiscovery();
            }
        }.start();
    }

    /******************************************************************************************************************
     * Connect related methods
     */

    /**
     * Connects to an application associated to the given service name.
     * 
     * @param serviceName
     *            The name of the service to connect to.
     * 
     * @see getDiscoveredServiceNames
     */
    public void connect(String serviceName) {
        synchronized (lock) {
            // Stop discovering services.
            stopDiscovery();

            // Disconnect from any other applications.
            disconnect();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Attempting to connect to application at '" + serviceName + "'. url=" + uri + ", channelId="
                        + channelId);
            }

            // Get the Service object from the service map
            Service service = serviceMap.get(serviceName);

            if (service == null) {
                onError(null);
            }

            // Get an instance of Application.
            application = service.createApplication(uri, channelId);

            // Listen for the connect/disconnect events
            application.setOnConnectListener(this);
            application.setOnDisconnectListener(this);

            // NOTE: There are other listeners that we could register for but are not needed for this application
            // application.setOnClientConnectListener(this);
            // application.setOnClientDisconnectListener(this);
            // application.setOnErrorListener(this);

            // Add message listeners for all registered events
            for (String event : messageListenerMap.keySet()) {
                application.addOnMessageListener(event, this);
            }

            // Connect and launch the application.
            application.connect(this);
        }
    }

    /**
     * Connects to an application associated to the given service.
     * 
     * @param service
     *            The Service object to connect to.
     * 
     * @see getDiscoveredServices
     */
    public void connect(Service service) {
        // By connecting by name we will make sure the service is still
        // available before attempting to connect.
        connect(service.getName());
    }

    /**
     * Disconnects from the current application.
     */
    public void disconnect() {
        // If we are not connected, return.
        if (!isConnected()) {
            return;
        }

        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Attempting to disconnect from application '" + application.getId() + "'");
            }

            // Disconnect from the application
            application.disconnect();
            application = null;

            // Clear other data associated to the application
            client = null;
        }
    }

    /**
     * Returns a flag indicating whether or not there is a connected application.
     * 
     * @return
     */
    public boolean isConnected() {
        return ((application != null) && application.isConnected());
    }

    @Override
    public void onConnect(Client client) {
        // We are connected! :)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application.onConnect() client: " + client.toString());
        }

        synchronized (lock) {
            // Store off our client just in case we need it.
            this.client = client;
        }

        // Notify listeners that we are connected.
        notifyConnectivityListeners(ConnectivityListener.APPLICATION_CONNECTED);
    }

    @Override
    public void onDisconnect(Client client) {
        // We are disconnected! :(
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application.onDisconnect() client: " + client.toString());
        }

        synchronized (lock) {
            // Null out the application object
            application = null;

            // Clear other data associated to the application
            client = null;
        }
        
        // Notify listeners that we are no longer connected.
        notifyConnectivityListeners(ConnectivityListener.APPLICATION_DISCONNECTED);
    }

    @Override
    public void onSuccess(Client client) {
        // The application is launched, and is ready to accept messages. :)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application connect onSuccess() client: " + client.toString());
        }
    }

    @Override
    public void onError(Error error) {
        // Ohhhh snap! An error!
        if (BuildConfig.DEBUG) {
            String errorMsg = (error != null) ? error.toString() : "The service is not available.";
            Log.w(TAG, "Application connect onError() error: " + errorMsg);
        }

        // Notify listeners of the error.
        notifyConnectivityListeners(ConnectivityListener.APPLICATION_CONNECT_FAILED);
    }

    /******************************************************************************************************************
     * Communicate related methods
     */

    /**
     * Sends a message to the TV application.
     * 
     * @param event
     *            The application defined event name.
     * @param data
     *            The application defined data structure for the event.
     */
    public void sendMessage(String event, String data) {
        sendMessage(event, data, Message.TARGET_HOST);
    }

    /**
     * Sends a message to the given target.
     * 
     * @param event
     *            The application defined event name.
     * @param data
     *            The application defined data structure for the event.
     * @param target
     *            The target of the message. Can be the TV application (Message.TARGET_HOST), to all connected clients
     *            EXCEPT self (Message.TARGET_BROADCAST), to all clients INCLUDING self (Message.TARGET_ALL).
     */
    public void sendMessage(String event, String data, String target) {
        if (!isConnected()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Cannot send message. Not connected. event=" + event + ", data=" + data + ", target="
                        + target);
            }
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Sending message. event=" + event + ", data=" + data + ", target=" + target);
        }

        // Send a message to the target
        application.publish(event, data, target);
    }

    /**
     * Receives a message.
     * 
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application.onMessage() message: " + message.toString());
        }

        // Notify the registered listeners.
        synchronized (lock) {
            List<MessageListener> listeners = messageListenerMap.get(message.getEvent());
            if (listeners != null) {
                // Extract data
                String event = message.getEvent();
                String data = (String) message.getData();
                byte[] payload = message.getPayload();

                // Send to the listeners
                for (MessageListener listener : listeners) {
                    try {
                        listener.onMessage(event, data, payload);
                    } catch (Exception e) {
                        String simpleName = listener.getClass().getSimpleName();
                        Log.e(TAG, "Failed to send " + simpleName + " a '" + event + "' message updates. data=" + data,
                                e);
                    }
                }
            }
        }
    }

    /******************************************************************************************************************
     * Getter and Setter methods
     */

    /**
     * Returns the URL where the TV application lives.
     * 
     * @return
     */
    public String getApplicationUrl() {
        return this.url;
    }

    /**
     * Sets the URL where the TV application lives.<br>
     * <br>
     * As a part of this method, a connected application will be disconnected.
     * 
     * @param url
     */
    public void setApplicationUrl(String url) {
        disconnect();

        this.url = url;
        this.uri = Uri.parse(url);
    }

    /**
     * Returns the Channel ID for the TV application.
     * 
     * @return
     */
    public String getApplicationChannelId() {
        return channelId;
    }

    /**
     * Sets the Channel ID for the TV application.<br>
     * <br>
     * As a part of this method, a connected application will be disconnected.
     * 
     * @param channelId
     */
    public void setApplicationChanneId(String channelId) {
        disconnect();
        this.channelId = channelId;
    }

    /******************************************************************************************************************
     * Listener Registration related methods
     */

    /**
     * Registers the given listener for connectivity updates.
     * 
     * @param listener
     */
    public void registerConnectivityListener(ConnectivityListener listener) {
        if (listener == null) {
            return;
        }

        synchronized (lock) {
            // Add the listener to the list if its not already in there.
            if (!connectivityListenerList.contains(listener)) {
                connectivityListenerList.add(listener);

                if (BuildConfig.DEBUG) {
                    String simpleName = listener.getClass().getSimpleName();
                    int count = connectivityListenerList.size();
                    Log.v(TAG, "Registering " + simpleName + " for connectivity updates. count=" + count);
                }
            }
        }
    }

    /**
     * Unregisters the given listener from connectivity updates.
     * 
     * @param listener
     */
    public void unregisterConnectivityListener(ConnectivityListener listener) {
        if (listener == null) {
            return;
        }

        synchronized (lock) {
            // If the list is not null, then attempt to remove the listener.
            boolean removed = connectivityListenerList.remove(listener);

            if (BuildConfig.DEBUG) {
                String msg = removed ? "Unregistering " : "Could not unregister ";
                String simpleName = listener.getClass().getSimpleName();
                int count = connectivityListenerList.size();
                Log.v(TAG, msg + simpleName + " from connectivity updates. count=" + count);
            }
        }
    }

    /**
     * Sends a connectivity update to all registered listeners.
     * 
     * @param eventId
     */
    private void notifyConnectivityListeners(int eventId) {
        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                int count = connectivityListenerList.size();
                Log.d(TAG, "Sending a connectivity update. eventId=" + eventId + ", count=" + count);
            }
            for (ConnectivityListener listener : connectivityListenerList) {
                try {
                    if (BuildConfig.DEBUG) {
                        String simpleName = listener.getClass().getSimpleName();
                        Log.d(TAG, "Sending " + simpleName + " a connectivity update. eventId=" + eventId);
                    }
                    listener.onConnectivityUpdate(eventId);
                } catch (Exception e) {
                    String simpleName = listener.getClass().getSimpleName();
                    Log.e(TAG, "Failed to send " + simpleName + " a connectivity update. eventId=" + eventId, e);
                }
            }
        }
    }

    /**
     * Registers the given listener for message updates.
     * 
     * @param listener
     * @param events
     */
    public void registerMessageListener(MessageListener listener, String... events) {
        if ((listener == null) || (events == null)) {
            return;
        }

        for (String event : events) {
            registerMessageListener(listener, event);
        }
    }

    /**
     * Registers the given listener for message updates.
     * 
     * @param listener
     * @param event
     */
    public void registerMessageListener(MessageListener listener, String event) {
        if ((listener == null) || (event == null)) {
            return;
        }

        synchronized (lock) {
            // Get the list of registered listeners.
            List<MessageListener> listeners = messageListenerMap.get(event);

            // If the list is null, then create it.
            if (listeners == null) {
                listeners = new ArrayList<MessageListener>();
                messageListenerMap.put(event, listeners);
            }

            // Store the original size of the list
            int originalSize = listeners.size();

            // Add the listener to the list if its not already in there.
            if (!listeners.contains(listener)) {
                listeners.add(listener);

                if (BuildConfig.DEBUG) {
                    String simpleName = listener.getClass().getSimpleName();
                    int count = listeners.size();
                    Log.v(TAG, "Registering " + simpleName + " for '" + event + "' changed updates. count=" + count);
                }
            }

            // If this is the first listener registering for this event, register with the application for the event.
            if ((originalSize != 1) && (listeners.size() == 1) && isConnected()) {
                application.addOnMessageListener(event, this);
            }
        }
    }

    /**
     * Unregisters the given listener for message updates.
     * 
     * @param listener
     * @param events
     */
    public void unregisterMessageListener(MessageListener listener, String... events) {
        if ((listener == null) || (events == null)) {
            return;
        }

        for (String event : events) {
            unregisterMessageListener(listener, event);
        }
    }

    /**
     * Unregisters the given listener for message updates.
     * 
     * @param listener
     * @param event
     */
    public void unregisterMessageListener(MessageListener listener, String event) {
        if ((listener == null) || (event == null)) {
            return;
        }

        synchronized (lock) {
            // Get the list of registered listeners.
            List<MessageListener> listeners = messageListenerMap.get(event);

            // Store the original size of the list map
            int originalSize = listeners.size();

            // If the list is not null, then attempt to remove the listener.
            if (listeners != null) {
                boolean removed = listeners.remove(listener);

                if (BuildConfig.DEBUG) {
                    String msg = removed ? "Unregistering " : "Could not unregister ";
                    String simpleName = listener.getClass().getSimpleName();
                    int count = listeners.size();
                    Log.v(TAG, msg + simpleName + " from '" + event + "' changed updates. count=" + count);
                }

                // If the list size is zero remove it.
                if (listeners.size() == 0) {
                    messageListenerMap.remove(event);
                }
            }

            // If the size of the list map just changed to 0, then unregister with the application for the event.
            if ((originalSize != 0) && (listeners.size() == 0) && isConnected()) {
                application.removeOnMessageListener(event, this);
            }
        }
    }
}
