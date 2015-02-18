BasicGame.Game = function (game) {
    this.secondsLeft = 120;
};

BasicGame.Game.prototype = {

  create: function () {
      this.game.physics.startSystem(Phaser.Physics.ARCADE);
      
      var style = { font: "14px Arial", fill: "#cccccc", align: "left" };
      this.timerLabel = this.add.text(20, 20, "02:00", style);
      this.score = 0;
      var style_score = { font: "14px Arial", fill: "#cccccc", align: "right" };
      this.scoreLabel = this.add.text(this.game.width-100, 20, "000", style_score);
      this.instructionsLabel = this.add.text(this.game.width/2, this.game.height-20, "Press Z to Fire", style).anchor.setTo(0.5, 0.5);
      
      this.player1 = this.game.add.sprite(this.game.width / 4, this.game.height / 3, 'player');
      this.player1.tint = 0x00ff00;
      this.player1.anchor.setTo(0.5);
      this.player1.animations.add('fly', [ 0, 1, 2 ], 20, true);
      this.player1.play('fly');
      this.physics.enable(this.player1, Phaser.Physics.ARCADE);
      this.player1.body.drag.set(100);
      this.player1.body.maxVelocity.set(100);
      
      this.player2 = this.game.add.sprite(3*(this.game.width / 4), 2*(this.game.height / 3), 'player');
      this.player2.tint = 0xff0000;
      this.player2.anchor.setTo(0.5);
      this.player2.animations.add('fly', [ 0, 1, 2 ], 20, true);
      this.player2.play('fly');
      this.physics.enable(this.player2, Phaser.Physics.ARCADE);
      this.player2.body.drag.set(100);
      this.player2.body.maxVelocity.set(70);
      
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
      
      this.bullets = this.add.group();
      this.bullets.enableBody = true;
      this.bullets.physicsBodyType = Phaser.Physics.ARCADE;
      this.bulletTime = 0;
      
      this.bullets.createMultiple(40, 'bullet');
      this.bullets.setAll('anchor.x', 0.5);
      this.bullets.setAll('anchor.y', 0.5);
      
      this.cursors = this.game.input.keyboard.createCursorKeys();
      this.game.time.events.loop(1000, this.updateTimer, this);
  },

  update: function () {
      //  Main Game Loop
      if (this.cursors.up.isDown)  {
          this.game.physics.arcade.accelerationFromRotation(this.player1.rotation, 200, this.player1.body.acceleration);
      } else {
          this.player1.body.acceleration.set(0);
      }
      
      if (this.cursors.left.isDown) {
          this.player1.body.angularVelocity = -200;
      } else if (this.cursors.right.isDown) {
          this.player1.body.angularVelocity = 200;
      } else {
          this.player1.body.angularVelocity = 0;
      }
      
      // randomize p2 movement
      
      
      if (this.input.keyboard.isDown(Phaser.Keyboard.Z) || this.input.activePointer.isDown) {
          this.fire();
      }
          
      this.screenWrap(this.player1);
      this.screenWrap(this.player2);
      this.bullets.forEachExists(this.screenWrap, this);
      
      this.physics.arcade.overlap(this.bullets, this.player2, this.hit, null, this);
      var angle = this.physics.arcade.angleBetween(this.player2, this.player1);
      var deg = this.math.radToDeg(angle);
      this.player2.angle = deg;
      this.physics.arcade.moveToObject(this.player2, this.player1.body, 50, 200);
  },

  updateTimer: function updateTimer() {
      this.secondsLeft--;
      var minutes = Math.floor(this.secondsLeft/60);
      var seconds = this.secondsLeft - minutes * 60;
      if(seconds < 10) {
          seconds = '0'+seconds;
      }
              
      this.timerLabel.setText(minutes+":"+seconds);
      if(this.secondsLeft == 0) {
          this.secondsLeft = 180;
          this.quitGame();
      }
  },

  quitGame: function (pointer) {

    //  Here you should destroy anything you no longer need.
    //  Stop music, delete sprites, purge caches, free resources, all that good stuff.

    //  Then let's go back to the main menu.
    this.state.start('GameOver');

  },
    
  fire: function() {
      if (this.game.time.now > this.bulletTime) {
          this.bullet = this.bullets.getFirstExists(false);
          if (this.bullet) {
              this.bullet.reset(this.player1.body.x + 16, this.player1.body.y + 16);
              this.bullet.lifespan = 1000;
              this.bullet.rotation = this.player1.rotation;
              this.game.physics.arcade.velocityFromRotation(this.player1.rotation, 400, this.bullet.body.velocity);
              this.bulletTime = this.game.time.now + 50;
//              var color = 0;
//              switch (this.secondsLeft % 3) {
//                      case 0:
//                        color = 0xff0000;
//                        break;
//                      case 1:
//                        color = 0x00ff00;
//                        break;
//                      case 2:
//                        color = 0x0000ff;
//                        break;
//              }
              this.bullet.tint = 0x00ff00;
              this.sfx.play('shot');
          }
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
      
  hit: function() {
      this.score += 10;
      this.scoreLabel.setText(this.score);
      this.sfx.play('boss hit');
  },
    
    
};

