package com.example.voiceassistant.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private int currentPage = 1;
    private TextView tvTitle, tvDesc;
    private MaterialButton btnAction;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        tvTitle = findViewById(R.id.tv_onboarding_title);
        tvDesc = findViewById(R.id.tv_onboarding_desc);
        btnAction = findViewById(R.id.btn_onboarding_action);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        btnAction.setOnClickListener(v -> {
            if (currentPage < 3) {
                currentPage++;
                updateUI();
            } else {
                finishOnboarding();
            }
        });
    }

    private void updateUI() {
        int colorPrimary = getResources().getColor(R.color.primary);
        int colorDisabled = getResources().getColor(R.color.button_disabled);

        if (currentPage == 2) {
            tvTitle.setText(R.string.onboarding_title_2);
            tvDesc.setText(R.string.onboarding_desc_2);
            dot1.setBackgroundColor(colorDisabled);
            dot2.setBackgroundColor(colorPrimary);
        } else if (currentPage == 3) {
            tvTitle.setText(R.string.onboarding_title_3);
            tvDesc.setText(R.string.onboarding_desc_3);
            btnAction.setText(R.string.btn_get_started);
            dot2.setBackgroundColor(colorDisabled);
            dot3.setBackgroundColor(colorPrimary);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AppConstants.PREF_FIRST_RUN, false).apply();

        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
    }
}
