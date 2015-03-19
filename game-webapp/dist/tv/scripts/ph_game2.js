var game = new Phaser.Game(1280, 720, Phaser.CANVAS, 'phaser-example', { preload: preload, create: create, update: update, render: render }, true, false);

function preload() {

    game.load.image('space', 'assets/starfield.png');
    game.load.image('bullet', 'assets/bullets.png');
    game.load.image('ship', 'assets/ship.png');

}

var sprite;

var bullet;
var bullets;
var bulletTime = 0;

function create() {
    // Settings to make the app full screen
    this.game.scale.parentIsWindow = true;
    this.game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;

    //  This will run in Canvas mode, so let's gain a little speed and display
    game.renderer.clearBeforeRender = true;
    game.renderer.roundPixels = true;

    // Disable keyboard input
    game.input.enabled = false;

    //  We need arcade physics
    game.physics.startSystem(Phaser.Physics.ARCADE);

    this.game.time.advancedTiming = true;

    //  Our ships bullets
    bullets = game.add.group();
    bullets.enableBody = true;
    bullets.physicsBodyType = Phaser.Physics.ARCADE;

    //  All 40 of them
    bullets.createMultiple(40, 'bullet');
    bullets.setAll('anchor.x', 0.5);
    bullets.setAll('anchor.y', 0.5);

    //  Our player ship
    sprite = game.add.sprite(300, 300, 'ship');
    sprite.anchor.set(0.5);

    //  and its physics settings
    game.physics.enable(sprite, Phaser.Physics.ARCADE);

    sprite.body.drag.set(100);
    sprite.body.maxVelocity.set(200);

    // Set the background on the DOM
    var bgImage = "url(assets/starfield.png)";
    var bgRepeat = "repeat";
    $("body").css("background-image", bgImage);
    $("body").css("background-repeat", bgRepeat);

}

var tick = 0;

function update() {
    var turn = (tick % 3);

    if (turn == 0) {
        game.physics.arcade.accelerationFromRotation(sprite.rotation, 200, sprite.body.acceleration);
        sprite.body.angularVelocity = -300;
    }
    else if (turn == 1) {
        fireBullet();
    }
    else if (turn == 2) {
        screenWrap(sprite);
        bullets.forEachExists(screenWrap, this);
    }

    tick++;
}

function fireBullet () {

    if (game.time.now > bulletTime)
    {
        bullet = bullets.getFirstExists(false);

        if (bullet)
        {
            bullet.reset(sprite.body.x + 16, sprite.body.y + 16);
            bullet.lifespan = 2000;
            bullet.rotation = sprite.rotation;
            game.physics.arcade.velocityFromRotation(sprite.rotation, 400, bullet.body.velocity);
            bulletTime = game.time.now + 50;
        }
    }

}

function screenWrap (sprite) {

    if (sprite.x < 0)
    {
        sprite.x = game.width;
    }
    else if (sprite.x > game.width)
    {
        sprite.x = 0;
    }

    if (sprite.y < 0)
    {
        sprite.y = game.height;
    }
    else if (sprite.y > game.height)
    {
        sprite.y = 0;
    }

}

function render() {
    this.game.debug.text(this.game.time.fps || '--', 2, 14, "#00ff00");
}