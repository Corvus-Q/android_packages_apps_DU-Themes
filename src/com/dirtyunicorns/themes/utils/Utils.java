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

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.text.TextUtils;

public class Utils {

    public static boolean isLiveWallpaper(Context context) {
        WallpaperInfo info = WallpaperManager.getInstance(context).getWallpaperInfo();
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        return info != null && !TextUtils.isEmpty(info.getComponent().getPackageName())
                && wallpaperManager.isSetWallpaperAllowed();
    }
}
