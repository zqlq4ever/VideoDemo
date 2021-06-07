package com.luqian.videodemo;

import android.app.Activity;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.rtc.BaiduRtcRoom;
import com.baidu.rtc.RtcParameterSettings;
import com.baidu.rtc.videoroom.R;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * RTC 基类页面
 */
public class RtcBaseActivity extends Activity implements BaiduRtcRoom.BaiduRtcRoomDelegate {

    private static final String TAG = "RtcBaseActivity";

    protected ImageView ivCall;
    protected BaiduRtcRoom mVideoRoom;
    /**
     * 是否 1v1 呼叫
     */
    protected boolean callMode = false;
    protected String mAppId = "";
    protected String mTokenStr = "";
    protected String mUserId = "";
    protected String mUserName = "";
    protected String mRoomName = "";
    protected String mMediaServer = "";
    protected String mVideoResolution = "640x480";
    private Handler mHandler = null;
    boolean mAudioOnly = false;
    boolean mDisableBuildInNs = false;
    boolean mTestDataChannel = false;
    private final boolean mIsEnableExternalRender = false;
    //    BaiduRtcRoom.UserList mUserList = null;
    private String mInfo = "error";
    protected VideoCallProtocol callProtocol = null;
    protected long partnerId = 0;
    protected RtcEventListener eventListener;

    private final Runnable fireData = new Runnable() {
        @Override
        public void run() {
            String s = "100000111101110111100010100110101001010111000000100000001000000010000000"
                    + "1000000010100000100000001001001010000001111111111111111111000000100000001"
                    + "000000110011100111100001010101011110000100000001001001111";

            int len = s.length();
            BitSet bs = new BitSet(len);
            for (int i = 0; i < len; i++) {
                if (s.charAt(i) == '1') {
                    bs.set(i);
                }
            }
            //  设置同步数据（除了音频、视频外的）
            mVideoRoom.sendData(ByteBuffer.wrap(bs.toByteArray()));
            mVideoRoom.sendData(ByteBuffer.wrap("88888888888888众里\0\0\0\0寻他千百度. ".getBytes()));
            mHandler.postDelayed(fireData, 10000);
            //            mUserList = mVideoRoom.queryUserListOfRoom();
            //            Log.e(TAG,"mUserList "+ mUserList);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InitRTCRoom();
    }


    /**
     * 初始化 RTC
     */
    protected void InitRTCRoom() {
        mAppId = getIntent().getExtras().getString("appid");
        callMode = getIntent().getExtras().getBoolean("callMode");
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
        mTokenStr = getString(R.string.baidu_rtc_token_tmp);

        Log.i(TAG, "BaiduRTC(BRTC) SDK version is: " + BaiduRtcRoom.version());

        //  token 可以不写，是用户鉴权用的
        mVideoRoom = BaiduRtcRoom.initWithAppID(this, mAppId, mTokenStr);
        //  是否打开调试信息
//        mVideoRoom.setVerbose(true);
        //  RTC 统计信息上报
//        mVideoRoom.enableStatsToServer(true,"online");
        //  是否开启音频噪声抑制
        mVideoRoom.enableAns(mDisableBuildInNs);
        mVideoRoom.setBaiduRtcRoomDelegate(this);
        //  设置服务器地址
        mVideoRoom.setMediaServerURL(mMediaServer);

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

        mHandler = new Handler();
        //  额外扩展数据流发送（除了音视频数据流）
       /* if (mTestDataChannel) {
            mHandler.postDelayed(fireData, 10000);
        }*/
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
        super.onDestroy();
    }

    @Override
    public void onRoomEventUpdate(int roomEvents, long data, String extra_info) {

        Log.i(TAG, "onRoomEventUpdate is: " + roomEvents);

        switch (roomEvents) {
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_OK:
                if (eventListener != null) {
                    eventListener.onRtcRoomEeventLoginOk();
                }
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
                this.runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            mInfo,
                            Toast.LENGTH_SHORT)
                            .show();
                    RtcBaseActivity.this.finish();
                });

                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_REMOTE_COMING:
                Log.e(TAG, "onRoomEventUpdate : Coming UID " + data + " Display:" + extra_info);
                if (eventListener != null) {
                    eventListener.onRemoteComming(data);
                }
            /*runOnUiThread(() -> {
                mVideoRoom.startPublish();
                mVideoRoom.subscribeStreaming(0, data);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mVideoRoom == null) return;

                        mVideoRoom.stopPublish();

                        mVideoRoom.stopSubscribeStreaming(data);
                    }
                }, 10000);
            });*/
                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_REMOTE_LEAVING:
                Log.e(TAG, "onRoomEventUpdate : Leaving UID " + data);
//            runOnUiThread(() -> mVideoRoom.stopPublish());
                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_AVAILABLE_SEND_BITRATE:
                Log.e(TAG, "onRoomEventUpdate : Available BitRate: " + data);
           /* RtcParameterSettings cfg = RtcParameterSettings.getDefaultSettings();
            cfg.VideoMaxkbps = 1000;
            mVideoRoom.setParamSettings(cfg, RtcParameterSettings
                    .RtcParamSettingType.RTC_VIDEO_PARAM_SETTINGS_BITRATE);*/
                break;
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
                if (callMode) {
                    if (callProtocol != null) {
                        callProtocol.onMessage(extra_info);
                    }
                }
                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_ATTRIBUTE:
                Log.e(TAG, "onRoomEventUpdate onUserAttribute id: " + data + " attribute: " + extra_info);
                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_JOINED_ROOM:
                Log.e(TAG, "onRoomEventUpdate onUserJoinedRoom id: " + data + "name: " + extra_info);
                partnerId = data;
            /*if (mIsEnableExternalRender) {
                mVideoRoom.registerVideoObservers(new BigInteger(Long.toString(data)), new RTCVideoExternalRender(null, 0) {
                    @Override
                    public void onFrame(VideoFrame videoFrame) {
                        Log.d(TAG, "testing ........... ^^ ^^ ** video arrival!");
                    }
                });
            }*/
                break;
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_LEAVING_ROOM:
                Log.e(TAG, "onRoomEventUpdate onUserLeavingRoom id: " + data);
                break;
        }
    }

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

    }

    @Override
    public void onEngineStatisticsInfo(int statistics) {

    }

    @Override
    public void onRoomDataMessage(ByteBuffer data) {
        Log.i(TAG, "onRoomDataMessage : " + Charset.defaultCharset().decode(data).toString());
    }


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
    public void onLocalAudioMuteClicked(ImageView ivVoice) {
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
    public void onSwitchCameraClicked(ImageView ivSwitchCamera) {
        if (ivSwitchCamera.isSelected()) {
            ivSwitchCamera.setSelected(false);
            ivSwitchCamera.setImageResource(R.drawable.ic_switch_camera);
        } else {
            ivSwitchCamera.setSelected(true);
            ivSwitchCamera.setImageResource(R.drawable.btn_switch_camera_selected);
        }
        mVideoRoom.switchCamera();
    }


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
        if (callMode)
            return;
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
                || Build.MODEL.contains("NV5001")) { // Duer 1s 1C 1A 1L, legacy
            rtcSetting.AudioFrequency = 16000;
            rtcSetting.AudioChannel = 2;
            rtcSetting.AudioContentType = AudioAttributes.CONTENT_TYPE_MUSIC;
        }

        rtcSetting.AutoPublish = !callMode;       // default is true.  for mVideoRoom.startPublish() set to false.

        rtcSetting.AutoSubScribe = !callMode;     // default is true.  for mVideoRoom.subscribeStreaming() set to false.

        /* cfg.VideoRenderMode = RtcParameterSettings.RtcVideoRenderMode.RTC_VIDEO_RENDER_MODE_EXTERNAL;
        mIsEnableExternalRender = true;
        cfg.MicPhoneMuted = true;
        cfg.CameraMuted = true;*/

        mVideoRoom.setParamSettings(rtcSetting, RtcParameterSettings.RtcParamSettingType.RTC_PARAM_SETTINGS_ALL);

        mVideoRoom.loginRtcRoomWithRoomName(mRoomName, Long.parseLong(mUserId), mUserName);
    }


    public abstract static class RtcEventListener {
        public abstract void onRemoteComming(long userId);

        public abstract void onRtcRoomEeventLoginOk();
    }
}
