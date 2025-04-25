package com.Lrst6963.GPSTest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Sensorscom {
    private static SensorManager sensorManager;
    private static Sensor accelerometer;
    private static Sensor magnetometer;
    private static float[] gravityValues = new float[3];
    private static float[] geomagneticValues = new float[3];
    private static float bearing = 0f;
    private static boolean initialized = false;

    // 初始化传感器
    public static void init(Context context) {
        if (initialized) return;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    gravityValues = event.values.clone();
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagneticValues = event.values.clone();
                }

                if (gravityValues != null && geomagneticValues != null) {
                    float[] R = new float[9];
                    float[] I = new float[9];

                    boolean success = SensorManager.getRotationMatrix(R, I, gravityValues, geomagneticValues);
                    if (success) {
                        float[] orientation = new float[3];
                        SensorManager.getOrientation(R, orientation);
                        bearing = (float) Math.toDegrees(orientation[0]);
                        bearing = (bearing + 360) % 360; // 转换为0-360度
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 可以处理精度变化
            }
        };

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        initialized = true;
    }

    // 获取方向角度
    public static float getDegree() {
        return bearing;
    }

    // 释放资源
    public static void release() {
        if (sensorManager != null) {
            sensorManager.unregisterListener((SensorEventListener) sensorManager);
        }
        initialized = false;
    }
}
