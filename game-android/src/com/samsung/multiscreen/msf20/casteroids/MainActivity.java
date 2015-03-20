package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
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

    /** Reference to the no-tv label **/
    private TextView noTVlabel;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    ProgressDialog progressDialog;


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

        noTVlabel = (TextView) findViewById(R.id.no_tv_text);

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
        noTVDiscoveredButton = (Button) findViewById(R.id.info_button);
        noTVDiscoveredButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoTVDiscoveredScreen();
            }
        });

        //set the various buttons and text labels with the typeface
        playButton.setTypeface(customTypeface);
        selectTVButton.setTypeface(customTypeface);
        noTVDiscoveredButton.setTypeface(customTypeface);
        noTVlabel.setTypeface(customTypeface);

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
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a connectivity and message update listener
        connectivityManager.unregisterConnectivityListener(this);
        connectivityManager.unregisterMessageListener(this, Event.SLOT_UPDATE);
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
        	case WIFI_CONNECTED:
                // Start discovery on the new WiFi network.
                connectivityManager.startDiscovery();
        		break;
        	case WIFI_DISCONNECTED:
                // Stop discovery since we are not connected to a WiFi network.
                connectivityManager.stopDiscovery();
        		break;
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
        progressDialog = new ProgressDialog(MainActivity.this, R.style.CustomAppDialog);
        TextView title = new TextView(this);
        title.setText(R.string.title_connecting);
        title.setTextColor(getResources().getColor(R.color.dialogs_text_blue));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setPadding(30, 30, 0, 0);
        title.setTypeface(customTypeface);
        progressDialog.setCustomTitle(title);
        progressDialog.setMessage("Please Wait ...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                connectivityManager.disconnect();
                connectivityManager.startDiscovery();
            }
        });
        progressDialog.show();
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
        noTVDiscoveredButton.setVisibility(View.INVISIBLE);
        noTVlabel.setVisibility(View.GONE);

        // If the device is connected to WiFi, enable the appropriate button.
        if (connectivityManager.isConnectedToWifi()) {
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
                noTVlabel.setText(R.string.no_tv_discovered);
                noTVlabel.setVisibility(View.VISIBLE);
    		}
        } 
        // Else the device is not connected to WiFi, display a message to the user.
        else {
            noTVlabel.setText(R.string.wifi_disabled_warning);
            noTVlabel.setVisibility(View.VISIBLE);
        	return;
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


    private void showSelectTVScreen() {
        //start activity for result here
        Intent intent = new Intent();
        intent.setClass(this, SelectDeviceActivity.class);
        startActivityForResult(intent, SELECT_TV_RESULT_CODE);

    }

    private void showNoTVDiscoveredScreen() {
        launchIntent(HowToConnectActivity.class);
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
