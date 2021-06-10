package com.luqian.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;
import com.gyf.immersionbar.ImmersionBar;
import com.luqian.demo.dialog.CallPop;
import com.luqian.demo.dialog.ReceivedCallPop;
import com.luqian.rtc.RtcDelegate;
import com.luqian.rtc.common.CallStateObserver;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.impl.LoadingPopupView;

/**
 * 1 v 1 音视频
 */
public class VideoActivity extends AppCompatActivity implements CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private CallPop mCallPop;
    private ReceivedCallPop mReceivedCallPop;
    private ImageView mIvCall;
    public VideoViewModel mViewModel;
    public RtcDelegate rtcDelegate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImmersionBar.with(this)
                //  使用该属性,必须指定状态栏颜色
                .fitsSystemWindows(true)
                .statusBarColor(R.color.black)
                .init();
        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(this, R.color.black));

        setContentView(R.layout.activity_video);

        mViewModel = new ViewModelProvider(this).get(VideoViewModel.class);

        String userid = getIntent().getStringExtra("userid");
        String roomName = getIntent().getStringExtra("roomname");

        mIvCall = findViewById(R.id.iv_call);

        mIvCall.setImageResource(R.drawable.ic_start_call);
        mIvCall.setOnClickListener(view -> {
            if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.NORMAL) {
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

        rtcDelegate = new RtcDelegate(this);
        mViewModel.InitRTCRoom();

        LoadingPopupView loadingPopup = (LoadingPopupView) new XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(false)
                .asLoading("加载中")
                .show();
        loadingPopup.delayDismissWith(1500L, () -> {
            mViewModel.loginRtc(roomName, Long.parseLong(userid), "");
        });

        initLiveData();
    }

    private void initLiveData() {
        mViewModel.loginStatus.observe(this, rtcLoginStatus -> {
            if (rtcLoginStatus.isSuccess()) {
                mViewModel.mBaiduRtcRoom.setLocalDisplay(findViewById(R.id.local_rtc_video_view));
                mViewModel.mBaiduRtcRoom.setRemoteDisplay(findViewById(R.id.remote_rtc_video_view));
                findViewById(R.id.root).setVisibility(View.VISIBLE);
            }
        });
        mViewModel.mUserMsg.observe(this, msg -> {
            rtcDelegate.onMessage(msg);
        });
        mViewModel.mUserJoin.observe(this, userId -> {

        });
        mViewModel.mErrorInfo.observe(this, userId -> {

        });
    }


    public void cancelCall() {
        rtcDelegate.cancelCall();
    }


    public void receiveCall() {
        rtcDelegate.receiveCall();
    }


    private void showCallPop() {
        dismissCall();
        mViewModel.playMusic();
        if (mCallPop == null) {
            mCallPop = (CallPop) new XPopup.Builder(this)
                    .hasStatusBar(false)
                    .hasNavigationBar(false)
                    .dismissOnTouchOutside(false)
                    .dismissOnBackPressed(false)
                    .popupAnimation(PopupAnimation.NoAnimation)
                    .asCustom(new CallPop(this));
        }
        mCallPop.show();
    }


    private void showReceivePop() {
        dismissCall();
        mViewModel.playMusic();
        if (mReceivedCallPop == null) {
            mReceivedCallPop = (ReceivedCallPop) new XPopup.Builder(this)
                    .hasStatusBar(false)
                    .hasNavigationBar(false)
                    .dismissOnTouchOutside(false)
                    .dismissOnBackPressed(false)
                    .popupAnimation(PopupAnimation.NoAnimation)
                    .asCustom(new ReceivedCallPop(this));
        }
        mReceivedCallPop.show();
    }


    public void dismissCall() {
        if (mCallPop != null && mCallPop.isShow()) {
            mCallPop.dismiss();
        }
        mViewModel.stopMusic();
    }

    public void dismissReceive() {
        if (mReceivedCallPop != null && mReceivedCallPop.isShow()) {
            mReceivedCallPop.dismiss();
        }
        mViewModel.stopMusic();
    }


    /**
     * @param currentState  当前呼叫状态
     * @param role          呼叫角色：拨号方 / 接听方
     * @param command       状态改变原因指令
     * @param commandSource 指令来源：拨号方发出 / 接听方发出
     */
    @Override
    public void onStateChange(RtcDelegate.CallState currentState,
                              RtcDelegate.CallRole role,
                              RtcDelegate.CallCommand command,
                              RtcDelegate.CallRole commandSource) {

        runOnUiThread(() -> {
            XLog.d("onStateChange", currentState.toString());

            //  指令是自己发出
            boolean commandFromMe = role == commandSource;
            //  指令是他人发出
            boolean commandFromOther = role != commandSource;

            boolean isSender = role == RtcDelegate.CallRole.SENDER;

            boolean isReceive = role == RtcDelegate.CallRole.RECEIVER;

            //  不同呼叫状态做处理
            switch (currentState) {
                case NORMAL:
                    if (commandFromMe && command == RtcDelegate.CallCommand.CANCEL) {
                        if (isSender) {
                            dismissCall();
                        } else {
                            dismissReceive();
                        }
                        toast(R.string.canceled_call);
                    } else {
                        if (commandFromOther && command == RtcDelegate.CallCommand.CANCEL) {
                            if (isSender) {
                                dismissCall();
                                toast(R.string.refused_call);
                            } else {
                                dismissReceive();
                                toast(R.string.other_canceled_call);
                            }
                        } else if (isSender && command == RtcDelegate.CallCommand.REQUEST_TIMEOUT) {
                            //  发送方，呼叫超时
                            dismissCall();
                            toast(R.string.not_response_end);
                        } else if (isSender && command == RtcDelegate.CallCommand.BUSY_HERE) {
                            dismissCall();
                            toast(R.string.in_call_please_wait);
                        } else if (commandFromMe && command == RtcDelegate.CallCommand.FINISH) {
                            toast(R.string.end_call_over);
                            mViewModel.stopPublsh();
                        } else if (commandFromOther && command == RtcDelegate.CallCommand.FINISH) {
                            toast(R.string.other_end_call_over);
                            mViewModel.stopPublsh();
                        } else if (isReceive && command == RtcDelegate.CallCommand.REQUEST_TIMEOUT) {
                            dismissReceive();
                            toast(R.string.timeout_end);
                        }
                    }
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    break;
                case INVITING:
                    break;
                //  响铃
                case RINGING:
                    //  接收方，弹窗提醒接听
                    if (isReceive) {
                        onReceiveInvite();
                    } else {
                        //  拨号方，弹窗
                        mCallPop.setData(getString(R.string.wait_accept));
                    }
                    break;
                //  通话中
                case CALLING:
                    if (isSender) {
                        dismissCall();
                        toast(R.string.accept_establish);
                    } else {
                        toast(R.string.call_establish_ing);
                    }
                    mIvCall.setImageResource(R.drawable.btn_end_call);
                    mViewModel.startPublsh();
                    break;
                case RECEIVE_INVITING:
                    break;
            }
        });
    }


    @Override
    public void sendMessage(String msg) {
        runOnUiThread(() -> {
            mViewModel.sendMessageToUser(msg);
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
        if (rtcDelegate.getCurrentState() == RtcDelegate.CallState.CALLING) {
            rtcDelegate.finishCall();
        }
        mIvCall.setImageResource(R.drawable.ic_start_call);
        super.onBackPressed();
    }

    protected void toast(@StringRes int resId) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    protected void toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        mViewModel.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.stopPreview();
    }

    @Override
    protected void onDestroy() {
        mViewModel.logoutRtcRoom();
        super.onDestroy();
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
        mViewModel.muteCamera(iv.isSelected());

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
        mViewModel.muteMicphone(ivVoice.isSelected());
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
        mViewModel.switchCamera();
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
        mViewModel.switchLoundSpeaker();
    }
}
