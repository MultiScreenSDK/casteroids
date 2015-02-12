package com.samsung.multiscreen.msf20.game.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import com.samsung.multiscreen.Application;
import com.samsung.multiscreen.Channel.OnConnectListener;
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
 * Encapsulates the logic to Discover, Connect, and Communicate to compatible Samsung SmartTVs. This class was does not
 * contain any app specific logic.
 * 
 * @author Dan McCafferty
 * 
 */
public class ConnectivityManager implements OnConnectListener, OnMessageListener, OnServiceFoundListener,
        OnServiceLostListener, Result<Client> {
    // Used to identify the source of a log message.
    protected final String TAG;

    // The default time that service discovery can run. Set to 0 for no limit.
    protected static final long DEFAULT_DISCOVERY_TIMEOUT_MILLIS = (1000 * 60); // 1 minute

    // Reference to the context.
    protected final Context context;

    // The URI that the TV application runs at
    // TODO: Allow this to be set and changed
    private final Uri uri;

    // The Channel ID for the TV application
    private final String channelId;

    // The maximum time that service discovery can run. Set to 0 for no limit.
    private final long discoveryTimeoutMillis;

    // An singleton instance of this class
    private static ConnectivityManager instance = null;

    // The current Search object used during service discovery or null.
    private Search search = null;

    // The current Application object or null.
    private Application application = null;

    // The current Client object or null;
    private Client client = null;

    // A map of service name to Service object.
    private Map<String, Service> serviceMap = new HashMap<String, Service>();

    // A lock used to synchronize creation of this object and access to the service map
    protected static final Object lock = new Object();

    /**
     * Constructor.
     * 
     * @param context
     * @param url
     * @param channelId
     * @param discoveryTimeoutMillis
     */
    protected ConnectivityManager(Context context, String url, String channelId, long discoveryTimeoutMillis) {
        this.TAG = this.getClass().getSimpleName();

        this.context = context.getApplicationContext();

        this.uri = Uri.parse(url);
        this.channelId = channelId;
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

    /**
     * Returns the instance with the default discovery timeout.
     * 
     * @param context
     * @param url
     * @param channelId
     * @return
     */
    public static ConnectivityManager getInstance(Context context, String url, String channelId) {
        return getInstance(context, url, channelId, DEFAULT_DISCOVERY_TIMEOUT_MILLIS);
    }

    /**
     * Returns the instance.
     * 
     * @param context
     * @param url
     * @param channelId
     * @param discoveryTimeoutMillis
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

        // Stop any discovery actions
        stopDiscovery();

        // Disconnect from any applications
        disconnect();

        // Clear the service map to release Service objects
        serviceMap.clear();
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

        // Start the discovery timer to limit the discovery time.
        startDiscoveryTimer(discoveryTimeoutMillis);
    }

    /**
     * Stop service discovery.
     */
    public void stopDiscovery() {
        // If not searching, return.
        if (!isDiscovering()) {
            return;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Stopping discovery.");
        }

        // Stop the discovery process after some amount of time, preferably once the user has selected a service to work
        // with.
        search.stop();
        search = null;
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
     * Returns the set of discovered service names.
     * 
     * @return
     */
    public Set<String> getDiscoveredServiceNames() {
        return serviceMap.keySet();
    }

    /**
     * Returns the set of discovered Service objects.
     * 
     * @return
     */
    public Collection<Service> getDiscoveredServices() {
        return serviceMap.values();
    }

    @Override
    public void onFound(Service service) {
        synchronized (lock) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Search.onFound() service: " + service.toString());
            }

            // Add service to a service map.
            serviceMap.put(service.getName(), service);

            // FIXME: Remove
            connect(service);
        }
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
     */
    public void connect(String serviceName) {
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

        // Listen for the connect event
        application.setOnConnectListener(this);

        // TODO: Determine which other listeners we need
        // application.setOnConnectListener(this);
        // application.setOnDisconnectListener(this);
        // application.setOnClientConnectListener(this);
        // application.setOnClientDisconnectListener(this);
        // application.setOnErrorListener(this);

        // Connect and launch the application.
        application.connect(this);
    }

    /**
     * Connects to an application associated to the given service.
     * 
     * @param service
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

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Attempting to disconnect from application '" + application.getId() + "'");
        }

        // Disconnect from the application
        application.disconnect();
        application = null;

        // Clear other data associated to the application
        client = null;
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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application.onConnect() client: " + client.toString());
        }
        // Ignore since this application does not require client to client communication
    }

    @Override
    public void onSuccess(Client client) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application connect onSuccess() client: " + client.toString());
        }

        // The application is launched, and is ready to accept messages.
        this.client = client;

        // TODO: Implement

        // FIXME: Remove
        this.sendMessage(Event.ROTATE.getName(), Rotate.LEFT.getName());
    }

    @Override
    public void onError(Error error) {
        if (BuildConfig.DEBUG) {
            String errorMsg = (error != null) ? error.toString() : "The service is not available.";
            Log.w(TAG, "Application connect onError() error: " + errorMsg);
        }

        // Uh oh. Handle the error.
        // TODO: Implement
    }

    /******************************************************************************************************************
     * Communicate related methods
     */

    /**
     * Sends a message to the TV application.
     * 
     * @param event
     * @param data
     */
    public void sendMessage(String event, String data) {
        sendMessage(event, data, Message.TARGET_HOST);
    }

    /**
     * Sends a message to the given target.
     * 
     * @param event
     * @param data
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
            Log.w(TAG, "Sending message. event=" + event + ", data=" + data + ", target=" + target);
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
        // Process the message.
        // TODO: Implement.

        // TODO: add/remove listener based on when components register/unregister for event update
        // application.addOnMessageListener(event, this);
        // application.removeOnMessageListener(event, onMessageListener);
    }
}
