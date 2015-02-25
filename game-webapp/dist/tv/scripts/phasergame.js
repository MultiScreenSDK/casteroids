var GameManager;

$(GameManager = function(){
    "use strict";

    var game = new Phaser.Game(1280, 720, Phaser.AUTO, 'casteroids');

    //  Add the States your game has.
    //  You don't have to do this in the html, it could be done in your Boot state too, but for simplicity I'll keep it here.
    game.state.add('Boot', BasicGame.Boot);
    game.state.add('Preloader', BasicGame.Preloader);
    game.state.add('MainMenu', BasicGame.MainMenu);
    game.state.add('Game', BasicGame.Game);
    game.state.add('GameOver', BasicGame.GameOver);

    var sprite;
    var cursors;
    var bullet;
    var bullets;
    var bulletTime = 0;
    var thrusting;

    //  A placeholder for one player in the gave
    function Slot(color, colorCode) {
        this.color = color || 'unknown';
        this.colorCode = colorCode || 0x000000;
        this.available = true;
        // TODO: Move the clientId to the Player object. It's here because there currently isn't a Player object.
        this.clientId = null;
        this.player = null;
    }

    // The available slots in the game (4 players of different colors)
    var slots = [ new Slot('red', 0xff0000),
        new Slot('orange', 0xff8800),
        new Slot('green', 0x00ff00),
        new Slot('blue', 0x0000ff) ];

    // The response codes when adding a new player.
    var JoinResponseCode = {
        SUCCESS: 0,
        COLOR_TAKEN: 1,
        ERROR: 2
    };

    // Store client id to slot mappings.
    var clientIdToSlotMap = {};

    // Stores color to slot mappings.
    var colorToSlotMap = {};

    // Initialize the color to slot mappings.
    for (var i in slots) {
        var slot = slots[i];
        colorToSlotMap[slot.color] = slot;
        console.log('adding '+slot);
    }

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
        // slot.player = player;

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
        slot.player = null;

        // TODO: Remove this section after the above TODO is complete.
        // For now we are just stopping the ship from moving around after a disconnect
        sprite.body.angularVelocity = 0;
        thrusting = 0;

        // Remove client id to slot mapping
        delete clientIdToSlotMap[clientId];
    }

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
            sprite.body.angularVelocity = -velocity;
        } else if(direction == 'right') {
            sprite.body.angularVelocity = velocity;
        } else {
            sprite.body.angularVelocity = 0;
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
        thrusting = thrustEnabled;
    }

    // Fires a bullet from the client's spaceship. Called when the client sends a fire event.
    function onFire(clientId) {
        // Look up the requested slot by the color name
        var slot = clientIdToSlotMap[clientId];

        // If the slot is null then the player has been removed or the given client id is invalid.
        if (slot == null) {
            return;
        }

        // TODO: Make modifications on the player object on the slot object
        if(game.time.now > bulletTime) {
            bullet = bullets.getFirstExists(false); //what is this?

            if(bullet) {
                bullet.reset(sprite.body.x + 16, sprite.body.y + 16);
                bullet.lifespan = 2000;  //2 seconds
                bullet.rotation = sprite.rotation;
                game.physics.arcade.velocityFromRotation(sprite.rotation, 400, bullet.body.velocity); //what is this??
                bulletTime = game.time.now + 50;
            }
        }
    }

    //  Now start the Boot state.
    game.state.start('Boot');

    // Define what is exposed on the GameManager variable.
    return {
        slots: slots,
        JoinResponseCode: JoinResponseCode,
        addPlayer: function(clientId, name, color) { return addPlayer(clientId, name, color); },
        removePlayer: function(clientId) { return removePlayer(clientId); },
        onRotate: function(clientId, direction, strength) { return onRotate(clientId, direction, strength); },
        onThrust: function(clientId, thrustEnabled) { return onThrust(clientId, thrustEnabled); },
        onFire: function(clientId) { return onFire(clientId); }
    }

}());
