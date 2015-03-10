package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.samsung.multiscreen.msf20.casteroids.model.GameConnectivityManager;
import com.samsung.multiscreen.msf20.casteroids.model.GameState;
import com.samsung.multiscreen.msf20.casteroids.model.ScoreData;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;

import java.util.List;

/**
 * GameOver page for the game. This screen shows the scores for the various players
 * who participated in the game as well as allows the user to go back to the main screen.
 *
 * @author Nik Bhattacharya
 *
 */
public class GameOverActivity extends Activity implements ConnectivityListener {

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button mainScreenButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    /** Reference to the game over label */
    private TextView gameOverLabel;

    /** Table that holds all the scores */
    TableLayout tableLayout;

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
        setContentView(R.layout.activity_game_over);

        // Get an instance of the ConnectivtyManager
        connectivityManager = GameConnectivityManager.getInstance(getApplicationContext());

        //get the custom typeface from the application
        customTypeface = ((GameApplication)getApplication()).getCustomTypeface();

        //get a reference to the root view
        rootView = findViewById(R.id.root_view);

        //reference to game over label
        gameOverLabel = (TextView) findViewById(R.id.game_over_label);

        //get a reference to the scores table
        tableLayout = (TableLayout)findViewById(R.id.scores_table);

        // Initialize the play button
        mainScreenButton = (Button) findViewById(R.id.main_screen_button);
        mainScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMainScreenButtonClick();
            }
        });

        //set the various buttons with the typeface
        mainScreenButton.setTypeface(customTypeface);
        gameOverLabel.setTypeface(customTypeface);

        //if we are lollipop, do a custom animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            runLollipopCode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register for connectivity updates.
        connectivityManager.registerConnectivityListener(this);

        //capture the current state of the connection and show on the UI
        bindViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);
    }


    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */

    @Override
    public void onConnectivityUpdate(int eventId) {
        bindViews();
    }


    /******************************************************************************************************************
     * Private methods
     */

    /**
     * Android 5.0 (Lollipop) specific code here.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void runLollipopCode() {
        rootView.setVisibility(View.INVISIBLE);

        //show how we can support material design
        rootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            public boolean onPreDraw() {
                rootView.getViewTreeObserver().removeOnPreDrawListener(this);
                rootView.setVisibility(View.VISIBLE);

                Animator anim = ViewAnimationUtils.createCircularReveal(rootView, rootView.getWidth() / 2, rootView.getHeight() / 2, 0, rootView.getWidth());
                anim.setDuration(1000);
                anim.start();

                return false;
            }
        });
    }

    /**
     * Show the game scores in the table layout.
     */
    private void bindViews(){

        tableLayout.removeAllViews();

        GameState gameState = connectivityManager.getGameState();
        List<ScoreData> scoreDataList = gameState.getScoreData();
        for (int i=0; i<scoreDataList.size(); i++){
            TableRow tr = (TableRow)getLayoutInflater().inflate(R.layout.view_score, null);

            ScoreData data = scoreDataList.get(i);

            //get references to the various cells in the table row
            TextView positionText = (TextView) tr.findViewById(R.id.position_text);
            TextView nameText = (TextView) tr.findViewById(R.id.name_text);
            TextView scoreText = (TextView) tr.findViewById(R.id.score_text);

            //bind the color
            int playerColor = data.getColor().getColorInt();
            positionText.setTextColor(playerColor);
            nameText.setTextColor(playerColor);
            scoreText.setTextColor(playerColor);

            //bind the font
            positionText.setTypeface(customTypeface);
            nameText.setTypeface(customTypeface);
            scoreText.setTypeface(customTypeface);

            //bind the values
            //positionText.setText("" + (i+1)); //number, hence the empty quotes for coercion to a string
            nameText.setText(data.getName());
            scoreText.setText("" + data.getScore()); //number, hence the empty quotes for coercion to a string

            tableLayout.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void onMainScreenButtonClick() {
		launchIntent(MainActivity.class);
	}

    private void launchIntent(Class cls){
        //don't keep us in the back stack
        finish();

        //launch the main screen
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);
    }


}
