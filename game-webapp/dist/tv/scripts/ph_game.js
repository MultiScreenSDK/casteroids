BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
    this.isMuted = false;
};

BasicGame.Game.prototype = {
    players: {},

    player: function (id, order, color) {
        // Here I setup the user controlled ship
        this.X_POSITIONS = [this.game.width/4, 3*(this.game.width/4)];
        this.Y_POSITIONS = [this.game.height/4, 3*(this.game.height/4)];
        this.players[id] = this.game.add.sprite(this.X_POSITIONS[order], this.Y_POSITIONS[order], 'ship');
        this.players[id].id = id;
        this.players[id].order = order;
        this.players[id].isThrusting = false;
        this.players[id].isFiring = false;
        this.players[id].tint = color;
        this.players[id].anchor.setTo(0.5);
        this.physics.enable(this.players[id], Phaser.Physics.ARCADE);
        this.players[id].body.drag.set(BasicGame.PLAYER_DRAG);
        this.players[id].body.maxVelocity.set(BasicGame.PLAYER_MAX_SPEED);
        this.players[id].body.setSize(BasicGame.PLAYER_HITBOX_WIDTH, BasicGame.PLAYER_HITBOX_HEIGHT, 0, 0);

        this.players[id].hp = BasicGame.PLAYER_HP;
        this.players[id].bulletSpeed = BasicGame.PLAYER_BULLET_SPEED;
        this.players[id].bulletRange = BasicGame.PLAYER_FIRE_RANGE;
        this.players[id].bulletDelay = BasicGame.PLAYER_FIRE_DELAY;

        // Here I setup the bullets
        this.players[id].bullets = this.add.group();
        this.players[id].bullets.enableBody = true;
        this.players[id].bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.players[id].bulletTime = 0;

        this.players[id].bullets.createMultiple(40, 'laser');
        this.players[id].bullets.setAll('anchor.x', 0.5);
        this.players[id].bullets.setAll('anchor.y', 0.5);
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
//            this.physics.arcade.overlap(currentPlayer, this.alien, this.hit, null, this);

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
                    currentPlayer.body.acceleration.set(0);
                }

                if (currentPlayer.isFiring) {
                    this.fire(currentPlayer);
                }

                // replenish destroyed bullets
                if(currentPlayer.bullets.total < 40) {
                    currentPlayer.bullets.createMultiple(1, 'laser');
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
            // homing logic
            //          this.enemy.rotation = this.physics.arcade.angleBetween(this.enemy, this.player) + BasicGame.ORIENTATION_CORRECTION;
            //          this.physics.arcade.moveToObject(this.enemy, this.player.body, BasicGame.ENEMY_HOMING_SPEED, BasicGame.ENEMY_HOMING_MAX_TIME);
            this.alien.body.acceleration.set(BasicGame.ALIEN_MAX_SPEED);
            this.fire(this.alien);
        }
    },

    // Setup functions
    setupSystem: function () {
        // Here I setup some general utilities
        this.game.physics.startSystem(Phaser.Physics.ARCADE);
        this.cursors = this.input.keyboard.createCursorKeys();
        this.game.time.events.loop(1000, this.updateTimer, this);
        this.background = this.add.tileSprite(0, 0, 1280, 800, 'space');
    },

    setupPlayers: function () {
        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];
            if (!slot.available) {
                this.addPlayer(slot.clientId, slot.name, slot.colorCode);
            }
        }
    },

    setupAlien: function () {
        // Here I setup the computer controlled ship
        var randX = this.rnd.integerInRange(20, this.game.width - 20);
        var randY = this.rnd.integerInRange(20, this.game.height - 20);
        var randRotation = this.rnd.integerInRange(0, 360);

        this.alien = this.game.add.sprite(randX, randY, 'ufo');
        this.alien.tint = BasicGame.ALIEN_COLOR;
        this.alien.anchor.setTo(0.5);
        this.alien.animations.add('fly', [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15], 100, true);
        this.alien.play('fly');
        this.physics.enable(this.alien, Phaser.Physics.ARCADE);
        this.alien.rotation = randRotation;
        this.alien.body.drag.set(BasicGame.ALIEN_DRAG);
        this.alien.body.maxVelocity.set(BasicGame.ALIEN_MAX_SPEED);
        this.alien.body.setSize(BasicGame.ALIEN_HITBOX_WIDTH, BasicGame.ALIEN_HITBOX_HEIGHT, 0, 0);

        this.alien.hp = BasicGame.ALIEN_HP;
        this.alien.bulletSpeed = BasicGame.ALIEN_BULLET_SPEED;
        this.alien.bulletRange = BasicGame.ALIEN_FIRE_RANGE;
        this.alien.bulletDelay = BasicGame.ALIEN_FIRE_DELAY;

        this.alien.bullets = this.add.group();
        this.alien.bullets.enableBody = true;
        this.alien.bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.alien.bulletTime = 0;

        this.alien.bullets.createMultiple(40, 'laser');
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
        this.timerLabel = this.add.text(20, 20, "02:00", style);
        this.timerLabel.font = 'Revalia';
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
        if(this.secondsLeft == 0) {
            this.secondsLeft = BasicGame.GAME_LENGTH;
            this.quitGame();
        }
    },

    quitGame: function (pointer) {

        //  Here you should destroy anything you no longer need.
        //  Stop music, delete sprites, purge caches, free resources, all that good stuff.
        //  Then let's go back to the main menu.
//        this.state.states['GameOver'].scores = this.scores;
//        this.state.states['GameOver'].names = this.names;
        this.state.start('GameOver');

        // Notify the Game Manager that the game is over.
        GameManager.onGameOver(this.scores);
    },

    fire: function(origin) {
        if (this.game.time.now > origin.bulletTime) {
            origin.bullet = origin.bullets.getFirstExists(false);
            origin.bullet.source = origin.id;
            origin.bullet.body.setSize(BasicGame.BULLET_HITBOX_WIDTH, BasicGame.BULLET_HITBOX_HEIGHT, 0, 0);
            if (origin.bullet) {
                origin.bullet.reset(origin.body.x + 32, origin.body.y + 32);
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
        if(target == this.alien) {
            this.scores[bullet.source] += BasicGame.ALIEN_HIT_SCORE;
        } else {
            this.scores[bullet.source] += BasicGame.PLAYER_HIT_SCORE;
        }
        if(target.hp <= 0) {
            if(target == this.alien) {
                this.scores[bullet.source] += BasicGame.ALIEN_DESTROY_SCORE;
            }
            this.explode(target);
            target.isDead = true;
            target.tod = this.game.time.now;
            target.destroy();

            // Notify the GameManager that the player is out so that it can notify the client.
            GameManager.onPlayerOut(target.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
        }

        // deduct points from players on hit
        if(target != this.alien) {
            console.log(target);
            console.log(this.scores);
            if(this.scores[target.id] > 0) {
                this.scores[target.id] -= BasicGame.HIT_DEDUCT;
                this.scoreLabels[target.id].setText(this.scores[target.id]);
            }
        }

        // update score labels if shot is not from the alien
        if(bullet.source >= 0){
            this.scoreLabels[bullet.source].setText(this.scores[bullet.source]);
        }
    },

    explode: function (sprite) {
        var explosion = this.add.sprite(sprite.x, sprite.y, 'explosion');
        explosion.anchor.setTo(0.5, 0.5);
        explosion.animations.add('boom');
        explosion.play('boom', 25, false, true);

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
    addPlayer: function(clientId, name, colorCode) {
        if (this.game !== undefined) {
            //  Determine the new player's order.
            var order = Object.keys(this.players).length;

            // Initialize the new player
            this.player(clientId, order, colorCode);

            // Initialize the new player's text
            var style_score = { font: "14px Arial", fill: "#cccccc", align: "right" };
            this.scores[clientId] = 0;
            this.names[clientId] = name;
            this.scoreLabels[clientId] = this.add.text(this.game.width-100, 20*(order+1), "0", style_score);
            this.scoreLabels[clientId].font = 'Revalia';
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

            // TODO: Adrian, please remove the player from the game.
            //currentPlayer.destroy();
            delete this.player[clientId];
        }
    },

    // Called to rotate a specific player's spaceship.
    onRotate: function(clientId, direction, strength) {
        // Map the 0 to 20 range strength value to a 100 to 400 range angular velocity value for the game.
        var velocity = ((strength * 400) / 20) + 100;

        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null) {
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
    
    render: function() {
        this.game.debug.body(this.alien.bullet);
        this.game.debug.body(this.alien);
    },
};
