BasicGame.Game = function (game) {
    this.secondsLeft = BasicGame.GAME_LENGTH;
};


BasicGame.Game.prototype = {
    players: [],
    
    player: function (pos, id, color) {
        // Here I setup the user controlled ship
        
    this.X_POSITIONS = [this.game.width/4, 3*(this.game.width/4)];
    this.Y_POSITIONS = [this.game.height/4, 3*(this.game.height/4)];
      this.players[pos] = this.game.add.sprite((this.game.width/4)*(pos+1), this.Y_POSITIONS[pos], 'ship');
      this.players[pos].id = id;
      this.players[pos].tint = color;
      this.players[pos].anchor.setTo(0.5);
      // current sprite doesn't have any animation
//      this.player.animations.add('fly', [ 0, 1, 2 ], 20, true);
//      this.player.play('fly');
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
      this.setupSystem();
      this.setupPlayers();
      this.setupEnemy();
      this.setupExplosions();
      this.setupText();
      this.setupAudio();    
  },

  update: function () {
      //  Main Game Loop
      
      for (index = 0; index < this.players.length; index++) {
          // screen wrapping
          this.screenWrap(this.players[index]);
          this.players[index].bullets.forEachExists(this.screenWrap, this);
          // collision detection
          this.physics.arcade.overlap(this.players[index].bullets, this.enemy, this.hit, null, this);
          this.physics.arcade.overlap(this.enemy.bullets, this.players[index], this.hit, null, this);
          
          // players lifecycle
          if(this.players[index].isDead) {
              if(this.game.time.now - this.players[index].tod > BasicGame.PLAYER_RESPAWN_DELAY) {
                  this.setupPlayers();
              }
          } else {
              // player control
              if(index == 0) {
                  if (this.cursors.up.isDown)  {
                      this.game.physics.arcade.accelerationFromRotation(this.players[index].rotation-BasicGame.ORIENTATION_CORRECTION, BasicGame.PLAYER_ACC_SPEED, this.players[index].body.acceleration);
                      this.players[index].thrust = this.add.sprite(sprite.x+32, sprite.y+64, 'bullet');
                      this.players[index].thrust.anchor.setTo(0.5, 0.5);
                  } else {
                      this.players[index].body.acceleration.set(0);
                      this.players[index].thrust.destroy();
                  }

                  if (this.cursors.left.isDown) {
                      this.players[index].body.angularVelocity = -BasicGame.PLAYER_TURNING_SPEED;
                  } else if (this.cursors.right.isDown) {
                      this.players[index].body.angularVelocity = BasicGame.PLAYER_TURNING_SPEED;
                  } else {
                      this.players[index].body.angularVelocity = 0;
                  }

                  if (this.input.keyboard.isDown(Phaser.Keyboard.NUMPAD_0) || this.input.activePointer.isDown) {
                      this.fire(this.players[index]);
                  }
              } else if(index == 1) {
                  if (this.input.keyboard.isDown(Phaser.Keyboard.W))  {
                      this.game.physics.arcade.accelerationFromRotation(this.players[index].rotation-BasicGame.ORIENTATION_CORRECTION, BasicGame.PLAYER_ACC_SPEED, this.players[index].body.acceleration);
                  } else {
                      this.players[index].body.acceleration.set(0);
                  }

                  if (this.input.keyboard.isDown(Phaser.Keyboard.A)) {
                      this.players[index].body.angularVelocity = -BasicGame.PLAYER_TURNING_SPEED;
                  } else if (this.input.keyboard.isDown(Phaser.Keyboard.D)) {
                      this.players[index].body.angularVelocity = BasicGame.PLAYER_TURNING_SPEED;
                  } else {
                      this.players[index].body.angularVelocity = 0;
                  }

                  if (this.input.keyboard.isDown(Phaser.Keyboard.SPACEBAR)) {
                      this.fire(this.players[index]);
                  }
              }

              // replenish destroyed bullets
              if(this.players[index].bullets.total < 40) {
                  this.players[index].bullets.createMultiple(1, 'laser');
              }
          }
      }
      
      this.screenWrap(this.enemy);
      this.enemy.bullets.forEachExists(this.screenWrap, this);
      
      // enemy lifecycle
      if(this.enemy.isDead) {
          if(this.game.time.now - this.enemy.tod > BasicGame.ENEMY_RESPAWN_DELAY) {
              this.setupEnemy();
          }
      } else {
          // homing logic
//          this.enemy.rotation = this.physics.arcade.angleBetween(this.enemy, this.player) + BasicGame.ORIENTATION_CORRECTION;
//          this.physics.arcade.moveToObject(this.enemy, this.player.body, BasicGame.ENEMY_HOMING_SPEED, BasicGame.ENEMY_HOMING_MAX_TIME);
          this.enemy.body.acceleration.set(BasicGame.ENEMY_MAX_SPEED);
          this.fire(this.enemy);
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
      this.NUM_PLAYERS = 2;
      this.COLORS = [0x00FF00, 0xFFFF00];
      
      for (index = 0; index < this.NUM_PLAYERS; index++) {
          this.player(index, index, this.COLORS[index]); // re-using the index as the id, it can be changed later on
      }
  },
    
  setupEnemy: function () {
      // Here I setup the computer controlled ship
      var randX = this.rnd.integerInRange(20, this.game.width - 20);
      var randY = this.rnd.integerInRange(20, this.game.height - 20);
      var randRotation = this.rnd.integerInRange(0, 360);
      this.enemy = this.game.add.sprite(randX, randY, 'ufo');
      this.enemy.tint = BasicGame.ENEMY_COLOR;
      this.enemy.anchor.setTo(0.5);
      this.enemy.animations.add('fly', [0,1,2,3,4,5,6,7,8,9,10,11,12,13,,14,15], 100, true);
      this.enemy.play('fly');
      this.physics.enable(this.enemy, Phaser.Physics.ARCADE);
      this.enemy.rotation = randRotation;
      this.enemy.body.drag.set(BasicGame.ENEMY_DRAG);
      this.enemy.body.maxVelocity.set(BasicGame.ENEMY_MAX_SPEED);
      
      this.enemy.hp = BasicGame.ENEMY_HP;
      this.enemy.bulletSpeed = BasicGame.ENEMY_BULLET_SPEED;
      this.enemy.bulletRange = BasicGame.ENEMY_FIRE_RANGE;
      this.enemy.bulletDelay = BasicGame.ENEMY_FIRE_DELAY;
      
      this.enemy.bullets = this.add.group();
      this.enemy.bullets.enableBody = true;
      this.enemy.bullets.physicsBodyType = Phaser.Physics.ARCADE;
      this.enemy.bulletTime = 0;
      
      this.enemy.bullets.createMultiple(40, 'laser');
      this.enemy.bullets.setAll('anchor.x', 0.5);
      this.enemy.bullets.setAll('anchor.y', 0.5);
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
    
  setupExplosions: function () {
      // Here I setup the explosions
      
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
      this.instructionsLabel = this.add.text(this.game.width/2, this.game.height-20, "Press Z to Fire", style).anchor.setTo(0.5, 0.5);
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
    this.state.start('GameOver');

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
              this.sfx.play('shot');
          }
      }
  },
        
  hit: function(target, bullet) {
      bullet.destroy();
      this.sfx.play('boss hit');
      target.hp -= BasicGame.HIT_POW;
      if(target.hp <= 0) {
          if(target == this.enemy) {
              this.scores[bullet.source] += BasicGame.HIT_SCORE;
              this.scoreLabels[bullet.source].setText(this.scores[bullet.source]);
          } else if(target == this.player) {
              this.scores[bullet.source] -= BasicGame.HIT_DEDUCT;
              this.scoreLabels[bullet.source].setText(this.scores[bullet.source]);
          } 
          this.explode(target);
          target.isDead = true;
          target.tod = this.game.time.now;
          target.destroy();
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
  } 
};

