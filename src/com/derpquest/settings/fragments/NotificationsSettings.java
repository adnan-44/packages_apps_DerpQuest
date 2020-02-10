/*
 * Copyright (C) 2020 DerpFest ROM
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
package com.derpquest.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;
import com.derpquest.settings.Utils;

import com.derpquest.settings.preferences.AmbientLightSettingsPreview;
import com.derpquest.settings.preferences.CustomSeekBarPreference;
import com.derpquest.settings.preferences.SystemSettingSeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class NotificationsSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {

    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String FLASH_ON_CALL_WAITING_DELAY = "flash_on_call_waiting_delay";
    private static final String FLASH_ON_CALL_CATEGORY = "flash_on_call_category";
    private static final String PULSE_AMBIENT_LIGHT_COLOR = "pulse_ambient_light_color";
    private static final String PULSE_AMBIENT_LIGHT_DURATION = "pulse_ambient_light_duration";
    private static final String PULSE_AMBIENT_LIGHT_REPEAT_COUNT = "pulse_ambient_light_repeat_count";

    private ColorPickerPreference mEdgeLightColorPreference;
    private SystemSettingSeekBarPreference mEdgeLightDurationPreference;
    private SystemSettingSeekBarPreference mEdgeLightRepeatCountPreference;
    private Preference mChargingLeds;
    private SystemSettingSeekBarPreference mFlashOnCallWaitingDelay;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.derpquest_settings_notifications);
        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        Preference incallVibCategory = (Preference) findPreference(INCALL_VIB_OPTIONS);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }

        PreferenceCategory flashOnCallCategory = (PreferenceCategory) findPreference(FLASH_ON_CALL_CATEGORY);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(flashOnCallCategory);
        }

        mFlashOnCallWaitingDelay = (SystemSettingSeekBarPreference) findPreference(FLASH_ON_CALL_WAITING_DELAY);
        mFlashOnCallWaitingDelay.setValue(Settings.System.getInt(resolver,
              Settings.System.FLASH_ON_CALLWAITING_DELAY, 200));
        mFlashOnCallWaitingDelay.setOnPreferenceChangeListener(this);

        mChargingLeds = (Preference) findPreference("charging_light");
        if (mChargingLeds != null
                && !res.getBoolean(
                        com.android.internal.R.bool.config_intrusiveBatteryLed)) {
            prefScreen.removePreference(mChargingLeds);
        }

        mEdgeLightColorPreference = (ColorPickerPreference) findPreference(PULSE_AMBIENT_LIGHT_COLOR);
        mEdgeLightColorPreference.setOnPreferenceChangeListener(this);
        int edgeLightColor = Settings.System.getInt(resolver,
                Settings.System.PULSE_AMBIENT_LIGHT_COLOR, 0xFF3980FF);
        AmbientLightSettingsPreview.setAmbientLightPreviewColor(edgeLightColor);
        String edgeLightColorHex = String.format("#%08x", (0xFF3980FF & edgeLightColor));
        if (edgeLightColorHex.equals("#ff3980ff")) {
            mEdgeLightColorPreference.setSummary(R.string.default_string);
        } else {
            mEdgeLightColorPreference.setSummary(edgeLightColorHex);
        }
        mEdgeLightColorPreference.setNewPreviewColor(edgeLightColor);

        mEdgeLightDurationPreference = (SystemSettingSeekBarPreference) findPreference(PULSE_AMBIENT_LIGHT_DURATION);
        mEdgeLightDurationPreference.setOnPreferenceChangeListener(this);
        int duration = Settings.System.getInt(resolver,
                Settings.System.PULSE_AMBIENT_LIGHT_DURATION, 2);
        mEdgeLightDurationPreference.setValue(duration);

        mEdgeLightRepeatCountPreference = (SystemSettingSeekBarPreference) findPreference(PULSE_AMBIENT_LIGHT_REPEAT_COUNT);
        mEdgeLightRepeatCountPreference.setOnPreferenceChangeListener(this);
        int rCount = Settings.System.getInt(resolver,
                Settings.System.PULSE_AMBIENT_LIGHT_REPEAT_COUNT, 0);
        mEdgeLightRepeatCountPreference.setValue(rCount);

        int defaultDoze = res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessDoze);
        int defaultPulse = res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessPulse);
        if (defaultPulse == -1) {
            defaultPulse = defaultDoze;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getContentResolver();
        if (preference == mFlashOnCallWaitingDelay) {
            int val = (Integer) newValue;
            Settings.System.putInt(resolver, Settings.System.FLASH_ON_CALLWAITING_DELAY, val);
            return true;
        } else if (preference == mEdgeLightColorPreference) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#ff3980ff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            AmbientLightSettingsPreview.setAmbientLightPreviewColor(Integer.valueOf(String.valueOf(newValue)));
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.PULSE_AMBIENT_LIGHT_COLOR, intHex);
            return true;
        } else if (preference == mEdgeLightDurationPreference) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.PULSE_AMBIENT_LIGHT_DURATION, value);
            return true;
        } else if (preference == mEdgeLightRepeatCountPreference) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.PULSE_AMBIENT_LIGHT_REPEAT_COUNT, value);
            return true;
        }
        return false;
    }


    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.OWLSNEST;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.derpquest_settings_notifications;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
