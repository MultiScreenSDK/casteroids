BasicGame.GameOver = function (game) {
    this.secondsElapsed = 0; //initial
    this.gameOverScreenTimer = Phaser.TimerEvent;
};

BasicGame.GameOver.prototype = {

    init: function (scores, names) {
        this.names = names;
    },
    
    create: function () {
        var sprite = this.add.sprite(0, 0, 'gameover');
        sprite.anchor.setTo(0, 0);

        var anim = this.game.add.tween(sprite.scale).to({x:1.1, y:1.1}, 10000, Phaser.Easing.Linear.None,  true, 50, -1, true);
        anim.start();

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

        var heightIncrement = 40;

        var scores = GameManager.getLastScoreData();
        for (var i = 0; i < scores.length; i++) {
            heightIncrement = heightIncrement + 60;
            var scoreStyle = { font: "12px", fill: scores[i].hexColor, align: "left" };
            var scoreText = this.add.text(this.game.width / 2, heightIncrement, scores[i].name + " : " + scores[i].score, scoreStyle);
            scoreText.anchor.setTo(0.5, 0.0);
            scoreText.font = 'Revalia';
        }

        this.secondsElapsed = 0;
        this.gameOverScreenTimer = this.game.time.events.repeat(Phaser.Timer.SECOND, 6, this.updateTimer, this);

        //this.game.time.events.add(Phaser.Timer.SECOND * 5, this.gotoMainMenu, this);
    },

    update: function () {


    },

    updateTimer: function () {
        var secondsToStart = 5 - this.secondsElapsed;
        this.gameOverText.setText("Game will restart in " + secondsToStart + (secondsToStart==1 ? " second" : " seconds"));
        if (this.secondsElapsed == 5) {
            this.gotoMainMenu();
        }
        this.secondsElapsed = this.secondsElapsed + 1;
    },

    gotoMainMenu: function () {
        //  Then let's go back to the main menu.
        this.state.start('MainMenu');    
    }
};
