package com.samsung.multiscreen.msf20.game;

import android.app.Application;
import android.util.Log;

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
    }


}
