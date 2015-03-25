package com.samsung.multiscreen.msf20.connectivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import com.samsung.multiscreen.msf20.casteroids.BuildConfig;

/**
 * Encapsulates the logic to Discover, Connect, and Communicate to compatible Samsung SmartTVs.<br>
 * <br>
 * This class does not contain any application specific logic nor does it make assumptions about the application using
 * it.<br>
 * <br>
 * The class was architected in way where the code using this class does not need to know anything about the underlying
 * protocols or SDK being used.<br>
 * <br>
 * The scope of this class is tied to the scope of this example application. Sicne it only needs to send messages
 * directly to the TV Application and the messages are limited to String data, this class does not include logic for
 * sending messages directly to other clients nor logic for sending byte[] payloads with the message. This functionality
 * can easily be added to this class. See the notes in the class for more details.
 * 
 * @author Dan McCafferty
 * 
 */
public class ConnectivityManager implements OnConnectListener, OnDisconnectListener, OnMessageListener,
        OnServiceFoundListener, OnServiceLostListener, Result<Client> {

	// Used to identify the source of a log message.
	protected final String TAG;

	// An singleton instance of this class
	private static ConnectivityManager instance = null;

	// The default time that service discovery can run. Set to 0 for no limit.
	protected static final long DEFAULT_DISCOVERY_TIMEOUT_MILLIS = (1000 * 60); // 1 minute

	// The connection timeout. This is needed to be notified when connection to the TV App is lost because the TV lost
	// its WiFi connection or was turned off.
	private static final int CONNECTION_TIMEOUT_MILLIS = (1000 * 5); // 5 seconds

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

	//
	private String wifiNetworkName = null;

	// A flag that indicates whether or not the device is currently connected to a WiFi network.
	private boolean isConnectedToWifi = false;

	// A Broadcast receiver used to monitor WiFi connectivity changes.
	private WifiBroadcastReceiver wifiReceiver = null;

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

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Creating " + TAG + ".");
		}

		this.context = context.getApplicationContext();

		this.url = url;
		this.uri = Uri.parse(url);
		this.channelId = channelId;
		this.discoveryTimeoutMillis = discoveryTimeoutMillis;

		// Start monitoring WiFi connectivity changes.
		startMonitoringWifiConnectivity();
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
			// Stop monitoring WiFi connectivity changes.
			stopMonitoringWifiConnectivity();

			// Stop any discovery actions
			stopDiscovery();

			// Disconnect from any applications
			disconnect();

			// Clear the service map to release Service objects
			serviceMap.clear();

			// Clear the connectivity listener list and message listener map and release listener objects
			connectivityListenerList.clear();
			messageListenerMap.clear();
		}
	}

	/******************************************************************************************************************
	 * Discover related methods
	 */

	/**
	 * Start service discovery.
	 */
	public boolean startDiscovery() {
		synchronized (lock) {
			// If already searching, return true (we are discovering).
			if (isDiscovering()) {
				return true;
			}

			// If are not connected to a WiFi network, return false (we are not discovering).
			if (!isConnectedToWifi()) {
				return false;
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
		}

		// Start the discovery timer to limit the discovery time.
		startDiscoveryTimer(discoveryTimeoutMillis);

		// Notify listeners that discovery started.
		notifyConnectivityListeners(ConnectivityListener.DISCOVERY_STARTED);

		// Return true (we are discovering).
		return true;
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
			// Stop the discovery process after some amount of time, preferably once the user has selected a service to
			// work with.
			if (search != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Stopping discovery.");
				}
				search.stop();
				search = null;
			}
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
		// Millis set to 0 or less is used as an indication not to limit discovery.
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
	 * Connect related methods.<br>
	 * <br>
	 * This example application connects to the TV application and listens for connect and disconnect events. Since this
	 * example application does not need to communicate directly with other clients it does not store Client objects for
	 * the other clients that are connected to the same TV Application instance.
	 */

	/**
	 * Connects to an application associated to the given service name.
	 * 
	 * @param serviceName
	 *            The name of the service to connect to.
	 * @return Returns true if attempted to connect to an application otherwise false.
     *
     * @see #getDiscoveredServiceNames()
	 */
	public boolean connect(String serviceName) {
		synchronized (lock) {
			// If are not connected to a WiFi network, return false (we are not connecting).
			if (!isConnectedToWifi()) {
				return false;
			}

			// If we just initiated a disconnected from an application, we cannot attempt to connect to it until we get
			// the onDisconnect callback.
			if (disconnect()) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Cannot attempt connect. Waiting for disconnect attempt to complete.");
				}
				return false;
			}

			// Stop discovering services.
			stopDiscovery();

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

			// Set the connection timeout. This needs to be set in order to be notified when connection to the TV App is
			// lost because the TV lost its WiFi connection or was turned off.
			application.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);

			// Listen for the connect/disconnect events
			application.setOnConnectListener(this);
			application.setOnDisconnectListener(this);

			// NOTE: There are other listeners that we could register for but are not needed for this application
			// application.setOnClientConnectListener(this);
			// application.setOnClientDisconnectListener(this);
			// application.setOnErrorListener(this);

			// Add message listeners for all registered events
			for (String event : messageListenerMap.keySet()) {
				if (BuildConfig.DEBUG) {
					Log.v(TAG, "Adding message listener for '" + event + "'");
				}
				application.addOnMessageListener(event, this);
			}

			// Connect and launch the application.
			application.connect(this);
		}

		return true;
	}

	/**
	 * Connects to an application associated to the given service.
	 * 
	 * @param service
	 *            The Service object to connect to.
	 * @return Returns true if attempted to connect to an application otherwise false.
     *
     * @see #getDiscoveredServices()
	 */
	protected boolean connect(Service service) {
		// By connecting by name we will make sure the service is still available before attempting to connect.
		return connect(service.getName());
	}

	/**
	 * Disconnects from the current application.
	 * 
	 * @return Returns true if attempted to disconnect from an application otherwise false.
	 */
	public boolean disconnect() {
		// If we are not connected, return.
		if (!isConnected()) {
			return false;
		}

		synchronized (lock) {
			// Initiate the disconnect from the application. The disconnect will be confirmed by the onDisconnect()
			// callback.
			if (application != null) {
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Attempting to disconnect from application '" + application.getId() + "'");
				}
				application.disconnect();
				application = null;
			}
		}

		return true;
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
			Log.d(TAG, "Application.onConnect() client: " + getAsString(client));
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
			Log.d(TAG, "Application.onDisconnect() client: " + getAsString(client));
		}

		synchronized (lock) {
			// Reset the application object
			application = null;

			// Reset other data associated to the application connection.
			client = null;
		}

		// Notify listeners that we are no longer connected.
		notifyConnectivityListeners(ConnectivityListener.APPLICATION_DISCONNECTED);
	}

	@Override
	public void onSuccess(Client client) {
		// The application is launched, and is ready to accept messages. :)
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Application connect onSuccess() client: " + getAsString(client));
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

	/**
	 * Returns the toString() value of the given Client object or "null" if null.
	 * 
	 * @param client
	 * @return
	 */
	private String getAsString(Client client) {
		return (client != null) ? client.toString() : "null";
	}

	/******************************************************************************************************************
	 * Communicate related methods.<br>
	 * <br>
	 * This example application sends messages to the TV Application with String data. This example does not include
	 * logic for sending messages directly to other clients nor logic for sending byte[] payloads with the message.
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
				try {
					// Extract data
					String event = message.getEvent();
					String data = getMessageDataAsString(message);
					byte[] payload = message.getPayload();

					// Send to the listeners
					for (MessageListener listener : listeners) {
						try {
							if (BuildConfig.DEBUG) {
								String simpleName = listener.getClass().getSimpleName();
								Log.d(TAG, "Sending " + simpleName + " a '" + event + "' message update. data=" + data);
							}
							listener.onMessage(event, data, payload);
						} catch (Exception e) {
							String simpleName = listener.getClass().getSimpleName();
							Log.e(TAG, "Failed to send " + simpleName + " a '" + event + "' message update. data="
							        + data, e);
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Failed to process message. message=" + message, e);
				}
			}
		}
	}

	/**
	 * Normalizes the message data to a String. If the message data is a String it is casted to a String and returned.
	 * If the message data is not a String, the toString() value of the Object is returned. If the message data is NULL,
	 * then NULL is returned.
	 * 
	 * @param message
	 * @return
	 */
	private String getMessageDataAsString(Message message) {
		String dataStr = null;

		if (message.getData() != null) {
			if (message.getData() instanceof String) {
				dataStr = (String) message.getData();
			} else {
				dataStr = message.getData().toString();
			}
		}

		return dataStr;
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
	 * WiFi Connectivity related methods
	 */

	/**
	 * Return's the current WiFi networks name or NULL if not connected to a WiFi network.
	 * 
	 * @return
	 * @see #isConnectedToWifi()
	 */
	public String getWifiNetworkName() {
		return wifiNetworkName;
	}

	/**
	 * Returns whether or not the device is currently connected to a WiFi network.
	 * 
	 * @return
	 */
	public boolean isConnectedToWifi() {
		return isConnectedToWifi;
	}

	/**
	 * Starts monitoring WiFi connectivity changes.
	 */
	private void startMonitoringWifiConnectivity() {
		if (wifiReceiver != null) {
			return;
		}

		// Determine the current WiFi connectivity state.
		android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		processNetworkInfo(cm.getActiveNetworkInfo());

		// Monitor for WiFi connectivity changes.
		wifiReceiver = new WifiBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		context.registerReceiver(wifiReceiver, intentFilter);
	}

	/**
	 * Stops monitoring WiFi connectivity changes.
	 */
	private void stopMonitoringWifiConnectivity() {
		if (wifiReceiver == null) {
			return;
		}

		context.unregisterReceiver(wifiReceiver);
		wifiReceiver = null;
	}

	/**
	 * Receives and processes WiFi connectivity change broadcasts.
	 */
	private class WifiBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Received intent: " + action);
			}

			try {
				// If the network state changed, process the network info.
				if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					NetworkInfo ni = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					processNetworkInfo(ni);
				}
			}
			// Catch and log all exceptions
			catch (Exception e) {
				Log.e(TAG, "Caught exception while processing a '" + action + "' intent. intent=" + intent, e);
			}
		}
	}

	/**
	 * Determines whether or not the WiFi connectivity state changed and if so
	 * 
	 * @param ni
	 */
	private void processNetworkInfo(NetworkInfo ni) {
		// Determine whether or not we are connected to a WiFi Network
		boolean isConnectedToWifiUpdate = ((ni != null) && ni.isConnected() && (ni.getType() == android.net.ConnectivityManager.TYPE_WIFI));
		wifiNetworkName = isConnectedToWifiUpdate ? wifiNetworkName : null;

		// If the connectivity changed...
		if (isConnectedToWifi != isConnectedToWifiUpdate) {

			// Update the current WiFi network information
			wifiNetworkName = isConnectedToWifiUpdate ? ni.getExtraInfo() : null;
			isConnectedToWifi = isConnectedToWifiUpdate;

			// If we connected to a WiFi network, notify listeners.
			if (isConnectedToWifi) {
				// Notify listeners on the WiFi connectivity change.
				notifyConnectivityListeners(ConnectivityListener.WIFI_CONNECTED);
			}
			// Else we are not connected to a WiFi network stop discovery, disconnect from any applications, and notify
			// listeners.
			else {
				// Stop any discovery actions
				stopDiscovery();

				// Disconnect from any applications
				disconnect();

				// Notify listeners on the WiFi connectivity change.
				notifyConnectivityListeners(ConnectivityListener.WIFI_DISCONNECTED);
			}
		}
		
		// Log out the WiFi state
		if (BuildConfig.DEBUG) {
			Log.v(TAG, "wifiNetworkName=" + wifiNetworkName + ", isConnectedToWifi=" + isConnectedToWifi);
		}		
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
				if (BuildConfig.DEBUG) {
					Log.v(TAG, "Adding message listener for '" + event + "'");
				}
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
				if (BuildConfig.DEBUG) {
					Log.v(TAG, "Removing message listener from '" + event + "'");
				}
				application.removeOnMessageListener(event, this);
			}
		}
	}
}
