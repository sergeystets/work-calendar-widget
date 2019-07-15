package com.google.work.calendar.widget.utils;

import android.content.Context;

import java.util.Locale;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocaleUtils {

    private static final String LOCALE_LANGUAGE_RU = "ru";

    public static Locale getLocaleFor(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    public static boolean isRussianLocale(Context context) {
        return getLocaleFor(context).getLanguage().equals(LOCALE_LANGUAGE_RU);
    }
}
