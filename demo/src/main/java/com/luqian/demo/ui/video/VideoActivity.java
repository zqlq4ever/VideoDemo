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

        mViewModel = new ViewModelProvider(this).get(VideoViewModel.class);

        String userid = getIntent().getStringExtra("userid");
        String roomName = getIntent().getStringExtra("roomname");

        mIvCall = findViewById(R.id.iv_call);

        mIvCall.setImageResource(R.drawable.ic_start_call);
        mIvCall.setOnClickListener(view -> {
            if (mCallManager.getCurrentState() == CallManager.CallState.NORMAL) {
                mCallManager.startCall();
                mIvCall.setImageResource(R.drawable.btn_end_call);

                showCallPop();

            } else if (mCallManager.getCurrentState() == CallManager.CallState.CALLING) {
                mCallManager.finishCall();
                mIvCall.setImageResource(R.drawable.ic_start_call);
            } else {
                mCallManager.cancelCall();
                mIvCall.setImageResource(R.drawable.ic_start_call);
            }
        });

        mViewModel.InitRTCRoom();
        mCallManager = new CallManager(this);
        mViewModel.loginRtc(roomName, Long.parseLong(userid), "");

        mLoadingPopup = (LoadingPopupView) new XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(false)
                .asLoading("加载中")
                .show();

        initLiveData();
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
            mCallManager.onMessage(msg);
        });

        mViewModel.mUserLeave.observe(this, userId -> {
            toast("对方已退出通话");
            finish();
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
    public void onStateChange(CallManager.CallState currentState,
                              CallManager.CallRole role,
                              CallManager.CallCommand command,
                              CallManager.CallRole commandSource) {

        runOnUiThread(() -> {
            XLog.d("onStateChange", currentState.toString());

            //  指令是自己发出
            boolean commandFromMe = role == commandSource;
            //  指令是他人发出
            boolean commandFromOther = role != commandSource;

            boolean isSender = role == CallManager.CallRole.SENDER;

            boolean isReceive = role == CallManager.CallRole.RECEIVER;

            //  不同呼叫状态做处理
            switch (currentState) {
                case NORMAL:
                    if (commandFromMe && command == CallManager.CallCommand.CANCEL) {
                        if (isSender) {
                            dismissCall();
                        } else {
                            dismissReceive();
                        }
                        toast(R.string.canceled_call);
                    } else {
                        if (commandFromOther && command == CallManager.CallCommand.CANCEL) {
                            if (isSender) {
                                dismissCall();
                                toast(R.string.refused_call);
                            } else {
                                dismissReceive();
                                toast(R.string.other_canceled_call);
                            }
                        } else if (isSender && command == CallManager.CallCommand.REQUEST_TIMEOUT) {
                            //  发送方，呼叫超时
                            dismissCall();
                            toast(R.string.not_response_end);
                        } else if (isSender && command == CallManager.CallCommand.BUSY_HERE) {
                            dismissCall();
                            toast(R.string.in_call_please_wait);
                        } else if (commandFromMe && command == CallManager.CallCommand.FINISH) {
                            toast(R.string.end_call_over);
                            mViewModel.stopPublsh();
                        } else if (commandFromOther && command == CallManager.CallCommand.FINISH) {
                            toast(R.string.other_end_call_over);
                            mViewModel.stopPublsh();
                        } else if (isReceive && command == CallManager.CallCommand.REQUEST_TIMEOUT) {
                            dismissReceive();
                            toast(R.string.timeout_end);
                        }
                    }
                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    mViewModel.stopMusic();
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
        if (mCallManager.getCurrentState() == CallManager.CallState.CALLING) {
            mCallManager.finishCall();
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
