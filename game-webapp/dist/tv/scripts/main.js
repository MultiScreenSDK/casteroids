$(function(){

    "use strict";

    var ui = {
        rotate : $('#rotate'),
        thrust : $('#thrust'),
        fire : $('#fire'),
        client : $('#client')
    };

    window.msf.local(function(err, service){

        var channel = service.channel('com.samsung.multiscreen.game');

        channel.connect({name: 'TV'}, function (err) {
            if(err) return console.error(err);
        });

        channel.on('join', function(msg, from){
            channel.publish('game_start', '5');
        });

        channel.on('rotate', function(msg, from){
            ui.rotate.text(msg);
            ui.client.text((from.attributes.name || 'Unknown'));
        });

        channel.on('thrust', function(msg, from){
            ui.thrust.text(msg);
            ui.client.text((from.attributes.name || 'Unknown'));
        });

        channel.on('fire', function(msg, from){
            ui.fire.text(msg);
            ui.client.text((from.attributes.name || 'Unknown'));
        });

        channel.on('clientConnect', function(client){
            ui.rotate.text('--');
            ui.thrust.text('--');
            ui.fire.text('--');
            ui.client.text('clientConnect');
        });

        channel.on('clientDisconnect', function(client){
            ui.rotate.text('N/A');
            ui.thrust.text('N/A');
            ui.fire.text('N/A');
            ui.client.text('clientDisconnect');
        });

        channel.on('connect', function(client){
            ui.rotate.text('-');
            ui.thrust.text('-');
            ui.fire.text('-');
            ui.client.text('connect');
        });

        channel.on('disconnect', function(client){
            ui.rotate.text('N/A');
            ui.thrust.text('N/A');
            ui.fire.text('N/A');
            ui.client.text('disconnect');
        });

    });

});
