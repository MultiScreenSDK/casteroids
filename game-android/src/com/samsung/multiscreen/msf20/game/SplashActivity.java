package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.samsung.multiscreen.msf20.game.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.game.utils.ThreadUtils;

/**
 * Splash Screen for the game.
 *
 * @author Nik Bhattacharya
 *
 */
public class SplashActivity extends Activity {

    private GameConnectivityManager connectivityManager = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        // Get an instance of the ConnectivtyManager and start discovery of compatible Samsung SmartTVs.
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());
        connectivityManager.startDiscovery();

        //Give the connectivity manager a bit of time to find the TV
        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                launchActivity();
            }
        }, 1500);
    }

    private void launchActivity() {
        Intent mainActivityIntent = new Intent();
        mainActivityIntent.setClass(this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

}
