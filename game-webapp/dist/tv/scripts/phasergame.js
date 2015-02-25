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
        game.load.image('space', 'images/deep-space.jpg');
        game.load.image('bullet', 'images/bullets.png');
        game.load.image('ship', 'images/ship.png');
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
<<<<<<< HEAD
=======

    function screenWrap(sprite) {
        if(sprite.x < 0) {
            sprite.x = game.width;
        }
        else if(sprite.x > game.width) {
            sprite.x = 0;
        }

        if(sprite.y < 0) {
            sprite.y = game.height;
        } else if(sprite.y > game.height) {
            sprite.y = 0;
        }
    }

    function render() {

    }

    window.msf.local(function(err, service){

        var channel = service.channel('com.samsung.multiscreen.castroids');

        channel.connect({name: 'TV'}, function (err) {
            if(err) return console.error(err);
        });

        channel.on('connect', function(client){
            console.log('connect');

            // Send the slot update to the new client sp it knows what slots are available.
            sendSlotUpdate('all');
        });

        channel.on('disconnect', function(client){
            console.log('disconnect');
            removePlayer(client.id);
            sendSlotUpdate('all');
        });

        channel.on('clientConnect', function(client){
            console.log('clientConnect');

            // Send the slot update to the new client sp it knows what slots are available.
            sendSlotUpdate('all');
        });

        channel.on('clientDisconnect', function(client){
            console.log('clientDisconnect');
            removePlayer(client.id);
            sendSlotUpdate('all');
        });

        channel.on('join_request', function(msg, from) {
            console.log('join_request. from=' + (from.id || 'Unknown'));

            // Parse the JSON data received from the client.
            var joinRequestData = JSON.parse(msg);

            // Attempt to add the player to the game.
            var responseCode = addPlayer(from.id, joinRequestData.name, joinRequestData.color);

            // Create the join response data
            var joinResponse = { name : joinRequestData.name,
                color : joinRequestData.color,
                response_code : responseCode };

            // Send a join_response back to the client
            console.log('sending join_response ' + JSON.stringify(joinResponse) + ". to=" + from.id);
            channel.publish('join_response', JSON.stringify(joinResponse), from.id);

            // If the client successfully joined, send out the slot data update.
            if (responseCode == JoinResponseCode.SUCCESS) {
                sendSlotUpdate('all');

                // TODO: REMOVE statement below after the addPlayer method's TODO for starting the game is complete.
                sendGameStart(0);
            }
        });

        channel.on('quit', function(msg, from) {
            console.log('quit. from=' + (from.id || 'Unknown'));
            removePlayer(from.id);
            sendSlotUpdate('all');
        });

        channel.on('rotate', function(msg, from){
            console.log('rotate. from=' + (from.id || 'Unknown'));

            // Parse the JSON data received from the client.
            var rotateData = JSON.parse(msg);

            // Rotate the player
            onRotatePlayer(from.id, rotateData.rotate, rotateData.strength);
        });

        channel.on('thrust', function(msg, from){
            console.log('thrust. from=' + (from.id || 'Unknown'));
            onThrust(from.id, msg == 'on');
        });

        channel.on('fire', function(msg, from){
            console.log('fire. from=' + (from.id || 'Unknown'));
            if (msg == 'on') {
                onFire(from.id);
            }
        });

        function sendSlotUpdate(to) {
            // Create and populate the slot data array
            var slotData = [];
            for (var i in slots) {
                var slot = slots[i];
                slotData[i] = { available : slot.available, color : slot.color};
            }

            // Send a slot_update to the client(s)
            console.log('sending slot_update ' + JSON.stringify(slotData) + ". to=" + to);
            channel.publish('slot_update', JSON.stringify(slotData), to);
        }

        function sendGameStart(countdown) {
            // Send a game_start to all clients
            console.log('sending game_start ' + countdown + " secs. to=all");
            channel.publish('game_start', countdown.toString());
        }

        function sendPlayerOut(clientId, countdown) {
            // Send a player_out to the client
            console.log('sending player_out ' + countdown + " secs. to=" + clientId);
            channel.publish('player_out', countdown.toString(), clientId);
        }

        function sendGameOver() {
            // Send a game_over to all clients
            console.log('sending game_over. to=all');
            channel.publish('game_over', null);
        }

    });
>>>>>>> 9c05efe209016a78b2b1dbf6d9ac217c6bc24a8e
	
});
