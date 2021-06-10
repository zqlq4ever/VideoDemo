package com.luqian.rtc.common;

import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallRole;
import com.luqian.rtc.bean.CallState;

/**
 * 呼叫状态监听
 */
public interface CallStateObserver {
    /**
     * @param currentState  当前呼叫状态
     * @param role          呼叫角色：拨号方 / 接听方
     * @param command       状态改变原因指令
     * @param commandSource 指令来源：拨号方发出 / 接听方发出
     */
    void onStateChange(CallState currentState, CallRole role, CallCommand command, CallRole commandSource);

    void sendMessageToUser(String msg);
}