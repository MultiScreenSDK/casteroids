var connectivityManager = $(function(){
    "use strict";

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
            var responseCode = GameManager.addPlayer(from.id, joinRequestData.name, joinRequestData.color);

            // Create the join response data
            var joinResponse = { name : joinRequestData.name,
                color : joinRequestData.color,
                response_code : responseCode };

            // Send a join_response back to the client
            console.log('sending join_response ' + JSON.stringify(joinResponse) + ". to=" + from.id);
            channel.publish('join_response', JSON.stringify(joinResponse), from.id);

            // If the client successfully joined, send out the slot data update.
            if (responseCode == GameManager.JoinResponseCode.SUCCESS) {
                sendSlotUpdate('all');

                // TODO: REMOVE statement below after the addPlayer method's TODO for starting the game is complete.
                sendGameStart(0);
            }
        });

        channel.on('quit', function(msg, from) {
            console.log('quit. from=' + (from.id || 'Unknown'));
            GameManager.removePlayer(from.id);
            sendSlotUpdate('all');
        });

        channel.on('rotate', function(msg, from){
            console.log('rotate. from=' + (from.id || 'Unknown'));

            // Parse the JSON data received from the client.
            var rotateData = JSON.parse(msg);

            // Rotate the player
            GameManager.onRotate(from.id, rotateData.rotate, rotateData.strength);
        });

        channel.on('thrust', function(msg, from){
            console.log('thrust. from=' + (from.id || 'Unknown'));
            GameManager.onThrust(from.id, msg == 'on');
        });

        channel.on('fire', function(msg, from){
            console.log('fire. from=' + (from.id || 'Unknown'));
            if (msg == 'on') {
                GameManager.onFire(from.id);
            }
        });

        function sendSlotUpdate(to) {
            // Create and populate the slot data array
            var slotData = [];
            for (var i in GameManager.slots) {
                var slot = GameManager.slots[i];
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
