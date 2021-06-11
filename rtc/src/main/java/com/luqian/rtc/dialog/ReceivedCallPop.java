package com.luqian.rtc.dialog;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.rtc.videoroom.R;
import com.luqian.rtc.ui.video.VideoActivity;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * @author LUQIAN
 * @date 2021/6/8
 *
 * <p>收到来电
 */
@SuppressLint("ViewConstructor")
public class ReceivedCallPop extends CenterPopupView {

    private final VideoActivity mActivity;
    private TextView mTvTitle;
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
            mActivity.dismissReceive();
            mActivity.cancelCall();
        });
        findViewById(R.id.tv_receive).setOnClickListener(v -> {
            mActivity.dismissReceive();
            mActivity.receiveCall();
        });
        mTvTitle = findViewById(R.id.tv_title);
        setData(data);
    }


    @Override
    public void dismissWith(Runnable runnable) {
        super.dismissWith(runnable);
    }

    public void setData(@Nullable String data) {
        this.data = data;
        if (mTvTitle != null && !TextUtils.isEmpty(this.data)) {
            mTvTitle.setText(this.data);
        }
    }
}
