package com.samsung.multiscreen.msf20.casteroids;

import android.app.Application;
import android.graphics.Typeface;
import android.util.Log;

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.utils.ThreadUtils;

/**
 *
 * Casteroids Game Application singleton.
 *
 * @author Nik Bhattacharya
 */
public class GameApplication extends Application {

    /** Debug tag */
    private static final String TAG = GameApplication.class.getSimpleName();

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** Custom typeface used in the application */
    private Typeface customTypeface, customTypeface2;

    /******************************************************************************************************************
     * Android Lifecycle methods
     */

    @Override
    public void onCreate() {
        super.onCreate();
        if(BuildConfig.DEBUG) Log.d(TAG, "Game Application created");

        //store a reference to the UI thread
        ThreadUtils.setUiThread(getMainLooper());

        if(!ThreadUtils.runningOnUiThread()) {
            throw new RuntimeException("The Application needs to be running on the UI Thread");
        }

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        createCustomTypefaces();
    }


    /******************************************************************************************************************
     * Public methods
     */

    /**
     * Returns the custom typeface for the application.
     *
     * @return custom typeface for the game.
     */
    public Typeface getCustomTypeface() {
        return customTypeface;
    }

    /******************************************************************************************************************
     * Private methods
     */

    /**
     * Creates the custom typefaces that will be used throughout the application. Doing
     * this in the Game Application class ensures that the font is loaded from the assets directory
     * just one time.
     *
     */
    private void createCustomTypefaces() {
        customTypeface = Typeface.createFromAsset(getAssets(), "fonts/audiowideregular.ttf");
    }




}
