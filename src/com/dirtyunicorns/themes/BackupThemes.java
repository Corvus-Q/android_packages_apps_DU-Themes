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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

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

    private boolean mThemeNameExist = false;
    private Drawable mWallpaperDrawable;
    private EditText mThemeNameInput;
    private int mRelativeLayout;
    private ProgressBar mBackupProgressBar;
    private Resources mResources;
    private SharedPreferences mSharedPreferences;
    private String mBackupDate;
    private String mThemeName;
    private String mTimeStamp;
    private String mWpThemeName = null;
    private ThemeDatabase mThemeDatabase;
    private ThemesListener mThemesListener;
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
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.themes_backup, null);
        ViewStub stub = (ViewStub) view.findViewById(R.id.themes_backup_preview);
        ImageView imgView = (ImageView) view.findViewById(R.id.wp_background);
        mThemeNameInput = (EditText) view.findViewById(R.id.themeName);
        mBackupProgressBar = (ProgressBar) view.findViewById(R.id.backupBar);
        mBackupDate = getString(R.string.theme_backup_edittext_hint) + mTimeStamp;
        stub.setLayoutResource(getThemeBackupPreview());
        imgView.setImageDrawable(mWallpaperDrawable);
        stub.inflate();
        int maxLength = 20;
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity(), R.style.AccentDialogTheme)
        .setTitle(R.string.theme_backup_title)
        .setPositiveButton(android.R.string.ok, null)
        .setView(view);

        mThemeNameInput.setHint(mBackupDate);
        mThemeNameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        mThemeNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                mThemeNameInput.setHint(hasFocus ? "" : mBackupDate);
            }
        });

        builder.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = (Button) dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mThemeName = mThemeNameInput.getText().toString().trim();
                    if (mThemeName.isEmpty()) {
                        mThemeName = mBackupDate;
                    }
                    if (!isThemeNameExist(mThemeName)) {
                        new AsyncTask<Integer, Integer, String>() {

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                mBackupProgressBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected String doInBackground(Integer... params) {
                                addThemeBackup();
                                for (int i = 0; i < params[0]; i++) {
                                    if (mWpThemeName == null) {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                return "Backup finished";
                            }

                            @Override
                            protected void onPostExecute(String result) {
                                super.onPostExecute(result);
                                mBackupProgressBar.setVisibility(View.INVISIBLE);
                                dialog.dismiss();
                            }

                        }.execute(20);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.theme_name_exist_warning),
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mThemesListener != null) {
            mThemesListener.onCloseBackupDialog(null);
        }
    }

    private void addThemeBackup() {
        mThemeDatabase.addThemeDbUtils(new ThemeDbUtils(mThemeName, isDarkMode(),
            getIconsAccentColor(), getThemeNightColor(), getAccentPicker(),
            getThemeSwitch(), getAdaptiveIconShape(), getFont(), getIconsShape(),
            getSbIcons(), getThemeWp()));
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
        String accentPicker = mSharedPreferences.getString("theme_accent_color", "default");
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
        return themeWpBackup;
    }

    private String getThemeWp() {
        try {
            mWpThemeName = getWallpaperBitmap().toString();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return mWpThemeName;
    }

    private boolean isThemeNameExist(String themeName) {
        List<ThemeDbUtils> themeDatabaseList = mThemeDatabase.getAllThemeDbUtils();
        for (ThemeDbUtils name : themeDatabaseList) {
            String str = name.getThemeName();
            if (str.equals(themeName)) {
                mThemeNameExist = true;
            }
        }
        return mThemeNameExist;
    }
}
