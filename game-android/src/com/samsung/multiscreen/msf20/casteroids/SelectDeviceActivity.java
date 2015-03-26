package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.multiscreen.Service;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

import java.util.List;

/**
 * Activity that lets the user connect to a Device. Shown as a Dialog to the user
 * using the Theme.Dialog style.
 *
 * @author Nik Bhattacharya
 * @author Ali Khan
 * @author Adrian Hernandez
 */
public class SelectDeviceActivity extends Activity implements ConnectivityListener, AdapterView.OnItemClickListener {

    public static final int RESULT_ERROR = -2;
    public static final String SELECTED_SERVICE_KEY = "SELECTED_SERVICE";

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** Reference to the label to display to the user for any messaging */
    private TextView userMessageText;

    /** Reference to the List of TVs */
    private ListView tvList;

    /** List Adapter that holds the names of all the available services */
    private ArrayAdapter<Service> avblServices = null;

    /** Reference to the custom typeface */
    private Typeface customTypeface;


    /******************************************************************************************************************
     * Android Lifecycle methods
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_select_device);

        //Get an instance of the ConnectivtyManager and register for connectivity updates.
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        customTypeface = ((GameApplication) getApplication()).getCustomTypeface();

        //Store UI references
        userMessageText = (TextView) findViewById(R.id.select_tv_text);

        tvList = (ListView) findViewById(R.id.select_device_list);

        //set the listener on the list view
        tvList.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectivityManager.registerConnectivityListener(this);

        // Make sure the connectivity manager is in the correct state.
        bindList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectivityManager.unregisterConnectivityListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(avblServices != null) {
            try {
                String serviceName = avblServices.getItem(position).getName();
                setReturnValue(SelectDeviceActivity.RESULT_OK, serviceName);
            } catch (Exception ex) {
                setReturnValue(SelectDeviceActivity.RESULT_ERROR, null);
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        setReturnValue(SelectDeviceActivity.RESULT_CANCELED, null);
        finish();
    }


    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */


    @Override
    public void onConnectivityUpdate(int eventId) {


        switch (eventId) {
            case DISCOVERY_STOPPED:
                // Restart discovery as long as we don't have a discovered service.
                // TODO: Allow the user to refresh
                if (!connectivityManager.hasDiscoveredService()) {
                    connectivityManager.startDiscovery();
                }
                break;
            case DISCOVERY_FOUND_SERVICE:
                bindList();
                break;
            case DISCOVERY_LOST_SERVICE:
                bindList();
                break;
            case APPLICATION_CONNECTED:
            case APPLICATION_DISCONNECTED:
            case APPLICATION_CONNECT_FAILED:
                // Nothing needs to happen here, the main activity should handle these cases
                break;
        }
    }


    /******************************************************************************************************************
     * Private methods
     */


    /**
     * Binds the list view with the discovered services.
     */
    private void bindList() {
        List<Service> services = connectivityManager.getDiscoveredServices();

        // If we have at least one service, update the adapter
		if ((services != null) && (services.size() > 0)) {
			// bind the list view with the services
			avblServices = new TypefacedArrayAdapter(this, services);
			tvList.setAdapter(avblServices);
		} 
		// Else there aren't any services, treat as an implicit cancel.
		else {
			setReturnValue(SelectDeviceActivity.RESULT_CANCELED, null);
			finish();
		}
    }

    private void setReturnValue(int result, String selectedService){
        Intent intent = new Intent();
        intent.putExtra(SELECTED_SERVICE_KEY, selectedService);
        setResult(result, intent);
    }

    /******************************************************************************************************************
     * Inner Classes
     */
    private class TypefacedArrayAdapter extends ArrayAdapter<Service> {

        public TypefacedArrayAdapter(Context context, List<Service> objects) {
            super(context, R.layout.list_item, R.id.item_label, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_tv, parent, false);
            }

            View itemView = convertView;
            if(itemView != null) {
                TextView labelView = (TextView) itemView.findViewById(R.id.item_label);
                TextView serialView = (TextView) itemView.findViewById(R.id.item_serial);
                labelView.setTypeface(customTypeface);
                serialView.setTypeface(customTypeface);
                Service current = getItem(position);
                labelView.setText((current.getType() != null? "[" + current.getType() + "] ": "") + current.getName());
                //serialView.setText(current.getId());
            }
            return itemView;
        }
    }
}
