package com.samsung.multiscreen.msf20.connectivity;

import android.content.Context;
import android.util.Log;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A connectivity manager used to to discover UPNP devices.
 *
 * Created by Ali Khan on 3/23/15.
 */
public class UPNPConnectivityManager implements RegistryListener {

    private static final String TAG = UPNPConnectivityManager.class.getSimpleName();

    protected static final Object lock = new Object();

    // Singleton instance
    private static UPNPConnectivityManager instance = null;

    // Number of milliseconds the discovery should run for.
    private static final long DISCOVERY_TIMEOUT_MILLIS = (30 * 1000); // 30 seconds

    // Local context reference
    private Context context;

    // The actual UPNP service
    private UpnpService upnpService;

    // The list of discovered devices
    private List<String> discoveredDevices;


    /**
     * Private constructor
     *
     * @param context The application context
     */
    private UPNPConnectivityManager(Context context) {
        this.context = context;
    }

    /**
     * Returns the UPNPConnectivityManager instance
     *
     * @param context
     * @return
     */
    public static UPNPConnectivityManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new UPNPConnectivityManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Starts the process to discover UPNP devices
     */
    public void startDiscovery() {
        Log.d(TAG, "Registering for UPNP discoveries");
        if (upnpService == null) {
            upnpService = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration());
        }

        discoveredDevices = new ArrayList<>();

        // Register to listen for devices discoveries
        upnpService.getRegistry().addListener(this);

        upnpService.getControlPoint().search(new STAllHeader()); // Search for all types of devices - ssdp:all
        initializeStopDiscoveryTimer(DISCOVERY_TIMEOUT_MILLIS); // Stop the discovery after a configured amount of time
    }

    /**
     * Initializes a task to stop listening to discoveries after a set amount of time
     *
     * @param discoverTimeoutMillis The amount of time/delay before the UPNP discovery
     *                              is terminated
     */
    private void initializeStopDiscoveryTimer(long discoverTimeoutMillis) {
        TimerTask stopDiscoveryTimerTask = new TimerTask() {

            @Override
            public void run() {
                Log.d(TAG, "Un-registering for UPNP discoveries");
                // Stop listening for upnp events
                upnpService.getRegistry().removeListener(UPNPConnectivityManager.this);
                Log.d(TAG, "Discovered: " + discoveredDevices);
            }

        };

        Timer stopDiscoveryTimer = new Timer();
        stopDiscoveryTimer.schedule(stopDiscoveryTimerTask, discoverTimeoutMillis);
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remoteDevice) {
        Log.v(TAG, "Remote Device Discovery Started");
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {
        Log.w(TAG, "Remote Device Discovery Failed", e);
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice remoteDevice) {
        Log.v(TAG, "Remote Device Added: " + remoteDevice.getDisplayString());
        discoveredDevices.add(remoteDevice.getDisplayString());
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
        // Ignore updated devices
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
        Log.v(TAG, "Remote Device Removed: " + remoteDevice.getDisplayString());
        discoveredDevices.remove(remoteDevice.getDisplayString());
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
        Log.v(TAG, "Local Device Added: " + localDevice.getDisplayString());
        discoveredDevices.add(localDevice.getDisplayString());
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
        Log.v(TAG, "Local Device Removed: " + localDevice.getDisplayString());
        discoveredDevices.remove(localDevice.getDisplayString());
    }

    @Override
    public void beforeShutdown(Registry registry) {

    }

    @Override
    public void afterShutdown() {

    }

    /**
     * Returns a list of UPNP discovered devices
     *
     * @return A list of UPNP discovered devices
     */
    public List<String> getDiscoveredDevices() {
        return discoveredDevices;
    }
}
