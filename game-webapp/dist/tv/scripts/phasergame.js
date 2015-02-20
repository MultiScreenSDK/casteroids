$(function(){

    "use strict"; 
    var game = new Phaser.Game(1280, 720, Phaser.AUTO, 'asteroids-example', {preload: preload, create: create, update: update, render: render});

    var sprite;
    var cursors;
    var bullet;
    var bullets;
    var bulletTime = 0;
    var thrusting, turningLeft, turningRight, firing;

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

        // NOTE: The spacecraft's angular velocity is updated when handling rotate updates from the client.

        if(firing == true) {
            fireBullet();
        }

        screenWrap(sprite);

        bullets.forEachExists(screenWrap, this); //what is this??
    }

    function fireBullet() {
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
        });

        channel.on('disconnect', function(client){
            // TODO: Remove the client from the game
            sprite.body.angularVelocity = 0;
            thrusting = 0;
            firing = 0;
            console.log('disconnect');
        });

        channel.on('clientConnect', function(client){
            // TODO: Send the SlotData so the new client knows what slots are available.
            console.log('clientConnect');
        });

        channel.on('clientDisconnect', function(client){
            // TODO: Remove the client from the game
            sprite.body.angularVelocity = 0;
            thrusting = 0;
            firing = 0;
            console.log('clientDisconnect');
        });

        channel.on('join_request', function(msg, from) {
            // TODO: Confirm the join and send back a join_response
            console.log('join_request. from=' + (from.attributes.name || 'Unknown'));
        });

        channel.on('quit', function(msg, from) {
            // TODO: Remove the client from the game (similar to clientDisconnect)
            sprite.body.angularVelocity = 0;
            thrusting = 0;
            firing = 0;
            console.log('fire. from=' + (from.attributes.name || 'Unknown'));
        });

        channel.on('rotate', function(msg, from){
            // Parse the JSON data received from the client.
            var rotateData = JSON.parse(msg);

            // Map the 0 to 20 range value from the message's data to a 100 to 400 range value for the game.
            var velocity = ((rotateData.strength * 400) / 20) + 100;

            // Update the angular velocity based on the rotate direction (right, left, or none).
            if(rotateData.rotate == 'left') {
                sprite.body.angularVelocity = -velocity;
            } else if(rotateData.rotate == 'right') {
                sprite.body.angularVelocity = velocity;
            } else {
                sprite.body.angularVelocity = 0;
            }

            console.log('rotate. from=' + (from.attributes.name || 'Unknown'));
        });

        channel.on('thrust', function(msg, from){
            thrusting = (msg == 'on');
            console.log('thrust. from=' + (from.attributes.name || 'Unknown'));
        });

        channel.on('fire', function(msg, from){
            firing = (msg == 'on');
            console.log('fire. from=' + (from.attributes.name || 'Unknown'));
        });

    });
	
});
