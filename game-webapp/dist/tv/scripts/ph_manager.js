var GameManager;

/**
 * The GameManager acts as a layer of abstraction between the client connectivity and game logic.
 *
 * When the ConnectivityManager receives an event a client it parses it and sends it to the GameManager and the
 * GameManager determines how to handle that event based on the current game state (menu, game, game over, etc.).
 * Typically, it will result in the GameManager notifying the current game state of the event.
 *
 * Game states (menu, game, game over, etc.) notify the GameManager of game state changes and player updates and the
 * GameManager determines how to handle the event. Typically, it will result in the GameManager notifying the
 * ConnectivityManager which in return will notify the appropriate client(s).
 */
$(GameManager = function(){
    "use strict";

    // Create the Game object.
    var game = new Phaser.Game(1280, 720, Phaser.AUTO, 'casteroids');

    //Add States to the Game object.
    //the first argument is the name of the state and the second is the name of the function to call inside such state.
    var bootState = game.state.add('Boot', BasicGame.Boot);
    var loaderState = game.state.add('Preloader', BasicGame.Preloader);
    var menuState = game.state.add('MainMenu', BasicGame.MainMenu);
    var gameState = game.state.add('Game', BasicGame.Game);
    var overState = game.state.add('GameOver', BasicGame.GameOver);

    // The response codes when adding a new player.
    var JoinResponseCode = {
        SUCCESS: 0,
        COLOR_TAKEN: 1,
        ERROR: 2
    };

    //  A placeholder for one player in the gave
    function Slot(color, colorCode, hexColor) {
        // Whether or not this slot is available.
        this.available = true;

        // The client id associated to this slot.
        this.clientId = null;

        // The name associated to this slot.
        this.name = null;

        // The color and color code associated to this slot.
        this.color = color || 'unknown';
        this.colorCode = colorCode || 0x000000;
        this.hexColor = hexColor || '#000000'
    }

    // The available slots in the game (4 players of different colors)
    var slots = [
        new Slot('red', 0xFF0000, '#FF0000'),
        new Slot('orange', 0xFF8800, '#FF8800'),
        new Slot('green', 0x00FF00, '#00FF00'),
        new Slot('cyan', 0x00b8d4, '#00b8d4')
    ];

    // Store client id to slot mappings.
    var clientIdToSlotMap = {};

    // Stores color to slot mappings.
    var colorToSlotMap = {};

    // Initialize the color to slot mappings.
    for (var i in slots) {
        var slot = slots[i];
        colorToSlotMap[slot.color] = slot;
    }

    /******************************************************************************************************************
     * Game State Methods
     */

    // Called by the MainMenu state when its performing the countdown to the game start.
    function onGameStart(countdown) {
        // Notify the clients via the Connectivity manager.
        ConnectivityManager.onGameStart(countdown);
    }

    // Called by the Game state when a player is out (i.e. blown to smithereens)
    function onPlayerOut(clientId, countdown) {
        // Notify the clients via the Connectivity manager.
        ConnectivityManager.onPlayerOut(clientId, countdown);
    }

    // Called by the Game state when the game is over.
    function onGameOver(scores) {
        // Create a score data object consisting of the slot and scores objects.
        var scoreData = [];
        var index = 0;
        for (var clientId in clientIdToSlotMap) {
            var slot = clientIdToSlotMap[clientId];
            var score = scores[clientId] || 0;
            scoreData[index++] = { name : slot.name, color : slot.color, score: score};
        }

        // Notify the clients via the Connectivity manager.
        ConnectivityManager.onGameOver(scoreData);
    }

    /******************************************************************************************************************
     * Player Join/Quit Methods
     */

    // Attempts to add a player to the game. Called by the ConnectivityManager when it receives join event from a
    // client.
    function addPlayer(clientId, name, color) {
        // If we already have a player with this client id, remove it and add the new one.
        removePlayer(clientId);

        // Look up the requested slot by the color name
        var slot = colorToSlotMap[color];

        // If the slot is not valid, return the ERROR code.
        if (slot == null) {
            return JoinResponseCode.ERROR;
        }
        // Else if the slot is not available, return the COLOR_TAKEN code.
        else if (!slot.available)  {
            return JoinResponseCode.COLOR_TAKEN;
        }

        // Associate the client to the slot
        slot.available = false;
        slot.clientId = clientId;
        slot.name = name;

        // If in the Game state, add the Player to the game
        if (game.state.getCurrentState() == gameState) {
            gameState.addPlayer(clientId, name, slot.colorCode);
        }

        // Add the client id to slot mapping
        clientIdToSlotMap[clientId] = slot;

        // If in the Menu state, notify the Menu state.
        if (game.state.getCurrentState() == menuState) {
            menuState.onPlayerUpdate(Object.keys(clientIdToSlotMap).length);
        }

        // Return the SUCCESS code
        return JoinResponseCode.SUCCESS;
    }

    // Attempts to remove a player from the game. Called by the ConnectivityManager when it receives disconnect,
    // disconnect or quit event from a client.
    function removePlayer(clientId) {
        // Look up the requested slot by the color name
        var slot = clientIdToSlotMap[clientId];

        // If the slot is null then the player has already been removed or the given client id is invalid.
        if (slot == null) {
            return;
        }

        // Un-associate the client with the slot
        slot.available = true;
        slot.clientId = null;
        slot.name = null;

        // If in the Game state, remove the player from the game.
        if (game.state.getCurrentState() == gameState) {
            gameState.removePlayer(slot.clientId);
        }

        // Remove client id to slot mapping
        delete clientIdToSlotMap[clientId];

        // If in the Menu state, notify the Menu state.
        if (game.state.getCurrentState() == menuState) {
            menuState.onPlayerUpdate(Object.keys(clientIdToSlotMap).length);
        }
    }

    /******************************************************************************************************************
     * Player Control Methods
     */

    // If we are in the Game state request the Game state to rotate the client's spaceship otherwise ignore. Called
    // by the ConnectivityManager when it receives rotate event from a client.
    function onRotate(clientId, direction, strength) {
        if (game.state.getCurrentState() == gameState) {
            gameState.onRotate(clientId, direction, strength);
        }
    }

    // If we are in the Game state request the Game state to enable thrust on the client's spaceship otherwise ignore.
    // Called by the ConnectivityManager when it receives thrust event from a client.
    function onThrust(clientId, thrustEnabled) {
        if (game.state.getCurrentState() == gameState) {
            gameState.onThrust(clientId, thrustEnabled);
        }
    }

    // If we are in the Game state request the Game state to enable firing on the client's spaceship otherwise ignore.
    // Called by the ConnectivityManager when it receives fire event from a client.
    function onFire(clientId, fireEnabled) {
        if (game.state.getCurrentState() == gameState) {
            gameState.onFire(clientId, fireEnabled);
        }
    }
    
    // TODO add implementation
    function onGameOver() {
    }

    //  Now start the Boot state.
    game.state.start('Boot');

    // Define what is exposed on the GameManager variable.
    return {
        // Variables
        slots: slots,
        JoinResponseCode: JoinResponseCode,

        // Game State Methods
        onGameStart: function(countdown) { onGameStart(countdown); },
        onPlayerOut: function(clientId, countdown) { onPlayerOut(clientId, countdown); },
        onGameOver: function(scores) { onGameOver(scores); },

        // Player Join/Quit Methods
        addPlayer: function(clientId, name, color) { return addPlayer(clientId, name, color); },
        removePlayer: function(clientId) { return removePlayer(clientId); },

        // Player Control Methods
        onRotate: function(clientId, direction, strength) { onRotate(clientId, direction, strength); },
        onThrust: function(clientId, thrustEnabled) { onThrust(clientId, thrustEnabled); },
        onFire: function(clientId, fireEnabled) { onFire(clientId, fireEnabled); }
    }

}());
