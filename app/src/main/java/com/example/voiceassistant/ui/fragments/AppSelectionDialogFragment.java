package com.example.voiceassistant.ui.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSelectionDialogFragment extends DialogFragment {

    private RecyclerView rvApps;
    private AppAdapter adapter;
    private List<AppInfo> appList;
    private Set<String> selectedPackages;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_app_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvApps = view.findViewById(R.id.rv_apps);
        rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadApps();

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveAndDismiss());
    }

    private void loadApps() {
        SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedPackages = prefs.getStringSet(AppConstants.PREF_SELECTED_APPS, new HashSet<>());
        selectedPackages = new HashSet<>(savedPackages);

        new Thread(() -> {
            PackageManager pm = requireContext().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfo> apps = new ArrayList<>();

            for (ApplicationInfo packageInfo : packages) {
                // Only show launchable apps or installed apps, filter system apps if necessary
                if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                    apps.add(new AppInfo(
                            packageInfo.loadLabel(pm).toString(),
                            packageInfo.packageName,
                            packageInfo.loadIcon(pm)
                    ));
                }
            }

            Collections.sort(apps, (a1, a2) -> a1.name.compareToIgnoreCase(a2.name));

            requireActivity().runOnUiThread(() -> {
                appList = apps;
                adapter = new AppAdapter(appList, selectedPackages);
                rvApps.setAdapter(adapter);
            });
        }).start();
    }

    private void saveAndDismiss() {
        SharedPreferences prefs = requireContext().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(AppConstants.PREF_SELECTED_APPS, selectedPackages).apply();
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }
    }
}
