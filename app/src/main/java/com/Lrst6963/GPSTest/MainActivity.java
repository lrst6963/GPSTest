package com.Lrst6963.GPSTest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityMainBinding;


public class MainActivity extends BaseActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        // 检查并请求位置权限
        checkLocationPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_help) {
            showAppInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAppInfoDialog() {
        // 1. 初始化 Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_app_info, null);
        builder.setView(dialogView);
        // 2. 设置源码链接点击事件
        TextView tvSourceLink = dialogView.findViewById(R.id.tvSourceCodeLink);
        tvSourceLink.setOnClickListener(v -> {
            String url = "https://github.com/lrst6963/GPSTest";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
        // 3. 添加关闭按钮
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        // 4. 显示弹窗
        AlertDialog dialog = builder.create();
        dialog.show();
        // 可选：自定义按钮样式
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
    }

    private void setupViewPager() {
        // 设置ViewPager2
        binding.viewPager.setAdapter(new MainPagerAdapter(this));
        binding.viewPager.setOffscreenPageLimit(2);
        binding.viewPager.setUserInputEnabled(false); // 禁止滑动

        // 绑定BottomNavigationView和ViewPager2
        binding.navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                binding.viewPager.setCurrentItem(0, false);
            } else if (itemId == R.id.navigation_dashboard) {
                binding.viewPager.setCurrentItem(1, false);
            } else if (itemId == R.id.navigation_notifications) {
                binding.viewPager.setCurrentItem(2, false);
            }
            return true;
        });
    }


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 已经有权限，初始化ViewPager
            setupViewPager();
        } else {
            // 没有权限，判断是否需要显示解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // 显示解释对话框
                new AlertDialog.Builder(this)
                        .setTitle("需要位置权限")
                        .setMessage("此应用需要访问您的位置信息以提供相关服务")
                        .setPositiveButton("确定", (dialog, which) ->
                                requestLocationPermission())
                        .setNegativeButton("取消", (dialog, which) ->
                                finish())
                        .show();
            } else {
                // 直接请求权限
                requestLocationPermission();
            }
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，初始化ViewPager
                setupViewPager();
            } else {
                // 权限被拒绝，可以提示用户或关闭应用
                new AlertDialog.Builder(this)
                        .setTitle("权限被拒绝")
                        .setMessage("没有位置权限，部分功能将无法使用")
                        .setPositiveButton("确定", (dialog, which) -> {
                            // 即使没有权限也继续运行
                            setupViewPager();
                        })
                        .setNegativeButton("退出", (dialog, which) -> finish())
                        .show();
            }
        }
    }
}
