package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

/**
 * Landing page for the game. Depending on the connectivity manager, it shows a
 * different connection button.
 *
 * @author Nik Bhattacharya
 *
 */
public class MainActivity extends Activity implements ConnectivityListener{

    /** Code to send to the next screen when calling startActivityForResult */
    private static final  int SELECT_TV_RESULT_CODE = 1000;

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button playButton, selectTVButton, noTVDiscoveredButton;

    /** Reference to ImageButton */
    private ImageButton howToPlayButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    /** Reference to the Casteroids TextView */
    private TextView casteroidsText;


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
        howToPlayButton = (ImageButton) findViewById(R.id.how_to_play_button);
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToPlay();
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

        //get the Casteroids text view
        casteroidsText = (TextView)findViewById(R.id.game_title);

        //set the various buttons and text labels with the typeface
        playButton.setTypeface(customTypeface);
        selectTVButton.setTypeface(customTypeface);
        noTVDiscoveredButton.setTypeface(customTypeface);
        casteroidsText.setTypeface(customTypeface);

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

        //capture the current state of the connection and show on the UI
        bindViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);

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
