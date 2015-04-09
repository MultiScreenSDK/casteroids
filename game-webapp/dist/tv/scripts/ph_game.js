BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
    this.shipDimens = 48;
    this.halfShipDimens = this.shipDimens/2;

    this.ticks = 0;

    // The variables below were used during performance testing to enable/disable game features to see which had the
    // greatest impact on performance.
    this.isMuted = true;
    this.isAlien = true;
    this.isGameText = true;
    this.isPointsText = false;
    this.isBackground = true;
    this.isCollisionsDetection = true;
    this.isAggressiveUpdateCycle = true;
    this.isFPSdebug = false;
    this.isShortGame = false;
};

BasicGame.Game.prototype = {

    players: {}, //variable on the game prototype

    /******************************************************************************************************************
     * Phaser Lifecycle functions
     */

    preload: function () {
    },

    create: function () {
        //This game will run in Canvas mode, so let's gain a little speed and display
        this.game.renderer.clearBeforeRender = true;
        this.game.renderer.roundPixels = true;

        // Disable keyboard input
        this.game.input.enabled = false;

        // If the GameManager has a config object, then update the configuration using its values. These configurations
        // were used during performance testing to enable/disable game features to see which had the greatest impact on
        // performance.
        this.config = GameManager.getConfig();
        if (this.config !== undefined) {
            this.isMuted = !this.config.isSoundEnabled;
            this.isAlien = this.config.isAlienEnabled;
            this.isGameText = this.config.isGameTextEnabled;
            this.isPointsText = this.config.isPointsTextEnabled;
            this.isBackground = this.config.isBackgroundImageEnabled;
            this.isCollisionsDetection = this.config.isCollisionDetectionEnabled;
            this.isAggressiveUpdateCycle = this.config.isAggressiveUpdateCycle;
            this.isFPSdebug = this.config.isFpsEnabled;
            this.isShortGame = this.config.isShortGameEnabled;
        }

        // Set the length of this game
        this.secondsLeft = this.isShortGame ? BasicGame.SHORT_GAME_LENGTH : BasicGame.GAME_LENGTH;

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
        if (this.isPointsText && (this.ticks % 16 == 0)) {
            if (this.pointsPrompt1 != undefined && this.pointsPrompt1.exists && this.time.now > this.pointsExpire1) {
                this.pointsPrompt1.destroy();
            }
            if (this.pointsPrompt2 != undefined && this.pointsPrompt2.exists && this.time.now > this.pointsExpire2) {
                this.pointsPrompt2.destroy();
            }
        }

        this.ticks++;
    },

    /******************************************************************************************************************
     * Game Callback functions
     */

    /**
     * Called to add a player to the game. At the start of the game this is called from the setupPlayers method. If a
     * player joins during a game its called by the GameManager.
     *
     * @param position
     * @param clientId
     * @param name
     * @param color
     * @param colorCode
     * @param hexColor
     */
    addPlayer: function(position, clientId, name, color, colorCode, hexColor) {
        if (this.game !== undefined) {
            // Initialize the new player
            this.players[clientId] = this.game.add.sprite(0, 0, color);
            this.players[clientId].width = this.shipDimens;
            this.players[clientId].height = this.shipDimens;

            // Setup the user controlled ship
            var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
            var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));
            this.players[clientId].reset(randX, randY, BasicGame.PLAYER_HP);
            this.players[clientId].id = clientId;
            this.players[clientId].hexColor = hexColor;
            this.players[clientId].order = position;
            this.players[clientId].isThrusting = false;
            this.players[clientId].thrustCount = 0;
            this.players[clientId].isFiring = false;
            this.players[clientId].fireCount = 0;
            this.players[clientId].anchor.set(0.5);
            this.physics.enable(this.players[clientId], Phaser.Physics.ARCADE);
            this.players[clientId].body.drag.set(BasicGame.PLAYER_DRAG);
            this.players[clientId].body.maxVelocity.set(BasicGame.PLAYER_MAX_SPEED);

            this.players[clientId].bulletSpeed = BasicGame.PLAYER_BULLET_SPEED;
            this.players[clientId].bulletRange = BasicGame.PLAYER_FIRE_RANGE;
            this.players[clientId].bulletDelay = BasicGame.PLAYER_FIRE_DELAY;

            // Setup the bullets
            this.players[clientId].bullets = this.add.group();
            this.players[clientId].bullets.enableBody = true;
            this.players[clientId].bullets.physicsBodyType = Phaser.Physics.ARCADE;
            this.players[clientId].bulletTime = 0;

            this.players[clientId].bullets.createMultiple(10, 'bullets');
            this.players[clientId].bullets.setAll('anchor.x', 0.5);
            this.players[clientId].bullets.setAll('anchor.y', 0.5);

            // Initialize the new player's text
            var style_score = { font: "36px Arial", fill: hexColor, align: "center" };
            this.scores[clientId] = 0;
            this.names[clientId] = name;

            // Initialize the player's score label
            if(this.isGameText) {
                $('#player'+position).text(name + "\t\t0");
            }
        }
    },

    /**
     * Called to remove a player from the game. Called by the GameManager when it is informed that a player decided to
     * quit the game. :(
     *
     * @param clientId
     */
    removePlayer: function(clientId) {
        if (this.game !== undefined) {
            // Look up the player.
            var currentPlayer = this.players[clientId];

            // Clear the player's score label
            if(this.isGameText) {
                $('#player'+this.players[clientId].order).text('');
            }

            // If the player was not found, ignore and return.
            if (currentPlayer == null) {
                return;
            }

            this.players[clientId].destroy();
            delete this.players[clientId];
        }

        if($.isEmptyObject(this.players)) {
            // quitting since everyone left
            this.quitGame(false);
        }
    },

    /**
     * Called to rotate a specific player's spaceship. Called by the GameManager when it is informed of a rotation
     * change for a specific player.
     *
     * @param clientId
     * @param direction
     * @param strength
     */
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

    /**
     * Called to enable thrust on a specific player's spaceship. Called by the GameManager when it is informed of a
     * thrust event for a specific player.
     *
     * @param clientId
     * @param thrustEnabled
     */
    onThrust: function onThrust(clientId, thrustEnabled) {
        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null) {
            return;
        }

        // Update the isThrusting flag.
        currentPlayer.isThrusting = thrustEnabled;

        // If the thrust button is down, update the thrustCount flag. This is reset each time the user is presses the
        // button. This makes sure that a thust busrt is made each time the user taps the button.
        if (thrustEnabled) {
            // How many update cycles should the thrust be effective for.
            currentPlayer.thrustCount = 7;
        }
    },

    /**
     * Called to enable firing on a specific player's spaceship. Called by the GameManager when it is informed of a
     * fire event for a specific player.
     *
     * @param clientId
     * @param fireEnabled
     */
    onFire: function(clientId, fireEnabled) {
        // Look up the player.
        var currentPlayer = this.players[clientId];

        // If the player was not found, ignore and return.
        if (currentPlayer == null) {
            return;
        }

        // Update the isFiring flag. Whether or not the user is holding down the fire button.
        currentPlayer.isFiring = fireEnabled;

        // If the fire button is down, update the fireCount flag. This is reset each time the user is presses the
        // button. This makes sure that a bullet is shot each time the user taps the button.
        if (fireEnabled) {
            currentPlayer.fireCount = 1;
        }
    },

    /******************************************************************************************************************
     * Private functions
     */

    /**
     * Sets up the background based on the configuration. For performance reasons, the background is not drawn on the
     * canvas but instead the canvas is set as transparent and the background is drawn on the DOM.
     */
    setupBackground: function () {
        // Default to no background
        var bgImage = "none";
        var bgRepeat = "no-repeat";

        // If background is enabled...
        if (this.isBackground) {
            bgImage = "url(assets/starfield.png)";
            bgRepeat = "repeat";
        }

        // Update the body
        $("body").css("background-image", bgImage);
        $("body").css("background-repeat", bgRepeat);
    },

    /**
     * Sets up the system.
     */
    setupSystem: function () {
        // Setup some general utilities
        this.game.physics.startSystem(Phaser.Physics.ARCADE);
        this.game.time.events.loop(1000, this.updateTimer, this);
    },

    /**
     * Sets up the players based on the players that have joined up to this moment in time.
     */
    setupPlayers: function () {
        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];

            // Initialize the slot's score label
            $('#player'+slot.position).css('color', slot.hexColor);
            $('#player'+slot.position).text('');

            // If the slot is taken, create a player for that slot.
            if (!slot.available) {
                this.addPlayer(slot.position, slot.clientId, slot.name, slot.color, slot.colorCode, slot.hexColor);
            }

        }
    },

    /**
     * Sets up the alien's spaceship.
     */
    setupAlien: function () {
        // If the alien is not enabled then return.
        if (!this.isAlien) {
            return;
        }

        // Setup the computer controlled ship
        this.alien = this.game.add.sprite(0, 0, 'ufo');
        this.alien.hexColor = '#000000';

        var randX = this.rnd.integerInRange(20, this.game.width - 20);
        var randY = this.rnd.integerInRange(20, this.game.height - 20);
        var randAngle = this.rnd.integerInRange(0, 360);

        this.alien.reset(randX, randY, BasicGame.ALIEN_HP);
        this.alien.anchor.setTo(0.5);
        this.physics.enable(this.alien, Phaser.Physics.ARCADE);
        this.alien.rotation = randAngle;
        this.alien.body.rotation = randAngle;
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

    /**
     * Sets up the audio. For performance reasons, we defaulted audio off in the game.
     */
    setupAudio: function () {
        // If the game is muted, return.
        if (this.isMuted) {
            return;
        }

        // Setup audio
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

    /**
     * Sets up the game text. For performance reasons, the game text is not written on the canvas, but the canvas is set
     * as transparent and the text is written on the DOM.
     */
    setupText: function () {
        // Set up the countdown text
        $('#countdown').css('color', "#cccccc");
        $('#countdown').css('font-size', "36px");
        $('#countdown').text('');

        // Initialize the countdown text with the starting value
        if (this.isGameText) {
            var minutes = Math.floor(this.secondsLeft/60);
            var seconds = this.secondsLeft - minutes * 60;
            if(seconds < 10) {
                seconds = '0'+seconds;
            }
            $('#countdown').text(minutes+":"+seconds);
        }

        // Initialize objects
        this.scores = { };
        this.names = { };
    },

    /**
     * Called during the update cycle to to update the player status and respawn if necessary
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
     * Called during the update cycle to update a player based on the game state and the state of the controls (thrust,
     * fire, and rotate).
     *
     * @param currentPlayer
     */
    updatePlayer: function (currentPlayer) {
        // To maintain a high frames-per-second we need to distribute the work across update cycles. Here we are
        // enforcing a rule that only one player gets updated each cycle.
        //
        // If in aggressive mode, one update cycle the player processes firing and thrust and the next it will perform
        // collision detection. For example one player will update firing and thrust on update cycle 0 and check for
        // collisions on update cycle 4. Aggressive mode sacrifices responsiveness for performance.
        var updateCycle =  this.isAggressiveUpdateCycle ? (this.ticks % 8) : (this.ticks % 4);

        // If this is the current player's turn to update firing and thrusting....
        if (updateCycle == currentPlayer.order) {
            // Thrusting
            if ((currentPlayer.isThrusting) || (currentPlayer.thrustCount > 0)) {
                if (currentPlayer.thrustCount > 0) {
                    currentPlayer.thrustCount--;
                }
                this.game.physics.arcade.accelerationFromRotation(currentPlayer.rotation, BasicGame.PLAYER_ACC_SPEED,
                    currentPlayer.body.acceleration);
            } else {
                currentPlayer.body.acceleration.set(0);
            }

            // Firing
            if (currentPlayer.isFiring || (currentPlayer.fireCount > 0)) {
                this.fire(currentPlayer);
            }

            // Screen Wrapping
            this.screenWrap(currentPlayer);
            currentPlayer.bullets.forEachExists(this.screenWrap, this);
        }

        // If this is the current player's turn to check for collisions....
        if (updateCycle == (this.isAggressiveUpdateCycle ? (currentPlayer.order+4) : currentPlayer.order)) {
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
        }
    },

    /**
     * Called during the update cycle to update the alien status and respawn if necessary
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
     * Called during the update cycle to update the alien's spaceship based on the game state.
     */
    updateAlien: function() {
        // If this is not the alien's turn to update then return. To maintain a high frames-per-second we need to
        // distribute the work across update cycles. Here we are enforcing a rule that the alien gets updated every
        // 4th or 8th cycle if in aggressive mode. Aggressive mode sacrifices responsiveness for performance.
        var updateCycle = this.isAggressiveUpdateCycle ? (this.ticks % 8) : (this.ticks % 4);
        if (updateCycle != (this.isAggressiveUpdateCycle ? 7 : 3)) {
            return;
        }

        if(this.alien) {
            // Since the alien ship does not rotate, once it reaches max speed we don't need to call the acceleration
            // from rotation function anymore.
            if (this.alien.body.speed < BasicGame.ALIEN_MAX_SPEED) {
                this.game.physics.arcade.accelerationFromRotation(this.alien.body.rotation, BasicGame.ALIEN_MAX_SPEED,
                    this.alien.body.acceleration);
            }
            this.fire(this.alien);
            this.screenWrap(this.alien);
            this.alien.bullets.forEachExists(this.screenWrap, this);
        }
    },

    /**
     * Keeps track of the game countdown timer and handles last-10-seconds alert with a sound, red color and increased
     * size.
     */
    updateTimer: function updateTimer() {
        // Decrement the countdown
        this.secondsLeft--;

        // If the game text is enabled, display the countdown.
        if (this.secondsLeft >= 0 && this.isGameText) {
            var minutes = Math.floor(this.secondsLeft/60);
            var seconds = this.secondsLeft - minutes * 60;
            if(seconds < 10) {
                seconds = '0'+seconds;
            }

            $('#countdown').text(minutes+":"+seconds);

            if(this.secondsLeft <= 10) {
                $('#countdown').css('color', "#FFFF00");
                $('#countdown').css('font-size', "38px");

                if((this.secondsLeft == 10 || this.secondsLeft == 5 || this.secondsLeft < 3) && !this.isMuted) {
                    this.sfx.play("ping");
                }

                if(this.secondsLeft <= 3) {
                    $('#countdown').css('color', "#FF0000");
                    $('#countdown').css('font-size', "46px");
                }
            }
        }

        // Check if the game is over
        if(this.secondsLeft < 0) {
            this.secondsLeft = this.isShortGame ? BasicGame.SHORT_GAME_LENGTH : BasicGame.GAME_LENGTH;
            this.quitGame(true);
        }
    },

    /**
     * shows a bullet and set it on it's path based on its parent
     *
     * @param origin
     */
    fire: function(origin) {
        if ((origin.fireCount > 0) || (this.game.time.now > origin.bulletTime)) {

            // Get an available bullet, if available.
            origin.bullet = origin.bullets.getFirstExists(false);

            // If there is a bullet available...
            if (origin.bullet) {
                // Decrement the fire count
                if (origin.fireCount > 0) {
                    origin.fireCount--;
                }

                // Initialize the bullet if it wasn't already.
                if (origin.bullet.source === undefined) {
                    origin.bullet.source = origin.id;
                    origin.bullet.hexColor = origin.hexColor;
                    origin.bullet.body.enabled = true;
                    origin.bullet.body.setSize(BasicGame.BULLET_HITBOX_WIDTH, BasicGame.BULLET_HITBOX_HEIGHT, 0, 0);
                    origin.bullet.rotation = origin.rotation;
                    origin.bullet.visible = true;
                }

                // Setup the bullet for its cameo appearance.
                origin.bullet.reset(origin.body.x + this.halfShipDimens, origin.body.y + this.halfShipDimens);
                origin.bullet.lifespan = origin.bulletRange;
                this.game.physics.arcade.velocityFromRotation(origin.rotation, origin.bulletSpeed, origin.bullet.body.velocity);
                origin.bulletTime = this.game.time.now + origin.bulletDelay;
                if(!this.isMuted) {
                    this.sfx.play('shot');
                }
            }
        }
    },

    /**
     * handle the collision between a bullet and a target
     *
     * @param target
     * @param bullet
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
                this.showPoints(BasicGame.PLAYER_HIT_SCORE, this.players[target.id].x, this.players[target.id].y, this.players.hexColor, false);
            }
        }
        if(target.health <= 0) {
            if(target == this.alien) {
                this.scores[bullet.source] += BasicGame.ALIEN_DESTROY_SCORE;
                if(this.isPointsText){
                    this.showPoints(BasicGame.ALIEN_DESTROY_SCORE, target.body.x, target.body.y, this.players[bullet.source].hexColor, false);
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

            if (this.scores[target.id] < 0) {
                this.scores[target.id] = 0;
            }

            if(this.isGameText) {
                $('#player'+target.order).text(this.names[target.id] + "\t\t"+this.scores[target.id]);
            }

            if(this.isPointsText){
                var player = this.players[target.id];
                this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.hexColor, true);
            }
        }

        // update score labels if shot is not from the alien
        var attacker = this.players[bullet.source];
        if(attacker !== undefined && this.isGameText) {
            $('#player'+attacker.order).text(this.names[attacker.id] + "\t\t"+this.scores[attacker.id]);
        }

        bullet.kill();
    },

    /**
     * handle the collision between a two objects
     *
     * @param obj1
     * @param obj2
     */
    collide: function(obj1, obj2) {
        // If either object is not alive ignore the collision.
        if(!obj1.alive || !obj2.alive) {
            return;
        }

        // If obj1 is a player, process as a player out.
        if(obj1 !== this.alien) {
            this.playerOut(obj1, true);
        }

        // If obj2 is a player, process as a player out.
        if(obj2 !== this.alien) {
            this.playerOut(obj2, false);
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

    /**
     * Temporarily removes the player from the game after hitting the alien ship or by a bullet.
     *
     * @param player
     * @param leftCollision
     */
    playerOut: function (player, leftCollision) {
        // Deduct points from players on hit
        this.scores[player.id] -= BasicGame.PLAYER_HIT_DEDUCT;

        if (this.scores[player.id] < 0) {
            this.scores[player.id] = 0;
        }

        if(this.isPointsText) {
            if (leftCollision) {
                this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y, player.hexColor, true);
            } else {
                this.showPoints(-BasicGame.PLAYER_HIT_DEDUCT, player.x, player.y + (player.height / 2), player.hexColor, false);
            }
        }

        // Update the player's score
        if(this.isGameText) {
            $('#player'+player.order).text(this.names[player.id] + "\t\t"+this.scores[player.id]);
        }

        // Explode the player
        player.tod = this.game.time.now;
        this.explode(player, 'explosionBig'); //huge explosion
        player.kill();

        // Notify the GameManager that the player is out so that it can notify the client.
        GameManager.onPlayerOut(player.id, (BasicGame.PLAYER_RESPAWN_DELAY / 1000)); // seconds remaining
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
     * @param {left} -
     */
    showPoints: function (points, x, y, hexColor, left) {
        var sign = (points < 0) ? "" : "+";

        // If the score should be on the left
        if(left) {
            if(this.pointsPrompt1 != null || this.pointsPrompt1 != undefined) {
                this.pointsPrompt1.destroy();
            }
            this.pointsPrompt1 = this.add.text( x-40, y, sign + points,
                                              { font: '20px Wallpoet', fill: hexColor, align: 'center'});
            this.pointsPrompt1.anchor.setTo(0.5, 0.5);
            this.pointsExpire1 = this.time.now + 800;
        }
        // Else the score should be on the right
        else {
            if(this.pointsPrompt2 != null || this.pointsPrompt2 != undefined) {
                this.pointsPrompt2.destroy();
            }
            this.pointsPrompt2 = this.add.text( x+48, y, sign + points,
                                                { font: '25px Arial', fill: hexColor, align: 'center'});
            this.pointsPrompt2.anchor.setTo(0.5, 0.5);
            this.pointsExpire2 = this.time.now + 800;
        }
    },

    /**
     * Wraps sprites from one edge to the opposite
     *
     * @param sprite
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
     * Resets a player ship to a random position in the screen with the starting health points
     *
     * @param player
     */
    resetPlayer: function (player){
        var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
        var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));

        player.x = randX;
        player.y = randY;
        player.revive(BasicGame.PLAYER_HP);
    },

    /**
     * Resets an alien ship to a random position in the screen, facing a random  with the starting health points
     */
    resetAlien: function (){
        var randX = this.rnd.integerInRange(this.shipDimens, this.game.width - (this.shipDimens*2));
        var randY = this.rnd.integerInRange(this.shipDimens, this.game.height - (this.shipDimens*2));
        var randAngle = this.rnd.integerInRange(0, 360)

        this.alien.x = randX;
        this.alien.y = randY;
        this.alien.revive(BasicGame.ALIEN_HP);
        this.alien.rotation = randAngle;
        this.alien.body.rotation = randAngle;
    },

    /**
     * Ends the current game and releases resources before moving on to the game over or main menu state depending on
     * whether or not the game has just ended.
     *
     * @param gameEnded
     */
    quitGame: function (gameEnded) {

        // Here we destroy everything that we no longer need. Delete sprites, purge caches, free resources, etc. Then
        // move on to the game over or main menu state depending on whether or not the game has just ended.

        // Destroy the alien
        if (this.isAlien) {
            this.alien.bullets.destroy(true);
            this.alien.destroy();
        }

        // Destroy all the players
        for (var id in this.players) {
            this.players[id].bullets.destroy(true);
            this.players[id].destroy();
        }
        this.players = {};

        // Destroy the audio
        if (!this.isMuted) {
            this.sfx.destroy(true);
        }

        // If the game just ended, notify the Game Manager that the game is over and transition to the Game Over state
        if (gameEnded) {
            GameManager.onGameOver(this.scores);
            this.state.start('GameOver', true, false, this.scores, this.names);
        }
        // Else all the players left the game, go back to the main menu.
        else {
            this.state.start('MainMenu');
        }
    }

};