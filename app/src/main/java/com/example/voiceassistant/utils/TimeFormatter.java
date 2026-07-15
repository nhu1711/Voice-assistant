package com.example.voiceassistant.utils;

import android.content.Context;
import com.example.voiceassistant.R;
import com.example.voiceassistant.constants.AppConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Định dạng thời gian chuyên nghiệp, hỗ trợ đa ngôn ngữ
 */
public class TimeFormatter {
    
    public static String getDisplayTime(Context context) {
        String format = context.getString(R.string.time_display_format);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    public static String getTimeSpeech(Context context, String language) {
        Calendar calendar = Calendar.getInstance();
        int minute = calendar.get(Calendar.MINUTE);
        
        if (AppConstants.LANGUAGE_VIETNAMESE.equals(language)) {
            int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
            if (minute == 0) {
                return context.getString(R.string.time_spoken_vi_no_min, hour24);
            } else {
                return context.getString(R.string.time_spoken_vi_full, hour24, minute);
            }
        } else {
            // English logic: 12h format + AM/PM
            int hour12 = calendar.get(Calendar.HOUR);
            if (hour12 == 0) hour12 = 12;
            String amPm = calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            
            if (minute == 0) {
                return context.getString(R.string.time_spoken_en_no_min, hour12, amPm);
            } else {
                return context.getString(R.string.time_spoken_en_full, hour12, minute, amPm);
            }
        }
    }
}
