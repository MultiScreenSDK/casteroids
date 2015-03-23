package com.samsung.multiscreen.msf20.casteroids;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.samsung.multiscreen.msf20.casteroids.model.*;
import com.samsung.multiscreen.msf20.connectivity.ConnectivityListener;
import com.samsung.multiscreen.msf20.connectivity.MessageListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * GameOver page for the game. This screen shows the scores for the various players
 * who participated in the game as well as allows the user to go back to the main screen.
 *
 * @author Nik Bhattacharya
 *
 */
public class GameOverActivity extends Activity implements ConnectivityListener, MessageListener {

    private static final String TAG = GameOverActivity.class.getSimpleName();

    /** Reference to the connectivity manager */
    private GameConnectivityManager connectivityManager = null;

    /** References to buttons on the screen */
    private Button mainScreenButton;

    /** Reference to the custom typeface for the game */
    private Typeface customTypeface;

    /** Reference to the root view */
    private View rootView;

    /** Reference to the text labels */
    private TextView gameOverLabel, winnerLabel, instructionsText;

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

        // reference to the winner label
        winnerLabel = (TextView) findViewById(R.id.winner_label);

        //get a reference to the scores table
        tableLayout = (TableLayout)findViewById(R.id.scores_table);

        instructionsText = (TextView) findViewById(R.id.instructions_text);


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
        winnerLabel.setTypeface(customTypeface);
        instructionsText.setTypeface(customTypeface);

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
        connectivityManager.registerMessageListener(this, Event.GAME_START);

        //capture the current state of the connection and show on the UI
        bindViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister self as a listener
        connectivityManager.unregisterConnectivityListener(this);
        connectivityManager.unregisterMessageListener(this, Event.GAME_START);
    }

    @Override
    public void onBackPressed() {
        launchIntent(MainActivity.class);
    }

    /******************************************************************************************************************
     * Connectivity and Game Message Listeners
     */

    @Override
    public void onConnectivityUpdate(int eventId) {
        bindViews();
    }

    @Override
    public void onMessage(String event, String data, byte[] payload) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Received event '" + event + "'");
        }
        if (event.equals(Event.GAME_START.getName())){
            //show countdown
            int numSeconds = MessageDataHelper.decodeGameStartCountDownSeconds(data);


            //show a toast for any non 0 wait time.
            if(numSeconds != 0) {
                instructionsText.setVisibility(View.VISIBLE);
                instructionsText.setText(getStyledString("Game starting in " + numSeconds + ((numSeconds == 1) ? " second" : " seconds")));
            } else {
                instructionsText.setVisibility(View.INVISIBLE);
                Intent gameControllerActivity = new Intent();
                gameControllerActivity.setClass(this, GameControllerActivity.class);
                gameControllerActivity.putExtra("color", connectivityManager.getGameState().getJoinResponseData().getColor().getColorInt());
                startActivity(gameControllerActivity);
                finish();
            }
        }
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
        ScoreData highscore = null;
        List<ScoreData> scoreDataList = gameState.getScoreData();
        if(scoreDataList == null || scoreDataList.size() < 1) {
            return;
        }
        Collections.sort(scoreDataList);
        for (int i=0; i<scoreDataList.size(); i++) {
            ScoreData data = scoreDataList.get(i);
            if(highscore == null || data.getScore() > highscore.getScore()) {
                highscore = data;
            } else {
                tableLayout.addView(createPlayerRow(data), new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            }
        }
        if(highscore != null) {
            String winnerText = highscore.getName() + " " + highscore.getScore();
            winnerLabel.setText(winnerText);
            winnerLabel.setTextColor(highscore.getColor().getColorInt());
            gameOverLabel.setTextColor(highscore.getColor().getColorInt());
            // Check for first place ties
            if(scoreDataList.size() > 1 && highscore.getScore() == scoreDataList.get(1).getScore()) {
                tableLayout.addView(createPlayerRow(highscore), 0, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                winnerLabel.setVisibility(View.INVISIBLE);
                gameOverLabel.setVisibility(View.INVISIBLE);
            } else {
                winnerLabel.setVisibility(View.VISIBLE);
                gameOverLabel.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "High score is null");
        }
    }

    private TableRow createPlayerRow(ScoreData score) {
        TableRow tr = (TableRow)getLayoutInflater().inflate(R.layout.view_score, null);//get references to the various cells in the table row
        TextView positionText = (TextView) tr.findViewById(R.id.position_text);
        TextView nameText = (TextView) tr.findViewById(R.id.name_text);
        TextView scoreText = (TextView) tr.findViewById(R.id.score_text);

        //bind the color
        int playerColor = score.getColor().getColorInt();
        positionText.setTextColor(playerColor);
        nameText.setTextColor(playerColor);
        scoreText.setTextColor(playerColor);

        //bind the font
        positionText.setTypeface(customTypeface);
        nameText.setTypeface(customTypeface);
        scoreText.setTypeface(customTypeface);

        //bind the values
        //positionText.setText("" + (i+1)); //number, hence the empty quotes for coercion to a string
        nameText.setText(score.getName());
        scoreText.setText("" + score.getScore()); //number, hence the empty quotes for coercion to a string

        return tr;
    }

    private void onMainScreenButtonClick() {
		launchIntent(MainActivity.class);
	}

    private void launchIntent(Class cls){

        connectivityManager.sendQuitMessage();
        connectivityManager.disconnect();

        //launch the main screen
        Intent intent = new Intent();
        intent.setClass(this, cls);
        startActivity(intent);

        finish();
    }

    /**
     * An example of how to use a Spannable in Android to style specific
     * sections of a String.
     *
     * @param string the string to be styled
     * @return the spannable with the styles embedded
     */
    private Spannable getStyledString(String string) {

        Spannable spannable = new SpannableString(string);
        ArrayList<Integer> spans = new ArrayList<Integer>(spannable.length());
        for(int i = 0; i < spannable.length(); i++){
            if(Character.isDigit(spannable.charAt(i))){
                spans.add(new Integer(i));
            }
        }
        for(int j = 0; j < spans.size(); j++) {
            int index = spans.get(j).intValue();
            spannable.setSpan(new RelativeSizeSpan(1.7f), index, index+1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(connectivityManager.getGameState().getJoinResponseData().getColor().getColorInt()), index, index+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), index, index+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

}
