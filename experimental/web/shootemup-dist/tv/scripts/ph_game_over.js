
BasicGame.GameOver = function (game) {
};

BasicGame.GameOver.prototype = {

  create: function () {
      var gameOver_label = "GAME OVER";
      var prompt_label = "Press Z or Tap/Click the screen to restart";
      var style = { font: "12px Arial", fill: "#cccccc", align: "left" };
      var t = this.add.text(this.game.width / 2, this.game.height / 2, gameOver_label, style).anchor.setTo(0.5, 0.0);
      var t2 = this.add.text(this.game.width / 2, this.game.height / 2 + 80, prompt_label, style).anchor.setTo(0.5, 0.0);
      // TODO display score results
  },

  update: function () {
      if (this.input.keyboard.isDown(Phaser.Keyboard.Z) || this.input.activePointer.isDown) {
      this.back2Menu();
    }
  },

 back2Menu: function (pointer) {

    //  Then let's go back to the main menu.
    this.state.start('Game');

  }

};

