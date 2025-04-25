package com.Lrst6963.GPSTest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DashboardController implements GpsTrackingService.GpsCallback {

    private static final float WEAK_SIGNAL_THRESHOLD = 10.0f;
    private static final float STRONG_SIGNAL_RATIO = 0.4f;
    private static final float MEDIUM_SIGNAL_RATIO = 0.2f;
    private static final float WEAK_SIGNAL_RATIO = 0.1f;
    private static final int MIN_SATELLITES_FOR_ACCURACY = 4;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private long lastElapsedMillis = 0;
    private float lastTotalDistance = 0;
    private long elapsedMillis = 0;
    private String TAG = "DashboardController";

    // UI组件
    private TextView tvSpeedKmh, tvSpeedMs;
    private TextView tvTime, tvMaxSpeed, tvDistance, tvAvgSpeed;
    private TextView tvGpsStatus, tvSatellites;
    private TextView tvDirection, tvAltitude;
    private Button btnControl, btnReset, btnPause;
    private LinearLayout controlButtonsContainer;

    // 时间更新任务
    private final Runnable timeUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (isTracking && !isPaused) {
                long currentElapsed = System.currentTimeMillis() - startTime;
                tvTime.setText(context.getString(R.string.time_format, formatDuration(currentElapsed)));
            }
            handler.postDelayed(this, 1000); // 每秒更新
        }
    };

    // 服务相关
    private GpsTrackingService gpsService;
    private boolean isBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GpsTrackingService.LocalBinder binder = (GpsTrackingService.LocalBinder) service;
            gpsService = binder.getService();
            gpsService.registerCallback(DashboardController.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private boolean isPaused = false;
    private boolean isTracking = false;
    private float maxSpeed = 0;
    private float totalDistance = 0;
    private long startTime = 0;
    private long pausedElapsedMillis = 0;
    private Location previousLocation;
    private int usedSatellites = 0;
    private int visibleSatellites = 0;
    private boolean hasValidAvgSpeed = false;

    public DashboardController(Context context, View rootView) {
        this.context = context;
        initViews(rootView);

        // 绑定服务
        Intent intent = new Intent(context, GpsTrackingService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        btnControl.setOnClickListener(v -> toggleTracking());
        btnReset.setOnClickListener(v -> resetTracking());
        btnPause.setOnClickListener(v -> togglePause());
    }


    private void initViews(View root) {
        tvSpeedKmh = root.findViewById(R.id.textView1);
        tvSpeedMs = root.findViewById(R.id.textView3);
        tvTime = root.findViewById(R.id.tvTime);
        tvMaxSpeed = root.findViewById(R.id.tvMaxSpeed);
        tvDistance = root.findViewById(R.id.tvDistance);
        tvAvgSpeed = root.findViewById(R.id.tvAvgSpeed);
        tvGpsStatus = root.findViewById(R.id.tvGpsStatus);
        tvSatellites = root.findViewById(R.id.tvSatellites);
        btnControl = root.findViewById(R.id.btnControl);
        tvDirection = root.findViewById(R.id.tvDirection);
        tvAltitude = root.findViewById(R.id.tvAltitude);
        btnReset = root.findViewById(R.id.btnReset);
        btnPause = root.findViewById(R.id.btnPause);
        controlButtonsContainer = root.findViewById(R.id.controlButtonsContainer);

        resetDisplay();
    }

    public void onResume() {
        // 恢复时可能需要重新绑定服务
        if (!isBound) {
            Intent intent = new Intent(context, GpsTrackingService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void onPause() {
        // 暂停时停止更新
        handler.removeCallbacks(timeUpdateTask);
    }

    public void onDestroy() {
        stopTracking();
        if (isBound) {
            if (gpsService != null) {
                gpsService.unregisterCallback(this);
            }
            context.unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(timeUpdateTask);
    }

    private void togglePause() {
        if (!isPaused) {
            btnPause.setText(context.getString(R.string.resume));
            btnPause.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.orange)));
            isPaused = true;
            pausedElapsedMillis = System.currentTimeMillis() - startTime;
            handler.removeCallbacks(timeUpdateTask);
        } else {
            btnPause.setText(context.getString(R.string.pause));
            btnPause.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_200)));
            isPaused = false;
            startTime = System.currentTimeMillis() - pausedElapsedMillis;
            handler.post(timeUpdateTask);
        }
    }

    private void resetTracking() {
        resetTrackingData();
        resetDisplay();
        if (!isPaused) {
            startTracking();
        }
    }

    private void resetDisplay() {
        tvTime.setText(context.getString(R.string.time_format, "--:--:--"));
        tvMaxSpeed.setText(context.getString(R.string.max_speed_format, "-"));
        tvDistance.setText(context.getString(R.string.distance_format, "-"));
        tvAvgSpeed.setText(context.getString(R.string.avg_speed_format, "-"));
        tvSpeedKmh.setText(context.getString(R.string.speed_kmh_format, formatSpeedKmh(0)));
        tvSpeedMs.setText(context.getString(R.string.speed_ms_format, formatSpeedMs(0)));
        tvDirection.setText(context.getString(R.string.direction_format, "N", 0f));
        tvAltitude.setText(context.getString(R.string.altitude_format, 0f));
    }

    private void toggleTracking() {
        if (isTracking && !isPaused) {
            stopTracking();
            controlButtonsContainer.setVisibility(View.GONE);
        } else if (isPaused) {
            stopTracking();
            controlButtonsContainer.setVisibility(View.GONE);
            isPaused = false;
        } else {
            startTracking();
            controlButtonsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void startTracking() {
        try {
            isTracking = true;
            isPaused = false;
            resetTrackingData();
            Sensorscom.init(context);

            handler.post(timeUpdateTask);

            btnControl.setText(context.getString(R.string.stop));
            btnControl.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_500)));
            btnPause.setText(context.getString(R.string.pause));
            controlButtonsContainer.setVisibility(View.VISIBLE);

            Intent serviceIntent = new Intent(context, GpsTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            if (!isBound) {
                context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            } else if (gpsService != null) {
                gpsService.registerCallback(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "startTracking: 启动跟踪失败", e);
        }
    }

    private void stopTracking() {
        isTracking = false;
        isPaused = false;

        handler.removeCallbacks(timeUpdateTask);

        tvSpeedKmh.setText(context.getString(R.string.speed_kmh_format, formatSpeedKmh(0)));
        tvSpeedMs.setText(context.getString(R.string.speed_ms_format, formatSpeedMs(0)));
        btnControl.setText(context.getString(R.string.start));
        btnControl.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_500)));
        controlButtonsContainer.setVisibility(View.GONE);

        Intent intent = new Intent(context, GpsTrackingService.class);
        context.stopService(intent);

        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void resetTrackingData() {
        startTime = System.currentTimeMillis();
        maxSpeed = 0;
        totalDistance = 0;
        previousLocation = null;
        usedSatellites = 0;
        visibleSatellites = 0;
        hasValidAvgSpeed = false;
        pausedElapsedMillis = 0;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!isTracking) return;
        if (isPaused) {
            updateGpsStatus();
            return;
        }

        boolean isWeakSignal = location.getAccuracy() > WEAK_SIGNAL_THRESHOLD ||
                getSatelliteRatio() < WEAK_SIGNAL_RATIO ||
                usedSatellites < MIN_SATELLITES_FOR_ACCURACY;

        if (isWeakSignal) {
            if (hasValidAvgSpeed) {
                handler.post(() -> tvAvgSpeed.setText(context.getString(R.string.avg_speed_format, "-")));
            }
            return;
        }

        updateTrackingData(location);

        float spMs = location.getSpeed();
        float speedKmh = spMs * 3.6f;
        double altitude = location.hasAltitude() ? location.getAltitude() : 0;
        float bearings = Sensorscom.getDegree();
        String currentDirection = Compass.getCurrentDirection(context, bearings);

        handler.post(() -> {
            tvSpeedKmh.setText(context.getString(R.string.speed_kmh_format, formatSpeedKmh(speedKmh)));
            tvSpeedMs.setText(context.getString(R.string.speed_ms_format, formatSpeedMs(spMs)));
            tvDirection.setText(context.getString(R.string.direction_format, currentDirection, bearings));
            tvAltitude.setText(String.format(Locale.getDefault(), context.getString(R.string.altitude_format), altitude));

            if (speedKmh > maxSpeed) maxSpeed = speedKmh;
            tvMaxSpeed.setText(context.getString(R.string.max_speed_format, formatSpeed(maxSpeed)));
            tvDistance.setText(context.getString(R.string.distance_format, formatDistance(totalDistance)));

            long currentElapsed = System.currentTimeMillis() - startTime;
            if (currentElapsed > 0 && totalDistance > 0) {
                float avgSpeed = (totalDistance * 3600) / (currentElapsed / 1000f);
                tvAvgSpeed.setText(context.getString(R.string.avg_speed_format, formatSpeed(avgSpeed)));
                hasValidAvgSpeed = true;
            } else {
                tvAvgSpeed.setText(context.getString(R.string.avg_speed_format, "-"));
            }
        });
    }

    private void updateTrackingData(Location location) {
        if (previousLocation != null) {
            totalDistance += location.distanceTo(previousLocation) / 1000;
        }
        previousLocation = location;
        lastElapsedMillis = System.currentTimeMillis() - startTime;
        lastTotalDistance = totalDistance;
    }

    @Override
    public void onGpsStatusChanged(int satellites, int usedInFix) {
        visibleSatellites = satellites;
        usedSatellites = usedInFix;
        updateGpsStatus();
    }

    private void updateGpsStatus() {
        handler.post(() -> {
            tvSatellites.setText(context.getString(R.string.satellites_format, usedSatellites, visibleSatellites));

            float ratio = getSatelliteRatio();
            String status;
            int color;

            if (ratio >= STRONG_SIGNAL_RATIO) {
                status = context.getString(R.string.gps_status_strong, ratio * 100);
                color = R.color.green;
            } else if (ratio >= MEDIUM_SIGNAL_RATIO) {
                status = context.getString(R.string.gps_status_medium, ratio * 100);
                color = R.color.orange;
            } else if (ratio >= WEAK_SIGNAL_RATIO) {
                status = context.getString(R.string.gps_status_weak, ratio * 100);
                color = R.color.red;
            } else {
                status = context.getString(R.string.gps_status_lost);
                color = R.color.red;
            }

            tvGpsStatus.setText(status);
            tvGpsStatus.setTextColor(ContextCompat.getColor(context, color));
        });
    }

    private float getSatelliteRatio() {
        return visibleSatellites > 0 ? (float) usedSatellites / visibleSatellites : 0;
    }

    private String formatDuration(long millis) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    private String formatSpeed(float speed) {
        return speed > 0 ? String.format(Locale.getDefault(), "%.0f", speed) : "-";
    }

    private String formatDistance(float distance) {
        return distance > 0 ? String.format(Locale.getDefault(), "%.2f", distance) : "-";
    }

    private String formatSpeedKmh(float speed) {
        return speed > 0 ? String.format(Locale.getDefault(), "%.0f", speed) : "0";
    }

    private String formatSpeedMs(float speed) {
        return speed > 0 ? String.format(Locale.getDefault(), "%.0f", speed) : "0";
    }
}
