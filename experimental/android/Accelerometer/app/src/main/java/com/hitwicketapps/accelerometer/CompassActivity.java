package com.hitwicketapps.accelerometer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class CompassActivity extends Activity {

    private static final String TAG = CompassActivity.class.getSimpleName();
    float pitch = 0;
    float[] aValues = new float[3];
    float[] mValues = new float[3];

    CompassView compassView;
    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magneticField;

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        setContentView(R.layout.activity_compass);

        compassView = (CompassView)this.findViewById(R.id.compass_view);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        updateOrientation(0);
        compassView.setShowNumber(true);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType ()){
                case Sensor.TYPE_ACCELEROMETER:
                    aValues = event.values.clone ();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mValues = event.values.clone ();
                    break;
            }

            float[] R = new float[16];
            float[] orientationValues = new float[3];

            SensorManager.getRotationMatrix (R, null, aValues, mValues);
            SensorManager.getOrientation (R, orientationValues);

            orientationValues[1] = (float)Math.toDegrees (orientationValues[1]);

            updateOrientation(orientationValues[1]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void updateOrientation(float _pitch) {
        pitch = _pitch;

        if (compassView!= null) {
            compassView.setPitch(pitch);
            compassView.invalidate();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if(magneticField != null) {
            sensorManager.registerListener(sensorEventListener, magneticField, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop()
    {
        sensorManager.unregisterListener(sensorEventListener);

        super.onStop();
    }
}
