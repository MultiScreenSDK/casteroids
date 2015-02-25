
BasicGame.GameOver = function (game) {
};

BasicGame.GameOver.prototype = {
    
//  init: function(score) {
//      this.score = score;
//  }

  create: function () {
      var gameOver_label = "GAME OVER";
      var prompt_label = "Game will restart shortly";
//      var score_label = "Scored: " + this.score;
      var style = { font: "12px Arial", fill: "#cccccc", align: "left" };
      var style2 = { font: "18px Arial", fill: "#cccccc", align: "left" };
      var t = this.add.text(this.game.width / 2, this.game.height / 2 - 40, gameOver_label, style2).anchor.setTo(0.5, 0.0);
      var t2 = this.add.text(this.game.width / 2, this.game.height / 2 + 80, prompt_label, style).anchor.setTo(0.5, 0.0);
//      var t3 = this.add.text(this.game.width / 2, this.game.height / 2 + 120, score_label, style).anchor.setTo(0.5, 0.0);
  },

  update: function () {
      // wait for some time or check for something else before restarting
      // this.back2Menu();
    }
  },

 back2Menu: function (pointer) {

    //  Then let's go back to the main menu.
    this.state.start('Game');

  }

};

