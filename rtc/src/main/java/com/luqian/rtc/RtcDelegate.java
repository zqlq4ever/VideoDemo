package com.luqian.rtc;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

public class RtcDelegate {

    /**
     * 当前状态
     */
    private CallState currentState = CallState.STABLE;
    /**
     * 当前角色
     */
    private CallRole currentRole = CallRole.SENDER;
    /**
     * 呼叫状态监听
     */
    private final CallStateObserver observer;
    private final CallConfig config;
    private final Handler timerHandler;
    private Runnable timerRunnable;

    /**
     * 定义信令控制
     */
    enum CallCommand {

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


    /**
     * 定义呼叫状态
     */
    enum CallState {
        STABLE,     // 正常状态
        INVITING,   // 发起呼叫中
        RINGING,    // 响铃中
        CALLING,    // 通话中
        RECEIVE_INVITING,   //  收到通话邀请
    }


    /**
     * 呼叫角色：发送方、接收方
     */
    enum CallRole {
        SENDER,
        RECEIVER,
    }


    /**
     * 呼叫状态监听
     */
    interface CallStateObserver {
        void onStateChange(CallState state, CallRole role, CallCommand reasonCommand, CallRole commandSource);

        void sendMessage(String msg);
    }


    /**
     * 呼叫配置
     */
    public static class CallConfig {
        public int invitTimeout = 5_000;    // invite 发起呼叫超时时间
        public int ringTimeout = 10_000;    // ring 超时时间
    }


    public RtcDelegate(CallStateObserver observer, CallConfig config) {
        this.observer = observer;
        this.config = config;
        timerHandler = new Handler();
    }


    public CallState getCurrentState() {
        return currentState;
    }

    /**
     * 发起会话
     */
    public void startCall() {
        sendInvite();
        currentState = CallState.INVITING;
        currentRole = CallRole.SENDER;

        timerRunnable = () -> {
            currentState = CallState.STABLE;
            observer.onStateChange(currentState, currentRole, CallCommand.REQUEST_TIMEOUT, CallRole.SENDER);
        };
        timerHandler.postDelayed(timerRunnable, config.invitTimeout);
    }

    /**
     * 接收会话
     */
    public void receiveCall() {
        currentState = CallState.CALLING;
        timerHandler.removeCallbacks(timerRunnable);   // 关闭 ring 定时器
        sendOk();
        observer.onStateChange(currentState, currentRole, CallCommand.OK, CallRole.RECEIVER);
    }


    /**
     * 取消会话(未接通时)
     */
    public void cancelCall() {
        sendCancel();
        currentState = CallState.STABLE;
        timerHandler.removeCallbacks(timerRunnable);    // 关闭 ring 定时器
        observer.onStateChange(currentState, currentRole, CallCommand.CANCEL, currentRole);
    }


    /**
     * 结束会话
     */
    public void finishCall() {
        sendFinish();
        currentState = CallState.STABLE;
        observer.onStateChange(currentState, currentRole, CallCommand.FINISH, currentRole);
    }


    /**
     * 发送邀请 command
     */
    private void sendInvite() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.INVITE.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendRing() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.RING.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendOk() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.OK.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendFinish() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.FINISH.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendCancel() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.CANCEL.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendBusyHere() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.BUSY_HERE.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendTimeout() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.REQUEST_TIMEOUT.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送自定义信令消息
     */
    private void sendMessage(JSONObject msg) {
        observer.sendMessage(msg.toString());
    }


    /**
     * 收到自定义信令消息
     */
    public void onMessage(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            int command = obj.optInt("command");
            if (command == CallCommand.INVITE.getValue()) {
                onReceiveInvite();
            } else if (command == CallCommand.RING.getValue()) {
                onReceiveRing();
            } else if (command == CallCommand.OK.getValue()) {
                onReceiveOk();
            } else if (command == CallCommand.FINISH.getValue()) {
                onReceiveFinish();
            } else if (command == CallCommand.CANCEL.getValue()) {
                onReceiveCancel();
            } else if (command == CallCommand.BUSY_HERE.getValue()) {
                onReceiveBusyHere();
            } else if (command == CallCommand.REQUEST_TIMEOUT.getValue()) {
                onReceiveRequestTimeout();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 接收到通话邀请
     */
    private void onReceiveInvite() {
        switch (currentState) {
            case STABLE: {
                currentRole = CallRole.RECEIVER;
                currentState = CallState.RECEIVE_INVITING;
                sendRing();
                currentState = CallState.RINGING;

                // Ring 定时器;只由接受方去做此超时判断
                timerRunnable = () -> {
                    sendTimeout();
                    currentState = CallState.STABLE;
                    observer.onStateChange(
                            currentState,
                            currentRole,
                            CallCommand.REQUEST_TIMEOUT,
                            CallRole.RECEIVER);
                };
                timerHandler.postDelayed(timerRunnable, config.ringTimeout);
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.RING,
                        CallRole.RECEIVER);
            }
            break;
            default:
                sendBusyHere();
        }
    }


    private void onReceiveRing() {
        switch (currentState) {
            case INVITING:
                currentState = CallState.RINGING;
                timerHandler.removeCallbacks(timerRunnable);    //  关闭超时定时器
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.RING,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }

    private void onReceiveOk() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.CALLING;
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.OK,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }

    private void onReceiveFinish() {
        switch (currentState) {
            case CALLING:
                currentState = CallState.STABLE;
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.FINISH,
                        currentRole == CallRole.SENDER ? CallRole.RECEIVER : CallRole.SENDER);
                break;
            default:
                break;
        }
    }

    private void onReceiveCancel() {
        switch (currentState) {
            case RINGING:
                if (currentRole == CallRole.RECEIVER) {
                    timerHandler.removeCallbacks(timerRunnable);    //  关闭ring定时器
                }
                currentState = CallState.STABLE;
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.CANCEL,
                        currentRole == CallRole.SENDER ? CallRole.RECEIVER : CallRole.SENDER);
                break;
        }
    }

    private void onReceiveBusyHere() {
        switch (currentState) {
            case INVITING:
                currentState = CallState.STABLE;
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.BUSY_HERE,
                        currentRole == CallRole.SENDER ? CallRole.RECEIVER : CallRole.SENDER);
                break;
            default:
                break;
        }
    }

    private void onReceiveRequestTimeout() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.STABLE;
                observer.onStateChange(
                        currentState,
                        currentRole,
                        CallCommand.REQUEST_TIMEOUT,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }
}
