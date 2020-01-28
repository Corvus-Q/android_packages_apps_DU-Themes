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

import android.app.Activity;
import android.app.UiModeManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.internal.util.du.ThemesUtils;

import java.util.Objects;

public class Utils {

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
