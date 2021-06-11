package com.luqian.rtc.common;

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
public class ReceiveManager {

    /**
     * 当前通话状态
     */
    private CallState currentState = CallState.NORMAL;
    /**
     * 当前角色
     */
    private final CallRole currentRole = CallRole.RECEIVER;
    /**
     * 呼叫状态监听
     */
    private final CallStateObserver observer;
    private final CallConfig config;
    private final Handler timerHandler;
    private Runnable timerRunnable;


    public ReceiveManager(CallStateObserver observer) {
        this.observer = observer;
        this.config = new CallConfig();
        timerHandler = new Handler();
    }


    /**
     * 收到通话邀请
     */
    public void receiveCall() {
        sendOkCommand();
        // 关闭 ring 定时器
        timerHandler.removeCallbacks(timerRunnable);
        currentState = CallState.CALLING;
        observer.onRecieveStateChange(currentState, CallCommand.OK, CallRole.RECEIVER);
    }


    /**
     * 主动取消通话(未接通时)
     */
    public void cancelCall() {
        sendCancelCommand();
        currentState = CallState.NORMAL;
        // 关闭 ring 定时器
        timerHandler.removeCallbacks(timerRunnable);
        observer.onRecieveStateChange(currentState, CallCommand.CANCEL, currentRole);
    }


    /**
     * 结束通话
     */
    public void finishCall() {
        sendFinishCommand();
        currentState = CallState.NORMAL;
        observer.onRecieveStateChange(currentState, CallCommand.FINISH, currentRole);
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
            busy.putOpt("command", CallCommand.OTHER_BUSY.getValue());
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
            timeout.putOpt("command", CallCommand.TIMEOUT.getValue());
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
    public void onRtcUserCommandMessage(String commandMessage) {
        try {
            JSONObject obj = new JSONObject(commandMessage);
            int command = obj.optInt("command");
            if (command == CallCommand.INVITE.getValue()) {
                onInviteCommand();
            } else if (command == CallCommand.RING.getValue()) {
                onRingCommand();
            } else if (command == CallCommand.OK.getValue()) {
                onOkCommand();
            } else if (command == CallCommand.FINISH.getValue()) {
                onFinishCommand();
            } else if (command == CallCommand.CANCEL.getValue()) {
                onCancelCommand();
            } else if (command == CallCommand.TIMEOUT.getValue()) {
                onTimeoutCommand();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 接收到通话邀请 Command
     */
    private void onInviteCommand() {
        switch (currentState) {
            //  只有是正常情况下，才响应通话邀请
            case NORMAL: {
                //  状态为响铃中
                currentState = CallState.RINGING;
                //  切换成响铃状态
                observer.onRecieveStateChange(
                        currentState,
                        CallCommand.RING,
                        CallRole.RECEIVER);

                //  超时处理
                timerRunnable = () -> {
                    sendTimeoutCommand();
                    currentState = CallState.NORMAL;
                    observer.onRecieveStateChange(
                            currentState,
                            CallCommand.TIMEOUT,
                            CallRole.RECEIVER);
                };
                timerHandler.postDelayed(timerRunnable, config.ringTimeout);

                sendRingCommand();
            }
            break;
            default:
                sendBusyCommand();
        }
    }


    /**
     * 当接听方收到通话邀请后，回给拨号方发送 CallCommand.RING 信令
     *
     * <p>拨号方收到响铃 Command
     */
    private void onRingCommand() {
        switch (currentState) {
            //  当前状态是：拨号方发起通话中
            case INVITING:
                //  变为响铃中
                currentState = CallState.RINGING;
                //  关闭拨号超时任务
                timerHandler.removeCallbacks(timerRunnable);
                break;
            default:
                break;
        }
    }


    private void onOkCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.CALLING;
                observer.onRecieveStateChange(
                        currentState,
                        CallCommand.OK,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }


    //  CallState.CALLING 状态下，双方都需要处理消息
    private void onFinishCommand() {
        switch (currentState) {
            case CALLING:
                currentState = CallState.NORMAL;
                observer.onRecieveStateChange(
                        currentState,
                        CallCommand.FINISH,
                        CallRole.SENDER);
                break;
            default:
                break;
        }
    }


    private void onCancelCommand() {
        switch (currentState) {
            case RINGING:
                if (currentRole == CallRole.RECEIVER) {
                    timerHandler.removeCallbacks(timerRunnable);
                }
                currentState = CallState.NORMAL;
                observer.onRecieveStateChange(
                        currentState,
                        CallCommand.CANCEL,
                        CallRole.SENDER);
                break;
        }
    }


    private void onTimeoutCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onRecieveStateChange(
                        currentState,
                        CallCommand.TIMEOUT,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }
}
