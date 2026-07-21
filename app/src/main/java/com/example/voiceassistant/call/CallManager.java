package com.example.voiceassistant.call;

import android.app.Activity;
import android.content.Context;
import android.content.ActivityNotFoundException;
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
        Log.d(TAG, "SOS call step started");
        // BR-02: Kiểm tra quyền CALL_PHONE
        if (!PermissionHelper.hasCallPhonePermission(context)) {
            Log.w(TAG, "CALL_PHONE permission missing");
            // Note: TTS feedback should be handled or passed localized strings
            return false;
        }
        Log.d(TAG, "CALL_PHONE permission granted");

        String normalizedPhoneNumber = normalizePhoneNumberForCall(phoneNumber);
        if (normalizedPhoneNumber.isEmpty()) {
            Log.w(TAG, "Call Intent launch failed");
            return false;
        }
        
        try {
            // Intent thực hiện cuộc gọi
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.fromParts("tel", normalizedPhoneNumber, null));
            if (!(context instanceof Activity)) {
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            
            Log.d(TAG, "Call Intent launch requested");
            context.startActivity(callIntent);
            Log.d(TAG, "Call Intent launched successfully");
            return true;
            
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Call Intent launch failed");
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Call Intent launch failed");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Call Intent launch failed");
            return false;
        }
    }

    private String normalizePhoneNumberForCall(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String trimmedPhoneNumber = phoneNumber.trim();
        StringBuilder normalized = new StringBuilder();
        int digitCount = 0;
        for (int i = 0; i < trimmedPhoneNumber.length(); i++) {
            char current = trimmedPhoneNumber.charAt(i);
            if (Character.isDigit(current)) {
                normalized.append(current);
                digitCount++;
            } else if (current == '+' && normalized.length() == 0) {
                normalized.append(current);
            } else if (current == ' ' || current == '-' || current == '(' || current == ')') {
                // Common phone separators are ignored before building the tel URI.
            } else {
                return "";
            }
        }

        if (digitCount < 3 || digitCount > 15) {
            return "";
        }
        return normalized.toString();
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
