BasicGame.MainMenu = function (game) {
    this.secondsElapsed = 0; //initial
};

BasicGame.MainMenu.prototype = {

    init: function() {

    },

    create: function () {
        //  This is the preparation screen where players have time to join the game
        var sprite = this.add.sprite(0, 0, 'titlepage');
        sprite.anchor.setTo(0, 0);

        var anim = this.game.add.tween(sprite.scale).to({x:1.1, y:1.1}, 20000, Phaser.Easing.Linear.None,  true, 3000, -1, true);
        anim.start();

        this.style = { font: "3Opx", fill: "#cccccc"};
        this.loadingText = this.add.text(this.game.width / 2, this.game.height / 2 + 130, "Waiting for Players to join");
        //this.loadingText.style = this.style;
        this.loadingText.fill = "#fff";
        this.loadingText.font = 'Revalia';
        this.loadingText.stroke = '#000000';
        this.loadingText.strokeThickness = 2;
        this.loadingText.setShadow(5, 5, 'rgba(0,0,0,0.5)', 2);
        this.loadingText.anchor.setTo(0.5, 0.5);
    },

    update: function () {

    },

    startGame: function () {
        GameManager.onGameStart(0);
        this.state.start('Game');
    },

    updateTimer: function () {
        var secondsToStart = BasicGame.GAME_COUNTDOWN_LENGTH - this.secondsElapsed;
        this.loadingText.setText("Game starting in " + secondsToStart + (secondsToStart==1 ? " second" : " seconds"));
        GameManager.onGameStart(secondsToStart);
        if (this.secondsElapsed == BasicGame.GAME_COUNTDOWN_LENGTH) {
            this.startGame();
        }
        this.secondsElapsed = this.secondsElapsed + 1;
    },

    onPlayerUpdate: function (count) {
        console.log("onPlayerUpdate " + count);
        if (count > 0) {
            console.log("Starting timer");
            this.game.time.events.repeat(Phaser.Timer.SECOND, BasicGame.GAME_COUNTDOWN_LENGTH + 1, this.updateTimer, this);
        } else {
            //stop the countdown
            this.game.time.events.stop(true);

            this.secondsElapsed = 0; //reset
            this.loadingText.setText("Waiting for Players to join")
        }
    }
};
