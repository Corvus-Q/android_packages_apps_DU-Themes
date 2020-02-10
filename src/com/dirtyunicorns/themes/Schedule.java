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
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.MenuItem;
import android.widget.TimePicker;

import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.dirtyunicorns.themes.receivers.ThemesEndReceiver;
import com.dirtyunicorns.themes.receivers.ThemesStartReceiver;

import java.text.DateFormat;
import java.util.Calendar;

import static com.dirtyunicorns.themes.utils.Utils.clearAlarms;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndTheme;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeSummary;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeTime;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledEndThemeValue;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartTheme;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeSummary;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeTime;
import static com.dirtyunicorns.themes.utils.Utils.getScheduledStartThemeValue;
import static com.dirtyunicorns.themes.utils.Utils.getThemeSchedule;
import static com.dirtyunicorns.themes.utils.Utils.setEndAlarm;
import static com.dirtyunicorns.themes.utils.Utils.setEndTime;
import static com.dirtyunicorns.themes.utils.Utils.setStartAlarm;
import static com.dirtyunicorns.themes.utils.Utils.setStartTime;

public class Schedule extends Activity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new ScheduleFragment()).commit();
    }

    public static class ScheduleFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        public static final String PREF_THEME_SCHEDULE = "theme_schedule";
        public static final String PREF_THEME_SCHEDULED_START_THEME = "scheduled_start_theme";
        public static final String PREF_THEME_SCHEDULED_START_THEME_VALUE = "scheduled_start_theme_value";
        public static final String PREF_THEME_SCHEDULED_START_TIME = "theme_schedule_start_time";
        public static final String PREF_THEME_SCHEDULED_END_THEME = "scheduled_end_theme";
        public static final String PREF_THEME_SCHEDULED_END_THEME_VALUE = "scheduled_end_theme_value";
        public static final String PREF_THEME_SCHEDULED_END_TIME = "theme_schedule_end_time";
        public static final String PREF_THEME_SCHEDULED_REPEAT_DAILY = "theme_schedule_repeat_daily";
        public static final String PREF_ALARM_START_TIME = "theme_scheduled_start_time";
        public static final String PREF_ALARM_END_TIME = "theme_scheduled_end_time";
        public static final String PREF_THEME_SCHEDULED_TOAST = "theme_schedule_toast";

        private Calendar mStartDate, mEndDate;
        private Context mContext;
        private DateFormat timeFormat;
        private PackageManager mPm;
        private SharedPreferences mSharedPreferences;
        private SharedPreferences.Editor sharedPreferencesEditor;

        private DropDownPreference mThemeSchedule;
        private ListPreference mThemeScheduledStartTheme;
        private ListPreference mThemeScheduledEndTheme;
        private SwitchPreference mThemeScheduleRepeat;
        private SwitchPreference mThemeScheduleToast;

        private boolean scheduledStartTheme = false;
        private boolean scheduledEndTheme = false;
        private int scheduledThemeStatus;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.schedule);

            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(R.string.theme_schedule_title);
            }

            mContext = getActivity();

            // Alarm receiver
            mEndDate = Calendar.getInstance();
            mStartDate = Calendar.getInstance();
            mPm = mContext.getPackageManager();

            // Time format
            timeFormat = android.text.format.DateFormat.getTimeFormat(mContext);

            // Shared preferences
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
            sharedPreferencesEditor = mSharedPreferences.edit();

            // Theme schedule
            mThemeSchedule = (DropDownPreference) findPreference(PREF_THEME_SCHEDULE);
            mThemeScheduledStartTheme = (ListPreference) findPreference(PREF_THEME_SCHEDULED_START_THEME);
            mThemeScheduledEndTheme = (ListPreference) findPreference(PREF_THEME_SCHEDULED_END_THEME);
            mThemeScheduleRepeat = (SwitchPreference) findPreference(PREF_THEME_SCHEDULED_REPEAT_DAILY);
            mThemeScheduleToast = (SwitchPreference) findPreference(PREF_THEME_SCHEDULED_TOAST);

            updateThemeSchedule();
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
            if (key.equals(PREF_THEME_SCHEDULE)) {
                switch (getThemeSchedule(mSharedPreferences)) {
                    case "1":
                        clearAlarms(mContext);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
                        sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_TOAST);
                        sharedPreferencesEditor.remove(PREF_ALARM_START_TIME);
                        sharedPreferencesEditor.remove(PREF_ALARM_END_TIME);
                        sharedPreferencesEditor.commit();
                        mThemeScheduleRepeat.setVisible(false);
                        mThemeScheduleToast.setVisible(false);
                        mThemeScheduledStartTheme.setVisible(false);
                        mThemeScheduledEndTheme.setVisible(false);
                        mThemeSchedule.setValue("1");
                        break;
                    case "2":
                        mThemeScheduleRepeat.setVisible(true);
                        mThemeScheduleRepeat.setEnabled(true);
                        mThemeScheduleToast.setVisible(true);
                        mThemeScheduleToast.setEnabled(true);
                        mThemeScheduledStartTheme.setVisible(true);
                        mThemeScheduledStartTheme.setEnabled(true);
                        break;
                }
                mThemeSchedule.setSummary(mThemeSchedule.getEntry());
            }

            class ScheduledStartTheme extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                    showStartTimePicker();
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_START_THEME_VALUE,
                            getScheduledStartTheme(mSharedPreferences));
                    sharedPreferencesEditor.commit();
                }
            }

            if (key.equals(PREF_THEME_SCHEDULED_START_THEME) && getThemeSchedule(mSharedPreferences) != "1") {
                if (getScheduledStartTheme(mSharedPreferences) != null
                        && getScheduledStartThemeValue(mSharedPreferences) == null) {
                    mThemeScheduledStartTheme.setSummary(mThemeScheduledStartTheme.getEntry());
                    new ScheduledStartTheme().execute();
                    scheduledStartTheme = true;
                    mThemeScheduledStartTheme.setEnabled(false);
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
                    sharedPreferencesEditor.remove(PREF_ALARM_START_TIME);
                    sharedPreferencesEditor.commit();
                    mThemeScheduledStartTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledStartTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    mThemeScheduledStartTheme.setValue(null);
                    scheduledStartTheme = false;
                }
            }

            class ScheduledEndTheme extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                    showEndTimePicker();
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_END_THEME_VALUE,
                            getScheduledEndTheme(mSharedPreferences));
                    sharedPreferencesEditor.commit();
                }
            }

            if (key.equals(PREF_THEME_SCHEDULED_END_THEME) && getThemeSchedule(mSharedPreferences) != "1") {
                if (getScheduledEndTheme(mSharedPreferences) != null
                        && getScheduledEndThemeValue(mSharedPreferences) == null) {
                    mThemeScheduledEndTheme.setSummary(mThemeScheduledEndTheme.getEntry());
                    new ScheduledEndTheme().execute();
                    scheduledEndTheme = true;
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
                    sharedPreferencesEditor.remove(PREF_ALARM_END_TIME);
                    sharedPreferencesEditor.commit();
                    mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    mThemeScheduledEndTheme.setValue(null);
                    scheduledEndTheme = false;
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }

        @Override
        public void onResume() {
            super.onResume();
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
            updateThemeSchedule();
        }

        @Override
        public void onPause() {
            super.onPause();
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            updateThemeSchedule();
        }

        @Override
        public void onStop() {
            super.onStop();
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            updateThemeSchedule();
        }

        private void updateThemeSchedule() {
            if (getScheduledStartTheme(mSharedPreferences) != null) {
                mThemeScheduleRepeat.setEnabled(false);
                mThemeScheduledStartTheme.setEnabled(false);
                mThemeScheduledEndTheme.setEnabled(true);
                scheduledEndTheme = false;
            }

            if (getScheduledStartTheme(mSharedPreferences) != null && getScheduledEndTheme(mSharedPreferences) != null) {
                mThemeScheduleRepeat.setEnabled(false);
                mThemeScheduledStartTheme.setEnabled(false);
                mThemeScheduledEndTheme.setEnabled(false);
                scheduledStartTheme = true;
                scheduledEndTheme = true;
            }

            if (getScheduledStartTheme(mSharedPreferences) == null && getScheduledEndTheme(mSharedPreferences) == null) {
                clearAlarms(mContext);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
                sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_TOAST);
                sharedPreferencesEditor.remove(PREF_ALARM_START_TIME);
                sharedPreferencesEditor.remove(PREF_ALARM_END_TIME);
                sharedPreferencesEditor.commit();
                mThemeScheduleRepeat.setVisible(false);
                mThemeScheduleToast.setVisible(false);
                mThemeScheduledStartTheme.setVisible(false);
                mThemeScheduledEndTheme.setVisible(false);
                mThemeSchedule.setValue("1");
                scheduledStartTheme = false;
                scheduledEndTheme = false;
            }
            if (mThemeScheduledStartTheme != null) {
                if (scheduledStartTheme) {
                    mThemeScheduledStartTheme.setTitle(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                    mThemeScheduledStartTheme.setSummary(getScheduledStartThemeTime(mSharedPreferences));
                    scheduledStartTheme = false;
                } else {
                    mThemeScheduledStartTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledStartTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    scheduledStartTheme = true;
                }
            }
            if (mThemeScheduledEndTheme != null) {
                if (scheduledEndTheme) {
                    mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                    mThemeScheduledEndTheme.setSummary(getScheduledEndThemeTime(mSharedPreferences));
                    scheduledEndTheme = false;
                } else {
                    mThemeScheduledEndTheme.setTitle(mContext.getString(R.string.theme_schedule_theme_title));
                    mThemeScheduledEndTheme.setSummary(mContext.getString(R.string.theme_schedule_theme_summary));
                    scheduledEndTheme = true;
                }
            }
            if (mThemeScheduleRepeat != null) {
                if (mThemeScheduleRepeat.isChecked()) {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
                    sharedPreferencesEditor.putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, true);
                    sharedPreferencesEditor.apply();
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
                    sharedPreferencesEditor.putBoolean(PREF_THEME_SCHEDULED_REPEAT_DAILY, false);
                    sharedPreferencesEditor.apply();
                }
            }
            if (mThemeScheduleToast != null) {
                if (mThemeScheduleToast.isChecked()) {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_TOAST);
                    sharedPreferencesEditor.putBoolean(PREF_THEME_SCHEDULED_TOAST, true);
                    sharedPreferencesEditor.apply();
                } else {
                    sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_TOAST);
                    sharedPreferencesEditor.putBoolean(PREF_THEME_SCHEDULED_TOAST, false);
                    sharedPreferencesEditor.apply();
                }
            }
            mThemeSchedule.setSummary(mThemeSchedule.getEntry());
        }

        private void clearAll() {
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME_VALUE);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_THEME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_START_TIME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME_VALUE);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_THEME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_END_TIME);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_REPEAT_DAILY);
            sharedPreferencesEditor.remove(PREF_THEME_SCHEDULED_TOAST);
            sharedPreferencesEditor.remove(PREF_ALARM_START_TIME);
            sharedPreferencesEditor.remove(PREF_ALARM_END_TIME);
            sharedPreferencesEditor.commit();
            mThemeScheduleRepeat.setVisible(false);
            mThemeScheduleToast.setVisible(false);
            mThemeScheduledStartTheme.setVisible(false);
            mThemeScheduledEndTheme.setVisible(false);
            mThemeSchedule.setValue("1");
            scheduledStartTheme = false;
            scheduledEndTheme = false;
        }

        public void showStartTimePicker() {
            TimePickerDialog dialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mStartDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    mStartDate.set(Calendar.MINUTE, minute);
                    scheduledThemeStatus = 1;
                    setStartTime(mContext, mStartDate);
                    setStartAlarm(mContext);
                    ComponentName mStartReceiver = new ComponentName(mContext, ThemesStartReceiver.class);
                    mPm.setComponentEnabledSetting(mStartReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    if (mThemeScheduledStartTheme != null) {
                        mThemeScheduledStartTheme.setTitle(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                                + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                        mThemeScheduledStartTheme.setSummary(timeFormat.format(mStartDate.getTime()));
                        sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_START_TIME, timeFormat.format(mStartDate.getTime())).commit();
                        mThemeScheduledStartTheme.setEnabled(false);
                        mThemeScheduledEndTheme.setVisible(true);
                        mThemeScheduledEndTheme.setEnabled(true);
                        mThemeScheduleRepeat.setEnabled(false);
                        scheduledStartTheme = true;
                    }
                }
            }, mStartDate.get(Calendar.HOUR_OF_DAY), mStartDate.get(Calendar.MINUTE), false);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    scheduledThemeStatus = 2;
                    clearAlarms(mContext);
                    clearAll();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (scheduledThemeStatus == 0) {
                        clearAlarms(mContext);
                        clearAll();
                    }
                }
            });
            dialog.show();
        }

        public void showEndTimePicker() {
            TimePickerDialog dialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mEndDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    mEndDate.set(Calendar.MINUTE, minute);
                    scheduledThemeStatus = 1;
                    setEndTime(mContext, mEndDate);
                    setEndAlarm(mContext);
                    ComponentName mEndReceiver = new ComponentName(mContext, ThemesEndReceiver.class);
                    mPm.setComponentEnabledSetting(mEndReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    if (mThemeScheduledEndTheme != null) {
                        mThemeScheduledEndTheme.setTitle(getScheduledEndThemeSummary(mSharedPreferences, mContext)
                                + " " + mContext.getString(R.string.theme_schedule_start_scheduled));
                        mThemeScheduledEndTheme.setSummary(timeFormat.format(mEndDate.getTime()));
                        sharedPreferencesEditor.putString(PREF_THEME_SCHEDULED_END_TIME, timeFormat.format(mEndDate.getTime())).commit();
                        mThemeScheduledStartTheme.setEnabled(false);
                        mThemeScheduledEndTheme.setEnabled(false);
                        mThemeScheduleRepeat.setEnabled(false);
                        scheduledEndTheme = true;
                    }
                }
            }, mEndDate.get(Calendar.HOUR_OF_DAY),  mEndDate.get(Calendar.MINUTE), false);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    scheduledThemeStatus = 2;
                    clearAlarms(mContext);
                    clearAll();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (scheduledThemeStatus == 0) {
                        clearAlarms(mContext);
                        clearAll();
                    }
                }
            });
            dialog.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            goUpToTopLevelSetting(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
    }
}

