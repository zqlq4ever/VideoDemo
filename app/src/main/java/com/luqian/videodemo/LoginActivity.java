package com.luqian.videodemo;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.rtc.videoroom.R;
import com.luqian.videodemo.setting.SettingsActivity;

@SuppressLint("SetTextI18n")
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int REQUEST_CAMERA_PERMISSION_ID = 100;
    private static final int REQUEST_MIC_PERMISSION_ID = 101;

    /**
     * 用户登录任务
     */
    private UserLoginTask mAuthTask = null;
    private AutoCompleteTextView mUserID;
    private EditText mEtRoomID;
    private CheckBox mVideoRoomCheckbox;
    private CheckBox mCallModeCheckbox;
    private View mProgressView;
    private String mUserDisplayname;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        //  模拟用户 id
        int userId = 78657895 + (Build.SERIAL.hashCode() % 100000) + (int) (Math.random() * 10000);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mUserDisplayname = sp.getString("user_display_name", getString(R.string.sp_default_user_name));
        mUserID.setText(Integer.valueOf(sp.getInt("UserID", userId)).toString());

        populateAutoComplete();

        try {
            mEtRoomID.setText(sp.getString("RoomID", "99999999"));
        } catch (Exception e) {
            // update from old version, using int value
            mEtRoomID.setText(Integer.valueOf(sp.getInt("RoomID", 99999999)).toString());
        }

        mEtRoomID.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

                prefs.edit().putInt("UserID", Integer.valueOf(mUserID.getText().toString())).commit();

                if (mEtRoomID.getText().toString().isEmpty()) {
                    return true;
                }
                prefs.edit().putString("RoomID", mEtRoomID.getText().toString()).commit();
                return true;
            }
            return false;
        });

        findViewById(R.id.sign_in_button).setOnClickListener(view -> {
            attemptLogin();
            SharedPreferences spLogin =
                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

            spLogin.edit().putInt("UserID", Integer.valueOf(mUserID.getText().toString())).commit();

            if (mEtRoomID.getText().toString().isEmpty()) {
                return;
            }
            spLogin.edit().putString("RoomID", mEtRoomID.getText().toString()).commit();
        });


        findViewById(R.id.settings_button).setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SettingsActivity.class);
            startActivity(intent);
        });


        mVideoRoomCheckbox.setChecked(sp.getBoolean("video_room_checked", false));
        mVideoRoomCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (mCallModeCheckbox != null) {
                    mCallModeCheckbox.setChecked(false);
                }
            }
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
            prefs.edit().putBoolean("video_room_checked", isChecked).commit();
        });

        mCallModeCheckbox.setChecked(sp.getBoolean("call_mode_checked", false));
        mCallModeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mVideoRoomCheckbox.setChecked(false);
            }
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
            prefs.edit().putBoolean("call_mode_checked", isChecked).commit();
        });
    }


    private void initView() {
        mUserID = findViewById(R.id.userid);
        mEtRoomID = findViewById(R.id.roomid);
        mVideoRoomCheckbox = findViewById(R.id.checkbox_videoroom);
        mCallModeCheckbox = findViewById(R.id.checkbox_callMode);
        mProgressView = findViewById(R.id.login_progress);
    }


    private void populateAutoComplete() {
        if (!mayRequestPermissions()) {
            return;
        }
    }

    private boolean mayRequestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        requestCameraAndMicPermissions();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION_ID) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
        if (requestCode == REQUEST_MIC_PERMISSION_ID) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * 权限请求：相机和麦克风
     */
    private void requestCameraAndMicPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION_ID);
            } else if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_MIC_PERMISSION_ID);
            }
        }
    }


    /**
     * 尝试登录指定的帐户。
     * 如果存在表单错误（无效的名字、缺少字段等），
     * 出现错误并且没有进行实际的登录尝试。
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        // UI 重置
        mUserID.setError(null);
        mEtRoomID.setError(null);

        String userid = mUserID.getText().toString();
        String roomid = mEtRoomID.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(roomid)) {
            mEtRoomID.setError(getString(R.string.error_invalid_roomid));
            focusView = mEtRoomID;
            cancel = true;
        }

        if (TextUtils.isEmpty(userid)) {
            mUserID.setError(getString(R.string.error_field_required));
            focusView = mUserID;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            showProgress(true);
            mAuthTask = new UserLoginTask(userid, roomid);
            mAuthTask.execute((Void) null);
        }
    }


    /**
     * 加载中
     */
    private void showProgress(final boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate()
                .setDuration(200)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }


    /**
     * 表示用于身份验证的异步登录/注册任务
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUserid;
        private final String mRoomName;

        UserLoginTask(String userid, String roomid) {
            mUserid = userid;
            mRoomName = roomid;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Intent intent = new Intent(
                        LoginActivity.this,
                        mVideoRoomCheckbox.isChecked() ? RoomActivity.class : VideoCallActivity.class);

                intent.putExtra("callMode", mCallModeCheckbox.isChecked());
                intent.putExtra("userid", mUserid);
                intent.putExtra("roomname", mRoomName);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);

                boolean audio_only = prefs.getBoolean("key_audio_only", false);
                intent.putExtra("audio_only", audio_only);

                boolean disableBuildInNS = prefs.getBoolean("key_disable_builtin_ns", false);
                intent.putExtra("disable_builtin_ns", disableBuildInNS);

                String video_resolution = prefs.getString("video_resolution", "640x480-1000kbps");
                intent.putExtra("video_resolution", video_resolution);

                mUserDisplayname = prefs.getString("user_display_name", getString(R.string.sp_default_user_name));
                intent.putExtra("username", mUserDisplayname);

                String MediaServerUrl = prefs.getString("key_media_server_url", getString(R.string.pref_default_media_server_url));
                intent.putExtra("mediaserver", MediaServerUrl);

                String appid = prefs.getString("key_appid", "");
                intent.putExtra("appid", appid);

                boolean datachannel_test = prefs.getBoolean("key_datachannel_test", false);
                intent.putExtra("datachannel_test", datachannel_test);

                String rtmp_url = prefs.getString("key_rtmp_url", "");
                intent.putExtra("rtmp_url", rtmp_url);

                boolean rtmp_mix = prefs.getBoolean("key_rtmp_mix", false);
                intent.putExtra("rtmp_mix", rtmp_mix);

                boolean recording = prefs.getBoolean("key_recording", false);
                intent.putExtra("recording", recording);

                startActivity(intent);
            } else {
                mEtRoomID.setError(getString(R.string.error_incorrect_roomid));
                mEtRoomID.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

