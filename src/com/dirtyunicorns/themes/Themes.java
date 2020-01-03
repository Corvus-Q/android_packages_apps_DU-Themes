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
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MenuItem;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.internal.util.du.Utils;

public class Themes extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PREF_ACCENT_PICKER = "accent_picker";
    private static final String PREF_ADAPTIVE_ICON_SHAPE = "adapative_icon_shape";
    private static final String PREF_DARK_SWITCH = "dark_switch";
    private static final String PREF_FONT_PICKER = "font_picker";
    private static final String PREF_STATUSBAR_ICONS = "statusbar_icons";

    private IOverlayManager mOverlayManager;
    private SharedPreferences mSharedPreferences;
    private UiModeManager mUiModeManager;

    private ListPreference mAdaptiveIconShape;
    private ListPreference mFontPicker;
    private ListPreference mStatusbarIcons;
    private Preference mAccentPicker;
    private SwitchPreference mDarkModeSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mAccentPicker = findPreference(PREF_ACCENT_PICKER);
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

        mDarkModeSwitch = (SwitchPreference) findPreference(PREF_DARK_SWITCH);
        assert mDarkModeSwitch != null;
        mDarkModeSwitch.setChecked(isChecked());
        mDarkModeSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getContext().getSystemService(UiModeManager.class)
                        .setNightMode(!isChecked() ?
                                UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
                return true;
            }
        });

        mFontPicker = (ListPreference) findPreference(PREF_FONT_PICKER);
        if (Utils.isThemeEnabled("com.android.theme.font.notoserifsource")) {
            mFontPicker.setValue("2");
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
    }

    private boolean isChecked() {
        return mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_FONT_PICKER)) {
            String font_type = sharedPreferences.getString(PREF_FONT_PICKER, "1");
            if (font_type.equals("1")) {
                handleOverlays("com.android.theme.font.notoserifsource", false);
            } else if (font_type.equals("2")) {
                handleOverlays("com.android.theme.font.notoserifsource", true);
            }
            mFontPicker.setSummary(mFontPicker.getEntry());
        }

        if (key.equals(PREF_ADAPTIVE_ICON_SHAPE)) {
            String adapative_icon_shape = sharedPreferences.getString(PREF_ADAPTIVE_ICON_SHAPE, "1");
            switch (adapative_icon_shape) {
                case "1":
                    handleOverlays("com.android.theme.icon.teardrop", false);
                    handleOverlays("com.android.theme.icon.squircle", false);
                    handleOverlays("com.android.theme.icon.roundedrect", false);
                    break;
                case "2":
                    handleOverlays("com.android.theme.icon.teardrop", true);
                    handleOverlays("com.android.theme.icon.squircle", false);
                    handleOverlays("com.android.theme.icon.roundedrect", false);
                    break;
                case "3":
                    handleOverlays("com.android.theme.icon.teardrop", false);
                    handleOverlays("com.android.theme.icon.squircle", true);
                    handleOverlays("com.android.theme.icon.roundedrect", false);
                    break;
                case "4":
                    handleOverlays("com.android.theme.icon.teardrop", false);
                    handleOverlays("com.android.theme.icon.squircle", false);
                    handleOverlays("com.android.theme.icon.roundedrect", true);
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
    }

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccentSummary();
        updateIconShapeSummary();
        updateStatusbarIconsSummary();
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
            goUpToTopLevelSetting(getActivity());
            return true;
        }
        return false;
    }

    private static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
    }
}
