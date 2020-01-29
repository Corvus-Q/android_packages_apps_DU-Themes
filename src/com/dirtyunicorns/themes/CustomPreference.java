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

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.android.internal.util.du.Utils;

public class CustomPreference extends Preference {

    public CustomPreference(Context context) {
        super(context);
        setResources();
    }

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setResources();
    }

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setResources();
    }

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setResources();
    }

    private void setResources() {
        if (Utils.isThemeEnabled("com.android.theme.icon_pack.filled.android")) {
            setLayoutResource(R.layout.themes_main_filled);
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.rounded.android")) {
            setLayoutResource(R.layout.themes_main_rounded);
        } else if (Utils.isThemeEnabled("com.android.theme.icon_pack.circular.android")) {
            setLayoutResource(R.layout.themes_main_circular);
        } else {
            setLayoutResource(R.layout.themes_main);
        }
    }
}
