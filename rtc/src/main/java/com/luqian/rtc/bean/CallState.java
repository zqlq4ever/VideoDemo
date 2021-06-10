package com.luqian.rtc.bean;

/**
 * 定义通话状态
 */
public enum CallState {
    NORMAL,     // 正常状态
    INVITING,   // 发起呼叫中
    RINGING,    // 响铃中
    CALLING,    // 通话中
    RECEIVE_INVITING,   //  收到通话邀请
}