package com.luqian.demo.ui.video;

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
import com.luqian.demo.widget.dialog.CallPop;
import com.luqian.demo.widget.dialog.ReceivedCallPop;
import com.luqian.rtc.CallManager;
import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallRole;
import com.luqian.rtc.bean.CallState;
import com.luqian.rtc.common.CallStateObserver;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.impl.LoadingPopupView;

/**
 * 1 v 1 音视频通话
 */
public class VideoActivity extends AppCompatActivity implements CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private CallPop mCallPop;
    private ReceivedCallPop mReceivedCallPop;
    private ImageView mIvCall;
    private ImageView mIvAudio;
    private ImageView mIvSpeaker;
    private ImageView mIvVideo;
    private ImageView mIvCamera;
    public VideoViewModel mViewModel;
    public CallManager mCallManager;
    private LoadingPopupView mLoadingPopup;


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

        mIvCall = findViewById(R.id.iv_call);

        mIvAudio = findViewById(R.id.iv_audio);

        mIvSpeaker = findViewById(R.id.iv_speaker);

        mIvVideo = findViewById(R.id.iv_video);

        mIvCamera = findViewById(R.id.iv_camera);

        mViewModel = new ViewModelProvider(this).get(VideoViewModel.class);

        String userid = getIntent().getStringExtra("userid");

        String roomName = getIntent().getStringExtra("roomname");

        mIvCall.setOnClickListener(view -> {
            if (mCallManager.getCurrentState() == CallState.NORMAL) {

                mCallManager.startCall();

                mIvCall.setImageResource(R.drawable.btn_end_call);

                showCallPop();

            } else if (mCallManager.getCurrentState() == CallState.CALLING) {
                mCallManager.finishCall();
                mIvCall.setImageResource(R.drawable.ic_start_call);
            } else {
                mCallManager.cancelCall();
                mIvCall.setImageResource(R.drawable.ic_start_call);
            }
        });

        mIvAudio.setOnClickListener(this::muteMicphone);

        mIvSpeaker.setOnClickListener(this::switchSpeaker);

        mIvVideo.setOnClickListener(this::muteVideo);

        mIvCamera.setOnClickListener(this::switchCamera);

        mCallManager = new CallManager(this);

        mViewModel.InitRTCRoom();

        mViewModel.loginRtc(roomName, Long.parseLong(userid), "");

        showLoading();

        initLiveData();
    }


    private void showLoading() {
        mLoadingPopup = (LoadingPopupView) new XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(false)
                .asLoading("加载中")
                .show();
    }


    private void initLiveData() {

        mViewModel.loginStatus.observe(this, rtcLoginStatus -> {
            if (rtcLoginStatus.isSuccess()) {
                mViewModel.setLocalDisplay(findViewById(R.id.local_rtc_video_view));
                mViewModel.setRemoteDisplay(findViewById(R.id.remote_rtc_video_view));
                findViewById(R.id.root).setVisibility(View.VISIBLE);
                dismiss();
            } else {
                toast(rtcLoginStatus.getMsg());
                finish();
            }
        });

        mViewModel.mUserMsg.observe(this, msg -> {
            mCallManager.onRtcUserCommandMessage(msg);
        });

        mViewModel.mUserLeave.observe(this, userId -> {
            toast("对方已退出通话");
            finish();
        });

        mViewModel.mErrorInfo.observe(this, errorInfo -> {

        });
    }


    public void dismiss() {
        if (mLoadingPopup != null && mLoadingPopup.isShow()) {
            mLoadingPopup.dismiss();
        }
    }


    public void cancelCall() {
        mCallManager.cancelCall();
    }


    public void receiveCall() {
        mCallManager.receiveCall();
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


    /**
     * 弹出来电提醒框
     */
    private void showReceivePop() {

        dismissReceive();

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
        Long userId = mViewModel.mUserJoin.getValue();
        if (userId != null) {
            mReceivedCallPop.setData("收到" + userId + "的来电");
        }

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
     * @param currentState 当前呼叫状态
     * @param role         呼叫角色：拨号方 / 接听方
     * @param command      状态改变原因指令
     * @param commandFrom  指令来源：拨号方发出 / 接听方发出
     */
    @Override
    public void onStateChange(CallState currentState,
                              CallRole role,
                              CallCommand command,
                              CallRole commandFrom) {

        runOnUiThread(() -> {
            XLog.d("onStateChange", currentState.toString());

            //  指令是自己发出
            boolean commandFromMe = role == commandFrom;

            boolean isSender = role == CallRole.SENDER;

            boolean isReceiver = role == CallRole.RECEIVER;

            //  不同通话状态做处理
            switch (currentState) {
                //  切换为常态
                case NORMAL:
                    //  切换原因
                    switch (command) {
                        //  取消
                        case CANCEL:
                            if (commandFromMe) {
                                if (isSender) {
                                    dismissCall();
                                } else {
                                    dismissReceive();
                                }
                                toast(R.string.canceled_call);
                            } else {
                                if (isSender) {
                                    dismissCall();
                                    toast(R.string.refused_call);
                                } else {
                                    dismissReceive();
                                    toast(R.string.other_canceled_call);
                                }
                            }
                            break;
                        //  超时
                        case TIMEOUT:
                            if (isReceiver) {
                                dismissReceive();
                                toast(R.string.not_response_end);
                            } else {
                                dismissCall();
                                toast(R.string.timeout_end);
                            }
                            break;
                        //  对方正忙
                        case OTHER_BUSY:
                            dismissCall();
                            toast(R.string.in_call_please_wait);
                            break;
                        //  结束通话
                        case FINISH:
                            if (commandFromMe) {
                                toast(R.string.end_call_over);
                            } else {
                                toast(R.string.other_end_call_over);
                            }
                            mViewModel.stopPublsh();
                            break;
                    }

                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    mViewModel.stopMusic();
                    break;
                //  切换为响铃中
                case RINGING:
                    //  接收方，弹窗提醒接听
                    if (isReceiver) {
                        onReceiveInvite();
                    }
                    break;
                //  切换为通话中
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
            }
        });
    }


    @Override
    public void sendMessageToUser(String msg) {
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
        /*if (mCallManager.getCurrentState() == CallState.CALLING) {
            mCallManager.finishCall();
        }
        mIvCall.setImageResource(R.drawable.ic_start_call);
        super.onBackPressed();*/
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


    /**
     * 开启 / 关闭 本地视频
     */
    public void muteVideo(View view) {

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
    public void muteMicphone(View view) {
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
    public void switchCamera(View view) {
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
    public void switchSpeaker(View view) {
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
