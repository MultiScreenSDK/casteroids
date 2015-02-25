$(function(){

    "use strict"; 
    var game = new Phaser.Game(1280, 720, Phaser.AUTO, 'casteroids', {preload: preload, create: create, update: update, render: render});

    var sprite;
    var cursors;
    var bullet;
    var bullets;
    var bulletTime = 0;
    var thrusting;

    

    function preload() {
        game.load.image('space', 'assets/deep-space.jpg');
        game.load.image('bullet', 'assets/bullets.png');
        game.load.image('ship', 'assets/ship.png');
    }

    function create() {
        game.scale.parentIsWindow = true;
        game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;
        game.renderer.clearBeforeRender = false;
        game.renderer.roundPixels = true;

        //arcade physics
        game.physics.startSystem(Phaser.Physics.ARCADE);

        //space
        game.add.tileSprite(0, 0, game.width, game.height, 'space');

        //bullets
        bullets = game.add.group();
        bullets.enableBody = true;
        bullets.physicsBodyType = Phaser.Physics.ARCADE;

        //40 bullets
        bullets.createMultiple(40, 'bullet');
        bullets.setAll('anchor.x', -1.0);
        bullets.setAll('anchor.y', 0.5);

        //ship
        sprite = game.add.sprite(300, 300, 'ship');
        sprite.anchor.set(0.5);

        //add the sprites physics
        game.physics.enable(sprite, Phaser.Physics.ARCADE); //why this way and not setting the physicsBodyType??

        sprite.body.drag.set(100);
        sprite.body.maxVelocity.set(200);
    }

    function update() {
        if(thrusting == true) {
            //thrust
            game.physics.arcade.accelerationFromRotation(sprite.rotation, 200, sprite.body.acceleration);
        } else {
            //no acceleration
            sprite.body.acceleration.set(0); //what is the difference between set(num) vs assignment via = sign?
        }

        // NOTE: The spacecraft's angular velocity is updated when handling rotate events from the client.
        // NOTE: The spacecraft's firing of bullets is performed when handling fire events from the client.

        screenWrap(sprite);

        bullets.forEachExists(screenWrap, this); //what is this??
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
    function onRotatePlayer(clientId, direction, strength) {
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
	
});
