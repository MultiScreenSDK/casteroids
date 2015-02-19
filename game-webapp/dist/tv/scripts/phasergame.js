$(function(){

    "use strict"; 
    var game = new Phaser.Game(1280, 800, Phaser.CANVAS, 'asteroids-example', {preload: preload, create: create, update: update, render: render});

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
        bullets.setAll('anchor.x', 0.5);
        bullets.setAll('anchor.y', 0.5);

        //ship
        sprite = game.add.sprite(300, 300, 'ship');
        sprite.anchor.set(0.5);

        //add the sprites physics
        game.physics.enable(sprite, Phaser.Physics.ARCADE); //why this way and not settng the physicsBodyType??

        sprite.body.drag.set(100);
        sprite.body.maxVelocity.set(200);

        //Game input
        cursors = game.input.keyboard.createCursorKeys();
        game.input.keyboard.addKeyCapture([Phaser.Keyboard.SPACEBAR]);


    }

    function update() {
        if(thrusting == true) {
            //thrust
            game.physics.arcade.accelerationFromRotation(sprite.rotation, 200, sprite.body.acceleration);
        } else {
            //no acceleration
            sprite.body.acceleration.set(0); //what is the difference between set(num) vs assignment via = sign?
        }

        /*
        if(turningLeft == true) {
            //rotating left
            sprite.body.angularVelocity = -300;
        } else if(turningRight == true) {
            //rotating right
            sprite.body.angularVelocity = 300;
        } else {
            //no rotation
            sprite.body.angularVelocity = 0;
        }
        */

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

        channel.on('rotate', function(msg, from){
            var rotateData = JSON.parse(msg);

            var velocity = ((rotateData.strength * 400) / 20) + 100;

            if(rotateData.rotate == 'left') {
                //rotating left
                sprite.body.angularVelocity = -velocity;
            } else if(rotateData.rotate == 'right') {
                //rotating right
                sprite.body.angularVelocity = velocity;
            } else {
                //no rotation
                sprite.body.angularVelocity = 0;
            }

            console.log((from.attributes.name || 'Unknown'));
        });

        channel.on('thrust', function(msg, from){
            if(msg == 'on'){
                thrusting = true;
            } else {
                thrusting = false;
            }
            console.log((from.attributes.name || 'Unknown'));
        });

        channel.on('fire', function(msg, from){
            if(msg == 'on'){
                firing = true;
            } else {
                firing = false;
            }
            console.log((from.attributes.name || 'Unknown'));
        });

        channel.on('clientConnect', function(client){
            console.log('clientConnect');
        });

        channel.on('clientDisconnect', function(client){
            console.log('clientDisconnect');
        });

        channel.on('connect', function(client){
            console.log('connect');
        });

        channel.on('disconnect', function(client){
            console.log('disconnect');
        });

    });
	
});
