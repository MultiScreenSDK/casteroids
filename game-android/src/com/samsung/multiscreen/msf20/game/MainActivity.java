package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.game.model.GameConnectivityManager;

/**
 * Entry point into the Game.
 *
 * @author Nik Bhattacharya
 *
 */
public class MainActivity extends Activity implements ConnectivityListener {

    private GameConnectivityManager connectivityManager = null;

    private Button playButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the play button
        playButton = (Button) findViewById(R.id.play_button);
        playButton.setEnabled(false);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGame();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get an instance of the ConnectivtyManager and register for connectivity updates.
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        connectivityManager.registerConnectivityListener(this);

        // Enable the play button if we are currently connected
        playButton.setEnabled(connectivityManager.isConnected());

        // Make sure the connectivity manager is in the correct state.
        if (connectivityManager.hasDiscoveredService()) {
            String[] services = connectivityManager.getDiscoveredServiceNames();
            if ((services != null) && (services.length > 0)) {
                connectivityManager.connect(services[0]);
            }
        } else if (!connectivityManager.isDiscovering()) {
            connectivityManager.startDiscovery();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);
    }

    private void playGame() {
        Intent gameIntent = new Intent();
        gameIntent.setClass(this, GameActivity.class);
        startActivity(gameIntent);
    }

    @Override
    public void onConnectivityUpdate(int eventId) {
        switch (eventId) {
            case DISCOVERY_STOPPED:
                // Restart discovery as long as we don't have a discovered service.
                // TODO: Allow the user to refresh
                if (!connectivityManager.hasDiscoveredService()) {
                    connectivityManager.startDiscovery();
                }
                break;
            case DISCOVERY_FOUND_SERVICE:
                // Connect to the first service we find.
                // TODO: Display the available services to the user and allow user to select the service to connect to.
                Toast.makeText(this, "Attempting connection.", Toast.LENGTH_SHORT).show();
                String[] services = connectivityManager.getDiscoveredServiceNames();
                if ((services != null) && (services.length > 0)) {
                    connectivityManager.connect(services[0]);
                }
                break;
            case DISCOVERY_LOST_SERVICE:
                // Ignore
                break;
            case APPLICATION_CONNECTED:
                // TODO: Notify the user that the connection was made.
                Toast.makeText(this, "Successfully connected.", Toast.LENGTH_SHORT).show();
                // Enabled the play button
                if (playButton != null) {
                    playButton.setEnabled(true);
                }
                break;
            case APPLICATION_DISCONNECTED:
                // TODO: Notify the user that the connection was lost.
                Toast.makeText(this, "Lost connection.", Toast.LENGTH_SHORT).show();
                // Disable the play button
                if (playButton != null) {
                    playButton.setEnabled(false);
                }
                connectivityManager.startDiscovery();
                break;
            case APPLICATION_CONNECT_FAILED:
                // TODO: Notify the user that the connection attempt failed.
                Toast.makeText(this, "Failed to connect.", Toast.LENGTH_SHORT).show();
                // Re-start discover
                connectivityManager.startDiscovery();
                break;
        }
    }
}
