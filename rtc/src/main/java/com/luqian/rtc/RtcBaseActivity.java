package com.luqian.rtc;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.rtc.BaiduRtcRoom;
import com.baidu.rtc.RtcParameterSettings;
import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * RTC 基类页面
 */
public abstract class RtcBaseActivity extends AppCompatActivity implements BaiduRtcRoom.BaiduRtcRoomDelegate {

    private static final String TAG = "RtcBaseActivity";

    protected ImageView mIvCall;
    protected BaiduRtcRoom mVideoRoom;
    private MediaPlayer mPlayer = null;
    /**
     * 是否 1v1 呼叫
     */
    protected boolean mCallMode = false;
    protected String mAppId = "";
    protected String mToken = "";
    protected String mUserId = "";
    protected String mUserName = "";
    protected String mRoomName = "";
    protected String mMediaServer = "";
    protected String mVideoResolution = "1280x960";
    boolean mAudioOnly = false;
    boolean mDisableBuildInNs = false;
    boolean mTestDataChannel = false;
    private String mInfo = "error";
    protected RtcDelegate rtcDelegate = null;
    /**
     * 其他人 ID
     */
    protected long partnerId = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        InitRTCRoom();
    }

    private void initData() {
        mAppId = getIntent().getExtras().getString("appid");
        mCallMode = getIntent().getExtras().getBoolean("callMode");
        mUserId = getIntent().getExtras().getString("userid");
        mUserName = getIntent().getExtras().getString("username");
        String mediaServer = getIntent().getExtras().getString("mediaserver");
        mRoomName = getIntent().getExtras().getString("roomname");
        mAudioOnly = getIntent().getExtras().getBoolean("audio_only");
        mDisableBuildInNs = getIntent().getExtras().getBoolean("disable_builtin_ns");
        mTestDataChannel = getIntent().getExtras().getBoolean("datachannel_test");

        String res = getIntent().getExtras().getString("video_resolution");
        if (!res.isEmpty()) {
            mVideoResolution = res;
        }

        //  服务器
        if (TextUtils.isEmpty(mediaServer)) {
            mMediaServer = getString(R.string.sp_default_media_server_url);
        } else {
            mMediaServer = mediaServer;
        }

        //  房间名
        if (TextUtils.isEmpty(mRoomName)) {
            mRoomName = getString(R.string.sp_default_room_name);
        }

        //  APPID
        if (TextUtils.isEmpty(mAppId)) {
            mAppId = getString(R.string.baidu_rtc_app_id);
        }

        //  Token
        mToken = getString(R.string.baidu_rtc_token_tmp);
    }


    /**
     * 初始化 RTC
     */
    protected void InitRTCRoom() {
        XLog.i(TAG, "BaiduRTC(BRTC) SDK version is: " + BaiduRtcRoom.version());
        //  token 可以不写，是用户鉴权用的
        mVideoRoom = BaiduRtcRoom.initWithAppID(this, mAppId, mToken);
        mVideoRoom.setBaiduRtcRoomDelegate(this);

        //  是否开启音频噪声抑制
        mVideoRoom.enableAns(mDisableBuildInNs);
        //  设置服务器地址
        mVideoRoom.setMediaServerURL(mMediaServer);
        //  是否打开调试信息
        BaiduRtcRoom.setVerbose(true);
        //  RTC 统计信息上报
        mVideoRoom.enableStatsToServer(true, "online");

        String rtmp_url = getIntent().getExtras().getString("rtmp_url");
        boolean rtmp_mix = getIntent().getExtras().getBoolean("rtmp_mix");
        boolean recording = getIntent().getExtras().getBoolean("recording");

        if (rtmp_url.contains("rtmp")) {
            mVideoRoom.setLiveStreamingURL(rtmp_url);
            mVideoRoom.setLiveStreamingMix(rtmp_mix);
        }

        if (recording) {
            mVideoRoom.setRecording(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoRoom.startPreview();
    }

    @Override
    protected void onDestroy() {
        mVideoRoom.logoutRtcRoom();
        mVideoRoom.destroy();
        stopMusic();
        super.onDestroy();
    }


    public void playMusic() {
        mPlayer = MediaPlayer.create(this, R.raw.miui);
        mPlayer.setLooping(true);
        mPlayer.start();
    }

    public void stopMusic() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }
    }


    @Override
    public void onRoomEventUpdate(int roomEvents, long data, String extra_info) {
        runOnUiThread(() -> {
            Log.i(TAG, "onRoomEventUpdate is: " + roomEvents);
            switch (roomEvents) {
                //  登录房间成功
                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_OK:
                    loginRtcSuccess();
                    break;

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_ERROR:

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_CONNECTION_LOST:
                    switch (roomEvents) {
                        case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_ERROR: {
                            mInfo = getString(R.string.login_room_failed);
                            break;
                        }
                        case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_CONNECTION_LOST: {
                            mInfo = getString(R.string.connection_lost);
                            break;
                        }
                    }
                    toast(mInfo);
                    finish();

                    break;

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_REMOTE_COMING:
                    Log.e(TAG, "onRoomEventUpdate : Coming UID " + data + " Display:" + extra_info);
                    break;

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_REMOTE_LEAVING:
                    Log.e(TAG, "onRoomEventUpdate : Leaving UID " + data);
                    break;

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_AVAILABLE_SEND_BITRATE:
                    Log.e(TAG, "onRoomEventUpdate : Available BitRate: " + data);
                    break;

                //  用户消息
                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_MESSAGE:
                    Log.e(TAG, "onRoomEventUpdate onUserMessage id: " + data + " msg: " + extra_info);

                    if (extra_info.contains("ping getattribute")) {
                        mVideoRoom.getUserAttribute(data);
                    }
                    if (extra_info.contains("ping sendmessage")) {
                        mVideoRoom.sendMessageToUser("hello", 0);
                    }
                    if (extra_info.contains("ping setattribute")) {
                        mVideoRoom.setUserAttribute("{'name':'jim','tel':'123456789'}");
                    }
                    if (extra_info.contains("ping disabandroom")) {
                        mVideoRoom.disbandRoom();
                    }
                    if (extra_info.contains("ping shutupuser")) {
                        mVideoRoom.shutUpUserWithId(123456);
                    }
                    if (extra_info.contains("ping unshutupuser")) {
                        mVideoRoom.shutUpUserWithId(123456, false);
                    }
                    if (extra_info.contains("ping kickoffuser")) {
                        mVideoRoom.kickOffUserWithId(123456);
                    }
                    //  1v1 呼叫模式
                    if (mCallMode && rtcDelegate != null) {
                        //  发送信令
                        rtcDelegate.onMessage(extra_info);
                    }
                    break;

                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_ATTRIBUTE:
                    Log.e(TAG, "onRoomEventUpdate onUserAttribute id: " + data + " attribute: " + extra_info);
                    break;

                //  用户加入房间
                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_JOINED_ROOM:
                    Log.e(TAG, "onRoomEventUpdate onUserJoinedRoom id: " + data + " name: " + extra_info);
                    partnerId = data;
                    break;

                //  用户离开房间
                case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_LEAVING_ROOM:
                    Log.e(TAG, "onRoomEventUpdate onUserLeavingRoom id: " + data);
                    if (mCallMode) {
                        finish();
                    }
                    break;
            }
        });
    }

    abstract void loginRtcSuccess();

    @Override
    public void onPeerConnectStateUpdate(int connecStates) {
        Log.i(TAG, "onPeerConnectStateUpdate is: " + connecStates);
        //        public static final int RTC_STATE_STREAM_UP                        = 2000;
        //        public static final int RTC_STATE_SENDING_MEDIA_OK                 = 2001;
        //        public static final int RTC_STATE_SENDING_MEDIA_FAILED             = 2002;
        //        public static final int RTC_STATE_STREAM_DOWN                      = 2003;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL0          = 2100;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL1          = 2101;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL2          = 2102;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL3          = 2103;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL4          = 2104;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL5          = 2105;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL6          = 2106;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL7          = 2107;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL8          = 2108;
        //        public static final int RTC_STATE_STREAM_SLOW_LINK_LEVEL9          = 2109;
    }

    @Override
    public void onStreamInfoUpdate(String[] streamId) {

    }

    @Override
    public void onErrorInfoUpdate(int errorInfo) {
        XLog.e("onErrorInfoUpdate", errorInfo);
    }

    @Override
    public void onEngineStatisticsInfo(int statistics) {
        XLog.e("onEngineStatisticsInfo:", statistics);
    }

    @Override
    public void onRoomDataMessage(ByteBuffer data) {
        XLog.i(TAG, "onRoomDataMessage : " + Charset.defaultCharset().decode(data).toString());
    }


    /**
     * 开启 / 关闭 本地视频
     */
    public void onLocalVideoMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.setImageResource(R.drawable.btn_voice);
        } else {
            iv.setSelected(true);
            iv.setImageResource(R.drawable.btn_voice_selected);
        }
        mVideoRoom.muteCamera(iv.isSelected());

        findViewById(R.id.local_rtc_video_view).setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
    }


    /**
     * 开启 或 关闭 声音
     */
    public void onLocalAudioMuteClicked(View view) {
        ImageView ivVoice = (ImageView) view;
        if (ivVoice.isSelected()) {
            ivVoice.setSelected(false);
            ivVoice.setImageResource(R.drawable.ic_mute_voice);
        } else {
            ivVoice.setSelected(true);
            ivVoice.setImageResource(R.drawable.ic_mute_voice_selected);
        }
        mVideoRoom.muteMicphone(ivVoice.isSelected());
    }


    /**
     * 切换前后摄像头
     */
    public void onSwitchCameraClicked(View view) {
        ImageView ivSwitchCamera = (ImageView) view;
        if (ivSwitchCamera.isSelected()) {
            ivSwitchCamera.setSelected(false);
            ivSwitchCamera.setImageResource(R.drawable.ic_switch_camera);
        } else {
            ivSwitchCamera.setSelected(true);
            ivSwitchCamera.setImageResource(R.drawable.btn_switch_camera_selected);
        }
        mVideoRoom.switchCamera();
    }


    /**
     * 切换扬声器 / 听筒
     */
    public void onSpeakerClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.setImageResource(R.drawable.ic_speaker);
        } else {
            iv.setSelected(true);
            iv.setImageResource(R.drawable.ic_speaker_selected);
        }
        mVideoRoom.switchLoundSpeaker();
    }


    public void onEndCallClicked(View view) {
        //  1v1 模式不处理
        if (mCallMode) {
            return;
        }
        finish();
    }


    protected void loginRtc() {
        RtcParameterSettings rtcSetting = RtcParameterSettings.getDefaultSettings();

        rtcSetting.VideoResolution = mVideoResolution;
        rtcSetting.VideoFps = 15;

        if (mVideoResolution.contains("128kbps")) {
            rtcSetting.VideoMaxkbps = 128;
        } else if (mVideoResolution.contains("256kbps")) {
            rtcSetting.VideoMaxkbps = 256;
        } else if (mVideoResolution.contains("350kbps")) {
            rtcSetting.VideoMaxkbps = 350;
        } else if (mVideoResolution.contains("500kbps")) {
            rtcSetting.VideoMaxkbps = 500;
        } else if (mVideoResolution.contains("800kbps")) {
            rtcSetting.VideoMaxkbps = 800;
        } else if (mVideoResolution.contains("1000kbps")) {
            rtcSetting.VideoMaxkbps = 1000;
        } else if (mVideoResolution.contains("1500kbps")) {
            rtcSetting.VideoMaxkbps = 1500;
        } else if (mVideoResolution.contains("2000kbps")) {
            rtcSetting.VideoMaxkbps = 2000;
        } else if (mVideoResolution.contains("3000kbps")) {
            rtcSetting.VideoMaxkbps = 3000;
        } else if (mVideoResolution.contains("5000kbps")) {
            rtcSetting.VideoMaxkbps = 5000;
        } else if (mVideoResolution.contains("12000kbps")) {
            rtcSetting.VideoMaxkbps = 12000;
        } else if (mVideoResolution.contains("40000kbps")) {
            rtcSetting.VideoMaxkbps = 40000;
        } else {
            rtcSetting.VideoMaxkbps = 1000;
        }

        rtcSetting.HasData = mTestDataChannel;

        if (mAudioOnly) {
            rtcSetting.HasVideo = false;
        }

        rtcSetting.ConnectionTimeoutMs = 5000;
        rtcSetting.ReadTimeoutMs = 5000;

        if (Build.MANUFACTURER.contains("Ainemo")
                || Build.MODEL.contains("NV6001")
                || Build.MODEL.contains("NV6101")
                || Build.MODEL.contains("NV2001")
                || Build.MODEL.contains("NV5001")) {
            rtcSetting.AudioFrequency = 16000;
            rtcSetting.AudioChannel = 2;
            rtcSetting.AudioContentType = AudioAttributes.CONTENT_TYPE_MUSIC;
        }

        rtcSetting.AutoPublish = !mCallMode;
        rtcSetting.AutoSubScribe = !mCallMode;

        mVideoRoom.setParamSettings(rtcSetting, RtcParameterSettings.RtcParamSettingType.RTC_PARAM_SETTINGS_ALL);
        mVideoRoom.loginRtcRoomWithRoomName(mRoomName, Long.parseLong(mUserId), mUserName);
    }


    protected void toast(@StringRes int resId) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    protected void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
