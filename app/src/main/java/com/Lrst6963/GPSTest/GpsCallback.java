package com.Lrst6963.GPSTest;

import android.location.Location;

public interface GpsCallback {
    void onLocationChanged(Location location);

    void onGpsStatusChanged(int satellites, int usedInFix);
}
