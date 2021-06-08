package com.luqian.videodemo;

import android.app.AlertDialog;
import android.os.Bundle;

import com.baidu.rtc.videoroom.R;
import com.gyf.immersionbar.ImmersionBar;

/**
 * 1 v 1 音视频
 */
public class VideoCallActivity extends RtcBaseActivity implements RtcDelegate.CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private AlertDialog callDialog;
    private AlertDialog receiveDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).init();

        RtcDelegate.CallConfig config = new RtcDelegate.CallConfig();
        rtcDelegate = new RtcDelegate(this, config);
        setContentView(R.layout.activity_videocall);

        ivCall = findViewById(R.id.iv_call);
        if (callMode) {
            ivCall.setImageResource(R.drawable.ic_start_call);
            ivCall.setOnClickListener(view -> {
                if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.kStable) {
                    rtcDelegate.startCall();
                    ivCall.setImageResource(R.drawable.btn_end_call);

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(VideoCallActivity.this);
                    dialog.setTitle(R.string.start_call);
                    dialog.setMessage(R.string.in_calling_state);
                    dialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> rtcDelegate.cancelCall());
                    callDialog = dialog.show();

                } else if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.kCalling) {
                    rtcDelegate.finishCall();
                    ivCall.setImageResource(R.drawable.ic_start_call);
                } else {
                    rtcDelegate.cancelCall();
                    ivCall.setImageResource(R.drawable.ic_start_call);
                }
            });
        }

        mVideoRoom.setLocalDisplay(findViewById(R.id.local_rtc_video_view));
        mVideoRoom.setRemoteDisplay(findViewById(R.id.remote_rtc_video_view));

        loginRtc();
    }


    @Override
    public void onStateChange(RtcDelegate.CallState state, RtcDelegate.CallRole role,
                              RtcDelegate.CallCommand reasonCommand, RtcDelegate.CallRole commandSource) {
        runOnUiThread(() -> {
            switch (state) {
                case kStable:
                    if (role == commandSource && reasonCommand == RtcDelegate.CallCommand.kCancel) {
                        if (role == RtcDelegate.CallRole.kSender) {
                            callDialog.dismiss();
                        } else {
                            receiveDialog.dismiss();
                        }
                        toast(R.string.canceled_call);
                    } else if (role != commandSource && reasonCommand == RtcDelegate.CallCommand.kCancel) {
                        if (role == RtcDelegate.CallRole.kSender) {
                            callDialog.dismiss();
                            toast(R.string.refused_call);
                        } else {
                            receiveDialog.dismiss();
                            toast(R.string.other_canceled_call);
                        }
                    } else if (role == RtcDelegate.CallRole.kSender &&
                            reasonCommand == RtcDelegate.CallCommand.kRequestTimeout) {
                        callDialog.dismiss();
                        toast(R.string.not_response_end);
                    } else if (role == RtcDelegate.CallRole.kSender &&
                            reasonCommand == RtcDelegate.CallCommand.kBusyHere) {
                        callDialog.dismiss();
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
                    ivCall.setImageResource(R.drawable.ic_start_call);
                    break;
                case kInviting:
                    break;
                case kRinging:
                    if (role == RtcDelegate.CallRole.kReceiver) {
                        onReceiveInvite();
                    } else {
                        callDialog.setMessage(getString(R.string.wait_accept));
                    }
                    break;
                case kCalling:
                    if (role == RtcDelegate.CallRole.kSender) {
                        callDialog.dismiss();
                        toast(R.string.accept_establish);
                    } else {
                        toast(R.string.call_establish_ing);
                    }
                    ivCall.setImageResource(R.drawable.btn_end_call);
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
    protected void onDestroy() {
        super.onDestroy();

    }
}
