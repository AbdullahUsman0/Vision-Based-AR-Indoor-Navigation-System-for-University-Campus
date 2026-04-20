package com.mahad.arnavigation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

public class SensorHelper implements SensorEventListener {
    public interface HeadingListener {
        void onHeadingUpdated(float headingDegrees);
    }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final HeadingListener headingListener;

    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private boolean hasAccel;
    private boolean hasMag;
    private float smoothedHeadingDegrees;

    public SensorHelper(@NonNull SensorManager sensorManager, @NonNull HeadingListener headingListener) {
        this.sensorManager = sensorManager;
        this.headingListener = headingListener;
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public boolean hasRequiredSensors() {
        return accelerometer != null && magnetometer != null;
    }

    public void start() {
        if (!hasRequiredSensors()) {
            return;
        }
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, gravity.length);
            hasAccel = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.length);
            hasMag = true;
        }

        if (!hasAccel || !hasMag) {
            return;
        }

        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
        if (!success) {
            return;
        }

        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        float azimuthRadians = orientationAngles[0];
        float azimuthDegrees = (float) Math.toDegrees(azimuthRadians);
        azimuthDegrees = normalizeDegrees(azimuthDegrees);

        smoothedHeadingDegrees = smoothAngle(smoothedHeadingDegrees, azimuthDegrees, 0.1f);
        headingListener.onHeadingUpdated(smoothedHeadingDegrees);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op; heading pipeline can tolerate changing sensor accuracy.
    }

    private float smoothAngle(float current, float target, float alpha) {
        float delta = ((target - current + 540f) % 360f) - 180f;
        return (current + alpha * delta + 360f) % 360f;
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }
}
