package com.luqian.videodemo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.rtc.RTCVideoView;
import com.baidu.rtc.videoroom.R;


public class VideoCallActivity extends RtcBaseActivity implements VideoCallProtocol.CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private AlertDialog callDialog;
    private AlertDialog receiveDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoCallProtocol.CallConfig config = new VideoCallProtocol.CallConfig();
        callProtocol = new VideoCallProtocol(this, config);

        setContentView(R.layout.activity_videocall);
        callBtnImage = (ImageView) findViewById(R.id.call_imageview);
        if (callMode) {
            callBtnImage.setImageResource(R.drawable.btn_start_call);
            callBtnImage.setOnClickListener(view -> {
                if (callProtocol.getCurrentState() == VideoCallProtocol.CallState.kStable) {
                    callProtocol.startCall();
                    callBtnImage.setImageResource(R.drawable.btn_end_call);

                    final AlertDialog.Builder dialog = new AlertDialog.Builder(VideoCallActivity.this);
                    dialog.setTitle(R.string.start_call);
                    dialog.setMessage(R.string.in_calling_state);
                    dialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> callProtocol.cancelCall());
                    callDialog = dialog.show();

                } else if (callProtocol.getCurrentState() == VideoCallProtocol.CallState.kCalling) {
                    callProtocol.finishCall();
                    callBtnImage.setImageResource(R.drawable.btn_start_call);
                } else {
                    callProtocol.cancelCall();
                    callBtnImage.setImageResource(R.drawable.btn_start_call);
                }
            });
        }

        mVideoRoom.setLocalDisplay((RTCVideoView) findViewById(R.id.local_rtc_video_view));
        mVideoRoom.setRemoteDisplay((RTCVideoView) findViewById(R.id.remote_rtc_video_view));
        doLogin();
    }


    @Override
    public void onStateChange(VideoCallProtocol.CallState state, VideoCallProtocol.CallRole role,
                              VideoCallProtocol.CallCommand reasonCommand, VideoCallProtocol.CallRole commandSource) {
        runOnUiThread(() -> {
            switch (state) {
                case kStable:
                    if (role == commandSource && reasonCommand == VideoCallProtocol.CallCommand.kCancel) {
                        if (role == VideoCallProtocol.CallRole.kSender)
                            callDialog.dismiss();
                        else
                            receiveDialog.dismiss();
                        Toast.makeText(getApplicationContext(), R.string.canceled_call, Toast.LENGTH_LONG).show();
                    } else if (role != commandSource && reasonCommand == VideoCallProtocol.CallCommand.kCancel) {
                        if (role == VideoCallProtocol.CallRole.kSender) {
                            callDialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    R.string.refused_call, Toast.LENGTH_LONG).show();
                        } else {
                            receiveDialog.dismiss();
                            Toast.makeText(getApplicationContext(),
                                    R.string.other_canceled_call, Toast.LENGTH_LONG).show();
                        }
                    } else if (role == VideoCallProtocol.CallRole.kSender &&
                            reasonCommand == VideoCallProtocol.CallCommand.kRequestTimeout) {
                        callDialog.dismiss();
                        Toast.makeText(getApplicationContext(), R.string.not_response_end, Toast.LENGTH_LONG).show();
                    } else if (role == VideoCallProtocol.CallRole.kSender &&
                            reasonCommand == VideoCallProtocol.CallCommand.kBusyHere) {
                        callDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                R.string.in_call_please_wait, Toast.LENGTH_LONG).show();
                    } else if (role == commandSource && reasonCommand == VideoCallProtocol.CallCommand.kBye) {
                        Toast.makeText(getApplicationContext(), R.string.end_call_over, Toast.LENGTH_LONG).show();
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role != commandSource && reasonCommand == VideoCallProtocol.CallCommand.kBye) {
                        Toast.makeText(getApplicationContext(),
                                R.string.other_end_call_over, Toast.LENGTH_LONG).show();
                        mVideoRoom.stopPublish();
                        mVideoRoom.stopSubscribeStreaming(partnerId);
                    } else if (role == VideoCallProtocol.CallRole.kReceiver &&
                            reasonCommand == VideoCallProtocol.CallCommand.kRequestTimeout) {
                        receiveDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                R.string.timeout_end, Toast.LENGTH_LONG).show();
                    }
                    callBtnImage.setImageResource(R.drawable.btn_start_call);
                    break;
                case kInviting:
                    break;
                case kRinging:
                    if (role == VideoCallProtocol.CallRole.kReceiver) {
                        onReceiveInvite();
                    } else {
                        callDialog.setMessage(getString(R.string.wait_accept));
                    }
                    break;
                case kCalling:
                    if (role == VideoCallProtocol.CallRole.kSender) {
                        callDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                R.string.accept_establish, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.call_establish_ing, Toast.LENGTH_SHORT).show();
                    }
                    callBtnImage.setImageResource(R.drawable.btn_end_call);
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
            dialog.setPositiveButton(R.string.answer, (dialogInterface, i) -> callProtocol.receiveCall());
            dialog.setNegativeButton(R.string.refuse_call, (dialogInterface, i) -> callProtocol.cancelCall());
            receiveDialog = dialog.show();
        });
    }
}
