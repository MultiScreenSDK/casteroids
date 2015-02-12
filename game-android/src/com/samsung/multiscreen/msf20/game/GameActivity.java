package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.game.model.ConnectivityListener;
import com.samsung.multiscreen.msf20.game.model.Fire;
import com.samsung.multiscreen.msf20.game.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.game.model.Rotate;
import com.samsung.multiscreen.msf20.game.model.Thrust;

public class GameActivity extends Activity implements View.OnTouchListener, ConnectivityListener {

    private static final String TAG = GameActivity.class.getSimpleName();

    private boolean turningLeft, turningRight, thrusting, firing;

    /** GameConnectivityManager enables sending messages to the TV */
    private GameConnectivityManager gameConnectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);
        setOnTouchListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Get an instance of the ConnectivtyManager and register for connectivity updates.
        gameConnectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        gameConnectivityManager.registerConnectivityListener(this);

        // If we are not connected return to the main screen.
        if (!gameConnectivityManager.isConnected()) {
            // TODO: Notify the user that we are not connected.
            Toast.makeText(this, "No connection.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        gameConnectivityManager.unregisterConnectivityListener(this);
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
}