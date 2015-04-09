package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 *
 * Information relating to which TVs are supported for the multi-screen API.
 *
 * @author Adrian Hernandez
 * @author Nik Bhattacharya
 */
public class HowToConnectActivity extends Activity {

    /** Reference to custom typeface */
    private Typeface customTypeface;


    /******************************************************************************************************************
     * Android Lifecycle methods
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //xml layout
        setContentView(R.layout.activity_how_to_connect);

        TextView titleTextView = (TextView) findViewById(R.id.how_to_connect_title_text);

        customTypeface = ((GameApplication) getApplication()).getCustomTypeface();
        titleTextView.setTypeface(customTypeface);
    }
}
