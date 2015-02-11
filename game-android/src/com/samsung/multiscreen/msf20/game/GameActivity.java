package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


public class GameActivity extends Activity implements View.OnTouchListener {

    private static final String TAG = GameActivity.class.getSimpleName();

    private boolean turningLeft, turningRight, goingUp, goingDown, thrusting, firing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);
        setOnTouchListners();
    }

    private void setOnTouchListners() {

        int[] buttons = new int[]{R.id.up_button, R.id.down_button, R.id.left_button, R.id.right_button, R.id.thrust_button, R.id.fire_button};

        for(int i = 0; i < buttons.length; i++) {
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

        switch (id) {
            case R.id.left_button:
                turningLeft = true;
                break;
            case R.id.right_button:
                turningRight = true;
                break;
            case R.id.up_button:
                goingUp = true;
                break;
            case R.id.down_button:
                goingDown = true;
                break;
            case R.id.thrust_button:
                thrusting = true;
                break;
            case R.id.fire_button:
                firing = true;
                break;
            default:
                break;
        }
        Log.v(TAG, toString());
    }

    private void handleUp(int id) {

        switch (id) {
            case R.id.left_button:
                turningLeft = false;
                break;
            case R.id.right_button:
                turningRight = false;
                break;
            case R.id.up_button:
                goingUp = false;
                break;
            case R.id.down_button:
                goingDown = false;
                break;
            case R.id.thrust_button:
                thrusting = false;
                break;
            case R.id.fire_button:
                firing = false;
                break;
            default:
                break;
        }
        Log.v(TAG, toString());
    }

    @Override
    public String toString() {
        return "GameActivity{" +
                "turningLeft=" + turningLeft +
                ", turningRight=" + turningRight +
                ", goingUp=" + goingUp +
                ", goingDown=" + goingDown +
                ", thrusting=" + thrusting +
                ", firing=" + firing +
                '}';
    }
}