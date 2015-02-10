package com.samsung.multiscreen.msf20.game;

import android.app.Application;
import android.util.Log;

import com.samsung.multiscreen.msf20.game.utils.ThreadUtils;

/**
 *
 * Game Application singleton.
 *
 * @author Nik Bhattacharya
 */
public class GameApplication extends Application {

    private static final String TAG = GameApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        if(BuildConfig.DEBUG) Log.d(TAG, "Game Application created");

        //store a reference to the UI thread
        ThreadUtils.setUiThread(getMainLooper());

        if(!ThreadUtils.runningOnUiThread()) {
            throw new RuntimeException("The Application needs to be running on the UI Thread");
        }
    }


}
