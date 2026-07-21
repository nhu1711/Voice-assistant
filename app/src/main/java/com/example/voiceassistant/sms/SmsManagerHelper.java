package com.example.voiceassistant.sms;

import android.content.Context;
import android.telephony.SmsManager;

import java.util.ArrayList;

public class SmsManagerHelper {

    public interface SmsCallback {
        void onSuccess();

        void onFailure();
    }

    private final Context appContext;

    public SmsManagerHelper(Context context) {
        appContext = context.getApplicationContext();
    }

    public void sendSms(String phoneNumber, String message, SmsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!isValidPhoneNumber(phoneNumber) || message == null || message.trim().isEmpty()) {
            callback.onFailure();
            return;
        }

        try {
            SmsManager smsManager = appContext.getSystemService(SmsManager.class);
            if (smsManager == null) {
                smsManager = SmsManager.getDefault();
            }

            ArrayList<String> messageParts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber.trim(), null, messageParts, null, null);
            callback.onSuccess();
        } catch (SecurityException exception) {
            callback.onFailure();
        } catch (RuntimeException exception) {
            callback.onFailure();
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        boolean hasDigit = false;
        String trimmedPhoneNumber = phoneNumber.trim();
        for (int i = 0; i < trimmedPhoneNumber.length(); i++) {
            char current = trimmedPhoneNumber.charAt(i);
            if (Character.isDigit(current)) {
                hasDigit = true;
                continue;
            }
            if (current == '+' || current == '-' || current == '(' || current == ')'
                    || current == '.' || Character.isWhitespace(current)) {
                continue;
            }
            return false;
        }
        return hasDigit;
    }
}
