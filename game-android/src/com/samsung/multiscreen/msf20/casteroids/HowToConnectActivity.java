package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 *
 * How to Play instructions are shown in a ViewPager so the user can swipe
 * through the various sections.
 *
 * @author Nik Bhattacharya
 */
public class HowToConnectActivity extends Activity {


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
        TextView textView = (TextView) findViewById(R.id.how_to_connect_help);
        Typeface customTypeface = ((GameApplication) getApplication()).getCustomTypeface();
        textView.setTypeface(customTypeface);

    }

    /******************************************************************************************************************
     * Inner Classes
     */

}
