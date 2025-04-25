package com.Lrst6963.GPSTest;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment implements Compass.CompassListener {

    private static final float SMOOTHING_FACTOR = 0.3f;
    private static final float ANGLE_THRESHOLD = 0.5f;
    private static final int ROTATION_ANIM_DURATION = 60; // 毫秒
    private static final int DIRECTION_CHANGE_ANIM_DURATION = 300;

    private FragmentNotificationsBinding binding;
    private Compass compass;
    private Vibrator vibrator;
    private String currentDirection = "";
    private float currentDisplayDegree = 0;

    // 角度跟踪系统
    private float smoothedDegree = 0;
    private float previousRawDegree = 0;
    private int rotationCount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        compass = new Compass(requireActivity().getApplicationContext());
        compass.setListener(this);
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        resetCompass();
        return binding.getRoot();
    }

    private void resetCompass() {
        smoothedDegree = 0;
        previousRawDegree = 0;
        rotationCount = 0;
        currentDirection = "";
        currentDisplayDegree = 0;
        binding.compassNeedle.setRotation(0);
        binding.compassDirectionText.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        resetCompass();
        compass.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    public void onDegreeChanged(float rawDegree) {
        // 1. 处理角度突变
        float delta = rawDegree - previousRawDegree;
        if (delta > 180) rotationCount--;
        else if (delta < -180) rotationCount++;
        previousRawDegree = rawDegree;

        // 2. 计算连续角度
        float continuousDegree = rawDegree + (rotationCount * 360);

        // 3. 应用平滑滤波
        smoothedDegree = smooth(continuousDegree, smoothedDegree, SMOOTHING_FACTOR);

        // 4. 转换为可视角度
        float displayDegree = normalizeDegree(smoothedDegree);

        // 5. 总是更新角度显示，无论变化大小
        updateDirectionText(currentDirection, displayDegree);

        // 5. 更新UI(带动画)
        if (Math.abs(displayDegree - currentDisplayDegree) > ANGLE_THRESHOLD) {
            animateNeedleRotation(displayDegree);
            currentDisplayDegree = displayDegree;
        }
    }

    private void updateDirectionText(String direction, float degree) {
        String directionText = String.format("%.0f° %s", degree, direction);
        binding.compassDirectionText.setText(directionText);
    }

    private void animateNeedleRotation(float newDegree) {
        // 计算最短旋转路径
        float fromDegree = -currentDisplayDegree;
        float toDegree = -newDegree;

        // 处理跨越360°的情况
        float diff = toDegree - fromDegree;
        if (diff > 180) {
            toDegree -= 360;
        } else if (diff < -180) {
            toDegree += 360;
        }

        RotateAnimation anim = new RotateAnimation(
                fromDegree,
                toDegree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        anim.setDuration(ROTATION_ANIM_DURATION);
        anim.setInterpolator(new LinearInterpolator());
        anim.setFillAfter(true);
        binding.compassNeedle.startAnimation(anim);
    }

    @Override
    public void onDirectionChanged(String direction) {
        if (!direction.equals(currentDirection)) {
            // 更新文本为"方向(角度)"格式
            String directionText = String.format("%.0f° %s", currentDisplayDegree, direction);
            animateDirectionChange(directionText);
            currentDirection = direction;
            vibrateOnDirectionChange(direction);
        } else {
            // 方向相同但角度可能变化，更新角度显示
            String directionText = String.format("%.0f° %s", currentDisplayDegree, direction);
            updateDirectionText(direction, currentDisplayDegree);
            binding.compassDirectionText.setText(directionText);
        }
    }

    private void animateDirectionChange(String newDirection) {
        // 先设置文本再动画
        binding.compassDirectionText.setText(newDirection);

        AnimationSet animSet = new AnimationSet(true);

        // 缩放动画
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.9f, 1.1f, 0.9f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(DIRECTION_CHANGE_ANIM_DURATION);

        // 透明度动画
        AlphaAnimation alphaAnim = new AlphaAnimation(0.7f, 1f);
        alphaAnim.setDuration(DIRECTION_CHANGE_ANIM_DURATION);

        animSet.addAnimation(scaleAnim);
        animSet.addAnimation(alphaAnim);
        binding.compassDirectionText.startAnimation(animSet);
    }

    private void vibrateOnDirectionChange(String direction) {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        try {
            long[] pattern = getVibrationPatternForDirection(direction);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long[] getVibrationPatternForDirection(String direction) {
        Context context = requireContext();
        Resources res = context.getResources();

        // 获取所有方向字符串资源
        String north = res.getString(R.string.direction_north);
        String northeast = res.getString(R.string.direction_northeast);
        String east = res.getString(R.string.direction_east);
        String southeast = res.getString(R.string.direction_southeast);
        String south = res.getString(R.string.direction_south);
        String southwest = res.getString(R.string.direction_southwest);
        String west = res.getString(R.string.direction_west);
        String northwest = res.getString(R.string.direction_northwest);
        // 根据基本方向分组
        if (direction.equals(north) || direction.equals(northeast) || direction.equals(northwest)) {
            return new long[]{0, 100}; // 北方振动模式
        } else if (direction.equals(east) || direction.equals(west)) {
            return new long[]{0, 50, 50, 50}; // 东西方振动模式
        } else if (direction.equals(south) || direction.equals(southeast) || direction.equals(southwest)) {
            return new long[]{0, 100, 50, 100}; // 南方振动模式
        } else {
            return new long[]{0, 50}; // 默认振动
        }
    }

    private float smooth(float input, float output, float alpha) {
        float effectiveAlpha = (Math.abs(input - output) > 45) ? alpha * 2 : alpha;
        return output + effectiveAlpha * (input - output);
    }

    private float normalizeDegree(float degree) {
        degree = degree % 360;
        return degree < 0 ? degree + 360 : degree;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compass.stop();
        binding = null;
    }
}
