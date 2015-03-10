package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.ConfigType;
import com.samsung.multiscreen.msf20.casteroids.model.ConfigTypeMap;
import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

/**
 * Landing page for the game. Depending on the connectivity manager, it shows a
 * different connection button.
 *
 * @author Nik Bhattacharya
 *
 */
public class MainActivity extends Activity implements ConnectivityListener, MessageListener {

    /** Code to send to the next screen when calling startActivityForResult */
    private static final  int SELECT_TV_RESULT_CODE = 1000;

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button playButton, settingsButton, howToPlayButton, selectTVButton, noTVDiscoveredButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    /** Reference to the game config gameConfigDialog */
    private Dialog gameConfigDialog;


    /******************************************************************************************************************
     * Android Lifecycle methods
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //content view
        setContentView(R.layout.activity_main);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //get a reference to the root view
        rootView = findViewById(R.id.root_view);

        // Initialize the how to play button
        howToPlayButton = (Button) findViewById(R.id.how_to_play_button);
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToPlay();
            }
        });

        // Initialize the how to play button
        settingsButton = (Button) findViewById(R.id.game_settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGameSettings();
            }
        });


        // Initialize the play button
        playButton = (Button) findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayButtonClick();
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
        noTVDiscoveredButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoTVDiscoveredScreen();
            }
        });

        //set the various buttons with the typeface
        howToPlayButton.setTypeface(customTypeface);
        settingsButton.setTypeface(customTypeface);
        playButton.setTypeface(customTypeface);
        selectTVButton.setTypeface(customTypeface);
        noTVDiscoveredButton.setTypeface(customTypeface);

        //if we are lollipop, do a custom animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runLollipopCode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register for connectivity updates.
        connectivityManager.registerConnectivityListener(this);

        //Register for configuration updates
        connectivityManager.registerMessageListener(this, Event.CONFIG_UPDATE);

        //capture the current state of the connection and show on the UI
        bindViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);

        //Register for configuration updates
        connectivityManager.unregisterMessageListener(this, Event.CONFIG_UPDATE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //disconnect
        connectivityManager.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_TV_RESULT_CODE) {
            // If the user selected a device...
            if (resultCode == Activity.RESULT_OK) {
                // If we are connected to the application, move to the player info screen. Otherwise we will wait to be
                // connected or for the error message.
                if (connectivityManager.isConnected()) {
                    launchIntent(PlayerInfoActivity.class);
                }
                // Else, wait for the connect notification.
            }
        }
    }

    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */

    @Override
    public void onConnectivityUpdate(int eventId) {
        switch (eventId) {
            case DISCOVERY_STOPPED:
                // Restart discovery as long as we don't have a discovered service.
                if (!connectivityManager.hasDiscoveredService()) {
                    connectivityManager.startDiscovery();
                }
                break;
            case APPLICATION_CONNECTED:
                // TODO: Remove toast
                Toast.makeText(this, "Successfully connected.", Toast.LENGTH_SHORT).show();
                // We are connected to the application move to the player info screen
                launchIntent(PlayerInfoActivity.class);
                break;
            case APPLICATION_DISCONNECTED:
            case APPLICATION_CONNECT_FAILED:
                // TODO: Notify the user that the connection attempt failed.
                Toast.makeText(this, "Failed to connect.", Toast.LENGTH_SHORT).show();
                // The application failed to connect or was disconnected, re-start discovery
                connectivityManager.startDiscovery();
                break;
            default:
                // ignore
        }

        // always rebind the views when an event comes in
        bindViews();
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        if(event.equals(Event.CONFIG_UPDATE.getName())){
            if(gameConfigDialog != null && gameConfigDialog.isShowing()) {
                Toast.makeText(this, "Another player edited the game configuration", Toast.LENGTH_SHORT).show();
                gameConfigDialog.dismiss();
            }
        }
    }

    /******************************************************************************************************************
     * Private methods
     */

    /**
     * Android 5.0 (Lollipop) specific code here.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void runLollipopCode() {
        rootView.setVisibility(View.INVISIBLE);

        //show how we can support material design
        rootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            public boolean onPreDraw() {
                rootView.getViewTreeObserver().removeOnPreDrawListener(this);
                rootView.setVisibility(View.VISIBLE);

                Animator anim = ViewAnimationUtils.createCircularReveal(rootView, rootView.getWidth() / 2, rootView.getHeight() / 2, 0, rootView.getWidth());
                anim.setDuration(1000);
                anim.start();

                return false;
            }
        });
    }


    /**
     * Hide and show buttons depending on the state of the connection.
     */
    private void bindViews(){
        playButton.setVisibility(View.GONE);
        selectTVButton.setVisibility(View.GONE);
        noTVDiscoveredButton.setVisibility(View.GONE);

        // Make sure the connectivity manager is in the correct state.
        if (connectivityManager.hasDiscoveredService()) {
            String[] services = connectivityManager.getDiscoveredServiceNames();
            if ((services != null) && (services.length > 0)) {
                boolean hasSingleService = services.length == 1;
                if(hasSingleService){
    				playButton.setEnabled(true);
                    playButton.setVisibility(View.VISIBLE);
                } else {
                    //multiple services
                    selectTVButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            //no tvs discovered yet
            noTVDiscoveredButton.setVisibility(View.VISIBLE);
        }
    }


    private void onPlayButtonClick() {
		// If we are connected to the application, move to the player info screen.
		if (connectivityManager.isConnected()) {
			launchIntent(PlayerInfoActivity.class);
		}
		// Else if there is at least one service, attempt to connect to a service.
		else if (connectivityManager.hasDiscoveredService()) {
			String[] services = connectivityManager.getDiscoveredServiceNames();
			if ((services != null) && (services.length > 0)) {
				// Disable the play button so it doesn't get pressed again while we are connecting.
				playButton.setEnabled(false);

				// Connect to the first available service.
				// There should only be one for this button to be visible.
				connectivityManager.connect(services[0]);
			}
		}
		// Else, this button should not even been enabled. Let's rebind the view.
		else {
			bindViews();
		}
	}

    private void showHowToPlay() {
        launchIntent(HowToPlayActivity.class);
    }

    private void showGameSettings() {
        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Get the info from the model
        final ConfigTypeMap configTypeMap = connectivityManager.getGameState().getConfigTypeMap();
        final ConfigType[] configTypes = configTypeMap.getConfigTypes();

        //Now massage the data in a way that the gameConfigDialog understands
        final CharSequence[] gameOptions = new CharSequence[configTypes.length];
        final boolean[] selectedOptions = new boolean[configTypes.length];

        for(int i=0; i< configTypes.length; i++) {
            ConfigType type = configTypes[i];
            gameOptions[i] = type.getDescription();
            selectedOptions[i] = configTypeMap.isEnabled(type);
        }

        //a java oddity here to enable an inner class to have a reference to a final variable
        final boolean[] isModified = new boolean[]{false};


        // Set the gameConfigDialog title
        builder.setTitle("Game Options")
                .setMultiChoiceItems(gameOptions, selectedOptions, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        isModified[0] = true;
                        selectedOptions[which] = isChecked;
                    }
                })

                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isModified[0]) {
                            for (int i = 0; i < configTypes.length; i++) {
                                ConfigType type = configTypes[i];
                                //get the value from the array
                                configTypeMap.setIsEnabled(type, selectedOptions[i]);
                            }

                            //save the configuration
                            connectivityManager.sendConfigUpdate(configTypeMap);
                            Toast.makeText(getApplicationContext(), "Saved Options", Toast.LENGTH_SHORT).show();
                        }

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        gameConfigDialog = builder.create();
        gameConfigDialog.show();

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




}
