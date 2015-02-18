package com.samsung.multiscreen.msf20.casteroids;

import android.app.Activity;
import android.content.Intent;
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
import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

/**
 *
 * Screen where player enters name and chooses Color.
 *
 * @author Nik Bhattacharya
 *
 */

public class PlayerInfoActivity extends Activity implements ConnectivityListener, View.OnClickListener{

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** Reference to the root view */
    private View rootView;

    /** References to buttons on the screen */
    private Button playButton, color1Button, color2Button, color3Button, color4Button;

    /** Reference to the name EditText */
    private EditText nameText;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the color selected by the player */
    private Color selectedColor = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //make full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //content view
        setContentView(R.layout.activity_player_info);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //Get reference to the root view
        rootView = findViewById(R.id.root_view);

        //Get references to the buttons
        playButton = (Button) findViewById(R.id.play_button);
        color1Button = (Button) findViewById(R.id.color1_button);
        color2Button = (Button) findViewById(R.id.color2_button);
        color3Button = (Button) findViewById(R.id.color3_button);
        color4Button = (Button) findViewById(R.id.color4_button);

        //Initialize the name edit text
        nameText = (EditText) findViewById(R.id.name_text);
        nameText.setFilters(new InputFilter[] {new InputFilter.AllCaps()});

        //attach listeners
        playButton.setOnClickListener(this);
        color1Button.setOnClickListener(this);
        color2Button.setOnClickListener(this);
        color3Button.setOnClickListener(this);
        color4Button.setOnClickListener(this);

        setColorButtonNames(color1Button, color2Button, color3Button, color4Button);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //register for connectivity changes
        connectivityManager.registerConnectivityListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //unregister for connectivity changes
        connectivityManager.unregisterConnectivityListener(this);
    }


    @Override
    public void onConnectivityUpdate(int eventId) {

    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        switch (viewId) {
            case R.id.color1_button:
                selectColor((Color) color1Button.getTag());
                break;
            case R.id.color2_button:
                selectColor((Color)color2Button.getTag());
                break;
            case R.id.color3_button:
                selectColor((Color)color3Button.getTag());
                break;
            case R.id.color4_button:
                selectColor((Color)color4Button.getTag());
                break;
            case R.id.play_button:
                if(checkUserSelections()) {
                    playGame();
                }
            default:
                break;
        }
    }

    private void selectColor(Color color) {
        this.selectedColor = color;
        rootView.setBackgroundColor(color.getColorInt());
    }

    private void setColorButtonNames(Button ...buttons) {

        Color[] values = Color.values();
        for(int i=0; i < values.length; i++) {
            Color c = values[i];
            Button b = buttons[i];
            b.setText(c.getName().toUpperCase());
            b.setTextColor(c.getColorInt());
            b.setTag(c);
        }

    }

    private boolean checkUserSelections() {
        String selectedName = nameText.getText().toString();
        if(TextUtils.isEmpty(selectedName)) {
            Toast.makeText(this, "You must input your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(selectedColor == null) {
            Toast.makeText(this, "You must choose a color", Toast.LENGTH_SHORT).show();
            return false;
        }


        return true;
    }

    private void playGame() {

        boolean joinedGame = connectivityManager.sendJoinMessage(nameText.getText().toString(), selectedColor);

        if(joinedGame) {

            Intent intent = new Intent();
            intent.setClass(this, GameActivity.class);
            startActivity(intent);

            //don't keep ourselves around
            finish();
        } else {
            Toast.makeText(this, "Could not join game. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
