/*
 * Copyright (C) 2019 The Dirty Unicorns Project
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

import static android.os.UserHandle.USER_SYSTEM;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
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
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.internal.util.du.ThemesUtils;
import com.android.internal.util.du.Utils;

import com.dirtyunicorns.themes.db.ThemeDatabase;

import java.util.Objects;

import static com.dirtyunicorns.themes.utils.Utils.isLiveWallpaper;

public class Themes extends PreferenceFragment implements ThemesListener {

    private static final String PREF_BACKUP_THEMES = "backup_themes";
    private static final String PREF_RESTORE_THEMES = "restore_themes";
    private static final String PREF_WP_PREVIEW = "wp_preview";
    private static final String PREF_THEME_ACCENT_PICKER = "theme_accent_picker";
    public static final String PREF_THEME_ACCENT_COLOR = "theme_accent_color";
    public static final String PREF_ADAPTIVE_ICON_SHAPE = "adapative_icon_shape";
    public static final String PREF_FONT_PICKER = "font_picker";
    public static final String PREF_STATUSBAR_ICONS = "statusbar_icons";
    public static final String PREF_THEME_SWITCH = "theme_switch";

    private static boolean mUseSharedPrefListener;
    private int mBackupLimit = 10;

    private Activity mActivity;
    private IOverlayManager mOverlayManager;
    private SharedPreferences mSharedPreferences;
    private ThemeDatabase mThemeDatabase;
    private UiModeManager mUiModeManager;

    private ListPreference mAdaptiveIconShape;
    private ListPreference mFontPicker;
    private ListPreference mStatusbarIcons;
    private ListPreference mThemeSwitch;
    private Preference mAccentPicker;
    private Preference mBackupThemes;
    private Preference mRestoreThemes;
    private Preference mWpPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        mActivity = getActivity();

        ActionBar actionBar = mActivity.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        mThemeDatabase = new ThemeDatabase(mActivity);

        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mWpPreview = findPreference(PREF_WP_PREVIEW);

        mAccentPicker = findPreference(PREF_THEME_ACCENT_PICKER);
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
                Intent intent = new Intent(mActivity, RestoreThemes.class);
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
        } else if (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES) {
            mThemeSwitch.setValue("2");
        } else {
            mThemeSwitch.setValue("1");
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
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            class PrepareData extends AsyncTask<Void, Void, Void> {

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
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                                }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                                }
                        handleOverlays("com.android.theme.font.notoserifsource", true);
                        break;
                        case "3":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.cagliostrosource", true);
                        break;
                        case "4":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }

                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.lgsmartgothicsource", true);
                        break;
                        case "5":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.rosemarysource", true);
                        break;
                        case "6":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.sonysketchsource", true);
                        break;
                        case "7":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.googlesans", true);
                        break;
                        case "8":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.slateforoneplus", true);
                        break;
                        case "9":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.samsungone", true);
                        break;
                        case "10":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.firasans", true);
                        break;
                        case "11":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.sfpro", true);
                        break;
                        case "12":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.circularstd", true);
                        break;
                        case "13":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.coolstorysource", true);
                        break;
                        case "14":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.rubikrubik", true);
                        break;
                        case "15":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.arvolato", true);
                        break;
                        case "16":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.surfersource", true);
                        break;
                        case "17":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.aclonicasource", true);
                        break;
                        case "18":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.amarantesource", true);
                        break;
                        case "19":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.comicsanssource")) {
                                handleOverlays("com.android.theme.font.comicsanssource", false);
                            }
                        handleOverlays("com.android.theme.font.bariolsource", true);
                        break;
                        case "20":
                            if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
                                handleOverlays("com.android.theme.font.notoserifsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.cagliostrosource")) {
                                handleOverlays("com.android.theme.font.cagliostrosource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.lgsmartgothicsource")) {
                                handleOverlays("com.android.theme.font.lgsmartgothicsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rosemarysource")) {
                                handleOverlays("com.android.theme.font.rosemarysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sonysketchsource")) {
                                handleOverlays("com.android.theme.font.sonysketchsource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.googlesans")) {
                                handleOverlays("com.android.theme.font.googlesans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.slateforoneplus")) {
                                handleOverlays("com.android.theme.font.slateforoneplus", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.samsungone")) {
                                handleOverlays("com.android.theme.font.samsungone", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.firasans")) {
                                handleOverlays("com.android.theme.font.firasans", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.sfpro")) {
                                handleOverlays("com.android.theme.font.sfpro", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.circularstd")) {
                                handleOverlays("com.android.theme.font.circularstd", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.coolstorysource")) {
                                handleOverlays("com.android.theme.font.coolstorysource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.rubikrubik")) {
                                handleOverlays("com.android.theme.font.rubikrubik", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.arvolato")) {
                                handleOverlays("com.android.theme.font.arvolato", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.surfersource")) {
                                handleOverlays("com.android.theme.font.surfersource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.aclonicasource")) {
                                handleOverlays("com.android.theme.font.aclonicasource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.amarantesource")) {
                                handleOverlays("com.android.theme.font.amarantesource", false);
                            }
                            if (Utils.isThemeEnabled("com.android.theme.font.bariolsource")) {
                                handleOverlays("com.android.theme.font.bariolsource", false);
                            }
                        handleOverlays("com.android.theme.font.comicsanssource", true);
                        break;

                    }
                    mFontPicker.setSummary(mFontPicker.getEntry());
                }
            }

            if (key.equals(PREF_FONT_PICKER)) {
                new PrepareData().execute();
            }

            if (key.equals(PREF_ADAPTIVE_ICON_SHAPE)) {
                String adapative_icon_shape = sharedPreferences.getString(PREF_ADAPTIVE_ICON_SHAPE, "1");
                switch (adapative_icon_shape) {
                    case "1":
                        handleOverlays("com.android.theme.icon.teardrop", false);
                        handleOverlays("com.android.theme.icon.squircle", false);
                        handleOverlays("com.android.theme.icon.roundedrect", false);
                        handleOverlays("com.android.theme.icon.cylinder", false);
                        handleOverlays("com.android.theme.icon.hexagon", false);
                    case "2":
                        handleOverlays("com.android.theme.icon.teardrop", true);
                        handleOverlays("com.android.theme.icon.squircle", false);
                        handleOverlays("com.android.theme.icon.roundedrect", false);
                        handleOverlays("com.android.theme.icon.cylinder", false);
                        handleOverlays("com.android.theme.icon.hexagon", false);
                    break;
                    case "3":
                        handleOverlays("com.android.theme.icon.teardrop", false);
                        handleOverlays("com.android.theme.icon.squircle", true);
                        handleOverlays("com.android.theme.icon.roundedrect", false);
                        handleOverlays("com.android.theme.icon.cylinder", false);
                        handleOverlays("com.android.theme.icon.hexagon", false);
                    break;
                    case "4":
                        handleOverlays("com.android.theme.icon.teardrop", false);
                        handleOverlays("com.android.theme.icon.squircle", false);
                        handleOverlays("com.android.theme.icon.roundedrect", true);
                        handleOverlays("com.android.theme.icon.cylinder", false);
                        handleOverlays("com.android.theme.icon.hexagon", false);;
                    break;
                    case "5":
                        handleOverlays("com.android.theme.icon.teardrop", false);
                        handleOverlays("com.android.theme.icon.squircle", false);
                        handleOverlays("com.android.theme.icon.roundedrect", false);
                        handleOverlays("com.android.theme.icon.cylinder", true);
                        handleOverlays("com.android.theme.icon.hexagon", false);
                    break;
                    case "6":
                        handleOverlays("com.android.theme.icon.teardrop", false);
                        handleOverlays("com.android.theme.icon.squircle", false);
                        handleOverlays("com.android.theme.icon.roundedrect", false);
                        handleOverlays("com.android.theme.icon.cylinder", false);
                        handleOverlays("com.android.theme.icon.hexagon", true);
                    break;
                }
                mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());
            }

            if (key.equals(PREF_STATUSBAR_ICONS)) {
                String statusbar_icons = sharedPreferences.getString(PREF_STATUSBAR_ICONS, "1");
                switch (statusbar_icons) {
                    case "1":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false);
                        break;
                    case "2":
                        handleOverlays("com.android.theme.icon_pack.filled.android", true);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false);
                        break;
                    case "3":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", true);
                        handleOverlays("com.android.theme.icon_pack.circular.android", false);
                        break;
                    case "4":
                        handleOverlays("com.android.theme.icon_pack.filled.android", false);
                        handleOverlays("com.android.theme.icon_pack.rounded.android", false);
                        handleOverlays("com.android.theme.icon_pack.circular.android", true);
                        break;
                }
                mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());
            }

            if (key.equals(PREF_THEME_SWITCH)) {
                String theme_switch = sharedPreferences.getString(PREF_THEME_SWITCH, "1");
                switch (theme_switch) {
                    case "1":
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_NO, ThemesUtils.PITCH_BLACK);
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_NO, ThemesUtils.SOLARIZED_DARK);
                        break;
                    case "2":
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.PITCH_BLACK);
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.SOLARIZED_DARK);
                        break;
                    case "3":
                        handleBackgrounds(true, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.PITCH_BLACK);
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.SOLARIZED_DARK);
                        break;
                    case "4":
                        handleBackgrounds(false, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.PITCH_BLACK);
                        handleBackgrounds(true, mActivity, UiModeManager.MODE_NIGHT_YES, ThemesUtils.SOLARIZED_DARK);
                        break;
                }
                mThemeSwitch.setSummary(mThemeSwitch.getEntry());
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
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
    }

    private void updateAccentSummary() {
        if (Utils.isThemeEnabled("com.android.theme.color.space")) {
            mAccentPicker.setSummary("Space");
        } else if (Utils.isThemeEnabled("com.android.theme.color.purple")) {
            mAccentPicker.setSummary("Purple");
        } else if (Utils.isThemeEnabled("com.android.theme.color.orchid")) {
            mAccentPicker.setSummary("Orchid");
        } else if (Utils.isThemeEnabled("com.android.theme.color.ocean")) {
            mAccentPicker.setSummary("Ocean");
        } else if (Utils.isThemeEnabled("com.android.theme.color.green")) {
            mAccentPicker.setSummary("Green");
        } else if (Utils.isThemeEnabled("com.android.theme.color.cinnamon")) {
            mAccentPicker.setSummary("Cinnamon");
        } else if (Utils.isThemeEnabled("com.android.theme.color.amber")) {
            mAccentPicker.setSummary("Amber");
        } else if (Utils.isThemeEnabled("com.android.theme.color.blue")) {
            mAccentPicker.setSummary("Blue");
        } else if (Utils.isThemeEnabled("com.android.theme.color.bluegrey")) {
            mAccentPicker.setSummary("Blue Grey");
        } else if (Utils.isThemeEnabled("com.android.theme.color.brown")) {
            mAccentPicker.setSummary("Brown");
        } else if (Utils.isThemeEnabled("com.android.theme.color.cyan")) {
            mAccentPicker.setSummary("Cyan");
        } else if (Utils.isThemeEnabled("com.android.theme.color.deeporange")) {
            mAccentPicker.setSummary("Deep Orange");
        } else if (Utils.isThemeEnabled("com.android.theme.color.deeppurple")) {
            mAccentPicker.setSummary("Deep Purple");
        } else if (Utils.isThemeEnabled("com.android.theme.color.grey")) {
            mAccentPicker.setSummary("Grey");
        } else if (Utils.isThemeEnabled("com.android.theme.color.indigo")) {
            mAccentPicker.setSummary("Indigo");
        } else if (Utils.isThemeEnabled("com.android.theme.color.lightblue")) {
            mAccentPicker.setSummary("Light Blue");
        } else if (Utils.isThemeEnabled("com.android.theme.color.lightgreen")) {
            mAccentPicker.setSummary("Light Green");
        } else if (Utils.isThemeEnabled("com.android.theme.color.lime")) {
            mAccentPicker.setSummary("Lime");
        } else if (Utils.isThemeEnabled("com.android.theme.color.orange")) {
            mAccentPicker.setSummary("Orange");
        } else if (Utils.isThemeEnabled("com.android.theme.color.pink")) {
            mAccentPicker.setSummary("Pink");
        } else if (Utils.isThemeEnabled("com.android.theme.color.red")) {
            mAccentPicker.setSummary("Red");
        } else if (Utils.isThemeEnabled("com.android.theme.color.teal")) {
            mAccentPicker.setSummary("Teal");
        } else if (Utils.isThemeEnabled("com.android.theme.color.yellow")) {
            mAccentPicker.setSummary("Yellow");
        } else {
            mAccentPicker.setSummary(getString(R.string.theme_accent_picker_default));
        }
    }

    private void updateIconShapeSummary() {
        if (Utils.isThemeEnabled("com.android.theme.icon.teardrop")) {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_teardrop));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.squircle")) {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_squircle));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.roundedrect")) {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_roundedrect));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.cylinder")) {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_cylinder));
        } else if (Utils.isThemeEnabled("com.android.theme.icon.hexagon")) {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_hexagon));
        } else {
            mAdaptiveIconShape.setSummary(getString(R.string.adaptive_icon_shape_default));
        }
    }

    private void updateStatusbarIconsSummary() {
        if (Utils.isThemeEnabled("com.android.theme.icon_pack.filled.android")) {
            mStatusbarIcons.setSummary(getString(R.string.statusbar_icons_filled));
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.rounded.android")) {
            mStatusbarIcons.setSummary(getString(R.string.statusbar_icons_rounded));
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.circular.android")) {
            mStatusbarIcons.setSummary(getString(R.string.statusbar_icons_circular));
        } else {
            mStatusbarIcons.setSummary(getString(R.string.statusbar_icons_default));
        }
    }

    private void handleBackgrounds(Boolean state, Context context, int mode, String[] overlays) {
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

    private void handleOverlays(String packagename, Boolean state) {
        try {
            mOverlayManager.setEnabled(packagename,
                    state, USER_SYSTEM);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mActivity.finish();
            return true;
        }
        return false;
    }
}
