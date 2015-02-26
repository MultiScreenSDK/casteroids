var GameManager;

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
    function Slot(playerPosition, color, colorCode) {
        // Whether or not this slot is available.
        this.available = true;

        // The player position associated to this slot.
        this.playerPosition = playerPosition;

        // The client id associated to this slot.
        this.clientId = null;

        // The color and color code associated to this slot.
        this.color = color || 'unknown';
        this.colorCode = colorCode || 0x000000;
    }

    // The available slots in the game (4 players of different colors)
    var slots = [ new Slot(0, 'red', 0xff0000),
        new Slot(1, 'orange', 0xff8800),
        new Slot(2, 'green', 0x00ff00),
        new Slot(3, 'blue', 0x0000ff) ];

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
        // TODO: Check current state
        // TODO: Update current state
        ConnectivityManager.onGameStart(countdown);
    }

    // Called by the Game state when a player is out (i.e. blown to smithereens)
    function sendPlayerOut(clientId, countdown) {
        // TODO: The method should take a player info or something else and we should look up the client id from it.
        ConnectivityManager.onPlayerOut(clientId, countdown);
    }

    // Called by the Game state when the game is over.
    function sendGameOver() {
        // TODO: Construct the scoreData. Does this method need data passed in to do so?
        var scoreData = null;
        ConnectivityManager.onGameOver(scoreData);
    }

    /******************************************************************************************************************
     * Player Join/Quit Methods
     */

    // Attempts to add a player to the game. Called when a client requests to join the game.
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

        // Create add the Player object to the game and slot.
        // TODO: Create and add the player to the game and slot
        menuState.onPlayerUpdate(1);

        // Add the client id to slot mapping
        clientIdToSlotMap[clientId] = slot;

        // If the game was not already initiated then start the countdown!
        // TODO: If the game was not already initiated then start the countdown!

        // Return the SUCCESS code
        return JoinResponseCode.SUCCESS;
    }

    // Attempts to remove a player from the game. Called when a player quits or a client disconnects.
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

        // Remove the player from the game and slot
        // TODO: Remove the player from the game
        menuState.onPlayerUpdate(0);

        // Remove client id to slot mapping
        delete clientIdToSlotMap[clientId];
    }

    /******************************************************************************************************************
     * Player Control Methods
     */

    // Rotate the client's spaceship. Called when a client sends a rotate command.
    function onRotate(clientId, direction, strength) {
        // Look up the requested slot by the color name
        var slot = clientIdToSlotMap[clientId];

        // If the slot is null then the player has been removed or the given client id is invalid.
        if (slot == null) {
            return;
        }

        // TODO: Make modifications on the player object on the slot object
        // Map the 0 to 20 range value from the message's data to a 100 to 400 range value for the game.
        var velocity = ((strength * 400) / 20) + 100;

        // Update the angular velocity based on the rotate direction (right, left, or none).
        if(direction == 'left') {
            //sprite.body.angularVelocity = -velocity;
        } else if(direction == 'right') {
            //sprite.body.angularVelocity = velocity;
        } else {
            //sprite.body.angularVelocity = 0;
        }
    }

    // Enables thrust on a clients spaceship. Called when the client sends a thrust event.
    function onThrust(clientId, thrustEnabled) {
        // Look up the requested slot by the color name
        var slot = clientIdToSlotMap[clientId];

        // If the slot is null then the player has been removed or the given client id is invalid.
        if (slot == null) {
            return;
        }

        // TODO: Make modifications on the player object on the slot object
    }

    // Fires a bullet from the client's spaceship. Called when the client sends a fire event.
    function onFire(clientId, fireEnabled) {
        // Look up the requested slot by the color name
        var slot = clientIdToSlotMap[clientId];

        // If the slot is null then the player has been removed or the given client id is invalid.
        if (slot == null) {
            return;
        }

        // TODO: Make modifications on the player object on the slot object
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
        onGameOver: function() { onGameOver(); },

        // Player Join/Quit Methods
        addPlayer: function(clientId, name, color) { return addPlayer(clientId, name, color); },
        removePlayer: function(clientId) { return removePlayer(clientId); },

        // Player Control Methods
        onRotate: function(clientId, direction, strength) { onRotate(clientId, direction, strength); },
        onThrust: function(clientId, thrustEnabled) { onThrust(clientId, thrustEnabled); },
        onFire: function(clientId, fireEnabled) { onFire(clientId, fireEnabled); }
    }

}());
