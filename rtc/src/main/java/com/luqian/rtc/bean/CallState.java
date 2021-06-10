package com.luqian.rtc.bean;

/**
 * 定义通话状态
 */
public enum CallState {
    NORMAL,     // 正常状态
    INVITING,   // 发起通话中
    RINGING,    // 响铃中
    CALLING,    // 通话中
}