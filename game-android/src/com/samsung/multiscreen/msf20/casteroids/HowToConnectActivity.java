package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samsung.multiscreen.msf20.connectivity.ConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.UPNPConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.UPNPDevice;

import java.util.List;

/**
 *
 * How to Play instructions are shown in a ViewPager so the user can swipe
 * through the various sections.
 *
 * @author Nik Bhattacharya
 */
public class HowToConnectActivity extends Activity {

    public static final String EXTRA_WIFI_NAME = "howtoconnect.wifi_name";
    private ListView discoveredDevicesListView;

    Typeface customTypeface;


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
        TextView discoveredTextView = (TextView) findViewById(R.id.how_to_connect_discovered_text);
        TextView supportedTextView = (TextView) findViewById(R.id.how_to_connect_help_text);

        customTypeface = ((GameApplication) getApplication()).getCustomTypeface();
        titleTextView.setTypeface(customTypeface);
        discoveredTextView.setTypeface(customTypeface);
        supportedTextView.setTypeface(customTypeface);

        discoveredDevicesListView = (ListView) findViewById(R.id.discovered_devices_list);

        String wifi_name = getIntent().getStringExtra(EXTRA_WIFI_NAME);

        List<UPNPDevice> discoveredDevices = UPNPConnectivityManager.getInstance(this).getDiscoveredDevices();
        if(discoveredDevices.size()>1) {
            TypefacedArrayAdapter typefacedArrayAdapter = new TypefacedArrayAdapter(this, discoveredDevices);
            discoveredDevicesListView.setAdapter(typefacedArrayAdapter);

            if(wifi_name != null) {
                String appended_label = discoveredTextView.getText() +" " + wifi_name;
                discoveredTextView.setText(appended_label);
                discoveredTextView.setVisibility(View.VISIBLE);
            }
        } else {
            discoveredDevicesListView.setVisibility(View.GONE);
            discoveredTextView.setVisibility(View.INVISIBLE);
        }


    }

    /******************************************************************************************************************
     * Inner Classes
     */
    private class TypefacedArrayAdapter extends ArrayAdapter<UPNPDevice> {

        public TypefacedArrayAdapter(Context context, List<UPNPDevice> objects) {
            super(context, R.layout.list_item, R.id.item_label, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout itemView = (RelativeLayout) getLayoutInflater().inflate(R.layout.list_item, parent, false);
            if(itemView != null) {
                TextView labelView = (TextView) itemView.findViewById(R.id.item_label);
                TextView serialView = (TextView) itemView.findViewById(R.id.item_serial);
                labelView.setTypeface(customTypeface);
                serialView.setTypeface(customTypeface);
                UPNPDevice current = getItem(position);
                labelView.setText(current.getFriendlyName());
                serialView.setText(current.getSerialNumber());
            }
            return itemView;
        }
    }
}
