BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
    this.isMuted = false;
    this.shipDimens = 48;
    this.halfShipDimens = this.shipDimens/2;
};

BasicGame.Game.prototype = {
    players: {},

    player: function (id, order, color) {
        // Here I setup the user controlled ship
        var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
        var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));
        this.players[id] = this.game.add.sprite(randX, randY, 'ship');
        this.players[id].id = id;
        this.players[id].order = order;
        this.players[id].isThrusting = false;
        this.players[id].isFiring = false;
        this.players[id].tint = color;
        this.players[id].anchor.set(0.5);
        this.physics.enable(this.players[id], Phaser.Physics.ARCADE);
        this.players[id].body.drag.set(BasicGame.PLAYER_DRAG);
        this.players[id].body.maxVelocity.set(BasicGame.PLAYER_MAX_SPEED);

        this.players[id].hp = BasicGame.PLAYER_HP;
        this.players[id].bulletSpeed = BasicGame.PLAYER_BULLET_SPEED;
        this.players[id].bulletRange = BasicGame.PLAYER_FIRE_RANGE;
        this.players[id].bulletDelay = BasicGame.PLAYER_FIRE_DELAY;

        // Here I setup the bullets
        this.players[id].bullets = this.add.group();
        this.players[id].bullets.enableBody = true;
        this.players[id].bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.players[id].bulletTime = 0;

        this.players[id].bullets.createMultiple(40, 'bullets');
        this.players[id].bullets.setAll('anchor.x', 0.5);
        this.players[id].bullets.setAll('anchor.y', 0.5);
    },

    preload: function () {
        //TODO:  Remove before shipping.  To calculate fps during testing. Used in the render function. Remove this before shipping.
        this.game.time.advancedTiming = true;
    },

    create: function () {
        this.setupSystem();
        this.setupText();
        this.setupPlayers();
        this.setupAlien();
        this.setupAudio();
    },

    update: function () {
        //  Main Game Loop

        for (var id in this.players) {
            var currentPlayer = this.players[id];

            // screen wrapping
            this.screenWrap(currentPlayer);
            currentPlayer.bullets.forEachExists(this.screenWrap, this);
            // collision detection
            this.physics.arcade.overlap(currentPlayer.bullets, this.alien, this.hit, null, this);
            this.physics.arcade.overlap(this.alien.bullets, currentPlayer, this.hit, null, this);

            this.physics.arcade.overlap(currentPlayer, this.alien, this.collide, null, this);

            for (var other_players_id in this.players) {
                if(other_players_id != id) {
                    //check for bullets
                    this.physics.arcade.overlap(currentPlayer.bullets, this.players[other_players_id], this.hit, null, this);

                    //check for collisions. both die if there is a collision
                    this.physics.arcade.overlap(currentPlayer, this.players[other_players_id], this.collide, null, this);
                }
            }

            // players lifecycle
            if(currentPlayer.isDead) {
                // If its time to respawn the player...
                if(this.game.time.now - currentPlayer.tod > BasicGame.PLAYER_RESPAWN_DELAY) {
                    this.player(currentPlayer.id, currentPlayer.order, currentPlayer.tint);

                    // Notify the GameManager that the player is back in so that it can notify the client.
                    GameManager.onPlayerOut(currentPlayer.id, 0); // 0 seconds remaining
                }
            }
            // player control
            else {
                if (currentPlayer.isThrusting) {
                    this.game.physics.arcade.accelerationFromRotation(currentPlayer.rotation-BasicGame.ORIENTATION_CORRECTION, BasicGame.PLAYER_ACC_SPEED, currentPlayer.body.acceleration);
                } else {
                    // TODO fix null body
                    currentPlayer.body.acceleration.set(0);
                }

                if (currentPlayer.isFiring) {
                    this.fire(currentPlayer);
                }

                // replenish destroyed bullets
                if(currentPlayer.bullets.total < 40) {
                    currentPlayer.bullets.createMultiple(1, 'bullets');
                }
            }
        }

        if (this.input.keyboard.isDown(Phaser.Keyboard.ESC)) {
            this.isMuted = !this.isMuted;
        }

        this.screenWrap(this.alien);
        this.alien.bullets.forEachExists(this.screenWrap, this);

        // alien lifecycle
        if(this.alien.isDead) {
            if(this.game.time.now - this.alien.tod > BasicGame.ALIEN_RESPAWN_DELAY) {
                this.setupAlien();
            }
        } else {
            this.alien.body.acceleration.set(BasicGame.ALIEN_MAX_SPEED);
            this.fire(this.alien);
        }
        
        // check for points prompt expiring
        if (this.pointsPrompt != undefined && this.pointsPrompt.exists && this.time.now > this.pointsExpire) {
            this.pointsPrompt.destroy();
        }
        // check for points prompt expiring
        if (this.pointsUpPrompt != undefined && this.pointsUpPrompt.exists && this.time.now > this.pointsUpExpire) {
            this.pointsUpPrompt.destroy();
        }
    },

    render: function() {
        //TODO:  Remove before shipping.  Show this during testing at the top left corner of the screen
        this.game.debug.text(this.game.time.fps || '--', 2, 14, "#00ff00");
    },

    // Setup functions
    setupSystem: function () {
        // Here I setup some general utilities
        this.game.physics.startSystem(Phaser.Physics.ARCADE);
        this.game.time.events.loop(1000, this.updateTimer, this);
        this.background = this.add.tileSprite(0, 0, 1280, 800, 'starfield');
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
        // Here I setup the computer controlled ship
        var randX = this.rnd.integerInRange(20, this.game.width - 20);
        var randY = this.rnd.integerInRange(20, this.game.height - 20);
        var randRotation = this.rnd.integerInRange(0, 360);
        var randAngularVelocity = this.rnd.integerInRange(100, 200);

        this.alien = this.game.add.sprite(randX, randY, 'ufo');
//        this.alien.tint = BasicGame.ALIEN_COLOR;
        this.alien.anchor.setTo(0.5);
//        this.alien.animations.add('fly', [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15], 100, true);
//        this.alien.play('fly');
        this.physics.enable(this.alien, Phaser.Physics.ARCADE);
        this.alien.rotation = randRotation;
        this.alien.body.angularVelocity = randAngularVelocity;
        this.alien.body.drag.set(BasicGame.ALIEN_DRAG);
        this.alien.body.maxVelocity.set(BasicGame.ALIEN_MAX_SPEED);

        this.alien.hp = BasicGame.ALIEN_HP;
        this.alien.bulletSpeed = BasicGame.ALIEN_BULLET_SPEED;
        this.alien.bulletRange = BasicGame.ALIEN_FIRE_RANGE;
        this.alien.bulletDelay = BasicGame.ALIEN_FIRE_DELAY;

        this.alien.bullets = this.add.group();
        this.alien.bullets.enableBody = true;
        this.alien.bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.alien.bulletTime = 0;

        this.alien.bullets.createMultiple(40, 'bullets');
        this.alien.bullets.setAll('anchor.x', 0.5);
        this.alien.bullets.setAll('anchor.y', 0.5);
    },

    setupAudio: function () {
        // Here I setup audio
        this.audio_fire = this.game.add.audio("playerFire");
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

    // Miscellaneous functions
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
            this.timerLabel.tint = 0xFF0000;
            if(!this.isMuted) {
                this.sfx.play("ping");
            }
        }
        if(this.secondsLeft == 0) {
            this.secondsLeft = BasicGame.GAME_LENGTH;
            this.quitGame();
        }
    },

    quitGame: function (pointer) {

        //  Here you should destroy anything you no longer need.
        //  delete sprites, purge caches, free resources, all that good stuff.
        //  Then move on to the game over state.

        // Notify the Game Manager that the game is over.
        GameManager.onGameOver(this.scores);

        this.state.start('GameOver', true, false, this.scores, this.names);


    },

    fire: function(origin) {
        if (this.game.time.now > origin.bulletTime) {
            origin.bullet = origin.bullets.getFirstExists(false);
            origin.bullet.source = origin.id;
            if (origin.bullet) {
                origin.bullet.reset(origin.body.x + this.halfShipDimens, origin.body.y + this.halfShipDimens);
                origin.bullet.body.setSize(BasicGame.BULLET_HITBOX_WIDTH, BasicGame.BULLET_HITBOX_HEIGHT, 0, 0);
                origin.bullet.lifespan = origin.bulletRange;
                origin.bullet.rotation = origin.rotation + BasicGame.ORIENTATION_CORRECTION;
                this.game.physics.arcade.velocityFromRotation(origin.rotation-BasicGame.ORIENTATION_CORRECTION, origin.bulletSpeed, origin.bullet.body.velocity);
                origin.bulletTime = this.game.time.now + origin.bulletDelay;
                origin.bullet.tint = origin.tint;
                if(!this.isMuted) {
                    this.sfx.play('shot');
                }
            }
        }
    },

    hit: function(target, bullet) {
        bullet.destroy();
        if(!this.isMuted) {
            this.sfx.play('boss hit');
        }
        target.hp -= BasicGame.HIT_POW;

        //always show a regular explosion as a bullet hits the target
        this.explode(target, 'explosion');

        if(target == this.alien) {
            this.scores[bullet.source] += BasicGame.ALIEN_HIT_SCORE;
        } else {
            this.scores[bullet.source] += BasicGame.PLAYER_HIT_SCORE;
            if(this.players[bullet.source] != undefined){
                this.showPoints(BasicGame.PLAYER_HIT_SCORE, this.players[target.id].x, this.players[target.id].y, this.players[bullet.source].tint);
            }
        }
        if(target.hp <= 0) {
            if(target == this.alien) {
                this.scores[bullet.source] += BasicGame.ALIEN_DESTROY_SCORE;
                this.showPoints(BasicGame.ALIEN_DESTROY_SCORE, target.body.x, target.body.y, this.players[bullet.source].tint);
            }
            target.isDead = true;
            target.tod = this.game.time.now;
            target.destroy();

            //resulted in death, show a massive explosion
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
            this.scoreLabels[target.id].setText(this.names[target.id] + "\t\t"+this.scores[target.id]);
            var player = this.players[target.id];
            this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.tint);
        }

        // update score labels if shot is not from the alien
        var attacker = this.scoreLabels[bullet.source];
        if(attacker != undefined){
            attacker.setText(this.names[bullet.source] + "\t\t"+this.scores[bullet.source]);
        }
    },
    
    collide: function(obj1, obj2) {
        // deduct points from players on hit
        if(obj1 !== this.alien) {
            this.scores[obj1.id] -= BasicGame.PLAYER_HIT_DEDUCT;
            var player = this.players[obj1.id];
            this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.tint);
            this.scoreLabels[obj1.id].setText(this.names[obj1.id] + "\t\t"+this.scores[obj1.id]);
            this.explode(obj1, 'explosionBig'); //huge explosion
            obj1.isDead = true;
            obj1.tod = this.game.time.now;
            obj1.destroy();
            // notify the GameManager that the player is out so that it can notify the client.
            GameManager.onPlayerOut(obj1.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
        }
        
        if(obj2 !== this.alien) {
            this.scores[obj2.id] -= BasicGame.PLAYER_HIT_DEDUCT;
            var player = this.players[obj2.id];
            this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y+(player.height/2), player.tint);
            this.scoreLabels[obj2.id].setText(this.names[obj2.id] + "\t\t"+this.scores[obj2.id]);
            this.explode(obj2, 'explosionBig'); //huge explosion
            obj2.isDead = true;
            obj2.tod = this.game.time.now;
            obj2.destroy();
            // notify the GameManager that the player is out so that it can notify the client.
            GameManager.onPlayerOut(obj2.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
        }
        this.sfx.play("death");
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
    
    showPoints: function (points, x, y, color) {
        var sign = "+";
        if(points < 0) {
            sign = "";
            this.pointsPrompt = this.add.text( x-40, y, sign + points,
                { font: '20px Wallpoet', fill: "#ffffff", align: 'center'});
            this.pointsPrompt.tint = color;
            this.pointsPrompt.anchor.setTo(0.5, 0.5);
            this.pointsExpire = this.time.now + 800;
        } else {
            this.pointsUpPrompt = this.add.text( x+48, y, sign + points,
                { font: '20px Wallpoet', fill: "#ffffff", align: 'center'});
            this.pointsUpPrompt.tint = color;
            this.pointsUpPrompt.anchor.setTo(0.5, 0.5);
            this.pointsUpExpire = this.time.now + 800;
        }
    },

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

    // Add a player to the game.
    addPlayer: function(position, clientId, name, colorCode, hexColor) {
        console.log("game.addPlayer " + position);
        if (this.game !== undefined) {
            // Initialize the new player
            this.player(clientId, position, colorCode);

            // Initialize the new player's text
            var style_score = { font: "12px", fill: "#fff", align: "right" };
            this.scores[clientId] = 0;
            this.names[clientId] = name;
            this.scoreLabels[clientId] = this.add.text(position*320, 35, name + "\t\t0", style_score);
            this.scoreLabels[clientId].font = 'Wallpoet';
            this.scoreLabels[clientId].tint = colorCode;
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
        var velocity = ((strength * 300) / 10) + 150;

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
    }

};
