package com.samsung.multiscreen.msf20.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.samsung.multiscreen.msf20.game.model.ConnectivityManager;
import com.samsung.multiscreen.msf20.game.utils.ThreadUtils;

/**
 * Splash Screen for the game.
 *
 * @author Nik Bhattacharya
 *
 */
public class SplashActivity extends Activity {

    private ConnectivityManager connectivityManager = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        // Create the ConnectivtyManager and start discovery of compatible Samsung SmartTVs.
        connectivityManager = ConnectivityManager.getInstance(getApplicationContext());
        connectivityManager.startDiscovery();
        
        // temporary
        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                launchActivity();
            }
        }, 1500);
    }

    private void launchActivity() {
        boolean isConnected = true; // FIXME
        if (isConnected) {
            Intent mainActivityIntent = new Intent();
            mainActivityIntent.setClass(this, MainActivity.class);
            startActivity(mainActivityIntent);
        } else {
            Intent connectActivityIntent = new Intent();
            connectActivityIntent.setClass(this, MainActivity.class); // FIXME
            
            startActivity(connectActivityIntent);
        }
    }

}
