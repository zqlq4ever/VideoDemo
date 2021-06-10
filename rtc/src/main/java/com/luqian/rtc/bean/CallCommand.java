package com.luqian.rtc.bean;

/**
 * 定义通话信令控制
 */
public enum CallCommand {

    INVITE(101),   // 发起呼叫
    RING(102),     // 回复铃响
    OK(103),       // 回话接通
    FINISH(104),   // 挂断回话
    CANCEL(105),   // 取消呼叫
    REQUEST_TIMEOUT(106),   // 超时无回应
    BUSY_HERE(107);         // 呼叫正忙

    private final int value;

    CallCommand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}