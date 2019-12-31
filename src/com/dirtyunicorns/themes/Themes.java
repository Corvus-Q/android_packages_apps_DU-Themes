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
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;

public class Themes extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        Preference accentpicker = findPreference("accent_picker");
        accentpicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }
}
