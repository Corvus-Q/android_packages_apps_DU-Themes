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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.UiModeManager;
import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.util.du.Utils;

public class Themes extends PreferenceFragment {

    private static final String PREF_DARK_SWITCH = "dark_switch";

    SwitchPreference mDarkModeSwitch;
    Preference mAccentPicker;
    UiModeManager mUiModeManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        mUiModeManager = getContext().getSystemService(UiModeManager.class);

        mAccentPicker = findPreference("accent_picker");
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
        mDarkModeSwitch.setChecked(isChecked());
        mDarkModeSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!isChecked()) {
                    enableDarkTheme();
                } else {
                    enableLightTheme();
                }
                return true;
            }
        });
    }

    public boolean isChecked() {
        return mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }

    private void enableDarkTheme() {
        final Context context = getContext();
        if (context != null) {
            context.getSystemService(UiModeManager.class)
                    .setNightMode(UiModeManager.MODE_NIGHT_YES);
        }
    }

    private void enableLightTheme() {
        final Context context = getContext();
        if (context != null) {
            context.getSystemService(UiModeManager.class)
                    .setNightMode(UiModeManager.MODE_NIGHT_NO);
        }
    }

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccentSummary();
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
}
