package com.example.voiceassistant.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.voiceassistant.constants.AppConstants;

public class PermissionHelper {
    
    /**
     * Kiểm tra quyền RECORD_AUDIO (BR-01)
     */
    public static boolean hasRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Yêu cầu quyền RECORD_AUDIO
     */
    public static void requestRecordAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{Manifest.permission.RECORD_AUDIO},
            AppConstants.REQUEST_RECORD_AUDIO
        );
    }
    
    /**
     * Kiểm tra quyền CALL_PHONE (BR-02)
     */
    public static boolean hasCallPhonePermission(Context context) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Yêu cầu quyền CALL_PHONE
     */
    public static void requestCallPhonePermission(Activity activity) {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{Manifest.permission.CALL_PHONE},
            AppConstants.REQUEST_CALL_PHONE
        );
    }
    
    /**
     * Kiểm tra quyền READ_CONTACTS
     */
    public static boolean hasReadContactsPermission(Context context) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Yêu cầu quyền READ_CONTACTS
     */
    public static void requestReadContactsPermission(Activity activity) {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{Manifest.permission.READ_CONTACTS},
            AppConstants.REQUEST_READ_CONTACTS
        );
    }
    
    /**
     * Kiểm tra quyền CAMERA
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Yêu cầu quyền CAMERA
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{android.Manifest.permission.CAMERA},
            AppConstants.REQUEST_CAMERA
        );
    }

    /**
     * Kiểm tra tất cả quyền cần thiết cho UC-01
     */
    public static boolean hasAllPermissionsForVoiceCommand(Context context) {
        return hasRecordAudioPermission(context) && hasCallPhonePermission(context) && hasReadContactsPermission(context);
    }

    /**
     * Yêu cầu tất cả các quyền cần thiết ngay khi khởi động
     */
    public static void requestAllPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
            activity,
            new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA
            },
            AppConstants.REQUEST_RECORD_AUDIO // Có thể dùng một code chung
        );
    }
}
