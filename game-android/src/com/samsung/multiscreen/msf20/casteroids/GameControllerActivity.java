package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.Fire;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.MessageDataHelper;
import com.samsung.multiscreen.msf20.casteroids.model.Rotate;
import com.samsung.multiscreen.msf20.casteroids.model.Thrust;
import com.samsung.multiscreen.msf20.casteroids.views.GyroView;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

import java.util.ArrayList;

/**
 * The game controller screen for Casteroids.
 *
 * @author Nik Bhattacharya
 */
public class GameControllerActivity extends Activity implements View.OnTouchListener, ConnectivityListener, MessageListener {

    /** Debugging */
    private static final String TAG = GameControllerActivity.class.getSimpleName();

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Keep track of which color the user selected */
    private int userSelectedColor;

    /** Keep track of state */
    private boolean turningLeft, turningRight, thrusting, firing;
    private int strengthLeft, strengthRight;

    /** GameConnectivityManager enables sending messages to the TV */
    private GameConnectivityManager gameConnectivityManager;

    /** Vibration service */
    private Vibrator vibrator;

    /** References to various buttons */
    private Button thrustButton, fireButton;

    /** Reference to the quit button */
    private ImageButton quitButton;

    /** Reference to text labels*/
    private TextView instructionsText;

    /** Reference to the ship */
    private ImageView shipView;

    /** Reference to the ship */
    private ImageView deathOverlayView;

    /** Device orientation */
    private float pitch = 0;

    /** Track device orientation */
    private float[] aValues = new float[3];

    /** Track device orientation */
    private float[] mValues = new float[3];

    /** Visual indicator of the device orientation */
    private GyroView compassView;

    /** Android SensorManager */
    private SensorManager sensorManager;

    /** Accelerometer sensor */
    private Sensor accelerometer;

    /** Magnetic Field sensor */
    private Sensor magneticField;

    /** Whether user input is enabled */
    private boolean isEnabled = true;

    /** Whether the current player out event has been processed */
    private boolean processedPlayerOutEvent = false;

    /** Stroke size in pixels */
    private int strokeSize;

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

        //xml layout
        setContentView(R.layout.activity_game);

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //get the stroke size
        strokeSize = getResources().getDimensionPixelSize(R.dimen.ship_stroke);

        //get the color from the intent
        userSelectedColor= getIntent().getIntExtra("color", getResources().getColor(R.color.pink_400));

        //sensor code
        compassView = (GyroView)this.findViewById(R.id.compass_view);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        compassView.setShowNumber(false);
        compassView.setGyroColor(userSelectedColor);

        //to control the vibration on button press
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //button touches
        setOnTouchListeners();

        thrustButton = (Button) findViewById(R.id.thrust_button);
        fireButton = (Button) findViewById(R.id.fire_button);
        quitButton = (ImageButton) findViewById(R.id.pause_button);
        instructionsText = (TextView) findViewById(R.id.instructions_text);

        //set the custom typefaces
        thrustButton.setTypeface(customTypeface);
        fireButton.setTypeface(customTypeface);
        instructionsText.setTypeface(customTypeface);

        //reference to the ship
        shipView = (ImageView)findViewById(R.id.ship_view);
        setShipColor(shipView, userSelectedColor);

        deathOverlayView = (ImageView) findViewById(R.id.death_overlay);
        deathOverlayView.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get an instance of the ConnectivtyManager and register for connectivity updates.
        gameConnectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        gameConnectivityManager.registerConnectivityListener(this);
        gameConnectivityManager.registerMessageListener(this, Event.GAME_START, Event.PLAYER_OUT, Event.GAME_OVER);

        // If we are not connected return to the main screen.
        if (!gameConnectivityManager.isConnected()) {
            // TODO: Notify the user that we are not connected.
            Toast.makeText(this, "No connection.", Toast.LENGTH_SHORT).show();
            finish();
        }

        //determine if the user input should be enabled or disabled based on whether or not the game is on!
        boolean hasGameStarted = (gameConnectivityManager.getGameState().getGameStartCountDownSeconds() == 0);
        enableViews(hasGameStarted);

        //start the sensor listeners
        if(accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if(magneticField != null) {
            sensorManager.registerListener(sensorEventListener, magneticField, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        gameConnectivityManager.unregisterConnectivityListener(this);
        gameConnectivityManager.unregisterMessageListener(this, Event.GAME_START, Event.PLAYER_OUT, Event.GAME_OVER);

        //stop the sensor listeners as it can drain the battery if you don't
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    public void onBackPressed() {
        sendQuitMessage(true); //send quit game message and disconnect.
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        //only send values if we are enabled
        if(isEnabled) {
            int id = v.getId();

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                handleDown(id);
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                handleUp(id);
            }
        }

        return false;
    }


    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */

    @Override
    public void onConnectivityUpdate(int eventId) {
        switch (eventId) {
            case DISCOVERY_STOPPED:
            case DISCOVERY_FOUND_SERVICE:
            case DISCOVERY_LOST_SERVICE:
            case APPLICATION_CONNECTED:
                // Ignore
                break;
            case APPLICATION_DISCONNECTED:
            case APPLICATION_CONNECT_FAILED:
                //Notify the user that the connection was lost.
                Toast.makeText(this, "Lost connection.", Toast.LENGTH_SHORT).show();
                // We lost connect, return to the main activity.
                finish();
                break;
        }
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Received event '" + event + "'");
        }
        if(event.equals(Event.GAME_OVER.getName())){
            Intent gameOverScreen = new Intent();
            gameOverScreen.setClass(this, GameOverActivity.class);
            startActivity(gameOverScreen);
            finish();
        } else if (event.equals(Event.GAME_START.getName())){
            //show countdown
            int numSeconds = MessageDataHelper.decodeGameStartCountDownSeconds(data);


            //show a toast for any non 0 wait time.
            if(numSeconds != 0) {
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText(getStyledString("Game starting in " + numSeconds + " seconds"));
            } else {
                instructionsText.setVisibility(View.INVISIBLE);
            }

            //Only 0 means we are in
            setUserInputEnabled(numSeconds == 0);
        } else if (event.equals(Event.PLAYER_OUT.getName())){
            //show countdown
            int numSeconds = MessageDataHelper.decodePlayerOutCountDownSeconds(data);

            //show a toast for any non 0 wait time.
            if(numSeconds != 0) {
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText(getStyledString("You are out. Prepare to re-enter in " + numSeconds + " seconds"));

                deathOverlayView.setVisibility(View.VISIBLE);

                if(!processedPlayerOutEvent) {
                    vibrator.vibrate(300);
                }
            }

            //Only 0 means we are in
            setUserInputEnabled(numSeconds == 0);
            if(numSeconds == 0){
                processedPlayerOutEvent = false; //get ready for the next player out
                deathOverlayView.setVisibility(View.GONE);
                instructionsText.setVisibility(View.INVISIBLE);
            }
        }
    }


    /******************************************************************************************************************
     * Private methods
     */

    /**
     * Updates the internal state of the device as well as sends the message
     * over the the Compass View.
     *
     * @param pitch the current pitch of the device.
     */
    private void updateOrientation(float pitch) {
        this.pitch = pitch;

        if (compassView!= null) {
            compassView.setPitch(pitch);
            compassView.invalidate();
        }

        int threshold = 5;
        //strength value ranging from 0 to 20 based on the given threshold and pitch.
        int strength = (int)(((Math.abs(pitch)-threshold) * 20.0f) / (90.0f - threshold));
        
        if(pitch > -threshold && pitch < threshold) {
            if(turningRight) {
                setTurningRight(false, 0);
            }
            if(turningLeft) {
                setTurningLeft(false, 0);
            }
        } else if (pitch <= -threshold && (!turningRight || strengthRight != strength)) {
            setTurningRight(true, strength);
        } else if(pitch >= threshold && (!turningLeft || strengthLeft != strength)) {
            setTurningLeft(true, strength);
        }
    }

    private void setOnTouchListeners() {

        int[] buttons = new int[] { R.id.left_button, R.id.right_button, R.id.thrust_button, R.id.fire_button };

        for (int i = 0; i < buttons.length; i++) {
            int buttonId = buttons[i];

            View button = findViewById(buttonId);
            button.setOnTouchListener(this);
        }
    }



    private void handleDown(int id) {

        handleEvent(id, /** Down */
        true);
        Log.v(TAG, toString());
    }

    private void handleUp(int id) {

        handleEvent(id, /** Down */
        false);
        Log.v(TAG, toString());
    }

    private void handleEvent(int viewId, boolean value) {
        switch (viewId) {
            case R.id.thrust_button:
                setThrusting(value);
                break;
            case R.id.fire_button:
                setFiring(value);
                break;
            default:
                break;
        }
        Log.v(TAG, toString());
    }

    private void setTurningLeft(boolean value, int strength) {
        turningLeft = value;
        strengthLeft = strength;

        if (value) {
            gameConnectivityManager.sendRotateMessage(Rotate.LEFT, strength);
        } else {
            gameConnectivityManager.sendRotateMessage(Rotate.NONE, 0);
        }
    }

    private void setTurningRight(boolean value, int strength) {
        turningRight = value;
        strengthRight = strength;

        if (value) {
            gameConnectivityManager.sendRotateMessage(Rotate.RIGHT, strength);
        } else {
            gameConnectivityManager.sendRotateMessage(Rotate.NONE, 0);
        }
    }

    private void setThrusting(boolean value) {
        thrusting = value;

        if (value) {
            gameConnectivityManager.sendThrustMessage(Thrust.ON);
        } else {
            gameConnectivityManager.sendThrustMessage(Thrust.OFF);
        }
    }

    private void setFiring(boolean value) {
        firing = value;

        if (value) {
            gameConnectivityManager.sendFireMessage(Fire.ON);
            vibrator.vibrate(10);
        } else {
            gameConnectivityManager.sendFireMessage(Fire.OFF);
        }
    }

    /**
     * Sends a quit message to the game manager and disconnects.
     *
     * @param view calling view
     */
    public void quitGame(View view) {
        sendQuitMessage(true);
    }

    /**
     * Sends a quit message to the game manager and optionally disconnects.
     *
     * @param disconnect disconnect flag
     */
    private void sendQuitMessage(boolean disconnect) {
        gameConnectivityManager.sendQuitMessage();
        finish();
        if(disconnect) {
            gameConnectivityManager.disconnect();
        }
    }



    /**
     * An example of how to use a Spannable in Android to style specific
     * sections of a String.
     *
     * @param string the string to be styled
     * @return the spannable with the styles embedded
     */
    private Spannable getStyledString(String string) {

        Spannable spannable = new SpannableString(string);
        ArrayList<Integer> spans = new ArrayList<Integer>(spannable.length());
        for(int i = 0; i < spannable.length(); i++){
            if(Character.isDigit(spannable.charAt(i))){
                spans.add(new Integer(i));
            }
        }
        for(int j = 0; j < spans.size(); j++) {
            int index = spans.get(j).intValue();
            spannable.setSpan(new RelativeSizeSpan(1.7f), index, index+1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(userSelectedColor), index, index+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), index, index+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    /**
     * Sends messages to the server if user input is disabled
     * and changes the UI state as well.
     *
     * @param enabled true if enabled
     */
    private void setUserInputEnabled(boolean enabled){

        if(this.isEnabled != enabled){
            //first go ahead and enable/disable/hide views.
            enableViews(enabled);

            //toggle the setting
            this.isEnabled = enabled;

            //then send messages to the server if needed
            if(!this.isEnabled) {

                setFiring(false);
                setThrusting(false);
                setTurningRight(false, 0);
                setTurningLeft(false, 0);

            }
        }
    }

    /**
     * Only affects the views. Does not send any messages to the server.
     *
     * @param enabled true if enabled
     */
    private void enableViews(boolean enabled) {
        thrustButton.setEnabled(enabled);
        fireButton.setEnabled(enabled);
        if (enabled) {
            compassView.setVisibility(View.VISIBLE);
            setThrusting(thrustButton.isPressed());
            setFiring(fireButton.isPressed());
        } else {
            compassView.setVisibility(View.INVISIBLE);
        }
    }

    private void setShipColor(ImageView shipView, int color) {
        shipView.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        GradientDrawable drawable = (GradientDrawable)shipView.getBackground();

        //get the individual rgb values
        int startR = (color >> 16) & 0xff;
        int startG = (color >> 8) & 0xff;
        int startB = color & 0xff;

        //replace the alpha channel with transparency 0x19
        int alphaColor = (int)(0x19 << 24) |
                (int)(startR << 16) |
                (int)(startG  << 8) |
                (int)(startB);

        //set the fill color to the alpha transparent color
        drawable.setColor(alphaColor);

        //set the stroke color
        drawable.setStroke(strokeSize, color);
        drawable.invalidateSelf();
    }

    /******************************************************************************************************************
     * Inner Classes
     */

    /**
     * SensorEventListener listens for Accelerometer and Magnetic Field events and updates
     * the state of the device orientation.
     *
     */
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType ()){
                case Sensor.TYPE_ACCELEROMETER:
                    aValues = event.values.clone ();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mValues = event.values.clone ();
                    break;
            }

            float[] R = new float[16];
            float[] orientationValues = new float[3];

            SensorManager.getRotationMatrix (R, null, aValues, mValues);
            SensorManager.getOrientation (R, orientationValues);

            orientationValues[1] = (float)Math.toDegrees (orientationValues[1]);

            updateOrientation(orientationValues[1]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}