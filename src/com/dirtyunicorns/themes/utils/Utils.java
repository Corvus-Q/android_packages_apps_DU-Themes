/*
 * Copyright (C) 2020 The Dirty Unicorns Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dirtyunicorns.themes.utils;

import static android.os.UserHandle.USER_SYSTEM;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULE;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_END_THEME;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_END_THEME_VALUE;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_END_TIME;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_START_THEME;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_START_THEME_VALUE;
import static com.dirtyunicorns.themes.Themes.PREF_THEME_SCHEDULED_START_TIME;

import android.app.Activity;
import android.app.UiModeManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.text.TextUtils;
import android.widget.Button;

import com.dirtyunicorns.themes.R;

import com.android.internal.util.du.ThemesUtils;

import java.util.Objects;

public class Utils {

    public static String getThemeSchedule(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULE, "1");
    }

    public static String getScheduledStartTheme(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_START_THEME, null);
    }

    public static String getScheduledStartThemeValue(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_START_THEME_VALUE, null);
    }

    public static String getScheduledStartThemeTime(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_START_TIME, null);
    }

    public static String getScheduledStartThemeSummary(SharedPreferences mSharedPreferences, Context context) {
        String scheduledStartThemeSummary = mSharedPreferences.getString(PREF_THEME_SCHEDULED_START_THEME, null);

        if (scheduledStartThemeSummary != null) {
            switch (scheduledStartThemeSummary) {
                case "1":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_light);
                    break;
                case "2":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_google_dark);
                    break;
                case "3":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_pitch_black);
                    break;
                case "4":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_solarized_dark);
                    break;
            }
        }
        return scheduledStartThemeSummary;
    }

    public static String getScheduledEndTheme(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_END_THEME, null);
    }

    public static String getScheduledEndThemeValue(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_END_THEME_VALUE, null);
    }

    public static String getScheduledEndThemeTime(SharedPreferences mSharedPreferences) {
        return mSharedPreferences.getString(PREF_THEME_SCHEDULED_END_TIME, null);
    }

    public static String getScheduledEndThemeSummary(SharedPreferences mSharedPreferences, Context context) {
        String scheduledEndThemeSummary = mSharedPreferences.getString(PREF_THEME_SCHEDULED_END_THEME, null);
        if (scheduledEndThemeSummary != null) {
            switch (scheduledEndThemeSummary) {
                case "1":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_light);
                    break;
                case "2":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_google_dark);
                    break;
                case "3":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_pitch_black);
                    break;
                case "4":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_solarized_dark);
                    break;
            }
        }
        return scheduledEndThemeSummary;
    }

    public static void handleOverlays(String packagename, Boolean state, IOverlayManager mOverlayManager) {
        try {
            mOverlayManager.setEnabled(packagename,
                    state, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void handleBackgrounds(Boolean state, Context context, int mode, String[] overlays, IOverlayManager mOverlayManager) {
        if (context != null) {
            Objects.requireNonNull(context.getSystemService(UiModeManager.class))
                    .setNightMode(mode);
        }
        for (int i = 0; i < overlays.length; i++) {
            String background = overlays[i];
            try {
                mOverlayManager.setEnabled(background, state, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setForegroundDrawable(String packagename, Button buttonAccent, Activity activity) {
        if (com.android.internal.util.du.Utils.isThemeEnabled(packagename)) {
            buttonAccent.setForeground(activity.getResources().getDrawable(
                    R.drawable.accent_picker_checkmark, null));
        } else {
            buttonAccent.setForeground(null);
        }
    }

    public static boolean isLiveWallpaper(Context context) {
        WallpaperInfo info = WallpaperManager.getInstance(context).getWallpaperInfo();
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        return info != null && !TextUtils.isEmpty(info.getComponent().getPackageName())
                && wallpaperManager.isSetWallpaperAllowed();
    }

    public static void setDefaultAccentColor(IOverlayManager overlayManager) {
        for (int i = 0; i < ThemesUtils.ACCENTS.length; i++) {
            String accent = ThemesUtils.ACCENTS[i];
            try {
                overlayManager.setEnabled(accent, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void enableAccentColor(IOverlayManager overlayManager, String accentPicker) {
        try {
            for (int i = 0; i < ThemesUtils.ACCENTS.length; i++) {
                String accent = ThemesUtils.ACCENTS[i];
                try {
                    overlayManager.setEnabled(accent, false, USER_SYSTEM);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            overlayManager.setEnabled(accentPicker, true, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
