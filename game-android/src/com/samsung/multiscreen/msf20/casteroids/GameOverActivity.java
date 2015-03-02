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

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

/**
 * GameOver page for the game. This screen shows the scores for the various players
 * who participated in the game as well as allows the user to go back to the main screen.
 *
 * @author Nik Bhattacharya
 *
 */
public class GameOverActivity extends Activity implements ConnectivityListener {

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button mainScreenButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //content view
        setContentView(R.layout.activity_game_over);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //get a reference to the root view
        rootView = findViewById(R.id.root_view);

        // Initialize the play button
        mainScreenButton = (Button) findViewById(R.id.main_screen_button);
        mainScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMainScreenButtonClick();
            }
        });



        //set the various buttons with the typeface
        mainScreenButton.setTypeface(customTypeface);

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

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);
    }



    private void onMainScreenButtonClick() {
		launchIntent(MainActivity.class);
	}

    private void launchIntent(Class cls){
        //don't keep us in the back stack
        finish();

        //launch the main screen
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
    }

    @Override
    public void onConnectivityUpdate(int eventId) {
		bindViews();
    }
}
