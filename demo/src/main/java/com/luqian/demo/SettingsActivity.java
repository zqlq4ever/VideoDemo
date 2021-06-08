package com.luqian.demo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.gyf.immersionbar.ImmersionBar;

import java.util.List;

/**
 * 设置页面
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        ImmersionBar.with(this)
                .fitsSystemWindows(true)  //    使用该属性,必须指定状态栏颜色
                .statusBarColor(R.color.white)
                .init();
    }

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
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


    /**
     * 将首选项的摘要与其值绑定。
     * 当 SP 的值更改，其摘要（下方的文本行首选项标题）更新以反映该值。
     *
     * <p>总结就是：
     * 调用此方法后立即更新。确切的显示格式取决于SP的类型。
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
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

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.sp_headers, target);
    }


    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralSPFragment.class.getName().equals(fragmentName)
                || VideoSPFragment.class.getName().equals(fragmentName)
                || AudioSPFragment.class.getName().equals(fragmentName);
    }


    /**
     * 是否为平板尺寸（SettingsActivity 根据尺寸自适应页面）
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * 通用设置
     */
    public static class GeneralSPFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.sp_general);
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


    /**
     * 音频设置
     */
    public static class AudioSPFragment extends PreferenceFragment {
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


    /**
     * 视频设置
     */
    public static class VideoSPFragment extends PreferenceFragment {
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
