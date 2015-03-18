package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.utils.MaterialInterpolator;
import com.samsung.multiscreen.msf20.casteroids.views.CustomToast;
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

    private static final String TAG = MainActivity.class.getSimpleName();

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button playButton, selectTVButton, noTVDiscoveredButton;

    /** Reference to ImageButton */
    private ImageButton gameOptionsButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    /** How to play button animator */
    private ObjectAnimator animator;

    ProgressDialog progressDialog;

    /** Reference to the Spinner with the options **/
    private ListView optionsView;

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
        gameOptionsButton = (ImageButton) findViewById(R.id.game_options_button);
        gameOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGameOptions();
            }
        });

        animator = ObjectAnimator.ofFloat(gameOptionsButton, "rotation", 360);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(15000);

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

        optionsView = (ListView) findViewById(R.id.game_options_spinner);
        final TypefacedArrayAdapter adapter = new TypefacedArrayAdapter(this, getResources().getStringArray(R.array.options_array));
        optionsView.setAdapter(adapter);
        optionsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // How to Play
                    launchIntent(HowToPlayActivity.class);
                } else if (position == 1) { // How to Connect
                    // launchIntent(HowToConnectActivity.class); // TODO Create this class
                }
            }
        });

        //set the various buttons and text labels with the typeface
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

        //Register for connectivity and message updates.
        connectivityManager.registerConnectivityListener(this);
        connectivityManager.registerMessageListener(this, Event.SLOT_UPDATE);


        //capture the current state of the connection and show on the UI
        bindViews();

        animator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a connectivity and message update listener
        connectivityManager.unregisterConnectivityListener(this);
        connectivityManager.unregisterMessageListener(this, Event.SLOT_UPDATE);

        animator.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //disconnect
        connectivityManager.disconnect();

        animator.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_TV_RESULT_CODE) {
            if (resultCode == SelectDeviceActivity.RESULT_OK) {
                displayProgressIndicator();
                // If the user selected a device...
                connectivityManager.connect(data.getStringExtra(SelectDeviceActivity.SELECTED_SERVICE_KEY));
                // Wait for the connect notification.
            } else if (resultCode == SelectDeviceActivity.RESULT_ERROR) {
                // Looks like something went wrong when trying to select a device to use
                CustomToast.makeText(this, "Failed to select device to connect to.", Toast.LENGTH_SHORT).show();
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
				CustomToast.makeText(this, "Successfully connected.", Toast.LENGTH_SHORT).show();
				
				// Wait for the slot update before moving to the next screen. The slot update is sent when the TV
				// Application is initialized.
				break;
            case APPLICATION_DISCONNECTED:
                cancelProgressIndicator();

                CustomToast.makeText(this, "Application disconnected.", Toast.LENGTH_SHORT).show();
                break;
            case APPLICATION_CONNECT_FAILED:
                cancelProgressIndicator();

                // Notify the user that the connection attempt failed.
                CustomToast.makeText(this, "Failed to connect.", Toast.LENGTH_SHORT).show();

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
    public void onMessage(String eventName, String data, byte[] payload) {
		Event event = Event.getByName(eventName);
		switch (event) {
			case SLOT_UPDATE:
                cancelProgressIndicator();
                
                // The slot update indicates that the TV Application is initialized, move to the player info screen
                launchIntent(PlayerInfoActivity.class);
				break;
			default:
				// ignore
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

    private void displayProgressIndicator() {
        progressDialog = ProgressDialog.show(MainActivity.this, "Connecting", "Please Wait ...", true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                connectivityManager.disconnect();
                connectivityManager.startDiscovery();
            }
        });

    }

    private void cancelProgressIndicator() {
        progressDialog.dismiss();
    }


    /**
     * Hide and show buttons depending on the state of the connection.
     */
    private void bindViews(){
        playButton.setVisibility(View.GONE);
        selectTVButton.setVisibility(View.GONE);
        noTVDiscoveredButton.setVisibility(View.GONE);

        // Make sure the connectivity manager is in the correct state.
		String[] services = connectivityManager.getDiscoveredServiceNames();
		if ((services != null) && (services.length > 0)) {
			boolean hasSingleService = services.length == 1;
			if (hasSingleService) {
				playButton.setVisibility(View.VISIBLE);
			} else {
				// multiple services
				selectTVButton.setVisibility(View.VISIBLE);
			}
		} else {
			// no tvs discovered yet
			noTVDiscoveredButton.setVisibility(View.VISIBLE);
		}
    }


    private void onPlayButtonClick() {
        // If we are connected to the application, move to the player info screen.
		if (connectivityManager.isConnected()) {
            displayProgressIndicator();
			launchIntent(PlayerInfoActivity.class);
		}
		// Else if there is at least one service, attempt to connect to a service.
		else if (connectivityManager.hasDiscoveredService()) {
			String[] services = connectivityManager.getDiscoveredServiceNames();
			if ((services != null) && (services.length > 0)) {
                displayProgressIndicator();

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

    private void showGameOptions() {
        if(optionsView.getVisibility() == View.VISIBLE) {
            ScaleAnimation scaleDown =  new ScaleAnimation(1f, 0.2f, 1f, 0.2f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);
            scaleDown.setDuration(300);
            scaleDown.setInterpolator(new MaterialInterpolator());
            scaleDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    optionsView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            optionsView.startAnimation(scaleDown);
        } else {
            ScaleAnimation scaleUp =  new ScaleAnimation(0.2f, 1f, 0.2f, 1f, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f);
            scaleUp.setDuration(300);
            scaleUp.setFillAfter(true);
            scaleUp.setInterpolator(new MaterialInterpolator());
            scaleUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    optionsView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            optionsView.startAnimation(scaleUp);
        }
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
        CustomToast.makeText(this, "Starting discovery...", Toast.LENGTH_SHORT).show();
        if(!connectivityManager.hasDiscoveredService()) {
            connectivityManager.startDiscovery();
        }
    }

    private void launchIntent(Class cls){
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
    }

    private class TypefacedArrayAdapter extends ArrayAdapter<String> {

        public TypefacedArrayAdapter(Context context, String[] objects) {
            super(context, R.layout.list_item, R.id.item_label, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = (TextView) super.getView(position, convertView, parent);
            if(tv != null) {
                tv.setTypeface(customTypeface);
            }
            return tv;
        }
    }
}
