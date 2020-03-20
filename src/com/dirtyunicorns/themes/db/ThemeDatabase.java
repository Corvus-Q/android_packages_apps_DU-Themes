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

package com.dirtyunicorns.themes.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dirtyunicorns.themes.utils.ThemeDbUtils;

import java.util.ArrayList;
import java.util.List;

public class ThemeDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "themeDb";
    private static final String THEME_TABLE = "themeTableDb";
    private static final String KEY_ID = "id";
    private static final String KEY_THEME_NAME = "themeName";
    private static final String KEY_THEME_DAY_NIGHT = "themeDayNight";
    private static final String KEY_THEME_ACCENT = "themeAccent";
    private static final String KEY_THEME_NIGHT_COLOR = "themeNightColor";
    private static final String KEY_ACCENT_PICKER = "accentPicker";
    private static final String KEY_THEME_SWITCH = "themeSwitch";
    private static final String KEY_ADAPTATIVE_ICON_SHAPE = "adaptativeIconShape";
    private static final String KEY_THEME_FONT = "themeFont";
    private static final String KEY_THEME_ICON_SHAPE = "themeIconShape";
    private static final String KEY_THEME_SB_ICONS = "themeSbIcons";
    private static final String KEY_THEME_WP = "themeWp";
    private static final String KEY_THEME_NAVBAR_STYLE = "themeNavbarStyle";

    public ThemeDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_THEME_TABLE = "CREATE TABLE " + THEME_TABLE + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_THEME_NAME + " TEXT,"
                + KEY_THEME_DAY_NIGHT + " TEXT," + KEY_THEME_ACCENT + " TEXT,"
                + KEY_THEME_NIGHT_COLOR + " TEXT," + KEY_ACCENT_PICKER + " TEXT,"
                + KEY_THEME_SWITCH + " TEXT," + KEY_ADAPTATIVE_ICON_SHAPE + " TEXT,"
                + KEY_THEME_FONT + " TEXT," + KEY_THEME_ICON_SHAPE + " TEXT,"
                + KEY_THEME_SB_ICONS + " TEXT," + KEY_THEME_WP + " TEXT,"
                + KEY_THEME_NAVBAR_STYLE + " TEXT" + ")";
        db.execSQL(CREATE_THEME_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + THEME_TABLE);
        onCreate(db);
    }

    public void addThemeDbUtils(ThemeDbUtils themeDbUtils) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_THEME_NAME, themeDbUtils.getThemeName());
        values.put(KEY_THEME_DAY_NIGHT, themeDbUtils.getThemeDayOrNight());
        values.put(KEY_THEME_ACCENT, themeDbUtils.getThemeAccent());
        values.put(KEY_THEME_NIGHT_COLOR, themeDbUtils.getThemeNightColor());
        values.put(KEY_ACCENT_PICKER, themeDbUtils.getAccentPicker());
        values.put(KEY_THEME_SWITCH, themeDbUtils.getThemeSwitch());
        values.put(KEY_ADAPTATIVE_ICON_SHAPE, themeDbUtils.getAdaptiveIconShape());
        values.put(KEY_THEME_FONT, themeDbUtils.getThemeFont());
        values.put(KEY_THEME_ICON_SHAPE, themeDbUtils.getThemeIconShape());
        values.put(KEY_THEME_SB_ICONS, themeDbUtils.getThemeSbIcons());
        values.put(KEY_THEME_WP, themeDbUtils.getThemeWp());
        values.put(KEY_THEME_NAVBAR_STYLE, themeDbUtils.getThemeNavbarStyle());

        db.insert(THEME_TABLE, null, values);
        db.close();
    }

    public ThemeDbUtils getThemeDbUtils(String str) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(THEME_TABLE, new String[] {KEY_ID, KEY_THEME_NAME,
                        KEY_THEME_DAY_NIGHT, KEY_THEME_ACCENT, KEY_THEME_NIGHT_COLOR,
                        KEY_ACCENT_PICKER, KEY_THEME_SWITCH, KEY_ADAPTATIVE_ICON_SHAPE,
                        KEY_THEME_FONT, KEY_THEME_ICON_SHAPE, KEY_THEME_SB_ICONS,
                        KEY_THEME_WP, KEY_THEME_NAVBAR_STYLE}, KEY_THEME_NAME + " = ?",
                        new String[] {str}, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        ThemeDbUtils themeDbUtils = new ThemeDbUtils(
                Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4),
                cursor.getString(5), cursor.getString(6),
                cursor.getString(7), cursor.getString(8),
                cursor.getString(9), cursor.getString(10),
                cursor.getString(11), cursor.getString(12));
        cursor.close();

        return themeDbUtils;
    }

    public List<ThemeDbUtils> getAllThemeDbUtils() {
        List<ThemeDbUtils> themeDbUtilsList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + THEME_TABLE;

        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ThemeDbUtils themeDbUtils = new ThemeDbUtils();
                themeDbUtils.setID(Integer.parseInt(cursor.getString(0)));
                themeDbUtils.setThemeName(cursor.getString(1));
                themeDbUtils.setThemeDayOrNight(cursor.getString(2));
                themeDbUtils.setThemeAccent(cursor.getString(3));
                themeDbUtils.setThemeNightColor(cursor.getString(4));
                themeDbUtils.setAccentPicker(cursor.getString(5));
                themeDbUtils.setThemeSwitch(cursor.getString(6));
                themeDbUtils.setAdaptiveIconShape(cursor.getString(7));
                themeDbUtils.setThemeFont(cursor.getString(8));
                themeDbUtils.setThemeIconShape(cursor.getString(9));
                themeDbUtils.setThemeSbIcons(cursor.getString(10));
                themeDbUtils.setThemeWp(cursor.getString(11));
                themeDbUtils.setThemeNavbarStyle(cursor.getString(12));
                themeDbUtilsList.add(themeDbUtils);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return themeDbUtilsList;
    }

    public void updateThemeDbUtils(ThemeDbUtils themeDbUtils, String str) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_THEME_NAME, themeDbUtils.getThemeName());
        values.put(KEY_THEME_DAY_NIGHT, themeDbUtils.getThemeDayOrNight());
        values.put(KEY_THEME_ACCENT, themeDbUtils.getThemeAccent());
        values.put(KEY_THEME_NIGHT_COLOR, themeDbUtils.getThemeNightColor());
        values.put(KEY_ACCENT_PICKER, themeDbUtils.getAccentPicker());
        values.put(KEY_THEME_SWITCH, themeDbUtils.getThemeSwitch());
        values.put(KEY_ADAPTATIVE_ICON_SHAPE, themeDbUtils.getAdaptiveIconShape());
        values.put(KEY_THEME_FONT, themeDbUtils.getThemeFont());
        values.put(KEY_THEME_ICON_SHAPE, themeDbUtils.getThemeIconShape());
        values.put(KEY_THEME_SB_ICONS, themeDbUtils.getThemeSbIcons());
        values.put(KEY_THEME_WP, themeDbUtils.getThemeWp());
        values.put(KEY_THEME_NAVBAR_STYLE, themeDbUtils.getThemeNavbarStyle());

        db.update(THEME_TABLE, values, KEY_THEME_NAME + " = ?",
                new String[] {str});
        db.close();
    }

    public void deleteThemeDbUtils(String str) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(THEME_TABLE, KEY_THEME_NAME + " = ?",
                new String[] {str});
        db.close();
    }

    public int getThemeDbUtilsCount() {
        String countQuery = "SELECT  * FROM " + THEME_TABLE;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }
}
