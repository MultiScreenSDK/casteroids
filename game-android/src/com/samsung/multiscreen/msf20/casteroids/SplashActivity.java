package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.utils.ThreadUtils;

/**
 * Splash Screen for the Casteroids game.
 *
 * @author Nik Bhattacharya
 *
 */
public class SplashActivity extends Activity {

    /** Reference to the Connectivity Manager */
    private GameConnectivityManager connectivityManager = null;


    /******************************************************************************************************************
     * Android Lifecycle methods
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        TextView gameTitle = (TextView)findViewById(R.id.game_title);
        gameTitle.setTypeface(((GameApplication)getApplication()).getCustomTypeface());

        // Get an instance of the ConnectivtyManager and start discovery of compatible Samsung SmartTVs.
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        connectivityManager.startDiscovery();

        //Give the connectivity manager a bit of time to find the TV
        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                launchActivity();
            }
        }, 200);
    }


    /******************************************************************************************************************
     * Private methods
     */

    /**
     * Launch the appropriate activity from the splash screen.
     */
    private void launchActivity() {

        //create the animation
        View leftDoor = findViewById(R.id.left_door);
        View rightDoor = findViewById(R.id.right_door);


        //create the unlock animation
        View lock1 = findViewById(R.id.view_2);
        View lock2 = findViewById(R.id.view_3);

        ObjectAnimator unlockLock1 = ObjectAnimator.ofFloat(lock1, "rotation", -360);
        ObjectAnimator unlockLock2 = ObjectAnimator.ofFloat(lock2, "rotation", 720);

        int unlockDoorsDuration = 1200;

        AnimatorSet unlockDoors = new AnimatorSet();
        unlockDoors.playTogether(unlockLock1, unlockLock2);
        unlockDoors.setDuration(unlockDoorsDuration);
        unlockDoors.setInterpolator(new AccelerateInterpolator(1.0f));
        unlockDoors.start();

        //create the open doors animation
        int leftDoorWidth = leftDoor.getWidth();
        int rightDoorWidth = rightDoor.getWidth();
        ObjectAnimator openLeftDoor = ObjectAnimator.ofFloat(leftDoor, View.TRANSLATION_X, -leftDoorWidth);
        ObjectAnimator openRightDoor = ObjectAnimator.ofFloat(rightDoor, View.TRANSLATION_X, rightDoorWidth);

        AnimatorSet openDoors = new AnimatorSet();
        openDoors.playTogether(openLeftDoor, openRightDoor);
        openDoors.setDuration(1300);
        openDoors.setInterpolator(new AccelerateInterpolator(1.2f));
        openDoors.setStartDelay(unlockDoorsDuration + 250 /** slight extra delay after the doors unlock */);

        openDoors.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ThreadUtils.postOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Intent mainActivityIntent = new Intent();
                        mainActivityIntent.setClass(SplashActivity.this, MainActivity.class);
                        startActivity(mainActivityIntent);
                        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_up_anim);

                        finish();
                    }
                }, 500);
            }
        });

        openDoors.start();


    }

}
