package com.samsung.multiscreen.msf20.game.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import android.os.CountDownTimer;
import android.util.Log;

import com.koushikdutta.async.BuildConfig;
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

/**
 * Encapsulates the logic to Discover, Connect, and Communicate to compatible
 * Samsung SmartTVs.
 * 
 * @author Dan McCafferty
 * 
 */
public class ConnectivityManager implements OnConnectListener, OnMessageListener, OnServiceFoundListener,
        OnServiceLostListener, Result<Client> {
    // Used to identify the source of a log message.
    private static final String TAG = ConnectivityManager.class.getSimpleName();

    // The URL that the TV application runs at
    // TODO: Pass in.
    private static final String url = "http://dev-multiscreen-examples.s3-website-us-west-1.amazonaws.com/examples/helloworld/tv/";
    
    // The Channel ID for the TV application
    // TODO: Pass in.
    private static final String channelId = "com.samsung.multiscreen.game";
    
    // The maximum time that service discovery can run. Set to 0 for no limit.
    // TODO: Pass in.
    private static final int MAX_SERVICE_DISCOVERY_TIME_MILLIS = (1000 * 60); // 1 minute
    
    // The URI that the TV application runs at
    private static final Uri uri = Uri.parse(url);

    // An singleton instance of this class
    private static ConnectivityManager instance = null;

    // Reference to the context.
    private final Context context;

    // The current Search object used during service discovery or null.
    private Search search = null;

    // The current Application object or null.
    private Application application = null;

    // The current Client object or null;
    private Client client = null;

    // A map of service name to Service object.
    private Map<String, Service> serviceMap = new HashMap<String, Service>();

    // A lock used to synchronize access to the service map
    private static final Object lock = new Object();

    /**
     * Constructor.
     * 
     * @param context
     */
    private ConnectivityManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns the instance.
     * 
     * @param context
     * @return
     */
    public static ConnectivityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConnectivityManager(context);
                }
            }
        }
        return instance;
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
        startDiscoveryTimer(MAX_SERVICE_DISCOVERY_TIME_MILLIS);
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

        // Stop the discovery process after some amount of time, preferably
        // once the user has selected a service to work with.
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
            Log.d(TAG, "Attempting to connect to application at '" + serviceName + "'. url=" + url + ", channelId="
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
     * Returns a flag indicating whether or not there is a connected
     * application.
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
        // Ignore since this application does not require client to client
        // communication
    }

    @Override
    public void onSuccess(Client client) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Application connect onSuccess() client: " + client.toString());
        }

        // The application is launched, and is ready to accept messages.
        this.client = client;
        // TODO: Implement
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
     * Send a message to the TV application.
     * 
     * @param event
     * @param messageData
     */
    public void sendMessage(String event, String messageData) {
        sendMessage(event, messageData, Message.TARGET_HOST);
    }

    /**
     * Send a message to the given target.
     * 
     * @param event
     * @param messageData
     * @param target
     *            The target of the message. Can be the TV application
     *            (Message.TARGET_HOST), to all connected clients EXCEPT self
     *            (Message.TARGET_BROADCAST), to all clients INCLUDING self
     *            (Message.TARGET_ALL).
     */
    public void sendMessage(String event, String messageData, String target) {
        if (!isConnected()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Cannot send message. Not connected. event=" + event + ", data=" + messageData);
            }
            return;
        }

        // Send a message to the target
        application.publish(event, messageData, target);
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
        //application.addOnMessageListener(event, this);
        //application.removeOnMessageListener(event, onMessageListener);        
    }
}
