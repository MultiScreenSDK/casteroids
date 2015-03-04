var BasicGame = {
    GAME_LENGTH: 120,
    GAME_COUNTDOWN_LENGTH: 5,
    GAME_OVER_DELAY: 5000,
    ORIENTATION_CORRECTION: Math.PI/2,
    HIT_POW: 10,
    ALIEN_HIT_SCORE: 10,
    ALIEN_DESTROY_SCORE: 50,
    PLAYER_DESTROY_SCORE: 100,
    PLAYER_HIT_DEDUCT: 10,
    PLAYER_HIT_SCORE: 10,
    PLAYER_HP: 10,
    PLAYER_COLOR: 0x00ff00,
    PLAYER_DRAG: 200,
    PLAYER_MAX_SPEED: 200,
    PLAYER_BULLET_SPEED: 400,
    PLAYER_FIRE_DELAY: 50,
    PLAYER_FIRE_RANGE: 800,
    PLAYER_RESPAWN_DELAY: 2000,
    PLAYER_ACC_SPEED: 200,
    PLAYER_TURNING_SPEED: 200,
    PLAYER_HITBOX_WIDTH: 40,
    PLAYER_HITBOX_HEIGHT: 40,
    ALIEN_HP: 100,
    ALIEN_COLOR: 0xf50057,
    ALIEN_DRAG: 100,
    ALIEN_MAX_SPEED: 70,
    ALIEN_BULLET_SPEED: 250,
    ALIEN_FIRE_DELAY: 1000,
    ALIEN_FIRE_RANGE: 800,
    ALIEN_HOMING_SPEED: 50,
    ALIEN_HOMING_MAX_TIME: 200,
    ALIEN_RESPAWN_DELAY: 2000,
    ALIEN_HITBOX_WIDTH: 40,
    ALIEN_HITBOX_HEIGHT: 40,
    BULLET_HITBOX_WIDTH: 5,
    BULLET_HITBOX_HEIGHT: 5,
};

BasicGame.Boot = function (game) {

};

BasicGame.Boot.prototype = {

    init: function () {
        //  We re not using multi-touch so it's recommended setting this to 1
        this.input.maxPointers = 1;
        this.scale.pageAlignHorizontally = true;
        this.scale.pageAlignVertically = true;
    },

    preload: function () {
        //  Here we load the assets required for our preloader (in this case a loading bar)
        this.load.image('preloaderBar', 'assets/preloader-bar.png');
    },

    create: function () {
        //  By this point the preloader assets have loaded to the cache, we've set the game settings
        //  So now let's start the real preloader going
        this.state.start('Preloader');

    }

};
