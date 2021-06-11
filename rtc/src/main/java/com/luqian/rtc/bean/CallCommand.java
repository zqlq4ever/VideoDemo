package com.luqian.rtc.bean;

/**
 * 定义通话信令控制
 */
public enum CallCommand {

    INVITE(101),   // 发起通话
    RING(102),     // 铃响
    OK(103),       // 接通
    FINISH_BY_CALL(104),   // 挂断
    CANCEL_DIAL(105),   // 取消呼叫
    CALL_TIMEOUT(106),   // 呼叫超时
    OTHER_BUSY(107),        // 对方正忙
    REFUSE(108),         // 拒绝接听
    FINISH_BY_RECEIVE(109),  // 挂断
    RECEIVE_CALL(110);  // 接收到电话邀请

    private final int value;

    CallCommand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}