package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Toast;

import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;
import com.samsung.multiscreen.msf20.game.model.Event;
import com.samsung.multiscreen.msf20.game.model.Fire;
import com.samsung.multiscreen.msf20.game.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.game.model.GameOption;
import com.samsung.multiscreen.msf20.game.model.Rotate;
import com.samsung.multiscreen.msf20.game.model.Thrust;
import com.samsung.multiscreen.msf20.game.views.CompassView;

/**
 * The spaceship game happens here.
 *
 * @author Nik Bhattacharya
 */
public class GameActivity extends Activity implements View.OnTouchListener, ConnectivityListener, MessageListener {

    /** Debugging */
    private static final String TAG = GameActivity.class.getSimpleName();

    /** Keep track of state */
    private boolean turningLeft, turningRight, thrusting, firing;

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

        //sensor code
        compassView = (CompassView)this.findViewById(R.id.compass_view);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        compassView.setShowNumber(true);

        //to control the vibration on button press
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //button touches
        setOnTouchListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get an instance of the ConnectivtyManager and register for connectivity updates.
        gameConnectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        gameConnectivityManager.registerConnectivityListener(this);
        gameConnectivityManager.registerMessageListener(this, Event.GAME_STATE.getName());
        gameConnectivityManager.sendGameOptionMessage(GameOption.JOIN);

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
        gameConnectivityManager.unregisterMessageListener(this, Event.GAME_STATE.getName());

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

        if(pitch > -10 && pitch < 10) {
            setTurningRight(false);
            setTurningLeft(false);
        } else if (pitch <= -10 && !turningRight) {
            setTurningRight(true);
        } else if(pitch >= 10 && !turningLeft) {
            setTurningLeft(true);
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
                setTurningLeft(value);
                break;
            case R.id.right_button:
                setTurningRight(value);
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

    private void setTurningLeft(boolean value) {
        turningLeft = value;

        if (value) {
            gameConnectivityManager.sendRotateMessage(Rotate.LEFT);
        } else {
            gameConnectivityManager.sendRotateMessage(Rotate.NONE);
        }
    }

    private void setTurningRight(boolean value) {
        turningRight = value;

        if (value) {
            gameConnectivityManager.sendRotateMessage(Rotate.RIGHT);
        } else {
            gameConnectivityManager.sendRotateMessage(Rotate.NONE);
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
        return "GameActivity{" + "turningLeft=" + turningLeft + ", turningRight=" + turningRight + ", thrusting="
                + thrusting + ", firing=" + firing + '}';
    }

    @Override
    public void onBackPressed() {
        pauseGame(null);
    }

    public void pauseGame(View view) {
        // TODO send message
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
                // TODO: Notify the user that the connection was lost.
                Toast.makeText(this, "Lost connection.", Toast.LENGTH_SHORT).show();
                // We lost connect, return to the main activity.
                finish();
                break;
        }
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        // TODO Auto-generated method stub
        
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