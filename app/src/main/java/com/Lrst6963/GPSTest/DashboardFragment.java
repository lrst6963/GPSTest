package com.Lrst6963.GPSTest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

public class DashboardFragment extends Fragment {

    private DashboardController dashboardController;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        dashboardController = new DashboardController(requireContext(), root);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dashboardController != null) {
            dashboardController.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dashboardController != null) {
            dashboardController.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dashboardController != null) {
            dashboardController.onDestroy();
        }
    }
}
