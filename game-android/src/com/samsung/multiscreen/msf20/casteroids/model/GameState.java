package com.samsung.multiscreen.msf20.casteroids.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains game state data collected from events received from the TV Application.<br>
 * <br>
 * This class is intended to be used when a screen is initializing and then that screen should register for and process
 * updates.
 * 
 * @author Dan McCafferty
 * 
 */
public class GameState {

    // The current slot data list received from the TV application.
    private List<SlotData> slotData = null;

    // The current join response data received from the TV application.
    private JoinResponseData joinResponseData = null;
    
    // The current game start count down seconds received from the TV application.
    private int gameStartCountDownSeconds = -1;

    // The current score data list received from the TV application.
    private List<ScoreData> scoreData = null;

    // The current player out count down seconds received from the TV application.
    private int playerOutCountDownSeconds = -1;

    /**
     * Constructor
     */
    public GameState() {
        super();
        
        // FIXME: Remove. Added here so UI can use it while the TV application is being updated.
        slotData = new ArrayList<SlotData>();
        for (Color color : Color.values()) {
        	slotData.add(new SlotData(true, color));
        }
    }

    /**
     * Called when updated slot data is received.
     * 
     * @param slotData
     */
    protected void onSlotData(List<SlotData> slotData) {
        this.slotData = slotData;
    }

    /**
     * Called when a join response data is received.
     * 
     * @param joinResponseData
     */
    protected void onJoinResponse(JoinResponseData joinResponseData) {
    	this.joinResponseData = joinResponseData;
    }
    
    /**
     * Called when a game is starting.
     * 
     * @param gameStartCountDownSeconds
     */
    protected void onGameStart(int gameStartCountDownSeconds) {
        this.gameStartCountDownSeconds = gameStartCountDownSeconds;
        this.scoreData = null;
        this.playerOutCountDownSeconds = -1;
    }

    /**
     * Called when the game is over.
     * 
     * @param scoreData
     */
    protected void onGameOver(List<ScoreData> scoreData) {
    	this.joinResponseData = null;
        this.gameStartCountDownSeconds = -1;
        this.scoreData = scoreData;
        this.playerOutCountDownSeconds = -1;
    }

    /**
     * Called when the player's space craft was blown to smithereens (i.e. destroyed) and is out of the game.
     * 
     * @param playerOutCountDownSeconds
     */
    protected void onPlayerOut(int playerOutCountDownSeconds) {
        this.playerOutCountDownSeconds = playerOutCountDownSeconds;
    }

    /**
     * Returns the current slot data list received from the TV application or null if not applicable.<br>
     * <br>
     * This is intended to be used when a screen is initializing and then that screen should register for and process
     * updates.
     * 
     * @return
     */
    public List<SlotData> getSlotData() {
        return slotData;
    }

    /**
     * The current join response data received from the TV application.
     * 
     * @return
     */
    public JoinResponseData getJoinResponseData() {
    	return joinResponseData;
    }
    
    /**
     * Returns the current game start count down seconds received from the TV application or -1 if not applicable.<br>
     * <br>
     * This is intended to be used when a screen is initializing and then that screen should register for and process
     * updates.
     * 
     * @return
     */
    public int getGameStartCountDownSeconds() {
        return gameStartCountDownSeconds;
    }

    /**
     * Returns the current score data list received from the TV application or null if not applicable.<br>
     * <br>
     * This is intended to be used when a screen is initializing and then that screen should register for and process
     * updates.
     * 
     * @return
     */
    public List<ScoreData> getScoreData() {
        return scoreData;
    }

    /**
     * Returns the current player out count down seconds received from the TV application or -1 if not applicable.<br>
     * <br>
     * This is intended to be used when a screen is initializing and then that screen should register for and process
     * updates.
     * 
     * @return
     */
    public int getPlayerOutCountDownSeconds() {
        return playerOutCountDownSeconds;
    }

    @Override
    public String toString() {
        return "GameState [slotData=" + slotData + ", gameStartCountDownSeconds=" + gameStartCountDownSeconds
                + ", scoreData=" + scoreData + ", playerOutCountDownSeconds=" + playerOutCountDownSeconds + "]";
    }
}
