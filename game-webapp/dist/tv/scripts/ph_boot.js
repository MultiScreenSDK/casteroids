var BasicGame = {
    GAME_LENGTH: 120,
    GAME_COUNTDOWN_LENGTH: 5,
    ORIENTATION_CORRECTION: Math.PI/2,
    HIT_POW: 10,
    HIT_SCORE: 10,
    HIT_DEDUCT: 10,
    PLAYER_HP: 50,
    PLAYER_COLOR: 0x00ff00,
    PLAYER_DRAG: 100,
    PLAYER_MAX_SPEED: 180,
    PLAYER_BULLET_SPEED: 400,
    PLAYER_FIRE_DELAY: 300,
    PLAYER_FIRE_RANGE: 1000,
    PLAYER_RESPAWN_DELAY: 3000,
    PLAYER_ACC_SPEED: 200,
    PLAYER_TURNING_SPEED: 200,
    ENEMY_HP: 30,
    ENEMY_COLOR: 0xff0000,
    ENEMY_DRAG: 100,
    ENEMY_MAX_SPEED: 70,
    ENEMY_BULLET_SPEED: 300,
    ENEMY_FIRE_DELAY: 1000,
    ENEMY_FIRE_RANGE: 600,
    ENEMY_HOMING_SPEED: 50,
    ENEMY_HOMING_MAX_TIME: 200,
    ENEMY_RESPAWN_DELAY: 2000,
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
