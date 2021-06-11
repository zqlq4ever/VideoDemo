package com.luqian.rtc.ui;

import static com.luqian.rtc.common.RtcConstant.ROUTER_RTC_VIDEO_CALL;

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
import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallState;
import com.luqian.rtc.common.CallManager;
import com.luqian.rtc.common.CallStateObserver;
import com.luqian.rtc.dialog.CallPop;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * 主动拨号方页面
 */
@Route(path = ROUTER_RTC_VIDEO_CALL)
public class VideoCallActivity extends BaseVideoActivtiy implements CallStateObserver {

    private static final String TAG = "VideoCallActivity";
    private CallPop mCallPop;
    private ImageView mIvCall;
    public CallManager mCallManager;

    @Autowired
    public String userid;
    @Autowired
    public String roomname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        setContentView(R.layout.activity_video_call);

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
            if (mCallManager.getCurrentState() == CallState.NORMAL) {

                mCallManager.startCall();

                mIvCall.setImageResource(R.drawable.btn_end_call);

                showCallPop();

            } else if (mCallManager.getCurrentState() == CallState.CALLING) {
                //  结束通话
                mCallManager.finishCall();
            }
        });

        ivAudio.setOnClickListener(this::muteMicphone);

        ivSpeaker.setOnClickListener(this::switchSpeaker);

        ivVideo.setOnClickListener(this::muteVideo);

        ivCamera.setOnClickListener(this::switchCamera);

        mCallManager = new CallManager(this);

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
        mCallManager.cancelDial();
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


    public void dismissCall() {
        if (mCallPop != null && mCallPop.isShow()) {
            mCallPop.dismiss();
        }
        mViewModel.stopMusic();
    }


    /**
     * @param currentState 当前呼叫状态
     * @param command      状态改变原因指令
     */
    @Override
    public void onCallStateChange(CallState currentState,
                                  CallCommand command) {

        runOnUiThread(() -> {
            XLog.d("onStateChange", currentState.toString());

            //  不同通话状态做处理
            switch (currentState) {
                //  切换为常态
                case NORMAL:
                    //  切换原因
                    switch (command) {
                        //  取消
                        case CANCEL_DIAL:
                            dismissCall();
                            toast(R.string.cancele_dial);
                            break;
                        //  取消
                        case REFUSE:
                            dismissCall();
                            toast(R.string.other_refuse_call);
                            break;
                        //  超时
                        case CALL_TIMEOUT:
                            dismissCall();
                            toast(R.string.timeout_end);
                            break;
                        //  对方正忙
                        case OTHER_BUSY:
                            dismissCall();
                            toast(R.string.in_call_please_wait);
                            break;
                        //  结束通话
                        case FINISH_BY_CALL:
                            toast(R.string.end_call);
                            mViewModel.stopPublsh();
                            finish();
                            break;
                        //  结束通话
                        case FINISH_BY_RECEIVE:
                            toast(R.string.other_end_call);
                            mViewModel.stopPublsh();
                            finish();
                            break;
                    }

                    mIvCall.setImageResource(R.drawable.ic_start_call);
                    mViewModel.stopMusic();
                    break;
                //  切换为通话中
                case CALLING:
                    dismissCall();
                    toast(R.string.accept_establish);

                    mIvCall.setImageResource(R.drawable.btn_end_call);
                    mViewModel.startPublsh();
                    break;
            }
        });
    }


    @Override
    public void sendMessageToUser(String msg) {
        runOnUiThread(() -> mViewModel.sendMessageToUser(msg));
    }


    @Override
    public void onBackPressed() {
        //  通话中，不能返回
        if (mCallManager.getCurrentState() == CallState.CALLING) {
            return;
        }
        super.onBackPressed();
    }
}

