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

package com.dirtyunicorns.themes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.android.internal.util.du.Utils;
import com.dirtyunicorns.themes.db.ThemeDatabase;
import com.dirtyunicorns.themes.utils.ThemeDbUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BackupThemes extends DialogFragment {

    public static final String TAG_BACKUP_THEMES = "backup_themes";

    private boolean mThemeName = false;
    private int mRelativeLayout;

    private Resources mResources;
    private ThemesListener mThemesListener;
    private Drawable mWallpaperDrawable;
    private SharedPreferences mSharedPreferences;
    private ThemeDatabase mThemeDatabase;
    private String mTimeStamp;
    private UiModeManager mUiModeManager;

    public BackupThemes(ThemesListener themesListener) {
        mThemesListener = themesListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResources = getResources();
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());
        mWallpaperDrawable = wallpaperManager.getDrawable();
        mTimeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mThemeDatabase = new ThemeDatabase(getActivity());
        mUiModeManager = getActivity().getSystemService(UiModeManager.class);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity(), R.style.AccentDialogTheme);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.themes_backup, null);
        ViewStub stub = (ViewStub) view.findViewById(R.id.themes_backup_preview);
        ImageView imgView = (ImageView) view.findViewById(R.id.wp_background);
        final EditText themeNameInput = (EditText) view.findViewById(R.id.themeName);
        final String backupDate = getString(R.string.theme_backup_edittext_hint) + mTimeStamp;
        int maxLength = 20;
        themeNameInput.setHint(backupDate);
        themeNameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        themeNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                themeNameInput.setHint(hasFocus ? "" : backupDate);
            }
        });
        stub.setLayoutResource(getThemeBackupPreview());
        imgView.setImageDrawable(mWallpaperDrawable);
        stub.inflate();
        builder.setTitle(R.string.theme_backup_title);
        builder.setView(view);
        builder.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String themeName = themeNameInput.getText().toString().trim();
                if (themeName.isEmpty()) {
                    themeName = backupDate;
                }
                if (!isThemeNameExist(themeName)) {
                    mThemeDatabase.addThemeDbUtils(new ThemeDbUtils(themeName, isDarkMode(),
                        getIconsAccentColor(), getThemeNightColor(), getAccentPicker(),
                        getThemeSwitch(), getAdaptiveIconShape(), getFont(), getIconsShape(),
                        getSbIcons(), getThemeWp()));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.theme_name_exist_warning),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
            }
        });

        builder.create();

        return builder.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mThemesListener != null) {
           mThemesListener.onCloseBackupDialog(null);
        }
    }

    private int getThemeBackupPreview() {
        int value = Integer.parseInt(getSbIcons());
        switch (value) {
            case 1:
                mRelativeLayout = R.layout.themes_main;
                break;
            case 2:
                mRelativeLayout = R.layout.themes_main_filled;
                break;
            case 3:
                mRelativeLayout = R.layout.themes_main_rounded;
                break;
            case 4:
                mRelativeLayout = R.layout.themes_main_circular;
                break;
        }
        return mRelativeLayout;
    }

    private String isDarkMode() {
        return String.valueOf(mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES);
    }

    private String getIconsAccentColor() {
        String iconsAccentColor = "#" + Integer.toHexString(mResources.getColor(
            R.color.qs_tile_background_active));
        return iconsAccentColor;
    }

    private String getThemeNightColor() {
        String themeNightColor = "#" + Integer.toHexString(mResources.getColor(
            R.color.qs_tile_panel_background_theme_restore));
        return themeNightColor;
    }

    private String getIconsShape() {
        String iconsShape = getActivity().getString(
                    com.android.internal.R.string.config_icon_mask);
        return iconsShape;
    }

    private String getAccentPicker() {
        String accentPicker = AccentPicker.getAccentColor();
        return accentPicker;
    }

    private String getThemeSwitch() {
        String themeSwitch = mSharedPreferences.getString("theme_switch", "1");
        return themeSwitch;
    }

    private String getAdaptiveIconShape() {
        String adaptativeIconShape = mSharedPreferences.getString("adapative_icon_shape", "1");
        return adaptativeIconShape;
    }

    private String getFont() {
        String fontType = mSharedPreferences.getString("font_picker", "1");
        return fontType;
    }

    private String getSbIcons() {
        String statusBarIcons = mSharedPreferences.getString("statusbar_icons", "1");
        return statusBarIcons;
    }

    private File getWallpaperBitmap() throws IOException {
        File rootDir = new File(getContext().getFilesDir() + "WallpaperBackup");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        File themeWpBackup = new File(rootDir + File.separator + mTimeStamp);
        new Thread() {
            @Override
            public void run() {
                try {
                    Bitmap themeWpBitmap = ((BitmapDrawable) mWallpaperDrawable).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    themeWpBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    themeWpBackup.createNewFile();
                    FileOutputStream fos = new FileOutputStream(themeWpBackup);
                    fos.write(baos.toByteArray());
                    fos.flush();
                    fos.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
        return themeWpBackup;
    }

    private String getThemeWp() {
        String wpThemeName = null;
        try {
            wpThemeName = getWallpaperBitmap().toString();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return wpThemeName;
    }

    private boolean isThemeNameExist(String themeName) {
        List<ThemeDbUtils> themeDatabaseList = mThemeDatabase.getAllThemeDbUtils();
        for (ThemeDbUtils name : themeDatabaseList) {
            String str = name.getThemeName();
            if (str.equals(themeName)) {
                mThemeName = true;
            }
        }
        return mThemeName;
    }
}
