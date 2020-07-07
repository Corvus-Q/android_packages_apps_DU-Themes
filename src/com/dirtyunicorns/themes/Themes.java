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

import static android.os.UserHandle.USER_SYSTEM;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeSummary;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeTime;
import static com.dirtyunicorns.themes.utils.Utils.getThemeSchedule;
import static com.dirtyunicorns.themes.utils.Utils.handleBackgrounds;
import static com.dirtyunicorns.themes.utils.Utils.handleOverlays;
import static com.dirtyunicorns.themes.utils.Utils.isLiveWallpaper;
import static com.dirtyunicorns.themes.utils.Utils.threeButtonNavbarEnabled;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.du.ThemesUtils;
import com.android.internal.util.du.Utils;
import com.dirtyunicorns.support.colorpicker.ColorPickerPreference;

import java.util.Calendar;
import java.util.Objects;

import static com.dirtyunicorns.themes.utils.Utils.isLiveWallpaper;

public class Themes extends PreferenceFragment {

    private static final String TAG = "Themes";

    private static final String PREF_WP_PREVIEW = "wp_preview";
    private static final String PREF_THEME_SCHEDULE = "theme_schedule";
    private static final String PREF_THEME_NAVBAR_PICKER = "theme_navbar_picker";
    private static final String PREF_QS_HEADER_STYLE = "qs_header_style";
    private static final String PREF_SWITCH_STYLE = "switch_style";
    private static final String PREF_TILE_STYLE = "qs_tile_style";

    public static final String PREF_THEME_NAVBAR_STYLE = "theme_navbar_style";
    public static final String PREF_ADAPTIVE_ICON_SHAPE = "adapative_icon_shape";
    public static final String PREF_FONT_PICKER = "font_picker";
    public static final String PREF_STATUSBAR_ICONS = "statusbar_icons";
    public static final String PREF_THEME_SWITCH = "theme_switch";

    private static final String ACCENT_COLOR = "accent_color";
    static final int DEFAULT_ACCENT_COLOR = 0xff1a73e8;

    private static boolean mUseSharedPrefListener;
    private String[] mNavbarName;

    private Context mContext;
    private IOverlayManager mOverlayManager;
    private SharedPreferences mSharedPreferences;
    private UiModeManager mUiModeManager;

    private ListPreference mAdaptiveIconShape;
    private ListPreference mFontPicker;
    private ListPreference mStatusbarIcons;
    private ListPreference mThemeSwitch;
    private ListPreference mQsHeaderStyle;
    private ListPreference mSwitchStyle;
    private ListPreference mQsTileStyle;
    private Preference mNavbarPicker;
    private Preference mThemeSchedule;
    private Preference mWpPreview;
    private ColorPickerPreference mAccentColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.themes);

        mContext = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setHasOptionsMenu(true);

        // Shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);

        // Theme services
        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        // Navbar summary
        mNavbarName = getResources().getStringArray(R.array.navbar_name);

        // Wallpaper preview
        mWpPreview = (Preference) findPreference(PREF_WP_PREVIEW);

        // Theme schedule
        mThemeSchedule = (Preference) findPreference(PREF_THEME_SCHEDULE);
        assert mThemeSchedule != null;
        mThemeSchedule.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), Schedule.class);
                startActivity(intent);
                return true;
            }
        });

        // Navbar picker
        mNavbarPicker = (Preference) findPreference(PREF_THEME_NAVBAR_PICKER);
        if (threeButtonNavbarEnabled(mContext)) {
            assert mNavbarPicker != null;
            mNavbarPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager manager = getFragmentManager();
                    Fragment frag = manager.findFragmentByTag(NavbarPicker.TAG_NAVBAR_PICKER);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    NavbarPicker navbarPickerFragment = new NavbarPicker();
                    navbarPickerFragment.show(manager, NavbarPicker.TAG_NAVBAR_PICKER);
                    return true;
                }
            });
        } else {
            prefSet.removePreference(mNavbarPicker);
        }

        // Navbar
        String navbarName = getOverlayName(ThemesUtils.NAVBAR_STYLES);
        if (navbarName != null) {
            mSharedPreferences.edit().putString("theme_navbar_style", navbarName).apply();
        }

        // Themes
        mThemeSwitch = (ListPreference) findPreference(PREF_THEME_SWITCH);
        if (Utils.isThemeEnabled("com.android.theme.corvusclear.system")) {
            mThemeSwitch.setValue("9");
        } else if (Utils.isThemeEnabled("com.android.theme.materialocean.system")) {
            mThemeSwitch.setValue("8");
        } else if (Utils.isThemeEnabled("com.android.theme.darkgrey.system")) {
            mThemeSwitch.setValue("7");
        } else if (Utils.isThemeEnabled("com.android.theme.bakedgreen.system")) {
            mThemeSwitch.setValue("6");
        } else if (Utils.isThemeEnabled("com.android.theme.chocox.system")) {
            mThemeSwitch.setValue("5");
        } else if (Utils.isThemeEnabled("com.android.theme.solarizeddark.system")) {
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

        // Font picker
        mFontPicker = (ListPreference) findPreference(PREF_FONT_PICKER);
        int fontPickerValue = getOverlayPosition(ThemesUtils.FONTS);
        if (fontPickerValue != -1) {
            mFontPicker.setValue(String.valueOf(fontPickerValue + 2));
        } else {
            mFontPicker.setValue("1");
        }
        mFontPicker.setSummary(mFontPicker.getEntry());

        // Adaptive icon shape
        mAdaptiveIconShape = (ListPreference) findPreference(PREF_ADAPTIVE_ICON_SHAPE);
        int iconShapeValue = getOverlayPosition(ThemesUtils.ADAPTIVE_ICON_SHAPE);
        if (iconShapeValue != -1) {
            mAdaptiveIconShape.setValue(String.valueOf(iconShapeValue + 2));
        } else {
            mAdaptiveIconShape.setValue("1");
        }
        mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());

        mQsTileStyle = (ListPreference) findPreference(PREF_TILE_STYLE);
        int qsTileStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_TILE_STYLE, 0);
        int qsTileStyleValue = getOverlayPosition(ThemesUtils.QS_TILE_THEMES);
        if (qsTileStyleValue != 0) {
            mQsTileStyle.setValue(String.valueOf(qsTileStyle));
        }
        mQsTileStyle.setSummary(mQsTileStyle.getEntry());
        mQsTileStyle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference == mQsTileStyle) {
                    String value = (String) newValue;
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.QS_TILE_STYLE, Integer.valueOf(value));
                    int valueIndex = mQsTileStyle.findIndexOfValue(value);
                    mQsTileStyle.setSummary(mQsTileStyle.getEntries()[valueIndex]);
                    String overlayName = getOverlayName(ThemesUtils.QS_TILE_THEMES);
                    if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                    }
                    if (valueIndex > 0) {
                        handleOverlays(ThemesUtils.QS_TILE_THEMES[valueIndex],
                                true, mOverlayManager);
                    }
                    return true;
                }
                return false;
            }
       });

        // Statusbar icons
        mStatusbarIcons = (ListPreference) findPreference(PREF_STATUSBAR_ICONS);
        int sbIconsValue = getOverlayPosition(ThemesUtils.STATUSBAR_ICONS);
        if (sbIconsValue != -1) {
            mStatusbarIcons.setValue(String.valueOf(sbIconsValue + 2));
        } else {
            mStatusbarIcons.setValue("1");
        }
        mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());

        // Statusbar icons
        mQsHeaderStyle = (ListPreference) findPreference(PREF_QS_HEADER_STYLE);
        int qsStyleValue = getOverlayPosition(ThemesUtils.QS_HEADER_THEMES);
        if (qsStyleValue != -1) {
            mQsHeaderStyle.setValue(String.valueOf(qsStyleValue + 2));
        } else {
            mQsHeaderStyle.setValue("1");
        }
        mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntry());

        // Statusbar icons
        mSwitchStyle = (ListPreference) findPreference(PREF_SWITCH_STYLE);
        int switchStyleValue = getOverlayPosition(ThemesUtils.SWITCH_STYLE);
        if (switchStyleValue != -1) {
            mSwitchStyle.setValue(String.valueOf(switchStyleValue + 2));
        } else {
            mSwitchStyle.setValue("1");
        }
        mSwitchStyle.setSummary(mSwitchStyle.getEntry());

        setWallpaperPreview();
        updateNavbarSummary();
        updateThemeScheduleSummary();

        mAccentColor = (ColorPickerPreference) findPreference(ACCENT_COLOR);
        int intColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, DEFAULT_ACCENT_COLOR, UserHandle.USER_CURRENT);
        String hexColor = String.format("#%08x", (0xff1a73e8 & intColor));
        mAccentColor.setNewPreviewColor(intColor);
        mAccentColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference == mAccentColor) {
                    String hex = ColorPickerPreference.convertToARGB(
                            Integer.valueOf(String.valueOf(newValue)));
                    int intHex = ColorPickerPreference.convertToColorInt(hex);
                    Settings.System.putIntForUser(getContext().getContentResolver(),
                    Settings.System.ACCENT_COLOR, intHex, UserHandle.USER_CURRENT);
                    return true;
                }
                return false;
            }
       });
    }

    private void setWallpaperPreview() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        mWpPreview.setIcon(wallpaperDrawable);
    }

    private int getOverlayPosition(String[] overlays) {
        int position = -1;
        for (int i = 0; i < overlays.length; i++) {
            String overlay = overlays[i];
            if (Utils.isThemeEnabled(overlay)) {
                position = i;
            }
        }
        return position;
    }

    private String getOverlayName(String[] overlays) {
        String overlayName = null;
        for (int i = 0; i < overlays.length; i++) {
            String overlay = overlays[i];
            if (Utils.isThemeEnabled(overlay)) {
                overlayName = overlay;
            }
        }
        return overlayName;
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
                    String fontType = sharedPreferences.getString(PREF_FONT_PICKER, "1");
                    String overlayName = getOverlayName(ThemesUtils.FONTS);
                    int fontTypeValue = Integer.parseInt(fontType);
                    if (overlayName != null) {
                        handleOverlays(overlayName, false, mOverlayManager);
                    }
                    if (fontTypeValue > 1) {
                        handleOverlays(ThemesUtils.FONTS[fontTypeValue - 2],
                                true, mOverlayManager);
                    }
                    mFontPicker.setSummary(mFontPicker.getEntry());
                }
            }

            if (key.equals(PREF_THEME_NAVBAR_STYLE)) {
                String navbarStyle = sharedPreferences.getString(PREF_THEME_NAVBAR_STYLE, "default");
                String overlayName = getOverlayName(ThemesUtils.NAVBAR_STYLES);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (navbarStyle != "default") {
                    handleOverlays(navbarStyle, true, mOverlayManager);
                }
                updateNavbarSummary();
            }

            if (key.equals(PREF_FONT_PICKER)) {
                new FontPicker().execute();
            }

            if (key.equals(PREF_ADAPTIVE_ICON_SHAPE)) {
                String adapativeIconShape = sharedPreferences.getString(PREF_ADAPTIVE_ICON_SHAPE, "1");
                String overlayName = getOverlayName(ThemesUtils.ADAPTIVE_ICON_SHAPE);
                int adapativeIconShapeValue = Integer.parseInt(adapativeIconShape);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (adapativeIconShapeValue > 1) {
                    handleOverlays(ThemesUtils.ADAPTIVE_ICON_SHAPE[adapativeIconShapeValue - 2],
                            true, mOverlayManager);
                }
                mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());
            }

            if (key.equals(PREF_STATUSBAR_ICONS)) {
                String statusbarIcons = sharedPreferences.getString(PREF_STATUSBAR_ICONS, "1");
                String overlayName = getOverlayName(ThemesUtils.STATUSBAR_ICONS);
                int statusbarIconsValue = Integer.parseInt(statusbarIcons);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (statusbarIconsValue > 1) {
                    handleOverlays(ThemesUtils.STATUSBAR_ICONS[statusbarIconsValue - 2],
                            true, mOverlayManager);
                }
                mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());
            }

            if (key.equals(PREF_QS_HEADER_STYLE)) {
                String qsHeaderStyle = sharedPreferences.getString(PREF_QS_HEADER_STYLE, "1");
                String overlayName = getOverlayName(ThemesUtils.QS_HEADER_THEMES);
                int qsHeaderStyleValue = Integer.parseInt(qsHeaderStyle);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (qsHeaderStyleValue > 1) {
                    handleOverlays(ThemesUtils.QS_HEADER_THEMES[qsHeaderStyleValue - 2],
                            true, mOverlayManager);
                }
                mQsHeaderStyle.setSummary(mQsHeaderStyle.getEntry());
            }

            if (key.equals(PREF_SWITCH_STYLE)) {
                String switchStyle = sharedPreferences.getString(PREF_SWITCH_STYLE, "1");
                String overlayName = getOverlayName(ThemesUtils.SWITCH_STYLE);
                int switchStyleValue = Integer.parseInt(switchStyle);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (switchStyleValue > 1) {
                    handleOverlays(ThemesUtils.SWITCH_STYLE[switchStyleValue - 2],
                            true, mOverlayManager);
                }
                mSwitchStyle.setSummary(mSwitchStyle.getEntry());
            }

            if (key.equals(PREF_THEME_SWITCH)) {
                String themeSwitch = sharedPreferences.getString(PREF_THEME_SWITCH, "1");
                switch (themeSwitch) {
                    case "1":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "2":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "3":
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "4":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "5":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "6":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "7":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "8":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
                        break;
                    case "9":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CORVUS_CLEAR, mOverlayManager);
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
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        setWallpaperPreview();
        updateThemeScheduleSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeScheduleSummary();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeScheduleSummary();
    }

    private void updateThemeScheduleSummary() {
        if (mThemeSchedule != null) {
            if (getThemeSchedule(mSharedPreferences).equals("1")) {
                mThemeSchedule.setSummary(mContext.getString(R.string.theme_schedule_summary));
            } else {
                if (!Calendar.getInstance().before(getScheduledStartThemeTime(mSharedPreferences))) {
                    mThemeSchedule.setSummary(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_dyn_summary));
                }
            }
        }
    }

    private void updateNavbarSummary() {
        if (mNavbarPicker != null) {
            int value = getOverlayPosition(ThemesUtils.NAVBAR_STYLES);
            if (value != -1) {
                mNavbarPicker.setSummary(mNavbarName[value]);
            } else {
                mNavbarPicker.setSummary(R.string.theme_accent_picker_default);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.themes_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.themes_reset:
                resetThemes();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void resetThemes() {
        new AlertDialog.Builder(getActivity(), R.style.AccentDialogTheme)
                .setTitle(mContext.getString(R.string.theme_reset_dialog_title))
                .setMessage(mContext.getString(R.string.theme_reset_dialog_message))
                .setCancelable(false)
                .setPositiveButton(getContext().getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new resetThemes().execute();
                            }
                        })
                .setNegativeButton(getContext().getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    class resetThemes extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... param) {
            mSharedPreferences.edit()
            // NavBar
            .remove(PREF_THEME_NAVBAR_STYLE)
            // Fonts
            .remove(PREF_FONT_PICKER)
            // Adapative icons
            .remove(PREF_ADAPTIVE_ICON_SHAPE)
            // Statusbar icons
            .remove(PREF_STATUSBAR_ICONS)
            // Themes
            .remove(PREF_THEME_SWITCH)
            // Header style
            .remove(PREF_QS_HEADER_STYLE)
            // Switch style
            .remove(PREF_SWITCH_STYLE)
            .apply();

            return null;
        }

        protected void onPostExecute(Void param) {
            Toast.makeText(mContext, mContext.getString(R.string.theme_reset_toast), Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}
