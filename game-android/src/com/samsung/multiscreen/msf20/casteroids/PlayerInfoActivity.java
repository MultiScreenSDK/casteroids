package com.samsung.multiscreen.msf20.casteroids;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Color;
import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.JoinResponseData;
import com.samsung.multiscreen.msf20.casteroids.model.MessageDataHelper;
import com.samsung.multiscreen.msf20.casteroids.model.SlotData;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

import java.util.List;

/**
 *
 * Screen where player enters name and chooses Color.
 *
 * @author Nik Bhattacharya
 *
 */

public class PlayerInfoActivity extends Activity implements ConnectivityListener, MessageListener, View.OnClickListener{

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** Reference to the root view */
    private View rootView;

    /** Reference to the initial background color of the root view */
    private int rootViewDefaultBackgoundColor;

    /** References to buttons on the screen */
    private Button playButton, color1Button, color2Button, color3Button, color4Button;

    /** Reference to the name EditText */
    private EditText nameText;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the slot selected by the player */
    private SlotData selectedSlotData = null;

    /** Player info preferences */
    SharedPreferences prefs = null;

    /** Reference to an ARGB animation evaluator that is cached for performance reasons */
    ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //content view
        setContentView(R.layout.activity_player_info);

        //shared prefs
        prefs = getSharedPreferences("playerinfo", Context.MODE_PRIVATE);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //Get reference to the root view
        rootView = findViewById(R.id.root_view);

        //set the root views initial background color
        rootViewDefaultBackgoundColor = this.getResources().getColor(R.color.blue_grey_500);

        //Get references to the buttons
        playButton = (Button) findViewById(R.id.play_button);
        color1Button = (Button) findViewById(R.id.color1_button);
        color2Button = (Button) findViewById(R.id.color2_button);
        color3Button = (Button) findViewById(R.id.color3_button);
        color4Button = (Button) findViewById(R.id.color4_button);

        //Initialize the name edit text
        nameText = (EditText) findViewById(R.id.name_text);
        nameText.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

        //set the type on the various ui elements
        color1Button.setTypeface(customTypeface);
        color2Button.setTypeface(customTypeface);
        color3Button.setTypeface(customTypeface);
        color4Button.setTypeface(customTypeface);
        playButton.setTypeface(customTypeface);
        nameText.setTypeface(customTypeface);

        //attach listeners
        playButton.setOnClickListener(this);
        color1Button.setOnClickListener(this);
        color2Button.setOnClickListener(this);
        color3Button.setOnClickListener(this);
        color4Button.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //register for connectivity changes
        connectivityManager.registerConnectivityListener(this);

        //register for SLOT changes
        connectivityManager.registerMessageListener(this, Event.SLOT_UPDATE, Event.JOIN_REQUEST, Event.JOIN_RESPONSE);

        //rebind the data
        bindAvailableSlots();

        //put in the player name if it was previously saved
        nameText.setText(getPlayerNameFromPreferences());
        nameText.setSelection(nameText.getText().length());
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister for connectivity changes
        connectivityManager.unregisterConnectivityListener(this);

        //unregister for SLOT changes
        connectivityManager.unregisterMessageListener(this, Event.SLOT_UPDATE, Event.JOIN_REQUEST, Event.JOIN_RESPONSE);
    }


    @Override
    public void onConnectivityUpdate(int eventId) {
        switch (eventId) {
            case DISCOVERY_STOPPED:
            case DISCOVERY_FOUND_SERVICE:
            case DISCOVERY_LOST_SERVICE:
            case APPLICATION_CONNECTED:
                // Ignore
                break;
            case APPLICATION_DISCONNECTED:
            case APPLICATION_CONNECT_FAILED:
                //Notify the user that the connection was lost.
                Toast.makeText(this, "Lost connection.", Toast.LENGTH_SHORT).show();
                // We lost connect, return to the main activity.
                finish();
                break;
        }
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        if(event.equals(Event.JOIN_REQUEST.getName())){
            Toast.makeText(this, "Joining game...", Toast.LENGTH_SHORT).show();
        } else if (event.equals(Event.JOIN_RESPONSE.getName())){

            JoinResponseData joinResponseData = MessageDataHelper.decodeJoinResponseData(data);
            if(joinResponseData.isSuccessful()){
                startGame(joinResponseData);
            } else {
                Toast.makeText(this, "Couldn't join game. Try again.", Toast.LENGTH_SHORT).show();
                bindAvailableSlots();
            }
        } else if (event.equals(Event.SLOT_UPDATE.getName())){
            bindAvailableSlots();
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        switch (viewId) {
            case R.id.color1_button:
                selectColorForSlot((SlotData) color1Button.getTag());
                break;
            case R.id.color2_button:
                selectColorForSlot((SlotData) color2Button.getTag());
                break;
            case R.id.color3_button:
                selectColorForSlot((SlotData) color3Button.getTag());
                break;
            case R.id.color4_button:
                selectColorForSlot((SlotData) color4Button.getTag());
                break;
            case R.id.play_button:
                if(checkUserSelections()) {
                    connectivityManager.sendJoinRequestMessage(nameText.getText().toString(), selectedSlotData.getColor());
                }
            default:
                break;
        }
    }

    private void selectColorForSlot(SlotData slotData) {

        //get the old slot data color
        int prevColor = this.selectedSlotData != null ? this.selectedSlotData.getColor().getColorInt() : rootViewDefaultBackgoundColor;

        //set the new slot data
        this.selectedSlotData = slotData;

        //get the new color
        int newColor = slotData.getColor().getColorInt();

        //run an animation changing the color from old to new
        animateBackgroundColor(prevColor, newColor);
    }

    private void animateBackgroundColor(int startColor, int endColor) {
        //run an animation changing the color from old to new
        ObjectAnimator colorFade = ObjectAnimator.ofObject(rootView, "backgroundColor", argbEvaluator, startColor, endColor);
        colorFade.setDuration(600);
        colorFade.start();
    }

    private void bindAvailableSlots() {

        Button[] buttons = new Button[]{color1Button, color2Button, color3Button, color4Button};

        List<SlotData> data = connectivityManager.getGameState().getSlotData();

        for (int i=0; i < data.size(); i++) {
            SlotData slot = data.get(i);

            //in case the current user selection is no longer valid
            if(!slot.isAvailable() && selectedSlotData != null && slot.equals(selectedSlotData)) {

                //animate the background back to the default color first
                animateBackgroundColor(selectedSlotData.getColor().getColorInt(), rootViewDefaultBackgoundColor);

                //clear out the selection
                selectedSlotData = null;

                //message the user
                Toast.makeText(this, "The Color you selected is no longer available. Please select another one.", Toast.LENGTH_SHORT).show();
            }

            Button b = buttons[i];
            Color c = slot.getColor();

            b.setText(c.getName().toUpperCase());
            b.setTextColor(c.getColorInt());
            b.setTag(slot);

            b.setEnabled(slot.isAvailable());
        }
    }

    private boolean checkUserSelections() {
        String selectedName = nameText.getText().toString();
        if(TextUtils.isEmpty(selectedName)) {
            Toast.makeText(this, "You must input your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(selectedSlotData == null) {
            Toast.makeText(this, "You must choose a color", Toast.LENGTH_SHORT).show();
            return false;
        }


        return true;
    }

    private void startGame(JoinResponseData data) {

        writePlayerNameToPreferences(data.getName());

        Intent intent = new Intent();
        intent.putExtra("color", data.getColor().getColorInt());
        intent.setClass(this, GameControllerActivity.class);
        startActivity(intent);

        //don't keep ourselves around
        finish();
    }

    private void writePlayerNameToPreferences(String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.apply();
    }

    private String getPlayerNameFromPreferences(){
        return prefs.getString("name", "");
    }



}
