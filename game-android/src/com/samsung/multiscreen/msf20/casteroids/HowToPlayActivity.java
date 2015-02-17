package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
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
 * How to Play describes how to the game.
 *
 *
 * @author Nik Bhattacharya
 */
public class HowToPlayActivity extends Activity {

    /** Pages between the several how to play screens */
    private ViewPager viewPager;

    /** Page Adapter instantiates and destroys the how to play instruction screens */
    private HowToPlayPageAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //xml layout
        setContentView(R.layout.activity_how_to_play);

        //create the view pager adapter
        viewPagerAdapter = new HowToPlayPageAdapter();

        //store references
        viewPager = (ViewPager) findViewById(R.id.view_pager);

        //set the adapter on the view pager
        viewPager.setAdapter(viewPagerAdapter);

    }

    /******************************************************************************************************************
     * Inner Classes
     */

    /**
     * Returns the How to Play screens.
     *
     */
    private class HowToPlayPageAdapter extends PagerAdapter {

        private int[] howToPlayPages = new int[]{ R.color.blue_200, R.color.brown_200, R.color.green_200};

        @Override
        public int getCount() {
            return howToPlayPages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = container.getContext();
            TextView v = new TextView(context);
            v.setText("Page " + (position+1));
            v.setTextSize(50.0f);
            v.setTextColor(0xffffffff);
            v.setGravity(Gravity.CENTER);
            v.setBackgroundColor(context.getResources().getColor(howToPlayPages[position]));

            container.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }
    }
}
