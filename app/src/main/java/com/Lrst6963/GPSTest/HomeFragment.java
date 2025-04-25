package com.Lrst6963.GPSTest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HomeFragment extends Fragment implements GpsTrackingService.GpsCallback {
    private static final String TAG = "HomeFragment";
    // 时间格式化
    private final SimpleDateFormat gpsTimeFormat =
            new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat localTimeFormat =
            new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
    private TextView tvLocationInfo;
    private Button btnToggle;
    private GpsTrackingService gpsTrackingService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GpsTrackingService.LocalBinder binder = (GpsTrackingService.LocalBinder) service;
            gpsTrackingService = binder.getService();
            gpsTrackingService.registerCallback(HomeFragment.this); // 注册回调
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gpsTrackingService = null;
            Log.d(TAG, "Service disconnected");
        }
    };
    private boolean isTracking = false;
    private boolean isLocationAvailable = false;
    private int satelliteCount = 0;
    private int usedInFixCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        tvLocationInfo = root.findViewById(R.id.tvLocationInfo);
        btnToggle = root.findViewById(R.id.btnToggle);

        btnToggle.setOnClickListener(v -> toggleLocationUpdates());

        Log.d(TAG, "HomeFragment created");
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 绑定服务
        Intent intent = new Intent(requireContext(), GpsTrackingService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 解绑服务
        if (gpsTrackingService != null) {
            gpsTrackingService.unregisterCallback(this); // 注销回调
            requireActivity().unbindService(serviceConnection);
        }
    }

    private void toggleLocationUpdates() {
        try {
            if (isTracking) {
                stopLocationUpdates();
                btnToggle.setText(R.string.start_tracking);
                btnToggle.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.purple_500));
                Log.d(TAG, "Location updates stopped");
            } else {
                startLocationUpdates();
                btnToggle.setText(R.string.stop_tracking);
                btnToggle.setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.red_500));
                Log.d(TAG, "Location updates started");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in toggleLocationUpdates: " + e.getMessage(), e);
        }
    }

    private void startLocationUpdates() {
        if (gpsTrackingService != null) {
            isTracking = true;
            isLocationAvailable = false;
            try {
                updateLocationUI(null);
            } catch (Exception e) {
                Log.e(TAG, "Error in startLocationUpdates: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "GPS Tracking Service is not bound");
        }
    }

    private void stopLocationUpdates() {
        if (gpsTrackingService != null) {
            isTracking = false;
            isLocationAvailable = false;
            updateLocationUI(null); // 显示未连接时的占位符
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + (location != null ? location.toString() : "null"));
        if (isTracking) {
            isLocationAvailable = true;
            updateLocationUI(location);
        }
    }

    @Override
    public void onGpsStatusChanged(int satellites, int usedInFix) {
        satelliteCount = satellites;
        usedInFixCount = usedInFix;
        if (isTracking && !isLocationAvailable) {
            updateLocationUI(null); // 更新 UI 以显示卫星数量
        }
    }

    private void updateLocationUI(Location location) {
        try {
            if (!isTracking || getActivity() == null) {
                return;
            }

            // 确保在主线程更新UI
            requireActivity().runOnUiThread(() -> {
                try {
                    // 获取当前时区偏移量
                    TimeZone tz = TimeZone.getDefault();
                    int offsetMillis = tz.getOffset(System.currentTimeMillis());
                    int offsetHours = offsetMillis / (1000 * 60 * 60);
                    String timeZoneStr = String.format(Locale.getDefault(), "UTC%s%d",
                            offsetHours >= 0 ? "+" : "", offsetHours);

                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    SpannableString title = new SpannableString(getString(R.string.location_info_title) + "\n");
                    title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.primary)),
                            0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    title.setSpan(new RelativeSizeSpan(1.5f), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.append(title);

                    if (!isLocationAvailable || location == null) {
                        // 未连接时的占位符
                        builder.append(String.format(Locale.getDefault(),
                                getString(R.string.satellites_info) + "\n" +
                                        getString(R.string.coordinates_placeholder) + "\n" +
                                        getString(R.string.altitude_placeholder) + "\n" +
                                        getString(R.string.speed_placeholder) + "\n" +
                                        getString(R.string.accuracy_placeholder) + "\n" +
                                        getString(R.string.gps_time_placeholder, timeZoneStr) + "\n" +
                                        getString(R.string.system_time_info, localTimeFormat.format(new Date())),
                                usedInFixCount,
                                satelliteCount));
                    } else {

                        // 获取精度信息
                        String accuracyInfo;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            float vertAccuracy = location.hasVerticalAccuracy() ?
                                    (float) location.getVerticalAccuracyMeters() : Float.NaN;
                            float horizAccuracy = location.hasAccuracy() ?
                                    (float) location.getAccuracy() : Float.NaN;

                            accuracyInfo = String.format(Locale.getDefault(),
                                    getString(R.string.accuracy_info_full) + "\n",
                                    horizAccuracy, vertAccuracy);
                        } else {
                            accuracyInfo = String.format(Locale.getDefault(),
                                    getString(R.string.accuracy_info_basic) + "\n",
                                    (float) location.getAccuracy());
                        }

                        String satellitesInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.satellites_info),
                                usedInFixCount,
                                satelliteCount
                        );
                        String coordinatesInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.coordinates_info),
                                (float) location.getLatitude(),
                                (float) location.getLongitude()
                        );
                        String altitudeInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.altitude_info),
                                (float) location.getAltitude()
                        );
                        float speedKmh = location.getSpeed() * 3.6f;
                        String speedInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.speed_info),
                                speedKmh,
                                location.getSpeed()
                        );
                        String gpsTimeInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.gps_time_info),
                                timeZoneStr,
                                gpsTimeFormat.format(new Date(location.getTime()))
                        );
                        String systemTimeInfo = String.format(
                                Locale.getDefault(),
                                getString(R.string.system_time_info),
                                localTimeFormat.format(new Date())
                        );

                        builder.append(satellitesInfo).append("\n")
                                .append(coordinatesInfo).append("\n")
                                .append(altitudeInfo).append("\n")
                                .append(speedInfo).append("\n")
                                .append(accuracyInfo)
                                .append(gpsTimeInfo).append("\n")
                                .append(systemTimeInfo);
                    }

                    tvLocationInfo.setText(builder);
                    Log.d(TAG, "Location UI updated");
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateLocationUI: " + e.getMessage(), e);
        }
    }
}
