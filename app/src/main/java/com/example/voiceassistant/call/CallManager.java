package com.example.voiceassistant.call;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.voiceassistant.contacts.ContactManager;
import com.example.voiceassistant.permissions.PermissionHelper;
import com.example.voiceassistant.tts.TTSManager;

/**
 * Quản lý thực hiện cuộc gọi
 * FR-04: Gọi điện từ danh bạ
 */
public class CallManager {
    
    private static final String TAG = "CallManager";
    private final Context context;
    private final TTSManager ttsManager;
    
    public CallManager(Context context, TTSManager ttsManager) {
        this.context = context;
        this.ttsManager = ttsManager;
    }
    
    /**
     * Thực hiện cuộc gọi đến số điện thoại
     * BR-02: Phải có quyền CALL_PHONE
     * 
     * @param phoneNumber Số điện thoại cần gọi
     * @param contactName Tên liên hệ (để đọc phản hồi)
     * @return true nếu cuộc gọi được thực hiện, false nếu không
     */
    public boolean makeCall(String phoneNumber, String contactName) {
        // BR-02: Kiểm tra quyền CALL_PHONE
        if (!PermissionHelper.hasCallPhonePermission(context)) {
            Log.w(TAG, "No CALL_PHONE permission");
            // Note: TTS feedback should be handled or passed localized strings
            return false;
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.w(TAG, "Phone number is empty");
            return false;
        }
        
        try {
            // Intent thực hiện cuộc gọi
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(callIntent);
            Log.d(TAG, "Calling: " + contactName + " - " + phoneNumber);
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when making call", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error making call", e);
            return false;
        }
    }
    
    /**
     * Thực hiện cuộc gọi với thông tin liên hệ
     */
    public boolean makeCall(ContactManager.ContactInfo contact) {
        if (contact == null) {
            return false;
        }
        return makeCall(contact.getPhoneNumber(), contact.getName());
    }
}
