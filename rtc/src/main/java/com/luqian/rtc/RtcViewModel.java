package com.luqian.rtc;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baidu.rtc.BaiduRtcRoom;
import com.baidu.rtc.RtcParameterSettings;
import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;
import com.luqian.rtc.bean.RtcLoginStatus;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author LUQIAN
 * @date 2021/6/10
 */
public class RtcViewModel extends ViewModel implements BaiduRtcRoom.BaiduRtcRoomDelegate {

    /**
     * 登录状态
     */
    public MutableLiveData<RtcLoginStatus> loginStatus = new MutableLiveData<>();
    /**
     * 用户加入房间
     */
    public MutableLiveData<Long> mUserJoin = new MutableLiveData<>();
    /**
     * 用户离开房间
     */
    public MutableLiveData<Long> mUserLeave = new MutableLiveData<>();
    /**
     * 错误信息
     */
    public MutableLiveData<Integer> mErrorInfo = new MutableLiveData<>();
    /**
     * 统计信息
     */
    public MutableLiveData<Integer> mStatistics = new MutableLiveData<>();
    /**
     * 用户信令消息
     */
    public MutableLiveData<String> mUserMsg = new MutableLiveData<>();

    private static final String TAG = "RtcViewModel";

    private MediaPlayer mPlayer = null;

    public BaiduRtcRoom mBaiduRtcRoom;


    /**
     * 初始化百度 RTC
     */
    public void InitRTCRoom() {
        XLog.i(TAG, "BaiduRTC(BRTC) SDK version is: " + BaiduRtcRoom.version());
        //  token 可以不写，是用户鉴权用的
        mBaiduRtcRoom = BaiduRtcRoom.initWithAppID(BaseApp.getApp(), "appmf3e64yj79e5", "");
        //  rtc 代理
        mBaiduRtcRoom.setBaiduRtcRoomDelegate(this);
        //  是否开启音频噪声抑制
        mBaiduRtcRoom.enableAns(true);
        //  是否打开调试信息
        BaiduRtcRoom.setVerbose(true);
        //  RTC 统计信息上报
        mBaiduRtcRoom.enableStatsToServer(true, "online");
        //  是否开启录制
        mBaiduRtcRoom.setRecording(false);
    }


    @Override
    public void onRoomEventUpdate(int roomEvents, long data, String extra_info) {
        Log.i(TAG, "onRoomEventUpdate is: " + roomEvents);
        switch (roomEvents) {
            //  登录房间成功
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_OK:
                loginStatus.postValue(new RtcLoginStatus(true, "登录房间成功"));
                break;

            //  登录房间失败
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_LOGIN_ERROR:
                loginStatus.postValue(new RtcLoginStatus(false, "登录房间失败"));
                break;

            //  网络断开连接
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_CONNECTION_LOST:
                loginStatus.postValue(new RtcLoginStatus(false, "网络断开连接"));
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

                /*if (extra_info.contains("ping getattribute")) {
                    mBaiduRtcRoom.getUserAttribute(data);
                }
                if (extra_info.contains("ping sendmessage")) {
                    mBaiduRtcRoom.sendMessageToUser("hello", 0);
                }
                if (extra_info.contains("ping setattribute")) {
                    mBaiduRtcRoom.setUserAttribute("{'name':'jim','tel':'123456789'}");
                }
                if (extra_info.contains("ping disabandroom")) {
                    mBaiduRtcRoom.disbandRoom();
                }
                if (extra_info.contains("ping shutupuser")) {
                    mBaiduRtcRoom.shutUpUserWithId(123456);
                }
                if (extra_info.contains("ping unshutupuser")) {
                    mBaiduRtcRoom.shutUpUserWithId(123456, false);
                }
                if (extra_info.contains("ping kickoffuser")) {
                    mBaiduRtcRoom.kickOffUserWithId(123456);
                }*/

                //  发送信令
                mUserMsg.postValue(extra_info);
                break;

            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_ATTRIBUTE:
                Log.e(TAG, "onRoomEventUpdate onUserAttribute id: " + data + " attribute: " + extra_info);
                break;

            //  用户加入房间
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_JOINED_ROOM:
                Log.e(TAG, "onRoomEventUpdate onUserJoinedRoom id: " + data + " name: " + extra_info);
                mUserJoin.postValue(data);
                break;

            //  用户离开房间
            case BaiduRtcRoom.BaiduRtcRoomDelegate.RTC_ROOM_EVENT_ON_USER_LEAVING_ROOM:
                Log.e(TAG, "onRoomEventUpdate onUserLeavingRoom id: " + data);
                mUserLeave.postValue(data);
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
        XLog.e("onErrorInfoUpdate", errorInfo);
        mErrorInfo.postValue(errorInfo);
    }

    @Override
    public void onEngineStatisticsInfo(int statistics) {
        XLog.e("onEngineStatisticsInfo:", statistics);
        mStatistics.postValue(statistics);
    }

    @Override
    public void onRoomDataMessage(ByteBuffer data) {
        XLog.e(TAG, "onRoomDataMessage : " + Charset.defaultCharset().decode(data).toString());
    }


    public void loginRtc(String roomName, long userId, String userName) {

        RtcParameterSettings rtcSetting = RtcParameterSettings.getDefaultSettings();

        String videoResolution = "1280x960";
        rtcSetting.VideoResolution = videoResolution;

        rtcSetting.VideoFps = 15;

        if (videoResolution.contains("128kbps")) {
            rtcSetting.VideoMaxkbps = 128;
        } else if (videoResolution.contains("256kbps")) {
            rtcSetting.VideoMaxkbps = 256;
        } else if (videoResolution.contains("350kbps")) {
            rtcSetting.VideoMaxkbps = 350;
        } else if (videoResolution.contains("500kbps")) {
            rtcSetting.VideoMaxkbps = 500;
        } else if (videoResolution.contains("800kbps")) {
            rtcSetting.VideoMaxkbps = 800;
        } else if (videoResolution.contains("1000kbps")) {
            rtcSetting.VideoMaxkbps = 1000;
        } else if (videoResolution.contains("1500kbps")) {
            rtcSetting.VideoMaxkbps = 1500;
        } else if (videoResolution.contains("2000kbps")) {
            rtcSetting.VideoMaxkbps = 2000;
        } else if (videoResolution.contains("3000kbps")) {
            rtcSetting.VideoMaxkbps = 3000;
        } else if (videoResolution.contains("5000kbps")) {
            rtcSetting.VideoMaxkbps = 5000;
        } else if (videoResolution.contains("12000kbps")) {
            rtcSetting.VideoMaxkbps = 12000;
        } else if (videoResolution.contains("40000kbps")) {
            rtcSetting.VideoMaxkbps = 40000;
        } else {
            rtcSetting.VideoMaxkbps = 1000;
        }

        rtcSetting.HasData = false;

        rtcSetting.HasVideo = false;

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

        rtcSetting.AutoPublish = false;
        rtcSetting.AutoSubScribe = false;

        mBaiduRtcRoom.setParamSettings(rtcSetting, RtcParameterSettings.RtcParamSettingType.RTC_PARAM_SETTINGS_ALL);
        mBaiduRtcRoom.loginRtcRoomWithRoomName(roomName, userId, userName);

    }


    public void playMusic() {
        mPlayer = MediaPlayer.create(BaseApp.getApp(), R.raw.miui);
        mPlayer.setLooping(true);
        mPlayer.start();
    }


    public void stopMusic() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }
    }


    public void stopPublsh() {
        if (mUserJoin.getValue() == null) return;
        mBaiduRtcRoom.stopPublish();
        mBaiduRtcRoom.stopSubscribeStreaming(mUserJoin.getValue());
    }


    public void startPublsh() {
        if (mUserJoin.getValue() == null) return;
        mBaiduRtcRoom.startPublish();
        mBaiduRtcRoom.subscribeStreaming(0, mUserJoin.getValue());
    }


    public void sendMessageToUser(String msg) {
        if (mUserJoin.getValue() == null) return;
        mBaiduRtcRoom.sendMessageToUser(msg, mUserJoin.getValue());
        XLog.e("sendMessage:", mUserJoin.getValue() + "--" + msg);
    }


    public void startPreview() {
        mBaiduRtcRoom.startPreview();
    }


    public void stopPreview() {
        mBaiduRtcRoom.stopPreview();
    }


    public void muteCamera(boolean mute) {
        mBaiduRtcRoom.muteCamera(mute);
    }


    public void muteMicphone(boolean mute) {
        mBaiduRtcRoom.muteMicphone(mute);
    }


    public void switchCamera() {
        mBaiduRtcRoom.switchCamera();
    }


    public void switchLoundSpeaker() {
        mBaiduRtcRoom.switchLoundSpeaker();
    }


    public void logoutRtcRoom() {
        mBaiduRtcRoom.logoutRtcRoom();
        mBaiduRtcRoom.destroy();
        stopMusic();
    }

}
