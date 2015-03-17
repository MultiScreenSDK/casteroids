package com.samsung.multiscreen.msf20.casteroids;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.multiscreen.msf20.casteroids.model.Color;
import com.samsung.multiscreen.msf20.casteroids.model.ConfigType;
import com.samsung.multiscreen.msf20.casteroids.model.ConfigTypeMap;
import com.samsung.multiscreen.msf20.casteroids.model.Event;
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.JoinResponseData;
import com.samsung.multiscreen.msf20.casteroids.model.MessageDataHelper;
import com.samsung.multiscreen.msf20.casteroids.model.SlotData;
import com.samsung.multiscreen.msf20.casteroids.views.CustomToast;
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

public class PlayerInfoActivity extends Activity implements ConnectivityListener, MessageListener, View.OnClickListener, View.OnTouchListener{

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** Reference to views */
    private View rootView;

    /** Reference to the ship view */
    private ImageView shipView;

    /** Reference to the initial background color of the root view */
    private int rootViewDefaultBackgoundColor;

    /** References to buttons on the screen */
    private Button playButton, color1Button, color2Button, color3Button, color4Button;

    /** */
    private ImageButton gameOptionsButton;

    /** References to the labels */
    private TextView enterNameText, selectShipText;

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

    /** Reference to the game config gameConfigDialog */
    private Dialog gameConfigDialog;

    /** Stroke size in pixels */
    private int strokeSize, strokeSizeWide;

    /** Button animator */
    private ObjectAnimator animator;

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

        //content view
        setContentView(R.layout.activity_player_info);

        //shared prefs
        prefs = getSharedPreferences("playerinfo", Context.MODE_PRIVATE);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the stroke size
        strokeSize = getResources().getDimensionPixelSize(R.dimen.ship_stroke);
        strokeSizeWide = getResources().getDimensionPixelSize(R.dimen.ship_stroke_wide);

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //Get reference to various views
        rootView = findViewById(R.id.root_view);
        shipView = (ImageView)findViewById(R.id.ship_view);
        enterNameText = (TextView)findViewById(R.id.name_label);
        selectShipText = (TextView) findViewById(R.id.color_label);

        //set the root views initial background color
        rootViewDefaultBackgoundColor = this.getResources().getColor(R.color.blue_grey_500);

        //Get references to the buttons
        playButton = (Button) findViewById(R.id.play_button);

        // Initialize the how to play button
        gameOptionsButton = (ImageButton) findViewById(R.id.game_options_button);
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
        enterNameText.setTypeface(customTypeface);
        selectShipText.setTypeface(customTypeface);

        //attach listeners
        playButton.setOnClickListener(this);
        gameOptionsButton.setOnClickListener(this);
        color1Button.setOnClickListener(this);
        color2Button.setOnClickListener(this);
        color3Button.setOnClickListener(this);
        color4Button.setOnClickListener(this);
        color1Button.setOnTouchListener(this);
        color2Button.setOnTouchListener(this);
        color3Button.setOnTouchListener(this);
        color4Button.setOnTouchListener(this);

        //create the animator
        animator = ObjectAnimator.ofFloat(gameOptionsButton, "rotation", 360);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(15000);


        //set the stroke color on the ship drawable
        setShipColor(shipView, 0x1affffff);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If not connected, exit this screen.
        if(!connectivityManager.isConnected()) {
        	finish();
        }
        
        //register for connectivity changes
        connectivityManager.registerConnectivityListener(this);

        //register for SLOT changes
        connectivityManager.registerMessageListener(this, Event.SLOT_UPDATE, Event.JOIN_REQUEST, Event.JOIN_RESPONSE, Event.CONFIG_UPDATE);

        //rebind the data
        bindAvailableSlots();

        //run the animation on the button
        animator.start();

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
        connectivityManager.unregisterMessageListener(this, Event.SLOT_UPDATE, Event.JOIN_REQUEST, Event.JOIN_RESPONSE, Event.CONFIG_UPDATE);

        animator.cancel();

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
                break;
            case R.id.game_options_button:
                showGameSettings();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        connectivityManager.disconnect();

        animator.cancel();
        super.onBackPressed();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int id = v.getId();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            handleEvent(id, true, v);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            handleEvent(id, false, v);
        }


        return false;
    }


    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */

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
                CustomToast.makeText(this, "Lost connection.", Toast.LENGTH_SHORT).show();
                // We lost connect, return to the main activity.
                finish();
                break;
        }
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        if(event.equals(Event.JOIN_REQUEST.getName())){
            CustomToast.makeText(this, "Joining game...", Toast.LENGTH_SHORT).show();
        } else if (event.equals(Event.JOIN_RESPONSE.getName())){

            JoinResponseData joinResponseData = MessageDataHelper.decodeJoinResponseData(data);
            if(joinResponseData.isSuccessful()){
                startGame(joinResponseData);
            } else {
                CustomToast.makeText(this, "Couldn't join game. Try again.", Toast.LENGTH_SHORT).show();
                bindAvailableSlots();
            }
        } else if (event.equals(Event.SLOT_UPDATE.getName())){
            bindAvailableSlots();
        } else  if(event.equals(Event.CONFIG_UPDATE.getName())){
            if(gameConfigDialog != null && gameConfigDialog.isShowing()) {
                Toast.makeText(this, "Another player edited the game configuration", Toast.LENGTH_SHORT).show();
                gameConfigDialog.dismiss();
            }
        }
    }


    /******************************************************************************************************************
     * Private methods
     */

    private void handleEvent(int viewId, boolean isDown, View v) {
        switch (viewId) {
            case R.id.color1_button:
            case R.id.color2_button:
            case R.id.color3_button:
            case R.id.color4_button:
                runAnimation(v, isDown);
                break;
            default:
                break;
        }
    }

    private void runAnimation(View v, boolean isDown){
        if(isDown){
            v.animate().scaleX(0.7f).scaleY(0.7f).setDuration(150).start();
        } else {
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
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
        animateShipAndTextColor(prevColor, newColor);


    }

    private void animateShipAndTextColor(int startColor, int endColor) {
        //run an animation changing the color from old to new
        ObjectAnimator colorFade = ObjectAnimator.ofObject(shipView, "colorFilter", argbEvaluator, startColor, endColor);
        colorFade.setDuration(600);
        //colorFade.start();

        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(startColor, endColor);
        valueAnimator.setEvaluator(argbEvaluator);
        valueAnimator.setDuration(800);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int color = (Integer)animation.getAnimatedValue();
                //set the edit text color
                nameText.setTextColor(color);
                setShipColor(shipView, color);
            }
        });
        valueAnimator.start();

    }

    private void setShipColor(ImageView shipView, int color) {
        shipView.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        setCustomBackgroundColor(shipView, color, strokeSizeWide);
    }


    /** This method expects that the view has a GradientDrawable shape background */
    private void setCustomBackgroundColor(View view, int color, int size) {
        GradientDrawable drawable = (GradientDrawable)view.getBackground();

        //get the individual rgb values
        int startR = (color >> 16) & 0xff;
        int startG = (color >> 8) & 0xff;
        int startB = color & 0xff;

        //replace the alpha channel with transparency 0x27
        int alphaColor = (int)(0x27 << 24) |
                (int)(startR << 16) |
                (int)(startG  << 8) |
                (int)(startB);

        //set the fill color to the alpha transparent color
        drawable.setColor(alphaColor);

        //set the stroke color
        drawable.setStroke(size, color);
        drawable.invalidateSelf();
    }

    private void bindAvailableSlots() {

        Button[] buttons = new Button[]{color1Button, color2Button, color3Button, color4Button};

        List<SlotData> data = connectivityManager.getGameState().getSlotData();

        for (int i=0; i < data.size(); i++) {
            SlotData slot = data.get(i);

            //in case the current user selection is no longer valid
            if(!slot.isAvailable() && selectedSlotData != null && /**(slot.equals(selectedSlotData))*/  slot.getColor().getColorInt() == selectedSlotData.getColor().getColorInt()) {

                //animate the background back to the default color first
                animateShipAndTextColor(selectedSlotData.getColor().getColorInt(), 0xffffffff);

                //clear out the selection
                selectedSlotData = null;

                //message the user
                CustomToast.makeText(this, "The Color you selected is no longer available. Please select another one.", Toast.LENGTH_SHORT).show();
            }

            Button button = buttons[i];
            Color c = slot.getColor();

            setCustomBackgroundColor(button, c.getColorInt(), strokeSize);
            button.setTextColor(c.getColorInt());
            button.setTag(slot);

            if(slot.isAvailable()) {
                button.setAlpha(1.0f);
            } else {
                button.setAlpha(0.1f);
            }
        }
    }

    private boolean checkUserSelections() {
        String selectedName = nameText.getText().toString();
        if(TextUtils.isEmpty(selectedName)) {
            CustomToast.makeText(this, "You must input your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(selectedSlotData == null) {
            CustomToast.makeText(this, "You must choose a color", Toast.LENGTH_SHORT).show();
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

    private void showGameSettings() {
        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this/*, R.style.AppDialog*/);

        //Get the info from the model
        final ConfigTypeMap configTypeMap = connectivityManager.getGameState().getConfigTypeMap();
        final ConfigType[] configTypes = configTypeMap.getConfigTypes();

        //Now massage the data in a way that the gameConfigDialog understands
        final CharSequence[] gameOptions = new CharSequence[configTypes.length];
        final boolean[] selectedOptions = new boolean[configTypes.length];

        for(int i=0; i< configTypes.length; i++) {
            ConfigType type = configTypes[i];
            gameOptions[i] = type.getDescription();
            selectedOptions[i] = configTypeMap.isEnabled(type);
        }

        //a java oddity here to enable an inner class to have a reference to a final variable
        final boolean[] isModified = new boolean[]{false};


        // Set the gameConfigDialog title
        builder.setTitle("Game Settings")
                .setMultiChoiceItems(gameOptions, selectedOptions, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        isModified[0] = true;
                        selectedOptions[which] = isChecked;
                    }
                })

                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isModified[0]) {
                            for (int i = 0; i < configTypes.length; i++) {
                                ConfigType type = configTypes[i];
                                //get the value from the array
                                configTypeMap.setIsEnabled(type, selectedOptions[i]);
                            }

                            //save the configuration
                            connectivityManager.sendConfigUpdate(configTypeMap);
                            CustomToast.makeText(getApplicationContext(), "Saved Options", Toast.LENGTH_SHORT).show();
                        }

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        gameConfigDialog = builder.create();
        gameConfigDialog.show();

    }



}
