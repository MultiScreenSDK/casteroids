BasicGame.MainMenu = function (game) {
    this.secondsElapsed = 0; //initial
    this.playerTextHeightOffset = 0;
    this.menuTimer = Phaser.TimerEvent;
};

BasicGame.MainMenu.prototype = {

    sprite: null,
    anim: null,

    /******************************************************************************************************************
     * Phaser Lifecycle functions
     */

    init: function() {

    },

    create: function () {
        // Settings to make the app full screen
        this.game.scale.parentIsWindow = true;
        this.game.scale.scaleMode = Phaser.ScaleManager.SHOW_ALL;

        //  This is the preparation screen where players have time to join the game
        this.sprite = this.add.sprite(0, 0, 'titlepage');
        this.sprite.anchor.setTo(0, 0);

        this.anim = this.game.add.tween(this.sprite.scale).to({x:1.1, y:1.1}, 20000, Phaser.Easing.Linear.None,  true, 3000, -1, true);
        this.anim.start();

        this.players = this.game.add.group();

        this.style = { font: "3Opx", fill: "#cccccc"};
        this.loadingText = this.add.text(this.game.width / 2, this.game.height / 2 + 130, "Waiting for Players to join");
        //this.loadingText.style = this.style;
        this.loadingText.fill = "#fff";
        this.loadingText.font = 'Revalia';
        this.loadingText.stroke = '#000000';
        this.loadingText.strokeThickness = 2;
        this.loadingText.setShadow(5, 5, 'rgba(0,0,0,0.5)', 2);
        this.loadingText.anchor.setTo(0.5, 0.5);
        this.loadingText.cacheAsBitmap = true;
    },

    update: function () {

    },

    /******************************************************************************************************************
     * Game Callback functions
     */

    onPlayerUpdate: function (count) {
        console.log("onPlayerUpdate " + count);
        this.updateConnectedPlayers(count);
        if (count > 0) {
            if (!this.menuTimer.running) {
                this.menuTimer = this.game.time.events.repeat(Phaser.Timer.SECOND, BasicGame.GAME_COUNTDOWN_LENGTH + 1, this.updateTimer, this);
            }

        } else {
            //stop the countdown
            this.game.time.events.remove(this.menuTimer);

            this.secondsElapsed = 0; //reset
            this.loadingText.setText("Waiting for Players to join")
            this.loadingText.cacheAsBitmap = true;
        }
    },

    /******************************************************************************************************************
     * Private functions
     */

    startGame: function () {
        // Clean up the background animation
        this.anim.stop();
        this.game.tweens.remove(this.anim);

        // Destroy the background
        this.sprite.destroy();

        // Clean up the players group
        this.players.destroy(true);

        // Start the game
        GameManager.onGameStart(0);
        this.secondsElapsed = 0; //reset
        this.state.start('Game');
    },

    updateTimer: function () {
        var secondsToStart = BasicGame.GAME_COUNTDOWN_LENGTH - this.secondsElapsed;
        this.loadingText.setText("Game starting in " + secondsToStart + (secondsToStart==1 ? " second" : " seconds"));
        this.loadingText.cacheAsBitmap = true;
        GameManager.onGameStart(secondsToStart);
        if (this.secondsElapsed == BasicGame.GAME_COUNTDOWN_LENGTH) {
            this.startGame();
        }
        this.secondsElapsed = this.secondsElapsed + 1;
    },

    updateConnectedPlayers: function(count) {

        this.players.removeAll();

        var activeSlots = this.activeSlots();

        if (count > 0) {

            for (var i = 0; i < count; i++) {
                var playerTextHorizontalPosition = (this.game.width / count / 2) * (i + 1);
                var playerTextStyle = { font: "12px", fill: activeSlots[i].hexColor, align: "center" };
                var playerText = this.add.text(playerTextHorizontalPosition, this.playerTextHeightOffset, activeSlots[i].name, playerTextStyle);
                playerText.anchor.setTo(-0.5, -0.5);
                playerText.font = 'Wallpoet';
                playerText.stroke = '#000000';
                playerText.strokeThickness = 2;
                playerText.setShadow(5, 5, 'rgba(0,0,0,0.5)', 2);
                playerText.cacheAsBitmap = true;
                this.players.add(playerText);
            }
        }
    },

    activeSlots: function() {
        var activeSlots = [];
        var slots = GameManager.slots;
        for (var i in slots) {
            var slot = slots[i];
            if (!slot.available) {
                activeSlots.push(slot);
            }
        }
        return activeSlots;
    }

};
