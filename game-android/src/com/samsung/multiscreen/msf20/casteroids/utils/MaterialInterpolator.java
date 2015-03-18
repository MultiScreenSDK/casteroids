package com.samsung.multiscreen.msf20.casteroids.utils;

import android.view.animation.Interpolator;

/**
 *
 * An interpolator taken from Material Design.
 *
 * @author Adrian Hernandez
 *
 */
public class MaterialInterpolator implements Interpolator {

    @Override
    public float getInterpolation(float x) {
        return (float) (6 * Math.pow(x, 2) - 8 * Math.pow(x, 3) + 3 * Math.pow(x, 4));
    }
}