package com.luqian.demo.widget.dialog;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.rtc.videoroom.R;
import com.luqian.demo.ui.video.VideoActivity;
import com.lxj.xpopup.core.CenterPopupView;
import com.lxj.xpopup.util.KeyboardUtils;

/**
 * @author LUQIAN
 * @date 2021/6/8
 *
 * <p>收到来电
 */
@SuppressLint("ViewConstructor")
public class ReceivedCallPop extends CenterPopupView {

    private final VideoActivity mActivity;
    private TextView mTvContent;
    private String data = "";

    public ReceivedCallPop(@NonNull VideoActivity mActivity) {
        super(mActivity);
        this.mActivity = mActivity;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.pop_received_call;
    }


    @Override
    protected void onCreate() {
        super.onCreate();
        findViewById(R.id.tv_cancel).setOnClickListener(v -> {
            dismiss();
            mActivity.cancelCall();
        });
        findViewById(R.id.tv_receive).setOnClickListener(v -> {
            dismiss();
            mActivity.receiveCall();
        });
        mTvContent = findViewById(R.id.tv_content);
    }

    @Override
    protected void onShow() {
        super.onShow();
        KeyboardUtils.hideSoftInput(mTvContent);
    }

    @Override
    public void dismissWith(Runnable runnable) {
        super.dismissWith(runnable);
    }

    public void setData(@Nullable String data) {
        this.data = data;
        if (mTvContent != null && !TextUtils.isEmpty(this.data)) {
            mTvContent.setText(this.data);
        }
    }
}
