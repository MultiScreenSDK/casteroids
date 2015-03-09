var ConnectivityManager;

/**
 * The ConnectivityManager manages the communication channel between the client(s) and the TV application and acts as a
 * layer of abstraction between the client(s) and the game. In order to listen for the correct events, it has to have
 * some knowledge of the game but does not include any game logic. This approach allows for the form of transport to
 * be changed without the game logic having to be changed.
 */
$(ConnectivityManager = function(){

    "use strict";

    var channel;
    var logToConsole = false;

    window.msf.local(function(err, service){
        channel = service.channel('com.samsung.multiscreen.castroids');

        channel.connect({name: 'TV'}, function (err) {
            if(err) return console.error(err);
        });

        channel.on('connect', function(client){
            if (logToConsole) {
                console.log('connect');
            }

            // Send the slot update to the new client so it knows what slots are available.
            sendSlotUpdate(client.id);
        });

        channel.on('disconnect', function(client){
            if (logToConsole) {
                console.log('disconnect');
            }
            GameManager.removePlayer(client.id);
            sendSlotUpdate();
        });

        channel.on('clientConnect', function(client){
            if (logToConsole) {
                console.log('clientConnect');
            }

            // Send the slot update to the new client sp it knows what slots are available.
            sendSlotUpdate(client.id);
        });

        channel.on('clientDisconnect', function(client){
            if (logToConsole) {
                console.log('clientDisconnect');
            }
            GameManager.removePlayer(client.id);
            sendSlotUpdate();
        });

        channel.on('join_request', function(msg, from) {
            if (logToConsole) {
                console.log('join_request. from=' + (from.id || 'Unknown'));
            }

            // Parse the JSON data received from the client.
            var joinRequestData = JSON.parse(msg);

            // Attempt to add the player to the game.
            var responseCode = GameManager.addPlayer(from.id, joinRequestData.name, joinRequestData.color);

            // Create the join response data
            var joinResponse = { name : joinRequestData.name,
                color : joinRequestData.color,
                response_code : responseCode };

            // Send a join_response back to the client
            if (logToConsole) {
                console.log('sending join_response ' + JSON.stringify(joinResponse) + ". to=" + from.id);
            }
            channel.publish('join_response', JSON.stringify(joinResponse), from.id);

            // If the client successfully joined, send out the slot data update.
            if (responseCode == GameManager.JoinResponseCode.SUCCESS) {
                sendSlotUpdate();
            }
        });

        channel.on('quit', function(msg, from) {
            if (logToConsole) {
                console.log('quit. from=' + (from.id || 'Unknown'));
            }
            GameManager.removePlayer(from.id);
            sendSlotUpdate();
        });

        channel.on('rotate', function(msg, from){
            if (logToConsole) {
                console.log('rotate. from=' + (from.id || 'Unknown'));
            }

            // Parse the JSON data received from the client.
            var rotateData = JSON.parse(msg);

            // Rotate the player
            GameManager.onRotate(from.id, rotateData.rotate, rotateData.strength);
        });

        channel.on('thrust', function(msg, from){
            if (logToConsole) {
                console.log('thrust. from=' + (from.id || 'Unknown'));
            }
            GameManager.onThrust(from.id, msg == 'on');
        });

        channel.on('fire', function(msg, from){
            if (logToConsole) {
                console.log('fire. from=' + (from.id || 'Unknown'));
            }
            GameManager.onFire(from.id, msg == 'on');
        });
    });

    // Send a slot_update to all or a specific client.
    function sendSlotUpdate(clientId) {
        // Create and populate the slot data array. It is a subset of the slots object used by the GameManager.
        //
        // NOTE: We create the SlotData here instead of in the GameManager since the ConnectivityManager to Client
        // contract has a subset of the data of the slots object used by the GameManager and we do not want the
        // GameManager to make any assumptions about the contract between the ConnectivityManager and the clients.
        var slotData = [];
        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];
            slotData[i] = { available : slot.available, color : slot.color};
        }

        // Send a slot_update to the client(s)
        if (logToConsole) {
            console.log('sending slot_update ' + JSON.stringify(slotData) + ". clientId=" + clientId);
        }
        if (clientId !== 'undefined') {
            channel.publish('slot_update', JSON.stringify(slotData), clientId);
        } else {
            channel.publish('slot_update', JSON.stringify(slotData));
        }
    }

    // Send a game_start to all clients
    function sendGameStart(countdown) {
        if (logToConsole) {
            console.log('sending game_start ' + countdown + " secs. to=all");
        }
        channel.publish('game_start', countdown.toString());
    }

    // Send a game_start to all clients
    function sendGameStarted(clientId) {
        if (logToConsole) {
            console.log('sending game_start ' + countdown + " secs. to=all");
        }
        var countdown = 0;
        channel.publish('game_start', countdown.toString(), clientId);
    }

    // Send a player_out to the client
    function sendPlayerOut(clientId, countdown) {
        if (logToConsole) {
            console.log('sending player_out ' + countdown + " secs. to=" + clientId);
        }
        channel.publish('player_out', countdown.toString(), clientId);
    }

    // Send a game_over to all clients
    function sendGameOver(scoreData) {
        if (logToConsole) {
            console.log('sending game_over ' + JSON.stringify(scoreData) + '. to=all');
        }
        channel.publish('game_over', JSON.stringify(scoreData));
    }

    // Define what is exposed on the ConnectivityManager variable.
    return {
        onGameStart: function(countdown) { return sendGameStart(countdown); },
        onGameStarted: function(clientId) { return sendGameStarted(clientId); },
        onPlayerOut: function(clientId, countdown) { return sendPlayerOut(clientId, countdown); },
        onGameOver: function(scoreData) { return sendGameOver(scoreData); }
    }

}());
