BasicGame.Game = function (game) {
    this.secondsLeft = 120;
};

BasicGame.Game.prototype = {

  create: function () {
      var style = { font: "14px Arial", fill: "#cccccc", align: "left" };
      this.timerLabel = this.add.text(20, 20, "02:00", style);
      this.instructionsLabel = this.add.text(this.game.width/2, this.game.height-20, "Press Z to Fire", style).anchor.setTo(0.5, 0.5);
      
      this.cursors = this.input.keyboard.createCursorKeys();
      this.game.time.events.loop(1000, this.updateTimer, this);
      
      this.player = this.add.sprite(this.game.width / 2, this.game.height / 2, 'player');
      this.player.anchor.setTo(0.5, 0.5);
      this.player.animations.add('fly', [ 0, 1, 2 ], 20, true);
      this.player.play('fly');
      this.player.speed = 0;
      this.player.direction = 0;
      this.physics.enable(this.player, Phaser.Physics.ARCADE);
      
      this.audio_fire = this.game.add.audio("playerFire");
      
      this.bullets = [];
      this.nextShotAt = 0;
      this.shotDelay = 100;
  },

  update: function () {
      //  Main Game Loop
      if (this.cursors.left.isDown) {
          this.player.angle--;
      } else if (this.cursors.right.isDown) {
          this.player.angle++;
      }
      
      if (this.cursors.up.isDown) {
          this.player.speed++;
      } else if (this.cursors.down.isDown) {
          this.player.speed--;
      }
      if (this.input.keyboard.isDown(Phaser.Keyboard.Z) || this.input.activePointer.isDown) {
          this.fire();
      }
      
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
      if (this.nextShotAt > this.time.now) {
          return;
      }
      this.nextShotAt = this.time.now + this.shotDelay;
      
      var bullet = this.add.sprite(this.player.x, this.player.y - 20, 'bullet');
      bullet.anchor.setTo(0.5, 0.5);
      this.physics.enable(bullet, Phaser.Physics.ARCADE);
      bullet.body.velocity.y = -500;
      var color = 0;
      switch (this.secondsLeft % 3) {
              case 0:
                color = 0xff0000;
                break;
              case 1:
                color = 0x00ff00;
                break;
              case 2:
                color = 0x0000ff;
                break;
      }
      bullet.tint = color;
      this.bullets.push(bullet);
      this.audio_fire.play();
  },

};

