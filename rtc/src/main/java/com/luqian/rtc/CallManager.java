package com.luqian.rtc;

import android.os.Handler;

import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallConfig;
import com.luqian.rtc.bean.CallRole;
import com.luqian.rtc.bean.CallState;
import com.luqian.rtc.common.CallStateObserver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 通话管理
 */
public class CallManager {

    /**
     * 当前通话状态
     */
    private CallState currentState = CallState.NORMAL;
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


    public CallManager(CallStateObserver observer) {
        this.observer = observer;
        this.config = new CallConfig();
        timerHandler = new Handler();
    }


    /**
     * 发起通话
     */
    public void startCall() {
        sendInviteCommand();
        currentState = CallState.INVITING;
        currentRole = CallRole.SENDER;

        timerRunnable = () -> {
            currentState = CallState.NORMAL;
            observer.onStateChange(currentState, currentRole, CallCommand.REQUEST_TIMEOUT, CallRole.SENDER);
        };
        timerHandler.postDelayed(timerRunnable, config.invitTimeout);
    }


    /**
     * 接收通话
     */
    public void receiveCall() {
        currentState = CallState.CALLING;
        timerHandler.removeCallbacks(timerRunnable);   // 关闭 ring 定时器
        sendOkCommand();
        observer.onStateChange(currentState, currentRole, CallCommand.OK, CallRole.RECEIVER);
    }


    /**
     * 取消通话(未接通时)
     */
    public void cancelCall() {
        sendCancelCommand();
        currentState = CallState.NORMAL;
        timerHandler.removeCallbacks(timerRunnable);    // 关闭 ring 定时器
        observer.onStateChange(currentState, currentRole, CallCommand.CANCEL, currentRole);
    }


    /**
     * 结束通话
     */
    public void finishCall() {
        sendFinishCommand();
        currentState = CallState.NORMAL;
        observer.onStateChange(currentState, currentRole, CallCommand.FINISH, currentRole);
    }


    /**
     * 发送通话 command
     */
    private void sendInviteCommand() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.INVITE.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送响铃 command
     */
    private void sendRingCommand() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.RING.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送接通 command
     */
    private void sendOkCommand() {
        JSONObject ok = new JSONObject();
        try {
            ok.putOpt("command", CallCommand.OK.getValue());
            sendMessage(ok);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送结束 command
     */
    private void sendFinishCommand() {
        JSONObject finish = new JSONObject();
        try {
            finish.putOpt("command", CallCommand.FINISH.getValue());
            sendMessage(finish);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送取消 command
     */
    private void sendCancelCommand() {
        JSONObject cancel = new JSONObject();
        try {
            cancel.putOpt("command", CallCommand.CANCEL.getValue());
            sendMessage(cancel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送繁忙 command
     */
    private void sendBusyCommand() {
        JSONObject busy = new JSONObject();
        try {
            busy.putOpt("command", CallCommand.BUSY_HERE.getValue());
            sendMessage(busy);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送超时 command
     */
    private void sendTimeoutCommand() {
        JSONObject timeout = new JSONObject();
        try {
            timeout.putOpt("command", CallCommand.REQUEST_TIMEOUT.getValue());
            sendMessage(timeout);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送自定义信令消息
     */
    private void sendMessage(JSONObject msg) {
        observer.sendMessageToUser(msg.toString());
    }


    /**
     * 获取当前通话状态
     */
    public CallState getCurrentState() {
        return currentState;
    }


    /**
     * 收到自定义信令消息
     */
    public void onRtcUserCommandMessage(String message) {
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
     * 接收到通话邀请 Command
     */
    private void onReceiveInvite() {
        switch (currentState) {
            case NORMAL: {
                currentRole = CallRole.RECEIVER;
                currentState = CallState.RECEIVE_INVITING;
                sendRingCommand();
                currentState = CallState.RINGING;

                // Ring 定时器;只由接受方去做此超时判断
                timerRunnable = () -> {
                    sendTimeoutCommand();
                    currentState = CallState.NORMAL;
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
                sendBusyCommand();
        }
    }


    /**
     * 收到响铃 Command
     */
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
                currentState = CallState.NORMAL;
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
                currentState = CallState.NORMAL;
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
                currentState = CallState.NORMAL;
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
                currentState = CallState.NORMAL;
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
