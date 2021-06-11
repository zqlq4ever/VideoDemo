package com.luqian.rtc.dialog;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.rtc.videoroom.R;
import com.luqian.rtc.ui.video.VideoCallActivity;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * @author LUQIAN
 * @date 2021/6/8
 *
 * <p>拨号
 */
@SuppressLint("ViewConstructor")
public class CallPop extends CenterPopupView {

    private final VideoCallActivity mActivity;
    private TextView mTvContent;
    private String data = "";

    public CallPop(@NonNull VideoCallActivity mActivity) {
        super(mActivity);
        this.mActivity = mActivity;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.pop_call;
    }


    @Override
    protected void onCreate() {
        super.onCreate();
        findViewById(R.id.tv_cancel).setOnClickListener(v -> {
            mActivity.dismissCall();
            mActivity.cancelCall();
        });
        mTvContent = findViewById(R.id.tv_content);
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
