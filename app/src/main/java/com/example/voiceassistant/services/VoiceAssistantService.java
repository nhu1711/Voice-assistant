package com.example.voiceassistant.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;
import com.example.voiceassistant.receivers.SystemBroadcastReceiver;
import com.example.voiceassistant.ui.activities.MainActivity;
import com.example.voiceassistant.utils.LocaleHelper;
import com.example.voiceassistant.tts.TTSManager;

/**
 * Foreground Service để trợ lý chạy nền (FR-09.1)
 */
public class VoiceAssistantService extends Service {
    private static final String TAG = "VoiceAssistantService";
    private SystemBroadcastReceiver systemReceiver;
    private TTSManager ttsManager;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize managers
        ttsManager = TTSManager.getInstance(this);
        
        createNotificationChannel();
        registerSystemReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && AppConstants.ACTION_STOP_SERVICE.equals(intent.getAction())) {
            Log.d(TAG, "Service Stop Requested");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(AppConstants.NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Service Started as Foreground");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task Removed (App swiped away)");
        // Foreground service is kept alive by system.
        super.onTaskRemoved(rootIntent);
    }

    private void registerSystemReceivers() {
        systemReceiver = new SystemBroadcastReceiver();
        systemReceiver.setTtsManager(ttsManager);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(systemReceiver, filter);
        Log.d(TAG, "System Receivers Registered");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    AppConstants.CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, VoiceAssistantService.class);
        stopIntent.setAction(AppConstants.ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, AppConstants.CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_running))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, 
                        getString(R.string.stop_service), stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service is being destroyed");
        
        if (systemReceiver != null) {
            unregisterReceiver(systemReceiver);
            systemReceiver = null;
        }
        
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
