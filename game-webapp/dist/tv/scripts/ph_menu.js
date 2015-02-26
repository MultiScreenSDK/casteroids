BasicGame.MainMenu = function (game) {

};

BasicGame.MainMenu.prototype = {


    create: function () {

        this.secondsLeft = BasicGame.GAME_COUNTDOWN_LENGTH; //initial
        this.isCountingDown = false;

        //  Create a Timer
        this.timer = this.game.time.create(false);
        //Set the timer to call back every 1 second
        this.timer.loop(1000, this.updateTimer, this);

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

    startGame: function (pointer) {
        this.state.start('Game');
        //GameManager.onGameStart(0); ??
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
