package com.Lrst6963.GPSTest;

import android.app.Application;
import android.content.Context;

import com.Lrst6963.GPSTest.tools.LocaleHelper;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.setLocale(base, LocaleHelper.getPersistedLanguage(base)));
    }
}
