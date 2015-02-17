package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

/**
 * Entry point into the Game.
 *
 * @author Nik Bhattacharya
 *
 */
public class MainActivity extends Activity implements ConnectivityListener {

    /** Code to send to the next screen when calling startActivityForResult */
    private static final  int SELECT_TV_RESULT_CODE = 1000;

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button playButton, howToPlayButton, selectTVButton, noTVDiscoveredButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //content view
        setContentView(R.layout.activity_main);

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        // Initialize the how to play button
        howToPlayButton = (Button) findViewById(R.id.how_to_play_button);
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToPlay();
            }
        });

        // Initialize the play button
        playButton = (Button) findViewById(R.id.play_button);
        playButton.setEnabled(false);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGame();
            }
        });

        // Initialize the select TV button
        selectTVButton = (Button) findViewById(R.id.select_tv_button);
        selectTVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectTVScreen();
            }
        });

        // Initialize the no TV discovered button
        noTVDiscoveredButton = (Button) findViewById(R.id.no_tv_button);
        noTVDiscoveredButton.setEnabled(true);
        noTVDiscoveredButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoTVDiscoveredScreen();
            }
        });

        //set the various buttons with the typeface
        howToPlayButton.setTypeface(customTypeface);
        playButton.setTypeface(customTypeface);
        selectTVButton.setTypeface(customTypeface);
        noTVDiscoveredButton.setTypeface(customTypeface);

    }

    @Override
    protected void onResume() {
        super.onResume();

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
        launchIntent(GameActivity.class);
    }

    private void showHowToPlay() {
        launchIntent(HowToPlayActivity.class);
    }

    private void showSelectTVScreen() {
        //start activity for result here
        Intent intent = new Intent();
        intent.setClass(this, SelectDeviceActivity.class);
        startActivityForResult(intent, SELECT_TV_RESULT_CODE);

    }

    private void showNoTVDiscoveredScreen() {
        //launchIntent(HowToPlayActivity.class); //FIXME

        //FIXME
        Toast.makeText(this, "Starting discovery...", Toast.LENGTH_SHORT).show();
        if(!connectivityManager.hasDiscoveredService()) {
            connectivityManager.startDiscovery();
        }
    }

    private void launchIntent(Class cls){
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
    }

    @Override
    public void onConnectivityUpdate(int eventId) {
        //disable all the buttons except for the No TV discovered
        noTVDiscoveredButton.setEnabled(true);
        playButton.setEnabled(false);

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
                    if(services.length == 1) {
                        connectivityManager.connect(services[0]);
                    }
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

                //disable the no tv found
                if(noTVDiscoveredButton != null) {
                    noTVDiscoveredButton.setEnabled(false);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SELECT_TV_RESULT_CODE){
            if(resultCode == Activity.RESULT_OK) {
                //user selected a TV and is ready to go
                playGame();
            }
        }
    }
}
