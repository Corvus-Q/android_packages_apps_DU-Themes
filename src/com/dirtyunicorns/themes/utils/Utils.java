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

import static android.content.Context.ALARM_SERVICE;
import static android.os.UserHandle.USER_SYSTEM;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULE;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_END_THEME;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_END_THEME_VALUE;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_END_TIME;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_REPEAT_DAILY;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_START_THEME;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_START_THEME_VALUE;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_THEME_SCHEDULED_START_TIME;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_ALARM_START_TIME;
import static com.dirtyunicorns.themes.Schedule.ScheduleFragment.PREF_ALARM_END_TIME;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;

import androidx.preference.PreferenceManager;

import com.android.internal.util.du.ThemesUtils;

import com.dirtyunicorns.themes.R;
import com.dirtyunicorns.themes.receivers.ThemesEndReceiver;
import com.dirtyunicorns.themes.receivers.ThemesStartReceiver;

import java.util.Calendar;
import java.util.Objects;

public class Utils {

    private static long mAlarmEndTime;
    private static long mAlarmStartTime;

    public static boolean isLiveWallpaper(Context context) {
        WallpaperInfo info = WallpaperManager.getInstance(context).getWallpaperInfo();
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        return info != null && !TextUtils.isEmpty(info.getComponent().getPackageName())
                && wallpaperManager.isSetWallpaperAllowed();
    }

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
                case "5":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_choco_x);
                    break;
                case "6":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_baked_green);
                    break;
                case "7":
                    scheduledStartThemeSummary = context.getString(R.string.theme_type_dark_grey);
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
                case "5":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_choco_x);
                    break;
                case "6":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_baked_green);
                    break;
                case "7":
                    scheduledEndThemeSummary = context.getString(R.string.theme_type_dark_grey);
                    break;
            }
        }
        return scheduledEndThemeSummary;
    }

    public static void handleOverlays(String packagename, Boolean state, IOverlayManager mOverlayManager) {
        try {
            mOverlayManager.setEnabled(packagename, state, USER_SYSTEM);
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

    public static void setEndAlarm(Context context) {
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent mEndIntent = new Intent(context, ThemesEndReceiver.class);
        PendingIntent mEndPendingIntent = PendingIntent.getBroadcast(context, 0, mEndIntent, 0);
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false)) {
            if (mAlarmMgr != null) {
                mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, getEndTime(context),
                        mEndPendingIntent);
            }
        } else {
            if (mAlarmMgr != null) {
                mAlarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, getEndTime(context),
                        AlarmManager.INTERVAL_DAY, mEndPendingIntent);
            }
        }
    }

    public static void setStartAlarm(Context context) {
        AlarmManager mAlarmMgr = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent mStartIntent = new Intent(context, ThemesStartReceiver.class);
        PendingIntent mStartPendingIntent = PendingIntent.getBroadcast(context, 0, mStartIntent, 0);
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false)) {
            if (mAlarmMgr != null) {
                mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, getStartTime(context),
                        mStartPendingIntent);
            }
        } else {
            if (mAlarmMgr != null) {
                mAlarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, getStartTime(context),
                        AlarmManager.INTERVAL_DAY, mStartPendingIntent);
            }
        }
    }

    private static long getEndTime(Context context) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mAlarmEndTime = mSharedPreferences.getLong(PREF_ALARM_END_TIME, 0);
        return mAlarmEndTime;
    }

    public static void setEndTime(Context context, Calendar endTime) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        mAlarmEndTime = endTime.getTimeInMillis();
        sharedPreferencesEditor.putLong(PREF_ALARM_END_TIME, mAlarmEndTime);
        sharedPreferencesEditor.apply();
    }

    private static long getStartTime(Context context) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mAlarmStartTime = mSharedPreferences.getLong(PREF_ALARM_START_TIME, 0);
        return mAlarmStartTime;
    }

    public static void setStartTime(Context context, Calendar startTime) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        mAlarmStartTime = startTime.getTimeInMillis();
        sharedPreferencesEditor.putLong(PREF_ALARM_START_TIME, mAlarmStartTime);
        sharedPreferencesEditor.apply();
    }

    public static void clearAlarms(Context context) {
        ComponentName endReceiver = new ComponentName(context, ThemesEndReceiver.class);
        ComponentName startReceiver = new ComponentName(context, ThemesStartReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(endReceiver,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(startReceiver,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Intent endIntent = new Intent(context, ThemesEndReceiver.class);
        Intent startIntent = new Intent(context, ThemesStartReceiver.class);
        PendingIntent endPendingIntent = PendingIntent.getBroadcast(context, 0, endIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent startPendingIntent = PendingIntent.getBroadcast(context, 0, startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        assert am != null;
        am.cancel(endPendingIntent);
        am.cancel(startPendingIntent);
    }

    public static boolean threeButtonNavbarEnabled(Context context) {
        boolean defaultToNavigationBar = context.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        boolean navigationBar = Settings.System.getInt(context.getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, defaultToNavigationBar ? 1 : 0) == 1;
        boolean hasNavbar = false;
        hasNavbar = com.android.internal.util.du.Utils.isThemeEnabled(
                "com.android.internal.systemui.navbar.threebutton") && navigationBar;
        return hasNavbar;
    }
}
