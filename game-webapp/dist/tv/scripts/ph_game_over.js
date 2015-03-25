BasicGame.GameOver = function (game) {
    this.secondsElapsed = 0; //initial
    this.gameOverScreenTimer = Phaser.TimerEvent;
};

BasicGame.GameOver.prototype = {

    sprite: null,
    anim: null,

    /******************************************************************************************************************
     * Phaser Lifecycle functions
     */

    init: function (scores, names) {
        this.names = names;
    },
    
    create: function () {
        this.sprite = this.add.sprite(0, 0, 'gameover');
        this.sprite.anchor.setTo(0, 0);

        this.anim = this.game.add.tween(this.sprite.scale).to({x:1.1, y:1.1}, 10000, Phaser.Easing.Linear.None,  true, 50, -1, true);
        this.anim.start();

        var gameOver_label = "GAME OVER";
        var prompt_label = "Game will restart in 5 seconds";
        var style = { font: "12px Arial", fill: "#cccccc", align: "left" };
        var style2 = { font: "18px Arial", fill: "#cccccc", align: "left" };
        var t = this.add.text(this.game.width / 2, this.game.height / 2, gameOver_label, style2);
        t.anchor.setTo(0.5, 0.0);
        t.font = 'Revalia';
        this.gameOverText = this.add.text(this.game.width / 2, this.game.height / 2 + 120, prompt_label, style);
        this.gameOverText.anchor.setTo(0.5, 0.0);
        this.gameOverText.font = 'Revalia';
        this.gameOverText.cacheAsBitmap = true;

        var heightIncrement = 40;

        this.scoreLabels = new Array();
        var scores = GameManager.getLastScoreData();
        scores.sort(function (a, b) {
            return b.score - a.score;
        });
        for (var i = 0; i < scores.length; i++) {
            heightIncrement = heightIncrement + 60;
            var scoreStyle = { font: "12px", fill: scores[i].hexColor, align: "left" };
            this.scoreLabels[i] = this.add.text(this.game.width / 2, heightIncrement, scores[i].name + " : " + scores[i].score, scoreStyle);
            this.scoreLabels[i].anchor.setTo(0.5, 0.0);
            this.scoreLabels[i].font = 'Revalia';
            this.scoreLabels[i].cacheAsBitmap = true;
        }

        var playersConnected = false;

        for (var i in GameManager.slots) {
            var slot = GameManager.slots[i];
            if (!slot.available) {
                playersConnected = true;
                break;
            }
        }

        this.secondsElapsed = 0;
        if (playersConnected) {
            this.gameOverScreenTimer = this.game.time.events.repeat(Phaser.Timer.SECOND, 6, this.updateTimer, this);
        } else {
            this.gotoMainMenu();
        }

    },

    update: function () {
    },

    /******************************************************************************************************************
     * Private functions
     */

    updateTimer: function () {
        var secondsToStart = BasicGame.GAME_COUNTDOWN_LENGTH - this.secondsElapsed;
        this.gameOverText.setText("Game will restart in " + secondsToStart + (secondsToStart==1 ? " second" : " seconds"));
        GameManager.onGameStart(secondsToStart);
        if (this.secondsElapsed == BasicGame.GAME_COUNTDOWN_LENGTH) {
            this.startGame();
        }
        this.secondsElapsed = this.secondsElapsed + 1;
    },

    startGame: function () {
        // Clean up the background animation
        this.anim.stop();
        this.game.tweens.remove(this.anim);

        // Destroy the background
        this.sprite.destroy();

        this.gameOverText.destroy();

        for (var i = 0; i < this.scoreLabels.length; i++) {
            this.scoreLabels[i].destroy();
        }
        this.scoreLabels = new Array();


        // Start the game
        GameManager.onGameStart(0);
        this.secondsElapsed = 0; //reset
        this.state.start('Game');
    },

    gotoMainMenu: function () {
        // Clean up the background animation
        this.anim.stop();
        this.game.tweens.remove(this.anim);

        //  Go back to the main menu.
        this.secondsElapsed = 0; //reset
        this.state.start('MainMenu');
    },

    onPlayerUpdate: function (count) {
        console.log("game over menu onPlayerUpdate " + count);
        if (count == 0) {
            console.log("no players connected");
            if (this.gameOverScreenTimer.running) {
                this.game.time.events.remove(this.gameOverScreenTimer);
            }
            this.gotoMainMenu();
        }
    }
};
