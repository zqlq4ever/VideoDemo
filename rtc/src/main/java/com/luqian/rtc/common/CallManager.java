package com.luqian.rtc.common;

import android.os.Handler;

import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallConfig;
import com.luqian.rtc.bean.CallRole;
import com.luqian.rtc.bean.CallState;

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
     * 发起通话邀请
     */
    public void startCall() {
        sendInviteCommand();
        currentState = CallState.INVITING;

        timerRunnable = () -> {
            currentState = CallState.NORMAL;
            observer.onCallStateChange(currentState, CallCommand.TIMEOUT, CallRole.SENDER);
        };
        timerHandler.postDelayed(timerRunnable, config.invitTimeout);
    }


    /**
     * 主动取消通话(未接通时)
     */
    public void cancelCall() {
        sendCancelCommand();
        currentState = CallState.NORMAL;
        // 关闭 ring 定时器
        timerHandler.removeCallbacks(timerRunnable);
        observer.onCallStateChange(currentState, CallCommand.CANCEL, CallRole.SENDER);
    }


    /**
     * 结束通话
     */
    public void finishCall() {
        sendFinishCommand();
        currentState = CallState.NORMAL;
        observer.onCallStateChange(currentState, CallCommand.FINISH, CallRole.SENDER);
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
            if (command == CallCommand.RING.getValue()) {
                onRingCommand();
            } else if (command == CallCommand.OK.getValue()) {
                onOkCommand();
            } else if (command == CallCommand.FINISH.getValue()) {
                onFinishCommand();
            } else if (command == CallCommand.CANCEL.getValue()) {
                onCancelCommand();
            } else if (command == CallCommand.OTHER_BUSY.getValue()) {
                onBusyCommand();
            } else if (command == CallCommand.TIMEOUT.getValue()) {
                onTimeoutCommand();
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
                observer.onCallStateChange(
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
                observer.onCallStateChange(
                        currentState,
                        CallCommand.FINISH,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }


    private void onCancelCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(
                        currentState,
                        CallCommand.CANCEL,
                        CallRole.RECEIVER);
                break;
        }
    }


    /**
     * 发起呼叫一方，收到对方正在通话的信令
     */
    private void onBusyCommand() {
        switch (currentState) {
            case INVITING:
                //  恢复正常
                currentState = CallState.NORMAL;
                observer.onCallStateChange(
                        currentState,
                        CallCommand.OTHER_BUSY,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }

    private void onTimeoutCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(
                        currentState,
                        CallCommand.TIMEOUT,
                        CallRole.RECEIVER);
                break;
            default:
                break;
        }
    }
}
