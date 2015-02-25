$(function(){
    //  Create your Phaser game and inject it into the gameContainer div.
    //  We did it in a window.onload event, but you can do it anywhere (requireJS load, anonymous function, jQuery dom ready)
    var game = new Phaser.Game(1280, 800, Phaser.AUTO, 'gameContainer');
    
    //  Add the States your game has.
    //  You don't have to do this in the html, it could be done in your Boot state too, but for simplicity I'll keep it here.
    game.state.add('Boot', BasicGame.Boot);
    game.state.add('Preloader', BasicGame.Preloader);
    game.state.add('MainMenu', BasicGame.MainMenu);
    game.state.add('Game', BasicGame.Game);
    game.state.add('GameOver', BasicGame.GameOver);
    
    //  Now start the Boot state.
    game.state.start('Boot');

    "use strict";
    
    //  A placeholder for one player in the gave
    function Slot(color, colorCode) {
        this.color = color || 'unknown';
        this.colorCode = colorCode || 0x000000;
        this.available = true;
        // TODO: Move the clientId to the Player object. It's here because there currently isn't a Player object.
        this.clientId = null;
        this.player = null;
    }

    var slots = [ new Slot('red', 0xff0000),
                  new Slot('orange', 0xff8800),
                  new Slot('green', 0x00ff00),
                  new Slot('blue', 0x0000ff) ];

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

    window.msf.local(function(err, service){
        var channel = service.channel('com.samsung.multiscreen.castroids');

        channel.connect({name: 'TV'}, function (err) {
            if(err) return console.error(err);
        });

        channel.on('connect', function(client){
            console.log('connect');

            // Send the slot update to the new client so it knows what slots are available.
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
            channel.publish('game_start', countdown);
        }

        function sendPlayerOut(clientId, countdown) {
            // Send a player_out to the client
            console.log('sending player_out ' + countdown + " secs. to=" + clientId);
            channel.publish('player_out', countdown, clientId);
        }

        function sendGameOver() {
            // Send a game_over to all clients
            console.log('sending game_over. to=all');
            channel.publish('game_over', null);
        }

    });
});
