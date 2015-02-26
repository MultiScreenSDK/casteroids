BasicGame.MainMenu = function (game) {
    this.secondsLeft = BasicGame.GAME_COUNTDOWN_LENGTH; //initial
    this.isCountingDown = false;

    //  Create a Timer
    this.timer = new Phaser.Timer(game);

    //Set the timer to call back every 1 second, but don't start it
    this.timer.loop(1000, this.updateTimer, this);

};

BasicGame.MainMenu.prototype = {

    init: function() {

    },

    create: function () {
        //  This is the preparation screen where players have time to join the game
        this.add.sprite(this.game.width / 2, 20, 'titlepage').anchor.setTo(0.5, 0.0);

        this.style = { font: "3Opx", fill: "#cccccc"};
        this.loadingText = this.add.text(this.game.width / 2, this.game.height / 2 + 130, "Waiting for Players to join");
        //this.loadingText.style = this.style;
        this.loadingText.fill = "#fff";
        this.loadingText.font = 'Revalia';
        this.loadingText.anchor.setTo(0.5, 0.5);
    },

    update: function () {

    },

    startGame: function () {
        GameManager.onGameStart(0);
        this.state.start('Game');
    },

    updateTimer: function() {

        console.log("Seconds Left " + this.secondsLeft);
        if (this.secondsLeft == 0) {
            this.startGame();
        }
        this.loadingText.setText("Starting in " + this.secondsLeft);
        GameManager.onGameStart(this.secondsLeft);
        this.secondsLeft--;
    },

    onPlayerUpdate: function (count) {
        console.log("onPlayerUpdate " + count);
        if (count > 0) {
            //FIXME: go ahead and start game immediately for now
            this.startGame();
            //FIXME END

            if (this.isCountingDown == false) {
                //start the countdown
                this.isCountingDown = true;
                this.timer.start();
            }
        } else {
            if (this.isCountingDown == true) {
                this.timer.stop();
                //stop the countdown
                this.isCountingDown = false;
                this.secondsLeft = BasicGame.GAME_COUNTDOWN_LENGTH; //reset
                this.loadingText.setText("Waiting for Players to join")
            }
        }
    }
};
