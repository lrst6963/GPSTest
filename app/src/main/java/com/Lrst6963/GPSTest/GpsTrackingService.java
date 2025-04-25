package com.Lrst6963.GPSTest;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class GpsTrackingService extends Service {
    private static final String TAG = "GpsTrackingService";
    private static final String KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled";
    private static final int NOTIFICATION_ID = 12345;
    private static final String CHANNEL_ID = "gps_tracking_channel";
    private static final int UPDATE_INTERVAL_MS = 1000;
    private final IBinder binder = new LocalBinder();
    private final List<GpsCallback> callbacks = new ArrayList<>(); // 支持多个回调
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GnssStatus.Callback gnssStatusCallback;
    private int satelliteCount = 0;
    private int usedInFixCount = 0;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
            Log.d(TAG, "Notifications enabled");
        } else {
            Log.e(TAG, "Notifications disabled, not starting foreground");
        }
        initLocationListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY; // 确保服务在后台持续运行
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        stopLocationUpdates();
    }

    // 公开方法
    public void registerCallback(@NonNull GpsCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void unregisterCallback(@NonNull GpsCallback callback) {
        callbacks.remove(callback);
    }

    // 私有方法
    private void initLocationListener() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                for (GpsCallback callback : callbacks) {
                    callback.onLocationChanged(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // 不需要实现
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "Provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "Provider disabled: " + provider);
            }
        };

        gnssStatusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                satelliteCount = status.getSatelliteCount();
                usedInFixCount = 0;
                for (int i = 0; i < satelliteCount; i++) {
                    if (status.usedInFix(i)) {
                        usedInFixCount++;
                    }
                }
                Log.d(TAG, "Satellite status changed: " + satelliteCount + " satellites, " + usedInFixCount + " used in fix");
                for (GpsCallback callback : callbacks) {
                    callback.onGpsStatusChanged(satelliteCount, usedInFixCount);
                }
            }
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    UPDATE_INTERVAL_MS,
                    0,
                    locationListener
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.registerGnssStatusCallback(gnssStatusCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            if (gnssStatusCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.GPSTrackingService))
                .setContentText(getString(R.string.Trackingbackground))
                .setSmallIcon(R.drawable.ic_gps_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GPS Tracking",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background GPS tracking service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 回调接口定义
    public interface GpsCallback {
        void onLocationChanged(Location location);

        void onGpsStatusChanged(int satellites, int usedInFix);
    }

    // Binder实现
    public class LocalBinder extends Binder {
        public GpsTrackingService getService() {
            return GpsTrackingService.this;
        }
    }
}
