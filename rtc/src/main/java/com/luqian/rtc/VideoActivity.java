package com.luqian.rtc;

import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;
import com.gyf.immersionbar.ImmersionBar;
import com.luqian.rtc.widget.dialog.CallPop;
import com.luqian.rtc.widget.dialog.ReceivedCallPop;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.impl.LoadingPopupView;

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
                //  使用该属性,必须指定状态栏颜色
                .fitsSystemWindows(true)
                .statusBarColor(R.color.black)
                .init();
        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(this, R.color.black));
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

        rtcDelegate = new RtcDelegate(this);

        LoadingPopupView loadingPopup = (LoadingPopupView) new XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(false)
                .asLoading("加载中")
                .show();
        loadingPopup.delayDismissWith(1500L, this::loginRtc);
    }


    @Override
    void loginRtcSuccess() {
        mVideoRoom.setLocalDisplay(findViewById(R.id.local_rtc_video_view));
        mVideoRoom.setRemoteDisplay(findViewById(R.id.remote_rtc_video_view));
        findViewById(R.id.root).setVisibility(View.VISIBLE);
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
                    .dismissOnBackPressed(false)
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
        stopMusic();
    }

    public void dismissReceive() {
        if (mReceivedCallPop != null && mReceivedCallPop.isShow()) {
            mReceivedCallPop.dismiss();
        }
        stopMusic();
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
            XLog.d(currentState.toString());

            //  指令是自己发出
            boolean commandFromMe = role == commandSource;
            //  指令是他人发出
            boolean commandFromOther = role != commandSource;

            boolean isSender = role == RtcDelegate.CallRole.SENDER;

            boolean isReceive = role == RtcDelegate.CallRole.RECEIVER;

            //  不同呼叫状态做处理
            switch (currentState) {
                case STABLE:
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
                            mVideoRoom.stopPublish();
                            mVideoRoom.stopSubscribeStreaming(partnerId);
                        } else if (commandFromOther && command == RtcDelegate.CallCommand.FINISH) {
                            toast(R.string.other_end_call_over);
                            mVideoRoom.stopPublish();
                            mVideoRoom.stopSubscribeStreaming(partnerId);
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
