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
	private List<SlotData> slotDataList = null;

	// The current join response data received from the TV application.
	private JoinResponseData joinResponseData = null;

	// The current game start count down seconds received from the TV application.
	private int gameStartCountDownSeconds = -1;

	// The current score data list received from the TV application.
	private List<ScoreData> scoreDataList = null;

	// The current player out count down seconds received from the TV application.
	private int playerOutCountDownSeconds = -1;

	// The ConfifTypeMap used to enable/disable game features. This was added to assist in the performance tuning of the
	// TV Application.
	private ConfigTypeMap configTypeMap = new ConfigTypeMap();

	/**
	 * Constructor
	 */
	public GameState() {
		super();

		// Initialize the SlotData.
		initializeSlotData();
	}

	/**
	 * Called when the game is connected to an application.
	 */
	protected void onConnected() {
		// Reset the SlotData.
		initializeSlotData();
		
		// Reset the ConfigTypeMap since each time the TV Application is restarted it resets the config. 
		configTypeMap = new ConfigTypeMap();
	}

	/**
	 * Called when the game is connected to an application.
	 */
	protected void onDisconnected() {
		// Reset the SlotData.
		initializeSlotData();
	}

	/**
	 * Called when updated slot data is received.
	 * 
	 * @param slotData
	 */
	protected void onSlotData(List<SlotData> slotData) {
		this.slotDataList = slotData;
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
		this.scoreDataList = null;
		this.playerOutCountDownSeconds = -1;
	}

	/**
	 * Called when the game is over.
	 * 
	 * @param scoreData
	 */
	protected void onGameOver(List<ScoreData> scoreData) {
		this.gameStartCountDownSeconds = -1;
		this.scoreDataList = scoreData;
		this.playerOutCountDownSeconds = -1;
	}

    protected void onGameQuit() {
        this.joinResponseData = null;
    }

	/**
	 * Called when the configuration update was sent out by another client. This was added to assist in the performance
	 * tuning of the TV Application.
	 * 
	 * @param configTypeMap
	 */
	protected void onConfigUpdate(ConfigTypeMap configTypeMap) {
		this.configTypeMap = configTypeMap;
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
	 * Returns a flag indicating whether or not at least one Slot is available.<br>
	 * <br>
	 * This is intended to be used by the UI logic to determine whether or not the user can join the game.
	 * 
	 * @return
	 */
	public boolean hasAvailableSlot() {
		if (hasSlotData()) {
			for (SlotData slotData : slotDataList) {
				if (slotData.isAvailable()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Initialize the SlotData list by defaulting everything on. It will be updated as soon as the application connects
	 * with the TV Application.
	 */
	private void initializeSlotData() {
		slotDataList = new ArrayList<SlotData>();
		for (Color color : Color.values()) {
			slotDataList.add(new SlotData(true, color));
		}
	}

	/**
	 * Returns whether or not there is SlotData available.
	 * 
	 * @return
	 */
	public boolean hasSlotData() {
		return ((slotDataList != null) && !slotDataList.isEmpty());
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
		return slotDataList;
	}

	/**
	 * Returns whether or not there is JoinResponseData available.
	 * 
	 * @return
	 */
	public boolean hasJoinResponseData() {
		return (joinResponseData != null);
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
	 * Returns whether or not there is ScoreData available.
	 * 
	 * @return
	 */
	public boolean hasScoreData() {
		return ((scoreDataList != null) && !scoreDataList.isEmpty());
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
		return scoreDataList;
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

	/**
	 * Returns ConfifTypeMap used to enable/disable game features. This was added to assist in the performance tuning of
	 * the TV Application.
	 * 
	 * @return
	 */
	public ConfigTypeMap getConfigTypeMap() {
		return configTypeMap;
	}

	@Override
	public String toString() {
		return "GameState [slotDataList=" + slotDataList + ", joinResponseData=" + joinResponseData
		        + ", gameStartCountDownSeconds=" + gameStartCountDownSeconds + ", scoreDataList=" + scoreDataList
		        + ", playerOutCountDownSeconds=" + playerOutCountDownSeconds + ", configTypeMap=" + configTypeMap + "]";
	}
}
