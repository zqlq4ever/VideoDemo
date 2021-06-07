package com.luqian.videodemo;

import android.os.Bundle;

import com.baidu.rtc.RTCVideoView;
import com.baidu.rtc.videoroom.R;

/**
 * 聊天室模式
 */
public class RoomActivity extends RtcBaseActivity {

    private static final String TAG = "RoomActivity";
//    private boolean[] viewRecord = new boolean[]{false,false,false,false,false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoroom);
        mVideoRoom.setLocalDisplay(findViewById(R.id.local_rtc_video_view));

        RTCVideoView[] vg = new RTCVideoView[5];
        vg[0] = findViewById(R.id.remote_rtc_video_view);
        vg[1] = findViewById(R.id.remote_rtc_video_view1);
        vg[2] = findViewById(R.id.remote_rtc_video_view2);
        vg[3] = findViewById(R.id.remote_rtc_video_view3);
        vg[4] = findViewById(R.id.remote_rtc_video_view4);
        mVideoRoom.setRemoteDisplayGroup(vg);

        eventListener = new RtcEventListener() {
            @Override
            public void onRemoteComming(long userId) {
//                for (int i = 0;i < viewRecord.length;i++) {
//                    if (!viewRecord[i]) {
//                        mVideoRoom.setRemoteDisplay(vg[i],userId);
//                        viewRecord[i] = true;
//                        break;
//                    }
//                }
            }

            @Override
            public void onRtcRoomEeventLoginOk() {
//                RTCVideoView[] vg = new RTCVideoView[5];
//                vg[0] = (RTCVideoView) findViewById(R.id.remote_rtc_video_view);
//                vg[1] = (RTCVideoView) findViewById(R.id.remote_rtc_video_view1);
//                vg[2] = (RTCVideoView) findViewById(R.id.remote_rtc_video_view2);
//                vg[3] = (RTCVideoView) findViewById(R.id.remote_rtc_video_view3);
//                vg[4] = (RTCVideoView) findViewById(R.id.remote_rtc_video_view4);
//                mVideoRoom.setRemoteDisplayGroup(vg);
            }
        };
        loginRtc();
    }
}