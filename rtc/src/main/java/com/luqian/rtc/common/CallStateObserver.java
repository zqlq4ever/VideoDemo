package com.luqian.rtc.common;

import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallState;

/**
 * 通话状态监听
 */
public interface CallStateObserver {
    /**
     * @param currentState 当前呼叫状态
     * @param command      状态改变原因指令
     */
    default void onCallStateChange(CallState currentState, CallCommand command) {
    }


    /**
     * @param currentState 当前呼叫状态
     * @param command      状态改变原因指令
     */
    default void onRecieveStateChange(CallState currentState, CallCommand command) {
    }


    /**
     * 发送信令
     *
     * @param msg 信令
     */
    void sendMessageToUser(String msg);
}