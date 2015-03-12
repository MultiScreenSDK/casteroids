BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
    this.isMuted = false;
    this.shipDimens = 48;
    this.halfShipDimens = this.shipDimens/2;

    this.ticks = 0;

    // The variables below were used during performance testing to enable/disable game features to see which had the
    // greatest impact on performance.
    this.isAlien = true;
    this.isPlayersTinting = true;
    this.isBulletTinting = true;
    this.isGameText = true;
    this.isPointsText = true;
    this.isBackground = true;
    this.isBackgroundTiled = true;
    this.isCollisionsDetection = true;
    this.isFPSdebug = true;
};

BasicGame.Game.prototype = {

    players: {}, //variable on the game prototype

    /******************************************************************************************************************
     * Phaser Lifecycle functions
     */

    preload: function () {
    },

    create: function () {
        // If the GameManager has a config object, then update the configuration using its values. These configurations
        // were used during performance testing to enable/disable game features to see which had the greatest impact on
        // performance.
        this.config = GameManager.getConfig();
        if (this.config !== undefined) {
            this.isMuted = !this.config.isSoundEnabled;
            this.isAlien = this.config.isAlienEnabled;
            this.isPlayersTinting = this.config.isSpaceshipTintingEnabled;
            this.isBulletTinting = this.config.isBulletTintingEnabled;
            this.isGameText = this.config.isGameTextEnabled;
            this.isPointsText = this.config.isPointsTextEnabled;
            this.isBackground = this.config.isBackgroundImageEnabled;
            this.isBackgroundTiled = this.config.isBackgroundImageTiled;
            this.isCollisionsDetection = this.config.isCollisionDetectionEnabled;
            this.isFPSdebug = this.config.isFpsEnabled;
        }

        // If the FPS debug flag is enabled, then advanced timing is required. Showing the FPS is is useful when
        // performance testing/tuning the application.
        this.game.time.advancedTiming = this.isFPSdebug;

        this.setupBackground();
        this.setupSystem();
        this.setupText();
        this.setupPlayers();
        this.setupAlien();
        this.setupAudio();
    },

    render: function() {
        // If the FPS debug flag is enabled, show the FPS at the top left corner of the screen. This is useful when
        // performance testing/tuning the application.
        if(this.isFPSdebug) {
            this.game.debug.text(this.game.time.fps || '--', 2, 14, "#00ff00");
        }
    },

    update: function () {
        //  Main Game Loop

        // players lifecycle
        this.playerLifecycle();

        // alien lifecycle
        this.alienLifecycle();

        // Every 16th update cycle, check for points prompt expiring. We do not do this every update cycle to distribute
        // the work in order to maintain a high frames-per-second.
        if (this.ticks % 16 == 0) {
            if (this.pointsPrompt != undefined && this.pointsPrompt.exists && this.time.now > this.pointsExpire) {
                this.pointsPrompt.destroy();
            }
            // check for points prompt expiring
            if (this.pointsUpPrompt != undefined && this.pointsUpPrompt.exists && this.time.now > this.pointsUpExpire) {
                this.pointsUpPrompt.destroy();
            }
        }

        this.ticks++;
    },

    /******************************************************************************************************************
     * Game Callback functions
     */

    // Add a player to the game.
    addPlayer: function(position, clientId, name, colorCode, hexColor) {
        console.log("game.addPlayer " + position);
        if (this.game !== undefined) {
            // Initialize the new player
            this.players[clientId] = this.game.add.sprite(0, 0, 'ship');
            // Here I setup the user controlled ship
            var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
            var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));

            this.players[clientId].reset(randX, randY, BasicGame.PLAYER_HP);
            this.players[clientId].id = clientId;
            this.players[clientId].order = position;
            this.players[clientId].isThrusting = false;
            this.players[clientId].isFiring = false;
            if(this.isPlayersTinting) {
                this.players[clientId].tint = colorCode;
            }
            this.players[clientId].anchor.set(0.5);
            this.physics.enable(this.players[clientId], Phaser.Physics.ARCADE);
            this.players[clientId].body.drag.set(BasicGame.PLAYER_DRAG);
            this.players[clientId].body.maxVelocity.set(BasicGame.PLAYER_MAX_SPEED);

            this.players[clientId].bulletSpeed = BasicGame.PLAYER_BULLET_SPEED;
            this.players[clientId].bulletRange = BasicGame.PLAYER_FIRE_RANGE;
            this.players[clientId].bulletDelay = BasicGame.PLAYER_FIRE_DELAY;

            // Here I setup the bullets
            this.players[clientId].bullets = this.add.group();
            this.players[clientId].bullets.enableBody = true;
            this.players[clientId].bullets.physicsBodyType = Phaser.Physics.ARCADE;
            this.players[clientId].bulletTime = 0;

            this.players[clientId].bullets.createMultiple(20, 'bullets');
            this.players[clientId].bullets.setAll('anchor.x', 0.5);
            this.players[clientId].bullets.setAll('anchor.y', 0.5);

            // Initialize the new player's text
            var style_score = { font: "12px", fill: "#fff", align: "center" };
            this.scores[clientId] = 0;
            this.names[clientId] = name;
            console.log("game.addPlayer.position");
            console.log(position);
            console.log("game.addPlayer.scoreLabels");
            console.log(this.scoreLabels);
            this.scoreLabels[clientId] = this.add.text(position*320, 35, name + "\t\t0", style_score);
            this.scoreLabels[clientId].font = 'Wallpoet';
            //            this.scoreLabels[clientId].tint = colorCode;
            console.log("game.addPlayer.hexColor");
            console.log(hexColor);
            this.scoreLabels[clientId].fill = hexColor;
            console.log(this.scoreLabels[clientId]);
        }
    },

    // Remove a player from the game.
    removePlayer: function(clientId) {
        if (this.game !== undefined) {
            // Look up the player.
            var currentPlayer = this.players[clientId];

            // If the player was not found, ignore and return.
            if (currentPlayer == null) {
                return;
            }

            this.players[clientId].destroy();
            delete this.players[clientId];
            this.scoreLabels[clientId].destroy();
        }

        if($.isEmptyObject(this.players)) {
            console.log("quitting since everyone left");
            this.quitGame();
        }
    },

    // Called to rotate a specific player's spaceship.
    onRotate: function(clientId, direction, strength) {
        // Map the 0 to 20 range strength value to a 150 to (150+300=450) range angular velocity value for the game.
        var velocity = ((strength * 300) / 20) + 150;

        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null || currentPlayer.body == null) {
            return;
        }

        // Update the angular velocity based on the rotate direction (right, left, or none).
        if(direction == 'left') {
            currentPlayer.body.angularVelocity = -velocity;
        } else if(direction == 'right') {
            currentPlayer.body.angularVelocity = velocity;
        } else {
            currentPlayer.body.angularVelocity = 0;
        }
    },

    // Called to enable thrust on a specific player's spaceship.
    onThrust: function onThrust(clientId, thrustEnabled) {
        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null) {
            return;
        }

        // Update the isThrusting flag.
        currentPlayer.isThrusting = thrustEnabled;
    },

    // Called to enable firing on a specific player's spaceship.
    onFire: function(clientId, fireEnabled) {
        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null) {
            return;
        }

        // Update the isFiring flag.
        currentPlayer.isFiring = fireEnabled;
    },

    // Called to set the game's sound on or off.
    onMute: function(mute) {
        this.isMuted = mute;
    },

    /******************************************************************************************************************
     * Private functions
     */
    updatePlayer: function (currentPlayer) {
        // If this is not the current player's turn to update then return. To maintain a high frames-per-second we need
        // to distribute the work across update cycles. Here we are enforcing a rule that only one player gets updated
        // each cycle.
        if ((this.ticks % 4) != currentPlayer.order) {
            return;
        }

        // Thrusting
        if (currentPlayer.isThrusting) {
            this.game.physics.arcade.accelerationFromRotation(currentPlayer.rotation-BasicGame.ORIENTATION_CORRECTION,
                BasicGame.PLAYER_ACC_SPEED, currentPlayer.body.acceleration);
        } else {
            // TODO fix null body
            currentPlayer.body.acceleration.set(0);
        }

        // Firing
        if (currentPlayer.isFiring) {
            this.fire(currentPlayer);
        }

        // Screen Wrapping
        this.screenWrap(currentPlayer);
        currentPlayer.bullets.forEachExists(this.screenWrap, this);

        // If collision detection is enabled....
        if(this.isCollisionsDetection) {
            // Collision detection with the alien.
            if (this.alien) {
                this.physics.arcade.overlap(currentPlayer.bullets, this.alien, this.hit, null, this);
                this.physics.arcade.overlap(this.alien.bullets, currentPlayer, this.hit, null, this);
                this.physics.arcade.overlap(currentPlayer, this.alien, this.collide, null, this);
            }

            // Collision detection with other players.
            var performCollisionCheck = false;
            for (var other_players_id in this.players) {
                // check if player's bullets collided with another player.
                if (other_players_id != currentPlayer.id) {
                    this.physics.arcade.overlap(currentPlayer.bullets, this.players[other_players_id], this.hit, null, this);
                }

                // Check for collisions with other players. The performCollisionCheck boolean variable used to avoid
                // duplicate checks like checking if A and B collided and then B and A collided by only comparing the
                // current player to other players after her in the list. This was added during performance tuning cycle
                // when we were focused on limiting the work done per update cycle in order to maintain a high
                // frames-per-second.
                if (performCollisionCheck) {
                    this.physics.arcade.overlap(currentPlayer, this.players[other_players_id], this.collide, null, this);
                }
                performCollisionCheck = (performCollisionCheck || (other_players_id == currentPlayer.id));
            }
        }
    },

    updateAlien: function() {
        // If this is not the alien's turn to update then return. To maintain a high frames-per-second we need to
        // distribute the work across update cycles. Here we are enforcing a rule that the alien gets updated every
        // other cycle.
        if (this.ticks % 2 == 0) {
            return;
        }

        if(this.alien) {
            this.game.physics.arcade.accelerationFromRotation(this.alien.body.rotation, BasicGame.ALIEN_MAX_SPEED,
                                                              this.alien.body.acceleration);
            this.fire(this.alien);
            this.screenWrap(this.alien);
            this.alien.bullets.forEachExists(this.screenWrap, this);
        }
    },

    setupBackground: function () {
        // Default to no background
        var bgImage = "none";
        var bgRepeat = "no-repeat";

        // If background is enabled...
        if (this.isBackground) {
            // If tiled background...
            if (this.isBackgroundTiled) {
                bgImage = "url(assets/starfield.png)";
                bgRepeat = "repeat";
            }
            // Else Single Image background...
            else {
                bgImage = "url(assets/starfield_full.jpg)";
                bgRepeat = "no-repeat";
            }
        }

        // Update the body
        $("body").css("background-image", bgImage);
        $("body").css("background-repeat", bgRepeat);
    },

    setupSystem: function () {
        // Here I setup some general utilities
        this.game.physics.startSystem(Phaser.Physics.ARCADE);
        this.game.time.events.loop(1000, this.updateTimer, this);
    },

    setupPlayers: function () {
        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];
            if (!slot.available) {
                this.addPlayer(slot.position, slot.clientId, slot.name, slot.colorCode, slot.hexColor);
            }
        }
    },

    setupAlien: function () {
        // If the alien is not enabled then return.
        if (!this.isAlien) {
            return;
        }

        // Here I setup the computer controlled ship
        this.alien = this.game.add.sprite(0, 0, 'ufo');

        var randX = this.rnd.integerInRange(20, this.game.width - 20);
        var randY = this.rnd.integerInRange(20, this.game.height - 20);
        var randAngle = this.rnd.integerInRange(0, 100);

        this.alien.reset(randX, randY, BasicGame.ALIEN_HP);
        this.alien.anchor.setTo(0.5);
        this.physics.enable(this.alien, Phaser.Physics.ARCADE);
        this.alien.angle = randAngle;
        this.alien.body.drag.set(BasicGame.ALIEN_DRAG);
        this.alien.body.maxVelocity.set(BasicGame.ALIEN_MAX_SPEED);

        this.alien.bulletSpeed = BasicGame.ALIEN_BULLET_SPEED;
        this.alien.bulletRange = BasicGame.ALIEN_FIRE_RANGE;
        this.alien.bulletDelay = BasicGame.ALIEN_FIRE_DELAY;

        this.alien.bullets = this.add.group();
        this.alien.bullets.enableBody = true;
        this.alien.bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.alien.bulletTime = 0;

        this.alien.bullets.createMultiple(BasicGame.ALIEN_BULLET_MAXNUM, 'bullets');
        this.alien.bullets.setAll('anchor.x', 0.5);
        this.alien.bullets.setAll('anchor.y', 0.5);
    },

    setupAudio: function () {
        // Here I setup audio
        this.sfx = this.game.add.audio("sfx");
        this.sfx.allowMultiple = true;

        this.sfx.addMarker('alien death', 1, 1.0);
        this.sfx.addMarker('boss hit', 3, 0.5);
        this.sfx.addMarker('escape', 4, 3.2);
        this.sfx.addMarker('meow', 8, 0.5);
        this.sfx.addMarker('numkey', 9, 0.1);
        this.sfx.addMarker('ping', 10, 1.0);
        this.sfx.addMarker('death', 12, 4.2);
        this.sfx.addMarker('shot', 17, 1.0);
        this.sfx.addMarker('squit', 19, 0.3);
    },


    setupText: function () {
        // Here I setup the labels and other texts
        var style = { font: "14px Arial", fill: "#cccccc", align: "left" };
        this.timerLabel = this.add.text((this.game.width/2)-10, 5, "02:00", style);
        this.timerLabel.font = 'Wallpoet';
        this.scores = { };
        this.names = { };
        this.scoreLabels = { };
    },

    /**
     * Checks the player status and respawn if necessary
     *
     */
    playerLifecycle: function () {
        for (var id in this.players) {
            var currentPlayer = this.players[id];

            // player is dead, determine if we should respawn at this time.
            if (!currentPlayer.alive) {
                // To maintain a high frames-per-second we need to distribute the work across update cycles. Here we are
                // enforcing a rule that players get an opportunity to respawn every 16th cycle.
                if ((this.ticks % 16 == 0) && (this.game.time.now - currentPlayer.tod > BasicGame.PLAYER_RESPAWN_DELAY)) {
                    this.resetPlayer(currentPlayer);
                    // Notify the GameManager that the player is back in so that it can notify the client.
                    GameManager.onPlayerOut(currentPlayer.id, 0); // 0 seconds remaining
                }
            }
            // player is alive
            else {
                this.updatePlayer(currentPlayer);
            }
        }
    },

    /**
     * Checks the alien status and respawn if necessary
     *
     */
    alienLifecycle: function () {
        // If the alien is disabled or does not exist, return.
        if (!this.isAlien || !this.alien) {
            return;
        }

        // If the alien is dead, determine if we should respawn at this time.
        if (!this.alien.alive) {
            // To maintain a high frames-per-second we need to distribute the work across update cycles. Here we are
            // enforcing a rule that aliens get an opportunity to respawn every 15th cycle.
            if ((this.ticks % 15 == 0) && (this.game.time.now - this.alien.tod > BasicGame.ALIEN_RESPAWN_DELAY)) {
                this.resetAlien(this.alien);
            }
        }
        // Else the alien is alive
        else {
            this.updateAlien();
        }
    },

    /**
     * Keeps track of the game countdown timer and handles last-10-seconds alert
     * with a sound, red tint and increased size.
     *
     */
    updateTimer: function updateTimer() {
        this.secondsLeft--;
        var minutes = Math.floor(this.secondsLeft/60);
        var seconds = this.secondsLeft - minutes * 60;
        if(seconds < 10) {
            seconds = '0'+seconds;
        }

        this.timerLabel.setText(minutes+":"+seconds);
        if(this.secondsLeft <= 10) {
            this.timerLabel.fontSize = 38;
            //            this.timerLabel.tint = 0xFF0000;
            this.timerLabel.fill = '#FFFF00';
            if(this.secondsLeft == 10 || this.secondsLeft == 5 || this.secondsLeft < 3 && !this.isMuted) {
                this.sfx.play("ping");
            }
            if(this.secondsLeft <= 3) {
                this.timerLabel.fontSize = 46;
                this.timerLabel.fill = '#FF0000';
            }
        }
        if(this.secondsLeft < 0) {
            this.secondsLeft = BasicGame.GAME_LENGTH;
            this.quitGame();
        }
    },

    /*
     * shows a bullet and set it on it's path based on its parent
     *
     */
    fire: function(origin) {
        if (this.game.time.now > origin.bulletTime) {
            origin.bullet = origin.bullets.getFirstExists(false);
            if (origin.bullet) {
                origin.bullet.source = origin.id;
                origin.bullet.reset(origin.body.x + this.halfShipDimens, origin.body.y + this.halfShipDimens);
                origin.bullet.visible = true;
                origin.bullet.body.enabled = true;
                origin.bullet.body.setSize(BasicGame.BULLET_HITBOX_WIDTH, BasicGame.BULLET_HITBOX_HEIGHT, 0, 0);
                origin.bullet.lifespan = origin.bulletRange;
                origin.bullet.rotation = origin.rotation + BasicGame.ORIENTATION_CORRECTION;
                this.game.physics.arcade.velocityFromRotation(origin.rotation-BasicGame.ORIENTATION_CORRECTION, origin.bulletSpeed, origin.bullet.body.velocity);
                origin.bulletTime = this.game.time.now + origin.bulletDelay;
                if (this.isBulletTinting) {
                    origin.bullet.tint = origin.tint;
                }
                if(!this.isMuted) {
                    this.sfx.play('shot');
                }
            }
        }
    },

    /*
     * handle the collision between a bullet and a target
     *
     */
    hit: function(target, bullet) {
        if(!this.isMuted) {
            this.sfx.play('boss hit');
        }
        target.damage(BasicGame.HIT_POW);

        //always show a regular explosion as a bullet hits the target
        this.explode(target, 'explosion');

        if(target == this.alien) {
            this.scores[bullet.source] += BasicGame.ALIEN_HIT_SCORE;
        } else {
            this.scores[bullet.source] += BasicGame.PLAYER_HIT_SCORE;
            if(this.players[bullet.source] != undefined && this.isPointsText) {
                this.showPoints(BasicGame.PLAYER_HIT_SCORE, this.players[target.id].x, this.players[target.id].y, this.players[bullet.source].tint);
            }
        }
        if(target.health <= 0) {
            if(target == this.alien) {
                this.scores[bullet.source] += BasicGame.ALIEN_DESTROY_SCORE;
                if(this.isPointsText){
                    this.showPoints(BasicGame.ALIEN_DESTROY_SCORE, target.body.x, target.body.y, this.players[bullet.source].tint);
                }
            }

            //resulted in death, show a massive explosion
            target.tod = this.game.time.now;
            this.explode(target, 'explosionBig');
            if(!this.isMuted) {
                this.sfx.play("death");
            }

            // If this is a player, notify the GameManager that the player is out so that it can notify the client.
            if(target != this.alien) {
                GameManager.onPlayerOut(target.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
            }
        }

        // deduct points from players on hit
        if(target !== this.alien) {
            this.scores[target.id] -= BasicGame.PLAYER_HIT_DEDUCT;
            if(this.isGameText) {
                this.scoreLabels[target.id].setText(this.names[target.id] + "\t\t"+this.scores[target.id]);
            }
            var player = this.players[target.id];
            if(this.isPointsText){
                this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.tint);
            }
        }

        // update score labels if shot is not from the alien
        var attacker = this.scoreLabels[bullet.source];
        if(attacker != undefined && this.isGameText){
            attacker.setText(this.names[bullet.source] + "\t\t"+this.scores[bullet.source]);
        }

        bullet.kill();
    },

    /*
     * handle the collision between a two objects
     *
     */
    collide: function(obj1, obj2) {
        // If either object is not alive ignore the collision.
        if(!obj1.alive || !obj2.alive) {
            return;
        }

        console.log("collide");
        console.log(obj1);
        console.log(obj2);

        // If obj1 is a player, process as a player out.
        if(obj1 !== this.alien) {
            this.playerOut(obj1);
        }

        // If obj2 is a player, process as a player out.
        if(obj2 !== this.alien) {
            this.playerOut(obj2);
        }

        // If obj2 is an alien, process as an alien out.
        if(obj2 === this.alien) {
            obj2.damage(BasicGame.HIT_POW);
            if(obj2.health <= 0) {
                obj2.tod = this.game.time.now;
                this.explode(obj2, 'explosionBig'); //huge explosion
            }
        }

        // If we are not muted, play the sound of death.
        if(!this.isMuted) {
            this.sfx.play("death");
        }
    },

    playerOut: function (playerObj) {
        // Deduct points from players on hit
        this.scores[playerObj.id] -= BasicGame.PLAYER_HIT_DEDUCT;
        var player = this.players[playerObj.id];
        this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.tint);

        // Update the player's score
        if(this.isGameText) {
            this.scoreLabels[playerObj.id].setText(this.names[playerObj.id] + "\t\t"+this.scores[playerObj.id]);
        }

        // Explore the player
        playerObj.tod = this.game.time.now;
        this.explode(playerObj, 'explosionBig'); //huge explosion
        playerObj.kill();

        // Notify the GameManager that the player is out so that it can notify the client.
        GameManager.onPlayerOut(playerObj.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
    },

    /**
     * Show an Explosion
     *
     * @param {sprite} sprite - the source sprite where to show the explosion
     * @param {sprite} explosionSprite - Named explosion sprite. @see the preloader for the explosion sprites
     */
    explode: function (sprite,explosionSprite) {
        var explosion = this.add.sprite(sprite.x, sprite.y, explosionSprite);
        explosion.anchor.setTo(0.5, 0.5);
        explosion.animations.add('boom');
        explosion.play('boom', 25, false, true);
    },

    /**
     * Display a text on the screen indicating points scored at a position with a given color,
     * the class automatically determines a + or - sign depending on the actual int value
     *
     * @param {points} -
     * @param {x} -
     * @param {y} -
     * @param {color} -
     */
    showPoints: function (points, x, y, color) {
        var sign = "+";
        if(points < 0) {
            if(this.pointsPrompt != null || this.pointsPrompt != undefined) {
                this.pointsPrompt.destroy();
            }
            sign = "";
            this.pointsPrompt = this.add.text( x-40, y, sign + points,
                                              { font: '20px Wallpoet', fill: "#ffffff", align: 'center'});
            this.pointsPrompt.tint = color;
            this.pointsPrompt.anchor.setTo(0.5, 0.5);
            this.pointsExpire = this.time.now + 800;
        } else {
            if(this.pointsUpPrompt != null || this.pointsUpPrompt != undefined) {
                this.pointsUpPrompt.destroy();
            }
            this.pointsUpPrompt = this.add.text( x+48, y, sign + points,
                                                { font: '20px Wallpoet', fill: "#ffffff", align: 'center'});
            this.pointsUpPrompt.tint = color;
            this.pointsUpPrompt.anchor.setTo(0.5, 0.5);
            this.pointsUpExpire = this.time.now + 800;
        }
    },

    /*
     * Wraps sprites from one edge to the opposite
     *
     */
    screenWrap: function(sprite) {
        if(sprite.x < 0) {
            sprite.x = this.game.width;
        }
        else if (sprite.x > this.game.width) {
            sprite.x = 0;
        }

        if (sprite.y < 0) {
            sprite.y = this.game.height;
        } else if (sprite.y > this.game.height) {
            sprite.y = 0;
        }
    },

    /**
     *  Resets a player ship to a random position in the screen with the starting health points
     *
     */
    resetPlayer: function (player){
        var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
        var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));

        player.x = randX;
        player.y = randY;
        player.revive(BasicGame.PLAYER_HP);
    },

    /**
     *  Resets an alien ship to a random position in the screen, facing a random  with the starting health points
     *
     */
    resetAlien: function (){
        var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
        var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));
        var randAngle = this.rnd.integerInRange(0, 360)

        this.alien.x = randX;
        this.alien.y = randY;
        this.alien.revive(BasicGame.ALIEN_HP);
        this.alien.angle = randAngle;
    },

    /**
     * Ends the current game and relases resources
     *
     */
    quitGame: function (pointer) {

        //  Here you should destroy anything you no longer need.
        //  delete sprites, purge caches, free resources, all that good stuff.
        //  Then move on to the game over state.

        for (var id in this.players) {
            this.players[id].destroy();
            this.scoreLabels[id].destroy();
        }
        this.players = {};

        // Notify the Game Manager that the game is over.
        GameManager.onGameOver(this.scores);

        this.state.start('GameOver', true, false, this.scores, this.names);

    },


};