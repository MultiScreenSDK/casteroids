package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.Fire;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.Rotate;
import com.samsung.multiscreen.msf20.casteroids.model.Thrust;
import com.samsung.multiscreen.msf20.casteroids.views.CompassView;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

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

    /** Keep track of state */
    private boolean turningLeft, turningRight, thrusting, firing;
    private int strengthLeft, strengthRight;

    /** GameConnectivityManager enables sending messages to the TV */
    private GameConnectivityManager gameConnectivityManager;

    /** Vibration service */
    private Vibrator vibrator;

    /** Device orientation */
    float pitch = 0;

    /** Track device orientation */
    float[] aValues = new float[3];

    /** Track device orientation */
    float[] mValues = new float[3];

    /** Visual indicator of the device orientation */
    CompassView compassView;

    /** Android SensorManager */
    SensorManager sensorManager;

    /** Accelerometer sensor */
    Sensor accelerometer;

    /** Magnetic Field sensor */
    Sensor magneticField;

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

        //get the color from the intent
        int color = getIntent().getIntExtra("color", getResources().getColor(R.color.pink_400));

        //sensor code
        compassView = (CompassView)this.findViewById(R.id.compass_view);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        compassView.setShowNumber(false);
        compassView.setGyroColor(color);

        //to control the vibration on button press
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //button touches
        setOnTouchListeners();

        Button thrustButton = (Button) findViewById(R.id.thrust_button);
        Button fireButton = (Button) findViewById(R.id.fire_button);
        Button pauseButton = (Button) findViewById(R.id.pause_button);
        TextView instructionsText = (TextView) findViewById(R.id.instructions_text);

        //set the custom typefaces
        thrustButton.setTypeface(customTypeface);
        fireButton.setTypeface(customTypeface);
        pauseButton.setTypeface(customTypeface);
        instructionsText.setTypeface(customTypeface);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get an instance of the ConnectivtyManager and register for connectivity updates.
        gameConnectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        gameConnectivityManager.registerConnectivityListener(this);
        gameConnectivityManager.registerMessageListener(this, Event.GAME_START, Event.GAME_OVER);

        // If we are not connected return to the main screen.
        if (!gameConnectivityManager.isConnected()) {
            // TODO: Notify the user that we are not connected.
            Toast.makeText(this, "No connection.", Toast.LENGTH_SHORT).show();
            finish();
        }

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
        gameConnectivityManager.unregisterMessageListener(this, Event.GAME_START, Event.GAME_OVER);

        //stop the sensor listeners as it can drain the battery if you don't
        sensorManager.unregisterListener(sensorEventListener);
    }

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
        int strength = getStrength(threshold, pitch);
        
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

    private int getStrength(int threshold, float pitch) {
    	// Creates and returns a strength value ranging from 0 to 20 based on the given threshold and pitch.
    	return (int)(((Math.abs(pitch)-threshold) * 20.0f) / (90.0f - threshold));
    }
    
    private void setOnTouchListeners() {

        int[] buttons = new int[] { R.id.left_button, R.id.right_button, R.id.thrust_button, R.id.fire_button };

        for (int i = 0; i < buttons.length; i++) {
            int buttonId = buttons[i];

            View button = findViewById(buttonId);
            button.setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int id = v.getId();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            handleDown(id);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            handleUp(id);
        }

        return false;
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
            case R.id.left_button:
                setTurningLeft(value, 45);
                break;
            case R.id.right_button:
                setTurningRight(value, 45);
                break;
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

    @Override
    public String toString() {
        return "GameControllerActivity{" + "turningLeft=" + turningLeft + ", turningRight=" + turningRight + ", thrusting="
                + thrusting + ", firing=" + firing + '}';
    }

    @Override
    public void onBackPressed() {
        quitGame(null);
    }

    /**
     * Sends a quit message to the game manager.
     *
     * @param view calling view
     */
    public void quitGame(View view) {
        gameConnectivityManager.sendQuitMessage();
        finish();
    }

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
            Toast.makeText(this, "Game Over", Toast.LENGTH_SHORT).show();
            quitGame(null); //FIXME:  Show the game results screen
        } else if (event.equals(Event.GAME_START.getName())){
            //show countdown
            Toast.makeText(this, "Game starting in " + gameConnectivityManager.getGameState().getGameStartCountDownSeconds() + " seconds", Toast.LENGTH_SHORT).show();
        }
    }
        
    //-----------------------------------------
    //Inner Classes
    //-----------------------------------------

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