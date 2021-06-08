package com.luqian.rtc;

import android.os.Bundle;

import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;
import com.gyf.immersionbar.ImmersionBar;
import com.luqian.rtc.widget.dialog.CallPop;
import com.luqian.rtc.widget.dialog.ReceivedCallPop;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * 1 v 1 音视频
 */
public class VideoActivity extends RtcBaseActivity implements RtcDelegate.CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private CallPop mCallPop;
    private ReceivedCallPop mReceivedCallPop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this)
                .fitsSystemWindows(true)  //    使用该属性,必须指定状态栏颜色
                .statusBarColor(R.color.black)
                .init();

        RtcDelegate.CallConfig config = new RtcDelegate.CallConfig();
        rtcDelegate = new RtcDelegate(this, config);
        setContentView(R.layout.activity_video);

        mIvCall = findViewById(R.id.iv_call);
        if (mCallMode) {
            mIvCall.setImageResource(R.drawable.ic_start_call);
            mIvCall.setOnClickListener(view -> {
                if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.STABLE) {
                    rtcDelegate.startCall();
                    mIvCall.setImageResource(R.drawable.btn_end_call);

                    showCallPop();

                } else if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.CALLING) {
                    rtcDelegate.finishCall();
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                } else {
                    rtcDelegate.cancelCall();
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                }
            });
        }

        mVideoRoom.setLocalDisplay(findViewById(R.id.local_rtc_video_view));
        mVideoRoom.setRemoteDisplay(findViewById(R.id.remote_rtc_video_view));

        loginRtc();
    }


    public void cancelCall() {
        rtcDelegate.cancelCall();
    }


    public void receiveCall() {
        rtcDelegate.receiveCall();
    }


    private void showCallPop() {
        dismissCall();
        playMusic();
        if (mCallPop == null) {
            mCallPop = (CallPop) new XPopup.Builder(this)
                    .hasStatusBar(false)
                    .hasNavigationBar(false)
                    .dismissOnTouchOutside(false)
                    .popupAnimation(PopupAnimation.NoAnimation)
                    .asCustom(new CallPop(this));
        }
        mCallPop.show();
    }


    private void showReceivePop() {
        dismissCall();
        playMusic();
        if (mReceivedCallPop == null) {
            mReceivedCallPop = (ReceivedCallPop) new XPopup.Builder(this)
                    .hasStatusBar(false)
                    .hasNavigationBar(false)
                    .dismissOnTouchOutside(false)
                    .popupAnimation(PopupAnimation.NoAnimation)
                    .asCustom(new ReceivedCallPop(this));
        }
        mReceivedCallPop.show();
    }


    public void dismissCall() {
        if (mCallPop != null && mCallPop.isShow()) {
            mCallPop.dismiss();
        }
        stopMusic();
    }

    public void dismissReceive() {
        if (mReceivedCallPop != null && mReceivedCallPop.isShow()) {
            mReceivedCallPop.dismiss();
        }
        stopMusic();
    }


    @Override
    public void onStateChange(RtcDelegate.CallState state,
                              RtcDelegate.CallRole role,
                              RtcDelegate.CallCommand reasonCommand,
                              RtcDelegate.CallRole commandSource) {

        runOnUiThread(() -> {
            switch (state) {
                case STABLE:
                    if (role == commandSource && reasonCommand == RtcDelegate.CallCommand.CANCEL) {
                        if (role == RtcDelegate.CallRole.SENDER) {
                            dismissCall();
                        } else {
                            dismissReceive();
                        }
                        toast(R.string.canceled_call);
                    } else if (role != commandSource && reasonCommand == RtcDelegate.CallCommand.CANCEL) {
                        if (role == RtcDelegate.CallRole.SENDER) {
                            dismissCall();
                            toast(R.string.refused_call);
                        } else {
                            dismissReceive();
                            toast(R.string.other_canceled_call);
                        }
                    } else if (role == RtcDelegate.CallRole.SENDER && reasonCommand == RtcDelegate.CallCommand.REQUEST_TIMEOUT) {
                        dismissCall();
                        toast(R.string.not_response_end);
                    } else if (role == RtcDelegate.CallRole.SENDER && reasonCommand == RtcDelegate.CallCommand.BUSY_HERE) {
                        dismissCall();
                        toast(R.string.in_call_please_wait);
                    } else if (role == commandSource && reasonCommand == RtcDelegate.CallCommand.FINISH) {
                        toast(R.string.end_call_over);
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role != commandSource && reasonCommand == RtcDelegate.CallCommand.FINISH) {
                        toast(R.string.other_end_call_over);
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role == RtcDelegate.CallRole.RECEIVER && reasonCommand == RtcDelegate.CallCommand.REQUEST_TIMEOUT) {
                        dismissReceive();
                        toast(R.string.timeout_end);
                    }
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    break;
                case INVITING:
                    break;
                case RINGING:
                    if (role == RtcDelegate.CallRole.RECEIVER) {
                        onReceiveInvite();
                    } else {
                        mCallPop.setData(getString(R.string.wait_accept));
                    }
                    break;
                case CALLING:
                    if (role == RtcDelegate.CallRole.SENDER) {
                        dismissCall();
                        toast(R.string.accept_establish);
                    } else {
                        toast(R.string.call_establish_ing);
                    }
                    mIvCall.setImageResource(R.drawable.btn_end_call);
                    mVideoRoom.startPublish();
                    mVideoRoom.subscribeStreaming(0, partnerId);
                    break;
                case RECEIVE_INVITING:
                    break;
            }
        });
    }


    @Override
    public void sendMessage(String msg) {
        runOnUiThread(() -> {
            mVideoRoom.sendMessageToUser(msg, partnerId);
            XLog.e("sendMessage:", partnerId + "--" + msg);
        });
    }


    /**
     * 接收到同一个房间 1v1 呼叫
     */
    private void onReceiveInvite() {
        runOnUiThread(this::showReceivePop);
    }


    @Override
    public void onBackPressed() {
        if (mCallMode) {
            if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.CALLING) {
                rtcDelegate.finishCall();
            }
            mIvCall.setImageResource(R.drawable.ic_start_call);
        }
        super.onBackPressed();
    }
}
