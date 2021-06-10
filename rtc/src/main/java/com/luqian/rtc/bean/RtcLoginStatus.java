package com.luqian.rtc.bean;

/**
 * @author LUQIAN
 * @date 2021/6/10
 */
public class RtcLoginStatus {
    /**
     * RTC 是否登录成功
     */
    private boolean isSuccess;
    /**
     * 附加信息
     */
    private String msg;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public RtcLoginStatus(boolean isSuccess, String msg) {
        this.isSuccess = isSuccess;
        this.msg = msg;
    }
}
