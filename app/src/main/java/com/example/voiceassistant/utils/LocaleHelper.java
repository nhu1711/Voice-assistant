package com.example.voiceassistant.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.example.voiceassistant.constants.AppConstants;

import java.util.Locale;

public class LocaleHelper {

    public static Context onAttach(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(AppConstants.PREF_LANGUAGE, Locale.getDefault().getLanguage());
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }
}
