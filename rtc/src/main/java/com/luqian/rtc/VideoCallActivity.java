package com.luqian.rtc;

import android.app.AlertDialog;
import android.os.Bundle;

import com.baidu.rtc.videoroom.R;
import com.gyf.immersionbar.ImmersionBar;
import com.luqian.rtc.widget.dialog.CallPop;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * 1 v 1 音视频
 */
public class VideoCallActivity extends RtcBaseActivity implements RtcDelegate.CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private AlertDialog receiveDialog;
    private CallPop mCallPop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this)
                .fitsSystemWindows(true)  //    使用该属性,必须指定状态栏颜色
                .statusBarColor(R.color.black)
                .init();

        RtcDelegate.CallConfig config = new RtcDelegate.CallConfig();
        rtcDelegate = new RtcDelegate(this, config);
        setContentView(R.layout.activity_videocall);

        mIvCall = findViewById(R.id.iv_call);
        if (mCallMode) {
            mIvCall.setImageResource(R.drawable.ic_start_call);
            mIvCall.setOnClickListener(view -> {
                if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.kStable) {
                    rtcDelegate.startCall();
                    mIvCall.setImageResource(R.drawable.btn_end_call);

                    showCallPop();

                } else if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.kCalling) {
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


    private void showCallPop() {
        dismissCall();
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


    public void dismissCall() {
        if (mCallPop != null && mCallPop.isShow()) {
            mCallPop.dismiss();
        }
    }


    @Override
    public void onStateChange(RtcDelegate.CallState state, RtcDelegate.CallRole role,
                              RtcDelegate.CallCommand reasonCommand, RtcDelegate.CallRole commandSource) {
        runOnUiThread(() -> {
            switch (state) {
                case kStable:
                    if (role == commandSource && reasonCommand == RtcDelegate.CallCommand.kCancel) {
                        if (role == RtcDelegate.CallRole.kSender) {
                            dismissCall();
                        } else {
                            receiveDialog.dismiss();
                        }
                        toast(R.string.canceled_call);
                    } else if (role != commandSource && reasonCommand == RtcDelegate.CallCommand.kCancel) {
                        if (role == RtcDelegate.CallRole.kSender) {
                            dismissCall();
                            toast(R.string.refused_call);
                        } else {
                            receiveDialog.dismiss();
                            toast(R.string.other_canceled_call);
                        }
                    } else if (role == RtcDelegate.CallRole.kSender &&
                            reasonCommand == RtcDelegate.CallCommand.kRequestTimeout) {
                        dismissCall();
                        toast(R.string.not_response_end);
                    } else if (role == RtcDelegate.CallRole.kSender &&
                            reasonCommand == RtcDelegate.CallCommand.kBusyHere) {
                        dismissCall();
                        toast(R.string.in_call_please_wait);
                    } else if (role == commandSource && reasonCommand == RtcDelegate.CallCommand.kFinish) {
                        toast(R.string.end_call_over);
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role != commandSource && reasonCommand == RtcDelegate.CallCommand.kFinish) {
                        toast(R.string.other_end_call_over);
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role == RtcDelegate.CallRole.kReceiver &&
                            reasonCommand == RtcDelegate.CallCommand.kRequestTimeout) {
                        receiveDialog.dismiss();
                        toast(R.string.timeout_end);
                    }
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    break;
                case kInviting:
                    break;
                case kRinging:
                    if (role == RtcDelegate.CallRole.kReceiver) {
                        onReceiveInvite();
                    } else {
                        mCallPop.setData(getString(R.string.wait_accept));
                    }
                    break;
                case kCalling:
                    if (role == RtcDelegate.CallRole.kSender) {
                        dismissCall();
                        toast(R.string.accept_establish);
                    } else {
                        toast(R.string.call_establish_ing);
                    }
                    mIvCall.setImageResource(R.drawable.btn_end_call);
                    mVideoRoom.startPublish();
                    mVideoRoom.subscribeStreaming(0, partnerId);
                    break;
                case kReceiveInviting:
                    break;
            }
        });
    }


    @Override
    public void sendMessage(String msg) {
        runOnUiThread(() -> mVideoRoom.sendMessageToUser(msg, partnerId));
    }


    /**
     * 接收到同一个房间 1v1 呼叫
     */
    private void onReceiveInvite() {
        runOnUiThread(() -> {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(VideoCallActivity.this);
            dialog.setTitle(R.string.receive_call);
            dialog.setMessage(R.string.make_sure_accept_call);
            dialog.setPositiveButton(R.string.answer, (dialogInterface, i) -> rtcDelegate.receiveCall());
            dialog.setNegativeButton(R.string.refuse_call, (dialogInterface, i) -> rtcDelegate.cancelCall());
            receiveDialog = dialog.show();
        });
    }


    @Override
    public void onBackPressed() {
        if (mCallMode) {
            if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.kCalling) {
                rtcDelegate.finishCall();
            }
            mIvCall.setImageResource(R.drawable.ic_start_call);
        }
        super.onBackPressed();
    }
}
