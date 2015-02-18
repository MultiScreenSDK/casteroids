// Phaser Init
window.onload = function() {

  //  Create your Phaser game and inject it into the gameContainer div.
  //  We did it in a window.onload event, but you can do it anywhere (requireJS load, anonymous function, jQuery dom ready, - whatever floats your boat)
  var game = new Phaser.Game(800, 600, Phaser.AUTO, 'gameContainer');

  //  Add the States your game has.
  //  You don't have to do this in the html, it could be done in your Boot state too, but for simplicity I'll keep it here.
  game.state.add('Boot', BasicGame.Boot);
  game.state.add('Preloader', BasicGame.Preloader);
  game.state.add('MainMenu', BasicGame.MainMenu);
  game.state.add('Game', BasicGame.Game);
  game.state.add('GameOver', BasicGame.GameOver);

  //  Now start the Boot state.
  game.state.start('Boot');

};
// END Phaser Init 

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

        channel.on('game_option', function(msg, from){
            channel.publish('game_state', 'start');
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
