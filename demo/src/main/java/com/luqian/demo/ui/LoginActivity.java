package com.luqian.demo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gyf.immersionbar.ImmersionBar;
import com.luqian.demo.R;
import com.luqian.demo.ui.video.VideoActivity;
import com.permissionx.guolindev.PermissionX;

@SuppressLint("SetTextI18n")
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private AutoCompleteTextView mUserID;
    private EditText mEtRoomID;
    private String mUserDisplayname;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImmersionBar.with(this)
                .fitsSystemWindows(true)  //    使用该属性,必须指定状态栏颜色
                .statusBarColor(R.color.white)
                .statusBarDarkFont(true)
                .init();

        initView();

        //  模拟用户 id
        int userId = 78657895 + (Build.SERIAL.hashCode() % 100000) + (int) (Math.random() * 10000);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mUserDisplayname = sp.getString("user_display_name", getString(R.string.sp_default_user_name));
        mUserID.setText(String.valueOf(sp.getLong("UserID", userId)));
        mUserID.setSelection(mUserID.getText().toString().length());

        try {
            mEtRoomID.setText(sp.getString("RoomID", "99999999"));
        } catch (Exception e) {
            mEtRoomID.setText(Integer.valueOf(sp.getInt("RoomID", 99999999)).toString());
        }

        mEtRoomID.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

                prefs.edit().putLong("UserID", Long.parseLong(mUserID.getText().toString())).commit();

                if (mEtRoomID.getText().toString().isEmpty()) {
                    return true;
                }
                prefs.edit().putString("RoomID", mEtRoomID.getText().toString()).commit();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_login_rtc).setOnClickListener(view -> {
            attemptLogin();
            SharedPreferences spLogin = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

            spLogin.edit().putLong("UserID", Long.parseLong(mUserID.getText().toString())).commit();

            if (mEtRoomID.getText().toString().isEmpty()) {
                return;
            }
            spLogin.edit().putString("RoomID", mEtRoomID.getText().toString()).commit();
        });


        findViewById(R.id.settings_button).setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }


    private void initView() {
        mUserID = findViewById(R.id.userid);
        mEtRoomID = findViewById(R.id.roomid);
    }


    /**
     * 权限请求：相机和麦克风
     */
    private void requestPer(String userId, String roomId) {
        PermissionX.init(this)
                .permissions(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA)
                .onForwardToSettings((scope, deniedList) ->
                        scope.showForwardToSettingsDialog(deniedList,
                                "请在设置中开启授权",
                                "去设置权限"))
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        toVideoPage(userId, roomId);
                    } else {
                        Toast.makeText(this, "请先授权", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void attemptLogin() {
        // UI 重置
        mUserID.setError(null);
        mEtRoomID.setError(null);

        String userId = mUserID.getText().toString();
        String roomId = mEtRoomID.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(roomId)) {
            mEtRoomID.setError(getString(R.string.error_invalid_roomid));
            focusView = mEtRoomID;
            cancel = true;
        }

        if (TextUtils.isEmpty(userId)) {
            mUserID.setError(getString(R.string.error_field_required));
            focusView = mUserID;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            requestPer(userId, roomId);
        }
    }


    /**
     * 打开音视频通话页面
     *
     * @param userId 用户 ID
     * @param roomId 房间 ID
     */
    private void toVideoPage(String userId, String roomId) {
        Intent intent = new Intent(LoginActivity.this, VideoActivity.class);

        intent.putExtra("userid", userId);
        intent.putExtra("roomname", roomId);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

        boolean audio_only = sp.getBoolean("key_audio_only", false);
        intent.putExtra("audio_only", audio_only);

        boolean disableBuildInNS = sp.getBoolean("key_disable_builtin_ns", false);
        intent.putExtra("disable_builtin_ns", disableBuildInNS);

        String video_resolution = sp.getString("video_resolution", "640x480-1000kbps");
        intent.putExtra("video_resolution", video_resolution);

        mUserDisplayname = sp.getString("user_display_name", getString(R.string.sp_default_user_name));
        intent.putExtra("username", mUserDisplayname);

        String MediaServerUrl = sp.getString("key_media_server_url", getString(R.string.sp_default_media_server_url));
        intent.putExtra("mediaserver", MediaServerUrl);

        String appid = sp.getString("key_appid", "");
        intent.putExtra("appid", appid);

        boolean datachannel_test = sp.getBoolean("key_datachannel_test", false);
        intent.putExtra("datachannel_test", datachannel_test);

        String rtmp_url = sp.getString("key_rtmp_url", "");
        intent.putExtra("rtmp_url", rtmp_url);

        boolean rtmp_mix = sp.getBoolean("key_rtmp_mix", false);
        intent.putExtra("rtmp_mix", rtmp_mix);

        boolean recording = sp.getBoolean("key_recording", false);
        intent.putExtra("recording", recording);

        startActivity(intent);
    }
}

