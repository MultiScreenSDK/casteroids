
BasicGame.MainMenu = function (game) {
    this.playButton = null;
};

BasicGame.MainMenu.prototype = {
    create: function () {
        //  This is the preparation screen where players have time to join the game
        this.add.sprite(this.game.width / 2, 20, 'titlepage').anchor.setTo(0.5, 0.0);
        this.loadingText = this.add.text(this.game.width / 2, this.game.height / 2 + 130, "Waiting for Player(s) to join ", { font: "20px monospace", fill: "#fff" });
        this.loadingText.font = 'Revalia';
        this.loadingText.anchor.setTo(0.5, 0.5);
        this.add.text(this.game.width / 2, this.game.height - 75, "Copyright (c) 2015", { font: "12px monospace", fill: "#fff", align: "center"}).anchor.setTo(0.5, 0.5);
  },
    
    update: function () {
        if (this.input.keyboard.isDown(Phaser.Keyboard.Z) || this.input.activePointer.isDown) {
            GameManager.onGameStart(0);
            this.startGame();
        }
    },
    
    startGame: function (pointer) {
        //  Ok, the Play Button has been clicked or touched, so let's stop the music (otherwise it'll carry on playing)
        //  And start the actual game
        this.state.start('Game');
  }
};
