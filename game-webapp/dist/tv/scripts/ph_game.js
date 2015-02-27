BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
    this.NUM_PLAYERS = 2;
    this.COLORS = [0x00FF00, 0xFFFF00];
    this.isMuted = false;
};


BasicGame.Game.prototype = {
    players: {},

    player: function (pos, id, color) {
        // Here I setup the user controlled ship
        this.X_POSITIONS = [this.game.width/4, 3*(this.game.width/4)];
        this.Y_POSITIONS = [this.game.height/4, 3*(this.game.height/4)];
        this.players[pos] = this.game.add.sprite(this.X_POSITIONS[pos], this.Y_POSITIONS[pos], 'ship');
        this.players[pos].id = id;
        this.players[pos].isThrusting = false;
        this.players[pos].isFiring = false;
        this.players[pos].tint = color;
        this.players[pos].anchor.setTo(0.5);
        this.physics.enable(this.players[pos], Phaser.Physics.ARCADE);
        this.players[pos].body.drag.set(BasicGame.PLAYER_DRAG);
        this.players[pos].body.maxVelocity.set(BasicGame.PLAYER_MAX_SPEED);

        this.players[pos].hp = BasicGame.PLAYER_HP;
        this.players[pos].bulletSpeed = BasicGame.PLAYER_BULLET_SPEED;
        this.players[pos].bulletRange = BasicGame.PLAYER_FIRE_RANGE;
        this.players[pos].bulletDelay = BasicGame.PLAYER_FIRE_DELAY;

        // Here I setup the bullets
        this.players[pos].bullets = this.add.group();
        this.players[pos].bullets.enableBody = true;
        this.players[pos].bullets.physicsBodyType = Phaser.Physics.ARCADE;
        this.players[pos].bulletTime = 0;

        this.players[pos].bullets.createMultiple(40, 'laser');
        this.players[pos].bullets.setAll('anchor.x', 0.5);
        this.players[pos].bullets.setAll('anchor.y', 0.5);
    },

    create: function () {
        console.log('HERE: create! 1');
        this.setupSystem();
        console.log('HERE: create! 2');
        this.setupPlayers();
        console.log('HERE: create! 3');
        this.setupAlien();
        this.setupText();
        this.setupAudio();
    },

    update: function () {
        //  Main Game Loop

        for (var index in this.players) {
            var currentPlayer = this.players[index];

            // screen wrapping
            this.screenWrap(currentPlayer);
            currentPlayer.bullets.forEachExists(this.screenWrap, this);
            // collision detection
            this.physics.arcade.overlap(currentPlayer.bullets, this.alien, this.hit, null, this);
            this.physics.arcade.overlap(this.alien.bullets, currentPlayer, this.hit, null, this);

            // players lifecycle
            if(currentPlayer.isDead) {
                if(this.game.time.now - currentPlayer.tod > BasicGame.PLAYER_RESPAWN_DELAY) {
                    this.player(index, index, currentPlayer.tint);
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
        console.log('HERE: setupPlayers! ');
        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];
            if (!slot.isAvailable) {
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
        var style_score = { font: "14px Arial", fill: "#cccccc", align: "right" };
        this.scores = [this.players.length];
        this.scoreLabels = [this.players.length];
        for (index = 0; index < this.players.length; index++) {
            this.scores[index] = 0;
            this.scoreLabels[index] = this.add.text(this.game.width-100, 20*(index+1), "0", style_score);
        }
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
        this.state.state['GameOver'].scores = this.scores;
        this.state.start('GameOver');

        // Notify the Game Manager that the game is over.
        GameManager.onGameOver();
    },

    fire: function(player) {
        if (this.game.time.now > player.bulletTime) {
            player.bullet = player.bullets.getFirstExists(false);
            player.bullet.source = player.id;
            if (player.bullet) {
                player.bullet.reset(player.body.x + 32, player.body.y + 32);
                player.bullet.lifespan = player.bulletRange;;
                player.bullet.rotation = player.rotation + BasicGame.ORIENTATION_CORRECTION;
                this.game.physics.arcade.velocityFromRotation(player.rotation-BasicGame.ORIENTATION_CORRECTION, player.bulletSpeed, player.bullet.body.velocity);
                player.bulletTime = this.game.time.now + player.bulletDelay;
                player.bullet.tint = player.tint;
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
        this.player(clientId, clientId, colorCode); // re-using the index as the id, it can be changed later on
       }
    },

    // Remove a player from the game.
    removePlayer: function(clientId) {
        if (this.game !== undefined) {
            delete this.player[clientId];
        }
    },

    // Called to rotate a specific player's spaceship.
    onRotate: function(clientId, direction, strength) {
        // Map the 0 to 20 range strength value to a 100 to 400 range angular velocity value for the game.
        var velocity = ((strength * 400) / 20) + 100;

        // Update the angular velocity based on the rotate direction (right, left, or none).
        if(direction == 'left') {
            this.players[clientId].body.angularVelocity = -velocity;
        } else if(direction == 'right') {
            this.players[clientId].body.angularVelocity = velocity;
        } else {
            this.players[clientId].body.angularVelocity = 0;
        }
    },

    // Called to enable thrust on a specific player's spaceship.
    onThrust: function onThrust(clientId, thrustEnabled) {
        this.players[clientId].isThrusting = thrustEnabled;
    },

    // Called to enable firing on a specific player's spaceship.
    onFire: function(clientId, fireEnabled) {
        this.players[clientId].isFiring = fireEnabled;
    }
};
