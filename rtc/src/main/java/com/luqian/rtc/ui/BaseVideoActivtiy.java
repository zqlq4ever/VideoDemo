package com.luqian.rtc.ui;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.baidu.rtc.videoroom.R;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;

/**
 * @author LUQIAN
 * @date 2021/6/11
 */
public class BaseVideoActivtiy extends AppCompatActivity {

    protected VideoViewModel mViewModel;
    protected LoadingPopupView mLoadingPopup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }


    protected void showLoading() {
        mLoadingPopup = (LoadingPopupView) new XPopup.Builder(this)
                .dismissOnBackPressed(false)
                .dismissOnTouchOutside(false)
                .hasShadowBg(false)
                .asLoading("加载中")
                .show();
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

