
BasicGame.Preloader = function (game) {
    this.background = null;
    this.preloadBar = null;
    WebFontConfig = {
        //  'active' means all requested fonts have finished loading
        active: function() {},

        //  The Google Fonts we want to load (specify as many as you like in the array)
        google: {
            families: ['Revalia', 'Wallpoet', 'Oswald']
        }
    };
};

BasicGame.Preloader.prototype = {

    preload: function () {

        //  Load the Google WebFont Loader script
        this.load.script('webfont', '//ajax.googleapis.com/ajax/libs/webfont/1.4.7/webfont.js');

        //  Show the loading progress bar asset we loaded in boot.js
        this.stage.backgroundColor = '#2d2d2d';

        this.preloadBar = this.add.sprite(this.game.width / 2 - 100, this.game.height / 2, 'preloaderBar');
        this.add.text(this.game.width / 2, this.game.height / 2 - 30, "Loading...", { font: "32px monospace", fill: "#fff" }).anchor.setTo(0.5, 0.5);


        //  This sets the preloadBar sprite as a loader sprite.
        //  What that does is automatically crop the sprite from 0 to full-width
        //  as the files below are loaded in.
        this.load.setPreloadSprite(this.preloadBar);

        //  Here we load the rest of the assets our game needs.
        this.load.image('titlepage', 'assets/casteroids.jpg');
        this.load.image('bullet', 'assets/bullets.png');
//        this.load.image('background', 'assets/background.png');
        this.load.image('space', 'assets/deep-space.jpg');
        this.load.spritesheet('ship', 'assets/ship.png', 64, 64);
        this.load.spritesheet('ufo', 'assets/ufo.png', 64, 64);
        this.load.spritesheet('explosion', 'assets/explosion.png', 32, 32);
        this.load.spritesheet('explosionBig', 'assets/explosion_big.png', 64, 64);
        this.load.spritesheet('player', 'assets/player.png', 64, 64);
        this.load.spritesheet('laser', 'assets/spr_bullet_strip02.png', 68, 68);
        this.load.audio('playerFire', ['assets/player-fire.ogg', 'assets/player-fire.wav']);
        this.load.audio('sfx', 'assets/fx_mixdown.ogg');
        //  + lots of other required assets here

    },

    create: function () {

        //  Once the load has finished we disable the crop because we're going to sit in the update loop for a short while as the music decodes
        this.preloadBar.cropEnabled = false;

    },

    update: function () {
        this.state.start('MainMenu');
    }
};
