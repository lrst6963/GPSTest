package com.Lrst6963.GPSTest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.example.myapplication.R;

/**
 * 指南针传感器处理工具类
 * 包含静态方向计算方法和实时传感器监听功能
 */
public class Compass implements SensorEventListener {
    private static final String TAG = "Compass";
    private final Context context;
    ;
    // 传感器服务
    private final SensorManager sensorManager;
    private final Sensor rotationSensor;

    // 回调接口
    private CompassListener listener;
    private float currentDegree = 0f;
    private boolean isSensorAvailable = false;

    /**
     * 方向变化监听接口
     */
    public interface CompassListener {
        void onDegreeChanged(float degree);

        void onDirectionChanged(String direction);
    }

    public Compass(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        this.isSensorAvailable = (rotationSensor != null);
    }

    /* ---------- 静态方向计算方法 ---------- */

    /**
     * 静态方法：根据角度获取方向字符串
     *
     * @param degree 角度值(0-359)
     * @return 方向字符串（北/东北/东/东南/南/西南/西/西北）
     */
    public static String getDirection(Context context, float degree) {
        degree = normalizeDegree(degree);
        if (degree >= 340 || degree < 20) return context.getString(R.string.direction_north);
        if (degree >= 20 && degree < 70) return context.getString(R.string.direction_northeast);
        if (degree >= 70 && degree < 110) return context.getString(R.string.direction_east);
        if (degree >= 110 && degree < 160) return context.getString(R.string.direction_southeast);
        if (degree >= 160 && degree < 200) return context.getString(R.string.direction_south);
        if (degree >= 200 && degree < 250) return context.getString(R.string.direction_southwest);
        if (degree >= 250 && degree < 290) return context.getString(R.string.direction_west);
        if (degree >= 290 && degree < 340) return context.getString(R.string.direction_northwest);
        return context.getString(R.string.direction_unknown);
    }


    /**
     * 静态方法：获取当前方向（简化调用）
     *
     * @param degree 当前角度
     * @return 方向字符串
     */
    public static String getCurrentDirection(Context context, float degree) {
        return getDirection(context, degree);
    }

    /**
     * 规范化角度到0-359范围
     */
    private static float normalizeDegree(float degree) {
        degree = degree % 360;
        return degree < 0 ? degree + 360 : degree;
    }

    /* ---------- 传感器监听相关方法 ---------- */

    public void setListener(CompassListener listener) {
        this.listener = listener;
    }

    /**
     * 启动传感器监听
     */
    public void start() {
        if (isSensorAvailable) {
            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
        }
    }

    /**
     * 停止传感器监听
     */
    public void stop() {
        sensorManager.unregisterListener(this);
    }

    /**
     * 检查设备是否支持方向传感器
     */
    public boolean isSensorAvailable() {
        return isSensorAvailable;
    }

    /**
     * 获取当前传感器检测到的角度
     */
    public float getCurrentDegree() {
        return currentDegree;
    }

    /* ---------- 传感器事件回调 ---------- */

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ORIENTATION) return;

        float degree = normalizeDegree(event.values[0]);
        if (Math.abs(degree - currentDegree) > 0.5f) { // 过滤微小变化
            currentDegree = degree;
            notifyListeners(degree);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 可在此处理精度变化（如提示用户校准）
    }

    private void notifyListeners(float degree) {
        if (listener != null) {
            listener.onDegreeChanged(degree);
            listener.onDirectionChanged(getCurrentDirection(context, degree)); // 使用实例方法
        }
    }
}
