package com.luqian.rtc.bean;

/**
 * 定义通话信令控制
 */
public enum CallCommand {

    INVITE(101),   // 发起通话
    RING(102),     // 铃响
    OK(103),       // 接通
    FINISH(104),   // 挂断
    CANCEL(105),   // 取消呼叫
    TIMEOUT(106),   // 呼叫超时
    OTHER_BUSY(107);         // 对方正忙

    private final int value;

    CallCommand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}