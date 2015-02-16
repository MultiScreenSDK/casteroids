
BasicGame.Game = function (game) {
    this.secondsLeft = 180;
    this.frame = 0;
};

BasicGame.Game.prototype = {

  create: function () {
      var style = { font: "14px Arial", fill: "#cccccc", align: "left" };
      this.timerLabel = this.add.text(20, 20, this.timerLabel, style);
  },

  update: function () {
    //  Main Game Loop
      this.updateTimer();
  },
    
    
      updateTimer: function updateTimer() {
      // This code displays the current system time minutes and seconds
      /*minutes = Math.floor(this.game.time.time / 60000) % 60;
      seconds = Math.floor(this.game.time.time / 1000) % 60;

      //If any of the digits becomes a single digit number, pad it with a zero  
      if (seconds < 10)
         seconds = '0' + seconds;

      if (minutes < 10)
         minutes = '0' + minutes;*/

          this.frame++;
          if(this.frame == this.game.time.fps ) {
              this.frame = 0;
              this.secondsLeft--;
              
              var minutes = Math.floor(this.secondsLeft/60);
              var seconds = this.secondsLeft - minutes * 60;
              if(seconds < 10) {
                  seconds = '0'+seconds;
              }
              
              this.timerLabel.setText(minutes+":"+seconds);
              if(this.secondsLeft == 0) {
                  this.quitGame();
              }
          }
  },

  quitGame: function (pointer) {

    //  Here you should destroy anything you no longer need.
    //  Stop music, delete sprites, purge caches, free resources, all that good stuff.

    //  Then let's go back to the main menu.
    this.state.start('GameOver');

  }

};

