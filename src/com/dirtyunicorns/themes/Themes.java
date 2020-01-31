/*
 * Copyright (C) 2019-2020 The Dirty Unicorns Project
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

package com.dirtyunicorns.themes;

import static android.content.Context.ALARM_SERVICE;
import static android.os.UserHandle.USER_SYSTEM;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndTheme;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeTime;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeValue;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeSummary;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartTheme;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeTime;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeValue;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeSummary;
import static com.dirtyunicorns.themes.utils.Utils.getThemeSchedule;
import static com.dirtyunicorns.themes.utils.Utils.handleBackgrounds;
import static com.dirtyunicorns.themes.utils.Utils.handleOverlays;
import static com.dirtyunicorns.themes.utils.Utils.isLiveWallpaper;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MenuItem;
import android.widget.TimePicker;

import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.internal.util.du.ThemesUtils;
import com.android.internal.util.du.Utils;

import com.dirtyunicorns.themes.db.ThemeDatabase;
import com.dirtyunicorns.themes.receivers.ThemesEndReceiver;
import com.dirtyunicorns.themes.receivers.ThemesStartReceiver;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Objects;

import static com.dirtyunicorns.themes.utils.Utils.isLiveWallpaper;

public class Themes extends PreferenceFragment implements ThemesListener {

    private static final String TAG = "Themes";

    private static final String PREF_BACKUP_THEMES = "backup_themes";
    private static final String PREF_RESTORE_THEMES = "restore_themes";
    private static final String PREF_WP_PREVIEW = "wp_preview";
    private static final String PREF_THEME_ACCENT_PICKER = "theme_accent_picker";
    public static final String PREF_THEME_ACCENT_COLOR = "theme_accent_color";
    public static final String PREF_ADAPTIVE_ICON_SHAPE = "adapative_icon_shape";
    public static final String PREF_FONT_PICKER = "font_picker";
    public static final String PREF_STATUSBAR_ICONS = "statusbar_icons";
    public static final String PREF_THEME_SWITCH = "theme_switch";
    public static final String PREF_THEME_SCHEDULE = "theme_schedule";
    public static final String PREF_THEME_SCHEDULED_START_THEME = "scheduled_start_theme";
    public static final String PREF_THEME_SCHEDULED_START_THEME_VALUE = "scheduled_start_theme_value";
    public static final String PREF_THEME_SCHEDULED_START_TIME = "theme_schedule_start_time";
    public static final String PREF_THEME_SCHEDULED_END_THEME = "scheduled_end_theme";
    public static final String PREF_THEME_SCHEDULED_END_THEME_VALUE = "scheduled_end_theme_value";
    public static final String PREF_THEME_SCHEDULED_END_TIME = "theme_schedule_end_time";
    public static final String PREF_THEME_SCHEDULED_REPEAT_DAILY = "theme_schedule_repeat_daily";

    private boolean scheduledStartTheme = false;
    private boolean scheduledEndTheme = false;
    private int mBackupLimit = 10;
    private static boolean mUseSharedPrefListener;

    private AlarmManager mAlarmMgr;
    private Calendar mStartDate, mEndDate;
    private Context mContext;
    private DateFormat timeFormat;
    private IOverlayManager mOverlayManager;
    private PendingIntent mStartPendingIntent, mEndPendingIntent;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private ThemeDatabase mThemeDatabase;
    private UiModeManager mUiModeManager;

    private DropDownPreference mThemeSchedule;
    private ListPreference mAdaptiveIconShape;
    private ListPreference mFontPicker;
    private ListPreference mStatusbarIcons;
    private ListPreference mThemeScheduledStartTheme;
    private ListPreference mThemeScheduledEndTheme;
    private ListPreference mThemeSwitch;
    private Preference mAccentPicker;
    private Preference mBackupThemes;
    private Preference mRestoreThemes;
    private Preference mWpPreview;
    private SwitchPreference mThemeScheduleRepeat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.themes);

        mContext = getActivity();

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mThemeDatabase = new ThemeDatabase(mContext);

        // Shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        sharedPreferencesEditor = mSharedPreferences.edit();

        // Alarm receiver
        mAlarmMgr = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
        mStartDate = Calendar.getInstance();
        mEndDate = Calendar.getInstance();
        Intent mStartIntent = new Intent(getActivity(), ThemesStartReceiver.class);
        mStartPendingIntent = PendingIntent.getBroadcast(getActivity(), 0, mStartIntent, 0);
        Intent mEndIntent = new Intent(getActivity(), ThemesEndReceiver.class);
        mEndPendingIntent = PendingIntent.getBroadcast(getActivity(), 0, mEndIntent, 0);

        // Time and Date format
        timeFormat = android.text.format.DateFormat.getTimeFormat(mContext);

        // Themes
        UiModeManager mUiModeManager = mContext.getSystemService(UiModeManager.class);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mWpPreview = findPreference(PREF_WP_PREVIEW);

        mThemeSchedule = (DropDownPreference) findPreference(PREF_THEME_SCHEDULE);
        mThemeScheduleRepeat = (SwitchPreference) findPreference(PREF_THEME_SCHEDULED_REPEAT_DAILY);

        if (mThemeScheduleRepeat != null) {
            if (mThemeScheduleRepeat.isChecked()) {
                mSharedPreferences.edit().putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, true).commit();
            } else {
                mSharedPreferences.edit().putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false).commit();
            }
        }

        mThemeScheduledStartTheme = (ListPreference) findPreference(PREF_THEME_SCHEDULED_START_THEME);
        if (mThemeScheduledStartTheme != null) {
            mThemeScheduledStartTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
            mThemeScheduledStartTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
        }
        mThemeScheduledEndTheme = (ListPreference) findPreference(PREF_THEME_SCHEDULED_END_THEME);
        if (mThemeScheduledEndTheme != null) {
            mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
            mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
        }

        mAccentPicker = findPreference(PREF_THEME_ACCENT_PICKER);
        assert mAccentPicker != null;
        mAccentPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager manager = getFragmentManager();
                Fragment frag = manager.findFragmentByTag(AccentPicker.TAG_ACCENT_PICKER);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }
                AccentPicker accentPickerFragment = new AccentPicker();
                accentPickerFragment.show(manager, AccentPicker.TAG_ACCENT_PICKER);
                return true;
            }
        });

        // Themes backup
        mBackupThemes = (Preference) findPreference(PREF_BACKUP_THEMES);
        assert mBackupThemes != null;
        mBackupThemes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (isLiveWallpaper(getActivity())) {
                    new AlertDialog.Builder(getActivity(), R.style.AccentDialogTheme)
                            .setTitle(getContext().getString(R.string.theme_backup_dialog_title))
                            .setMessage(getContext().getString(R.string.theme_backup_dialog_message))
                            .setCancelable(false)
                            .setPositiveButton(getContext().getString(R.string.theme_backup_dialog_positive),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FragmentManager manager = getFragmentManager();
                                            Fragment frag = manager.findFragmentByTag(BackupThemes.TAG_BACKUP_THEMES);
                                            if (frag != null) {
                                                manager.beginTransaction().remove(frag).commit();
                                            }
                                            BackupThemes backupThemesFragment = new BackupThemes(Themes.this);
                                            backupThemesFragment.show(manager, BackupThemes.TAG_BACKUP_THEMES);
                                        }
                                    })
                            .setNegativeButton(getContext().getString(R.string.theme_backup_dialog_negative),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();

                } else {
                    FragmentManager manager = getFragmentManager();
                    Fragment frag = manager.findFragmentByTag(BackupThemes.TAG_BACKUP_THEMES);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    BackupThemes backupThemesFragment = new BackupThemes(Themes.this);
                    backupThemesFragment.show(manager, BackupThemes.TAG_BACKUP_THEMES);
                }
                return true;
            }
        });

        // Themes restore
        mRestoreThemes = (Preference) findPreference(PREF_RESTORE_THEMES);
        assert mRestoreThemes != null;
        mRestoreThemes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, RestoreThemes.class);
                if (intent != null) {
                    setSharedPrefListener(true);
                    startActivity(intent);
                }
                return true;
            }
        });

        mThemeSwitch = (ListPreference) findPreference(PREF_THEME_SWITCH);
        if (Utils.isThemeEnabled("com.android.theme.solarizeddark.system")) {
            mThemeSwitch.setValue("4");
        } else if (Utils.isThemeEnabled("com.android.theme.pitchblack.system")) {
            mThemeSwitch.setValue("3");
        } else if (mUiModeManager != null) {
            if (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES) {
                mThemeSwitch.setValue("2");
            } else {
                mThemeSwitch.setValue("1");
            }
        }
        mThemeSwitch.setSummary(mThemeSwitch.getEntry());

        mFontPicker = (ListPreference) findPreference(PREF_FONT_PICKER);
        if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
            mFontPicker.setValue("2");
        } else if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
            mFontPicker.setValue("3");
        } else if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
            mFontPicker.setValue("4");
        } else if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
            mFontPicker.setValue("5");
        } else if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
            mFontPicker.setValue("6");
        } else if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
            mFontPicker.setValue("7");
        } else if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
            mFontPicker.setValue("8");
        } else if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
            mFontPicker.setValue("9");
        } else if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
            mFontPicker.setValue("10");
        } else if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
            mFontPicker.setValue("11");
        } else if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
            mFontPicker.setValue("12");
        } else if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
            mFontPicker.setValue("13");
        } else if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
            mFontPicker.setValue("14");
        } else if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
            mFontPicker.setValue("15");
        } else if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
            mFontPicker.setValue("16");
        } else if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
            mFontPicker.setValue("17");
        } else if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
            mFontPicker.setValue("18");
        } else if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
            mFontPicker.setValue("19");
        } else if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
            mFontPicker.setValue("20");
        } else {
            mFontPicker.setValue("1");
        }
        mFontPicker.setSummary(mFontPicker.getEntry());

        mAdaptiveIconShape = (ListPreference) findPreference(PREF_ADAPTIVE_ICON_SHAPE);
        if (Utils.isThemeEnabled("com.android.theme.icon.teardrop")) {
            mAdaptiveIconShape.setValue("2");
        } else if (Utils.isThemeEnabled("com.android.theme.icon.squircle")) {
            mAdaptiveIconShape.setValue("3");
        } else if (Utils.isThemeEnabled("com.android.theme.icon.roundedrect")) {
            mAdaptiveIconShape.setValue("4");
        } else if (Utils.isThemeEnabled("com.android.theme.icon.cylinder")) {
            mAdaptiveIconShape.setValue("5");
        } else if (Utils.isThemeEnabled("com.android.theme.icon.hexagon")) {
            mAdaptiveIconShape.setValue("6");
        } else {
            mAdaptiveIconShape.setValue("1");
        }
        mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());

        mStatusbarIcons = (ListPreference) findPreference(PREF_STATUSBAR_ICONS);
        if (Utils.isThemeEnabled("com.android.theme.icon_pack.filled.android")) {
            mStatusbarIcons.setValue("2");
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.rounded.android")) {
            mStatusbarIcons.setValue("3");
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.circular.android")) {
            mStatusbarIcons.setValue("4");
        } else {
            mStatusbarIcons.setValue("1");
        }
        mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());

        setWallpaperPreview();
        updateBackupPref();
        updateRestorePref();
    }

    private void setWallpaperPreview() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        mWpPreview.setIcon(wallpaperDrawable);
    }

    private void updateBackupPref() {
        mBackupThemes.setEnabled(getThemeCount() < mBackupLimit ? true : false);
        if (getThemeCount() == mBackupLimit) {
            mBackupThemes.setSummary(R.string.theme_backup_reach_limit_summary);
        } else {
            mBackupThemes.setSummary(R.string.theme_backup_summary);
        }
    }

    private void updateRestorePref() {
        mRestoreThemes.setEnabled(getThemeCount() > 0 ? true : false);
        if (getThemeCount() == 0) {
            mRestoreThemes.setSummary(R.string.theme_restore_no_backup_summary);
        } else {
            mRestoreThemes.setSummary(R.string.theme_restore_summary);
        }
    }

    private int getThemeCount() {
        int count = mThemeDatabase.getThemeDbUtilsCount();
        return count;
    }

    public OnSharedPreferenceChangeListener mSharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
            class FontPicker extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    String font_type = sharedPreferences.getString(PREF_FONT_PICKER, "1");
                    switch (font_type) {
                        case "1":
                            for (int i = 0; i < ThemesUtils.FONTS.length; i++) {
                                String fonts = ThemesUtils.FONTS[i];
                                try {
                                    mOverlayManager.setEnabled(fonts, false, USER_SYSTEM);
                                    } catch (RemoteException e) {
                                    e.printStackTrace();
                                    }
                                }
                            break;
                        case "2":
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                                }
                        handleOverlays("com.android.theme.font.notoserifsource", true, mOverlayManager);
                        break;
                        case "3":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.cagliostrosource", true, mOverlayManager);
                        break;
                        case "4":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }

                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.lgsmartgothicsource", true, mOverlayManager);
                        break;
                        case "5":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.rosemarysource", true);
                        break;
                        case "6":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.sonysketchsource", true, mOverlayManager);
                        break;
                        case "7":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.googlesans", true, mOverlayManager);
                        break;
                        case "8":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.slateforoneplus", true, mOverlayManager);
                        break;
                        case "9":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.samsungone", true, mOverlayManager);
                        break;
                        case "10":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.firasans", true, mOverlayManager);
                        break;
                        case "11":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.sfpro", true, mOverlayManager);
                        break;
                        case "12":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.circularstd", true, mOverlayManager);
                        break;
                        case "13":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.coolstorysource", true, mOverlayManager);
                        break;
                        case "14":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.rubikrubik", true, mOverlayManager);
                        break;
                        case "15":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.arvolato", true, mOverlayManager);
                        break;
                        case "16":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.surfersource", true);
                        break;
                        case "17":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.aclonicasource", true, mOverlayManager);
                        break;
                        case "18":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.amarantesource", true, mOverlayManager);
                        break;
                        case "19":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.bariolsource", true, mOverlayManager);
                        break;
                        case "20":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false, mOverlayManager);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false, mOverlayManager);
                            }
                        handleOverlays("com.android.theme.font.comicsanssource", true, mOverlayManager);
                        break;
                    }
                    mFontPicker.setSummary(mFontPicker.getEntry());
                }
            }

            if (key.equals(PREF_FONT_PICKER)) {
                new FontPicker().execute();
            }

            if (key.equals(PREF_ADAPTIVE_ICON_SHAPE)) {
                String adapative_icon_shape = sharedPreferences.getString(PREF_ADAPTIVE_ICON_SHAPE, "1");
                switch (adapative_icon_shape) {
                    case "1":
                        handleOverlays("com.android.theme.icon.teardrop", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", false, mOverlayManager);
                        break;
                    case "2":
                        handleOverlays("com.android.theme.icon.teardrop", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", false, mOverlayManager);
                        break;
                    case "3":
                        handleOverlays("com.android.theme.icon.teardrop", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", false, mOverlayManager);
                        break;
                    case "4":
                        handleOverlays("com.android.theme.icon.teardrop", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", false, mOverlayManager);;
                        break;
                    case "5":
                        handleOverlays("com.android.theme.icon.teardrop", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", false, mOverlayManager);
                        break;
                    case "6":
                        handleOverlays("com.android.theme.icon.teardrop", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.squircle", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.roundedrect", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.cylinder", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon.hexagon", true, mOverlayManager);
                        break;
                    }
                    mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());
                }

            if (key.equals(PREF_STATUSBAR_ICONS)) {
                String statusbar_icons = sharedPreferences.getString(PREF_STATUSBAR_ICONS, "1");
                switch (statusbar_icons) {
                    case "1":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false, mOverlayManager);
                        break;
                    case "2":
                        handleOverlays("com.android.theme.icon_pack.filled.android", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false, mOverlayManager);
                        break;
                    case "3":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", true, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false, mOverlayManager);
                        break;
                    case "4":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false, mOverlayManager);
                        handleOverlays("com.android.theme.icon_pack.circular.android", true, mOverlayManager);
                        break;
                }
                mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());
            }

            if (key.equals(PREF_THEME_SWITCH)) {
                String theme_switch = sharedPreferences.getString(PREF_THEME_SWITCH, "1");
                switch (theme_switch) {
                    case "1":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        break;
                    case "2":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        break;
                    case "3":
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        break;
                    case "4":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        break;
                }
                mThemeSwitch.setSummary(mThemeSwitch.getEntry());
            }

            if (key.equals(PREF_THEME_SCHEDULE)) {
                switch (getThemeSchedule(mSharedPreferences)) {
                    case "1":
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
                        sharedPreferencesEditor.commit();
                        mThemeScheduleRepeat.setVisible(false);
                        mThemeScheduledStartTheme.setVisible(false);
                        mThemeScheduledEndTheme.setVisible(false);
                        mThemeSchedule.setValue("1");
                        break;
                    case "2":
                        mThemeScheduleRepeat.setVisible(true);
                        mThemeScheduleRepeat.setEnabled(true);
                        mThemeScheduledStartTheme.setVisible(true);
                        mThemeScheduledStartTheme.setEnabled(true);
                        break;
                }
                mThemeSchedule.setSummary(mThemeSchedule.getEntry());
            }

            class ScheduledStartTheme extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                    showStartTimePicker();
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_START_THEME_VALUE,
                            getScheduledStartTheme(mSharedPreferences));
                    sharedPreferencesEditor.commit();
                }
            }

            if (key.equals(PREF_THEME_SCHEDULED_START_THEME) && getThemeSchedule(mSharedPreferences) != "1") {
                if (getScheduledStartTheme(mSharedPreferences) != null
                        && getScheduledStartThemeValue(mSharedPreferences) == null) {
                    mThemeScheduledStartTheme.setSummary(mThemeScheduledStartTheme.getEntry());
                    new ScheduledStartTheme().execute();
                    scheduledStartTheme = true;
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
                    sharedPreferencesEditor.commit();
                    mThemeScheduledStartTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledStartTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    mThemeScheduledStartTheme.setValue(null);
                    scheduledStartTheme = false;
                }
            }

            class ScheduledEndTheme extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                    showEndTimePicker();
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_END_THEME_VALUE,
                            getScheduledEndTheme(mSharedPreferences));
                    sharedPreferencesEditor.commit();
                }
            }

            if (key.equals(PREF_THEME_SCHEDULED_END_THEME) && getThemeSchedule(mSharedPreferences) != "1") {
                if (getScheduledEndTheme(mSharedPreferences) != null
                        && getScheduledEndThemeValue(mSharedPreferences) == null) {
                    mThemeScheduledEndTheme.setSummary(mThemeScheduledEndTheme.getEntry());
                    new ScheduledEndTheme().execute();
                    scheduledEndTheme = true;
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
                    sharedPreferencesEditor.commit();
                    mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    mThemeScheduledEndTheme.setValue(null);
                    scheduledEndTheme = false;
                }
            }
        }
    };

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    public static void setSharedPrefListener(boolean listener) {
        mUseSharedPrefListener = listener;
    }

    @Override
    public void onCloseBackupDialog(DialogFragment dialog) {
        updateBackupPref();
        updateRestorePref();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        setWallpaperPreview();
        updateBackupPref();
        updateRestorePref();
        updateAccentSummary();
        updateIconShapeSummary();
        updateStatusbarIconsSummary();
        updateThemeSchedule();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeSchedule();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeSchedule();
    }

    private void updateThemeSchedule() {
        if (getScheduledEndTheme(mSharedPreferences) == null && getScheduledStartTheme(mSharedPreferences) == null) {
            mThemeScheduleRepeat.setEnabled(false);
            mThemeScheduledEndTheme.setEnabled(false);
            scheduledEndTheme = false;
        }

        if (getScheduledStartTheme(mSharedPreferences) != null) {
            mThemeScheduleRepeat.setEnabled(false);
            mThemeScheduledEndTheme.setEnabled(true);
            scheduledEndTheme = false;
        }

        if (getScheduledEndTheme(mSharedPreferences) != null && getScheduledStartTheme(mSharedPreferences) != null) {
            mThemeScheduleRepeat.setEnabled(false);
            mThemeScheduledStartTheme.setEnabled(false);
            mThemeScheduledEndTheme.setEnabled(false);
            scheduledStartTheme = true;
            scheduledEndTheme = true;
        }

        if (getScheduledEndTheme(mSharedPreferences) == null && getScheduledStartTheme(mSharedPreferences) == null) {
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
            sharedPreferencesEditor.commit();
            mThemeScheduleRepeat.setVisible(false);
            mThemeScheduledStartTheme.setVisible(false);
            mThemeScheduledEndTheme.setVisible(false);
            mThemeSchedule.setValue("1");
            scheduledStartTheme = false;
            scheduledEndTheme = false;
        }
        if (mThemeScheduledStartTheme != null) {
            if (scheduledStartTheme) {
                mThemeScheduledStartTheme.setTitle(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                        + " " + getString(R.string.theme_schedule_start_scheduled));
                mThemeScheduledStartTheme.setSummary(getScheduledStartThemeTime(mSharedPreferences));
                mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                        + " " + getString(R.string.theme_schedule_start_scheduled));
                mThemeScheduledEndTheme.setSummary(getScheduledEndThemeTime(mSharedPreferences));
                scheduledStartTheme = false;
            } else {
                mThemeScheduledStartTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                mThemeScheduledStartTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));

                mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                scheduledStartTheme = true;
            }
        }
        if (mThemeScheduledEndTheme != null) {
            if (scheduledEndTheme) {
                mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                        + " " + getString(R.string.theme_schedule_start_scheduled));
                mThemeScheduledEndTheme.setSummary(getScheduledEndThemeTime(mSharedPreferences));
                mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                        + " " + getString(R.string.theme_schedule_start_scheduled));
                mThemeScheduledEndTheme.setSummary(getScheduledEndThemeTime(mSharedPreferences));
                scheduledEndTheme = false;
            } else {
                mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));

                mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                scheduledEndTheme = true;
            }
        }
        if (mThemeScheduleRepeat != null) {
            if (mThemeScheduleRepeat.isChecked()) {
                mSharedPreferences.edit().putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, true).commit();
            } else {
                mSharedPreferences.edit().putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false).commit();
            }
        }
        mThemeSchedule.setSummary(mThemeSchedule.getEntry());
    }

    private void updateAccentSummary() {
        if (mAccentPicker != null) {
            if (Utils.isThemeEnabled("com.android.theme.color.space")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_space));
            } else if (Utils.isThemeEnabled("com.android.theme.color.purple")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_purple));
            } else if (Utils.isThemeEnabled("com.android.theme.color.orchid")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_orchid));
            } else if (Utils.isThemeEnabled("com.android.theme.color.ocean")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_ocean));
            } else if (Utils.isThemeEnabled("com.android.theme.color.green")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_green));
            } else if (Utils.isThemeEnabled("com.android.theme.color.cinnamon")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_cinnamon));
            } else if (Utils.isThemeEnabled("com.android.theme.color.amber")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_amber));
            } else if (Utils.isThemeEnabled("com.android.theme.color.blue")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_blue));
            } else if (Utils.isThemeEnabled("com.android.theme.color.bluegrey")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_bluegrey));
            } else if (Utils.isThemeEnabled("com.android.theme.color.brown")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_brown));
            } else if (Utils.isThemeEnabled("com.android.theme.color.cyan")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_cyan));
            } else if (Utils.isThemeEnabled("com.android.theme.color.deeporange")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_deeporange));
            } else if (Utils.isThemeEnabled("com.android.theme.color.deeppurple")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_deeppurple));
            } else if (Utils.isThemeEnabled("com.android.theme.color.grey")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_grey));
            } else if (Utils.isThemeEnabled("com.android.theme.color.indigo")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_indigo));
            } else if (Utils.isThemeEnabled("com.android.theme.color.lightblue")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_lightblue));
            } else if (Utils.isThemeEnabled("com.android.theme.color.lightgreen")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_lightgreen));
            } else if (Utils.isThemeEnabled("com.android.theme.color.lime")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_lime));
            } else if (Utils.isThemeEnabled("com.android.theme.color.orange")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_orange));
            } else if (Utils.isThemeEnabled("com.android.theme.color.pink")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_pink));
            } else if (Utils.isThemeEnabled("com.android.theme.color.red")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_red));
            } else if (Utils.isThemeEnabled("com.android.theme.color.teal")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_teal));
            } else if (Utils.isThemeEnabled("com.android.theme.color.yellow")) {
                mAccentPicker.setSummary(mContext.getString(R.string.accent_colors_yellow));
            } else {
                mAccentPicker.setSummary(mContext.getString(R.string.theme_accent_picker_default));
            }
        }
    }

    private void updateIconShapeSummary() {
        if (Utils.isThemeEnabled("com.android.theme.icon.teardrop")) {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_teardrop));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.squircle")) {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_squircle));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.roundedrect")) {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_roundedrect));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.cylinder")) {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_cylinder));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.hexagon")) {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_roundedrect));
        } else {
            mAdaptiveIconShape.setSummary(mContext.getString(R.string.adaptive_icon_shape_default));
        }
    }

    private void updateStatusbarIconsSummary() {
        if (Utils.isThemeEnabled("com.android.theme.icon_pack.filled.android")) {
            mStatusbarIcons.setSummary(mContext.getString(R.string.statusbar_icons_filled));
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.rounded.android")) {
            mStatusbarIcons.setSummary(mContext.getString(R.string.statusbar_icons_rounded));
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.circular.android")) {
            mStatusbarIcons.setSummary(mContext.getString(R.string.statusbar_icons_circular));
        } else {
            mStatusbarIcons.setSummary(mContext.getString(R.string.statusbar_icons_default));
        }
    }

    public void showStartTimePicker() {
        new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mStartDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mStartDate.set(Calendar.MINUTE, minute);

                if (!PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false)) {
                    mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, mStartDate.getTimeInMillis(), mStartPendingIntent);
                } else {
                    mAlarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, mStartDate.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY, mStartPendingIntent);
                }

                if (mThemeScheduledStartTheme != null) {
                    mThemeScheduledStartTheme.setTitle(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                    mThemeScheduledStartTheme.setSummary(timeFormat.format(mStartDate.getTime()));
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_START_TIME, timeFormat.format(mStartDate.getTime())).commit();
                    mThemeScheduledEndTheme.setVisible(true);
                    mThemeScheduledEndTheme.setEnabled(true);
                    mThemeScheduleRepeat.setEnabled(false);
                    scheduledStartTheme = true;
                }
            }
        }, mStartDate.get(Calendar.HOUR_OF_DAY), mStartDate.get(Calendar.MINUTE), false).show();
    }

    public void showEndTimePicker() {
        new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mEndDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mEndDate.set(Calendar.MINUTE, minute);

                if (!PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false)) {
                    mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, mEndDate.getTimeInMillis(), mEndPendingIntent);
                } else {
                    mAlarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, mEndDate.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY, mEndPendingIntent);
                }

                if (mThemeScheduledEndTheme != null) {
                    mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                    mThemeScheduledEndTheme.setSummary(timeFormat.format(mEndDate.getTime()));
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_END_TIME, timeFormat.format(mEndDate.getTime())).commit();
                    mThemeScheduledStartTheme.setEnabled(false);
                    mThemeScheduledEndTheme.setEnabled(false);
                    mThemeScheduleRepeat.setEnabled(false);
                    scheduledEndTheme = true;
                }
            }
        }, mEndDate.get(Calendar.HOUR_OF_DAY), mEndDate.get(Calendar.MINUTE), false).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().finish();
            return true;
        }
        return false;
    }
}
