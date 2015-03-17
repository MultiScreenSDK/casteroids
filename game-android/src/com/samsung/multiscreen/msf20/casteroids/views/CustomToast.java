package com.samsung.multiscreen.msf20.casteroids.views;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.GameApplication;
import com.samsung.multiscreen.msf20.casteroids.R;

/**
 *
 * Creates a stylized toast message.
 *
 * @author Nik Bhattacharya
 */
public class CustomToast {

    public static Toast makeText(Context context, String text, int duration) {
        Toast t = new Toast(context);
        t.setGravity(Gravity.CENTER, 0, 0);
        TextView v = new TextView(context);
        v.setPadding(30, 30, 30, 30);
        v.setBackgroundResource(R.drawable.toast_bg);
        v.setTypeface(((GameApplication)context.getApplicationContext()).getCustomTypeface());
        v.setTextColor(0xff00ade9);
        v.setTextSize(20);
        v.setText(text);
        t.setView(v);
        t.setDuration(duration);

        return t;
    }
}
