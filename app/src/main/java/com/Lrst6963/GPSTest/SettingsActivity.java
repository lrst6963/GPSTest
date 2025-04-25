package com.Lrst6963.GPSTest;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.Lrst6963.GPSTest.tools.LocaleHelper;
import com.example.myapplication.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseActivity {

    private static final String KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled";
    private static final String KEY_DARK_MODE_ENABLED = "pref_dark_mode_enabled";
    private static final String KEY_SELECTED_LANGUAGE = "pref_selected_language";
    private static final String KEY_MAIN_AUTO_START = "pref_auto_start";

    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchAutoStart;
    private TextView tvLanguageValue;
    private TextView tvVersion;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        switchNotifications = findViewById(R.id.switch_notifications);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchAutoStart = findViewById(R.id.switch_main_autostart);
        tvLanguageValue = findViewById(R.id.tv_language_value);
        tvVersion = findViewById(R.id.tv_version);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(getString(R.string.version_format, versionName));
        } catch (Exception e) {
            tvVersion.setText(getString(R.string.version_unknown));
        }
    }

    private void loadSettings() {
        switchNotifications.setChecked(sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true));
        switchDarkMode.setChecked(sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false));
        switchAutoStart.setChecked(sharedPreferences.getBoolean(KEY_MAIN_AUTO_START, false));
        tvLanguageValue.setText(sharedPreferences.getString(KEY_SELECTED_LANGUAGE, getString(R.string.language_default)));
    }

    private void setupListeners() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply();
            showToast(isChecked ? R.string.notifications_enabled : R.string.notifications_disabled);
            resetActivity();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, isChecked).apply();
            applyDarkMode(isChecked);
            // 不需要recreate，使用新的API平滑过渡
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        switchAutoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_MAIN_AUTO_START, isChecked).apply();
        });

        findViewById(R.id.layout_language).setOnClickListener(v -> showLanguageSelectionDialog());
        findViewById(R.id.layout_privacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
    }

    // ==================== 语言切换功能 ====================
    private void showLanguageSelectionDialog() {
        String[] languages = getResources().getStringArray(R.array.languages);
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        String currentLanguage = sharedPreferences.getString(KEY_SELECTED_LANGUAGE, "zh");
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.select_language)
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    String selectedCode = languageCodes[which];
                    sharedPreferences.edit().putString(KEY_SELECTED_LANGUAGE, selectedCode).apply();
                    LocaleHelper.setLocale(this, selectedCode);
                    // 重启所有Activity以应用语言更改
                    resetActivity();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void resetActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== 深色模式优化 ====================
    private void applySavedTheme() {
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(KEY_DARK_MODE_ENABLED, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void applyDarkMode(boolean enabled) {
        // 可以在这里添加更多深色模式相关的UI更新
        if (enabled) {
            // 深色模式特定UI调整
        } else {
            // 浅色模式特定UI调整
        }
    }

    // ==================== 辅助方法 ====================
    private void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
