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

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ServiceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.dirtyunicorns.themes.db.ThemeDatabase;
import com.dirtyunicorns.themes.utils.ThemeDbUtils;
import com.dirtyunicorns.themes.utils.ThemesListItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dirtyunicorns.themes.utils.Utils.enableAccentColor;
import static com.dirtyunicorns.themes.utils.Utils.setDefaultAccentColor;

public class RestoreThemes extends Activity implements CompoundButton.OnCheckedChangeListener {

    public static final String TAG_RESTORE_THEMES = "restore_themes";

    private ArrayList<String> mSwitchList;
    private int mNumSwitches = 6;
    private int mSwitchId;
    private IOverlayManager mOverlayManager;
    private LinearLayoutManager mLayoutManager;
    private List<ThemesListItem> mThemesList;
    private RecyclerView mThemesRecyclerView;
    private RelativeLayout mThemePopup;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPrefEditor;
    private ThemesAdapter mThemesAdapter;
    private ThemeDatabase mThemeDatabase;
    private WallpaperManager mWallpaperManager;

    private Button mDeleteTheme;
    private Button mApplyTheme;
    private Switch[] mSwitchArray;
    private Switch mThemeSwitch;
    private Switch mAccentSwitch;
    private Switch mFontSwitch;
    private Switch mIconShapeSwitch;
    private Switch mSbIconSwitch;
    private Switch mWpSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.themes_restore);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mThemePopup = findViewById(R.id.theme_popup);
        mThemeSwitch = (Switch) findViewById(R.id.themeSwitch);
        mThemeSwitch.setOnCheckedChangeListener(this);
        mAccentSwitch = (Switch) findViewById(R.id.accentSwitch);
        mAccentSwitch.setOnCheckedChangeListener(this);
        mFontSwitch = (Switch) findViewById(R.id.fontSwitch);
        mFontSwitch.setOnCheckedChangeListener(this);
        mIconShapeSwitch = (Switch) findViewById(R.id.iconShapeSwitch);
        mIconShapeSwitch.setOnCheckedChangeListener(this);
        mSbIconSwitch = (Switch) findViewById(R.id.sbIconSwitch);
        mSbIconSwitch.setOnCheckedChangeListener(this);
        mWpSwitch = (Switch) findViewById(R.id.wpSwitch);
        mWpSwitch.setOnCheckedChangeListener(this);

        mOverlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(this.OVERLAY_SERVICE));
        mSwitchArray = new Switch[mNumSwitches];
        mSwitchList = new ArrayList<String>();
        for (int i = 0; i < mNumSwitches; i++) {
            mSwitchList.add("switch" + String.valueOf(i + 1));
        }
        mThemesList = new ArrayList<>();
        mThemesAdapter = new ThemesAdapter(this, mThemesList);
        mThemeDatabase = new ThemeDatabase(this);
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefEditor = mSharedPreferences.edit();
        mWallpaperManager = WallpaperManager.getInstance(this);

        mThemesRecyclerView = (RecyclerView) findViewById(R.id.themeRecyclerView);
        mThemesRecyclerView.setHasFixedSize(true);
        mThemesRecyclerView.setLayoutManager(mLayoutManager);
        mThemesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mThemesRecyclerView.setNestedScrollingEnabled(true);
        mThemesRecyclerView.setAdapter(mThemesAdapter);

        mThemesRecyclerView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {

                if (mThemesRecyclerView.canScrollHorizontally(1) &&
                        mThemesRecyclerView.computeHorizontalScrollRange() >= mThemesRecyclerView.getWidth()) {
                    mThemePopup.setVisibility(mSharedPreferences.getBoolean(
                            "ThemeReminder", true) ? View.VISIBLE : View.GONE);
                    mThemePopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSharedPrefEditor.putBoolean("ThemeReminder", false).apply();
                            mThemePopup.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mThemePopup.setVisibility(View.GONE);
                }
            }
        });

        mDeleteTheme = (Button) findViewById(R.id.deleteTheme);
        mDeleteTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mThemeDatabase.deleteThemeDbUtils(mThemesList.get(getCurrentItem()).getThemeName());
                getWallpaperBackupFile().delete();
                if (hasPreview() || hasNext()) {
                    setThemesData();
                } else {
                    finish();
                }
            }
        });

        mApplyTheme = (Button) findViewById(R.id.applyTheme);
        mApplyTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyThemeBackup();
            }
        });

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mThemesRecyclerView);

        setSwitchesChecked();
        setThemesData();
    }

    @Override
    public void onCheckedChanged(CompoundButton switches, boolean isChecked) {
        switch (switches.getId()) {
            case R.id.themeSwitch:
                mSwitchId = 0;
                mSwitchArray[0] = mThemeSwitch;
                break;
            case R.id.accentSwitch:
                mSwitchId = 1;
                mSwitchArray[1] = mAccentSwitch;
                break;
            case R.id.fontSwitch:
                mSwitchId = 2;
                mSwitchArray[2] = mFontSwitch;
                break;
            case R.id.iconShapeSwitch:
                mSwitchId = 3;
                mSwitchArray[3] = mIconShapeSwitch;
                break;
            case R.id.sbIconSwitch:
                mSwitchId = 4;
                mSwitchArray[4] = mSbIconSwitch;
                break;
            case R.id.wpSwitch:
                mSwitchId = 5;
                mSwitchArray[5] = mWpSwitch;
                break;
        }
        mSharedPrefEditor.putBoolean("switch" + String.valueOf(mSwitchId + 1),
            mSwitchArray[mSwitchId].isChecked());
        mSharedPrefEditor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        Themes.setSharedPrefListener(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Themes.setSharedPrefListener(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        Themes.setSharedPrefListener(false);
    }

    private void setSwitchesChecked() {
        if (isPersistentSwitches()) {
            mThemeSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(0), mThemeSwitch.isChecked()));
            mAccentSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(1), mAccentSwitch.isChecked()));
            mFontSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(2), mFontSwitch.isChecked()));
            mIconShapeSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(3), mIconShapeSwitch.isChecked()));
            mSbIconSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(4), mSbIconSwitch.isChecked()));
            mWpSwitch.setChecked(mSharedPreferences.getBoolean(
                mSwitchList.get(5), mWpSwitch.isChecked()));
        } else {
            for (int i = 0; i < mNumSwitches; i++) {
                mSharedPrefEditor.remove(mSwitchList.get(i));
            }
            mSharedPrefEditor.apply();
            mThemeSwitch.setChecked(mThemeSwitch.isChecked());
            mAccentSwitch.setChecked(mAccentSwitch.isChecked());
            mFontSwitch.setChecked(mFontSwitch.isChecked());
            mIconShapeSwitch.setChecked(mIconShapeSwitch.isChecked());
            mSbIconSwitch.setChecked(mSbIconSwitch.isChecked());
            mWpSwitch.setChecked(mWpSwitch.isChecked());
        }
    }

    private boolean isPersistentSwitches() {
        return mSharedPreferences.getBoolean("persistentSwitches", true);
    }

    private int getCurrentItem(){
        return ((LinearLayoutManager) mThemesRecyclerView.getLayoutManager())
                    .findFirstVisibleItemPosition();
    }

    private int getThemesCount() {
        return mThemesRecyclerView.getAdapter().getItemCount();
    }

    private boolean hasPreview() {
       return getCurrentItem() > 0;
    }

    private boolean hasNext() {
        return mThemesRecyclerView.getAdapter() != null &&
                getCurrentItem() < (getThemesCount() - 1);
    }

    private void preview() {
        if (getCurrentItem() > 0) {
            setCurrentItem(getCurrentItem() - 1, true);
        }
    }

    private void next() {
        RecyclerView.Adapter adapter = mThemesRecyclerView.getAdapter();
        if (adapter == null) return;
        if (getCurrentItem() < (adapter.getItemCount() - 1)) {
            setCurrentItem(getCurrentItem() + 1, true);
        }
    }

    private void setCurrentItem(int position, boolean smooth){
        if (smooth) {
            mThemesRecyclerView.smoothScrollToPosition(position);
        } else {
            mThemesRecyclerView.scrollToPosition(position);
        }
    }

    private void applyThemeBackup() {
        applyThemeSwitch();
        applyThemeFont();
        applyThemeIconShape();
        applyThemeSbIcons();
        applyThemeAccent();
        applyThemeWp();
    }

    private void applyThemeSwitch() {
        if (mThemeSwitch.isChecked()) {
            String newValue = mThemesList.get(getCurrentItem()).getThemeSwitch();
            mSharedPrefEditor.putString("theme_switch", newValue);
            mSharedPrefEditor.apply();
        }
    }

    private void applyThemeFont() {
        if (mFontSwitch.isChecked()) {
            String newValue = mThemesList.get(getCurrentItem()).getThemeFont();
            mSharedPrefEditor.putString("font_picker", newValue);
            mSharedPrefEditor.apply();
        }
    }

    private void applyThemeIconShape() {
        if (mIconShapeSwitch.isChecked()) {
            String newValue = mThemesList.get(getCurrentItem()).getAdaptiveIconShape();
            mSharedPrefEditor.putString("adapative_icon_shape", newValue);
            mSharedPrefEditor.apply();
        }
    }

    private void applyThemeSbIcons() {
        if (mSbIconSwitch.isChecked()) {
            String newValue = mThemesList.get(getCurrentItem()).getThemeSbIcons();
            mSharedPrefEditor.putString("statusbar_icons", newValue);
            mSharedPrefEditor.apply();
        }
    }

    private void applyThemeAccent() {
        if (mAccentSwitch.isChecked()) {
            String newValue = mThemesList.get(getCurrentItem()).getAccentPicker();
            if (newValue == "default") {
                setDefaultAccentColor(mOverlayManager);
            } else {
                enableAccentColor(mOverlayManager, newValue);
            }
        }
    }

    private void applyThemeWp() {
        if (mWpSwitch.isChecked()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Bitmap themeWpBitmap = BitmapFactory.decodeFile(
                                getWallpaperBackupFile().getPath());
                        mWallpaperManager.setBitmap(themeWpBitmap);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
    }

    private File getWallpaperBackupFile() {
        String themeWpBackup = mThemesList.get(getCurrentItem()).getThemeWp();
        File wpFile = new File(themeWpBackup);
        return wpFile;
    }

    private void setThemesData() {
        mThemesList.clear();
        List<ThemeDbUtils> themesDatabaseList = mThemeDatabase.getAllThemeDbUtils();
        for (ThemeDbUtils themes : themesDatabaseList) {
            String themeName = themes.getThemeName();
            String themeDayOrNight = themes.geThemeDayOrNight();
            String themeAccent = themes.getThemeAccent();
            String themeNightColor = themes.getThemeNightColor();
            String accentPicker = themes.getAccentPicker();
            String themeSwitch = themes.getThemeSwitch();
            String adaptativeIconShape = themes.getAdaptiveIconShape();
            String themeFont = themes.getThemeFont();
            String themeIconShape = themes.getThemeIconShape();
            String themeSbIcons = themes.getThemeSbIcons();
            String themeWp = themes.getThemeWp();
            mThemesList.add(new ThemesListItem(themeName, themeDayOrNight,
                    themeAccent, themeNightColor, accentPicker, themeSwitch,
                    adaptativeIconShape, themeFont, themeIconShape, themeSbIcons,
                    themeWp));
        }
        mThemesAdapter.notifyDataSetChanged();
        assert mThemesList != null;
        Collections.reverse(mThemesList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.restore_themes_menu, menu);
        MenuItem switchesItem = menu.findItem(R.id.persistent_switch);
        switchesItem.setChecked(isPersistentSwitches());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.persistent_switch:
                item.setChecked(!item.isChecked());
                mSharedPrefEditor.putBoolean("persistentSwitches", item.isChecked());
                mSharedPrefEditor.apply();
                setSwitchesChecked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
