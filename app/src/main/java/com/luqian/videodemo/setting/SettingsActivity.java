package com.luqian.videodemo.setting;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import com.baidu.rtc.videoroom.R;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0
                    ? listPreference.getEntries()[index]
                    : null);
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    };


    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * 将首选项的摘要与其值绑定。更具体地说，当
     * 首选项的值已更改，其摘要（下方的文本行
     * 首选项标题）更新以反映该值。总结也是
     * 调用此方法后立即更新。确切的显示格式是
     * 取决于偏好的类型。
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        if (preference instanceof SwitchPreference) {
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }


    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }


    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }


    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("user_display_name"));
            bindPreferenceSummaryToValue(findPreference("key_media_server_url"));
            bindPreferenceSummaryToValue(findPreference("key_appid"));
            bindPreferenceSummaryToValue(findPreference("key_rtmp_url"));
            bindPreferenceSummaryToValue(findPreference("key_rtmp_mix"));
            bindPreferenceSummaryToValue(findPreference("key_recording"));
            bindPreferenceSummaryToValue(findPreference("key_platform_server_url"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("key_audio_only"));
            bindPreferenceSummaryToValue(findPreference("key_disable_builtin_ns"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("video_resolution"));

            bindPreferenceSummaryToValue(findPreference("video_codec"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
