package com.luqian.rtc.ui.video;

import static com.luqian.rtc.common.RtcConstant.ROUTER_RTC_VIDEO_RECEIVE;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.baidu.rtc.videoroom.R;
import com.elvishew.xlog.XLog;
import com.gyf.immersionbar.ImmersionBar;
import com.luqian.rtc.common.ReceiveManager;
import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallRole;
import com.luqian.rtc.bean.CallState;
import com.luqian.rtc.common.CallStateObserver;
import com.luqian.rtc.dialog.ReceivedCallPop;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * 接听电话页面
 */
@Route(path = ROUTER_RTC_VIDEO_RECEIVE)
public class VideoReceiveActivity extends BaseVideoActivtiy implements CallStateObserver {

    private static final String TAG = "VideoReceiveActivity";
    private ReceivedCallPop mReceivedCallPop;
    private ImageView mIvCall;
    public ReceiveManager mReceiveManager;

    @Autowired
    public String userid;
    @Autowired
    public String roomname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        setContentView(R.layout.activity_video_receive);

        ImmersionBar.with(this)
                .fitsSystemWindows(true)
                .statusBarColor(R.color.black)
                .init();

        ARouter.getInstance().inject(this);

        mIvCall = findViewById(R.id.iv_call);

        ImageView ivAudio = findViewById(R.id.iv_audio);

        ImageView ivSpeaker = findViewById(R.id.iv_speaker);

        ImageView ivVideo = findViewById(R.id.iv_video);

        ImageView ivCamera = findViewById(R.id.iv_camera);

        mViewModel = new ViewModelProvider(this).get(VideoViewModel.class);

        mIvCall.setOnClickListener(view -> {
            if (mReceiveManager.getCurrentState() == CallState.CALLING) {
                mReceiveManager.finishCall();
            } else {
                mReceiveManager.cancelCall();
            }
            finish();
        });

        ivAudio.setOnClickListener(this::muteMicphone);

        ivSpeaker.setOnClickListener(this::switchSpeaker);

        ivVideo.setOnClickListener(this::muteVideo);

        ivCamera.setOnClickListener(this::switchCamera);

        mReceiveManager = new ReceiveManager(this);

        mViewModel.InitRTCRoom();

        mViewModel.loginRtc(roomname, Long.parseLong(userid), "");

        showLoading();

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
            mReceiveManager.onRtcUserCommandMessage(msg);
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
        mReceiveManager.cancelCall();
    }


    public void receiveCall() {
        mReceiveManager.receiveCall();
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


    public void dismissReceive() {
        if (mReceivedCallPop != null && mReceivedCallPop.isShow()) {
            mReceivedCallPop.dismiss();
        }
        mViewModel.stopMusic();
    }


    /**
     * @param currentState 当前呼叫状态
     * @param command      状态改变原因指令
     * @param commandFrom  指令来源：拨号方发出 / 接听方发出
     */
    @Override
    public void onRecieveStateChange(CallState currentState,
                                     CallCommand command,
                                     CallRole commandFrom) {

        runOnUiThread(() -> {
            XLog.d("onStateChange", currentState.toString());

            //  指令是自己发出
            boolean commandFromMe = CallRole.RECEIVER == commandFrom;

            //  不同通话状态做处理
            switch (currentState) {
                //  切换为常态
                case NORMAL:
                    //  切换原因
                    switch (command) {
                        //  取消
                        case CANCEL:
                            dismissReceive();
                            if (commandFromMe) {
                                toast(R.string.canceled_call);
                            } else {
                                toast(R.string.other_canceled_call);
                            }
                            break;
                        //  超时
                        case TIMEOUT:
                            dismissReceive();
                            toast(R.string.not_response_end);
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

                    mViewModel.stopMusic();
                    break;
                //  切换为响铃中
                case RINGING:
                    //  弹窗提醒接听
                    onReceiveInvite();
                    break;
                //  切换为通话中
                case CALLING:
                    toast(R.string.call_establish_ing);
                    mIvCall.setImageResource(R.drawable.btn_end_call);
                    mViewModel.startPublsh();
                    break;
            }
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
        //  通话中，不能返回
        if (mReceiveManager.getCurrentState() == CallState.CALLING) {
            return;
        }
//        mCallManager.finishCall();
//        mIvCall.setImageResource(R.drawable.ic_start_call);
        super.onBackPressed();
    }


    @Override
    public void sendMessageToUser(String msg) {
        runOnUiThread(() -> {
            mViewModel.sendMessageToUser(msg);
        });
    }
}

