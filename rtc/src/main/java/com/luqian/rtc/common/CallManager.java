package com.luqian.rtc.common;

import android.os.Handler;

import com.luqian.rtc.bean.CallCommand;
import com.luqian.rtc.bean.CallConfig;
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
            observer.onCallStateChange(currentState, CallCommand.CALL_TIMEOUT);
        };
        timerHandler.postDelayed(timerRunnable, config.invitTimeout);
    }


    /**
     * 取消拨号
     */
    public void cancelDial() {
        sendCancelCommand();
        currentState = CallState.NORMAL;
        // 关闭 ring 定时器
        timerHandler.removeCallbacks(timerRunnable);
        observer.onCallStateChange(currentState, CallCommand.CANCEL_DIAL);
    }


    /**
     * 结束通话
     */
    public void finishCall() {
        sendFinishCommand();
        currentState = CallState.NORMAL;
        observer.onCallStateChange(currentState, CallCommand.FINISH_BY_CALL);
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
            finish.putOpt("command", CallCommand.FINISH_BY_CALL.getValue());
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
            cancel.putOpt("command", CallCommand.CANCEL_DIAL.getValue());
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
            if (command == CallCommand.RECEIVE_CALL.getValue()) {
                onReceiveCallCommand();
            } else if (command == CallCommand.OK.getValue()) {
                onOkCommand();
            } else if (command == CallCommand.FINISH_BY_CALL.getValue()) {
                onFinishCommand();
            } else if (command == CallCommand.FINISH_BY_RECEIVE.getValue()) {
                onOtherFinishCommand();
            } else if (command == CallCommand.CANCEL_DIAL.getValue()) {
                onCancelCommand();
            } else if (command == CallCommand.OTHER_BUSY.getValue()) {
                onBusyCommand();
            } else if (command == CallCommand.CALL_TIMEOUT.getValue()) {
                onTimeoutCommand();
            } else if (command == CallCommand.REFUSE.getValue()) {
                onRefuseCommand();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 接听方收到通话邀请
     */
    private void onReceiveCallCommand() {
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
                observer.onCallStateChange(currentState, CallCommand.OK);
                break;
            default:
                break;
        }
    }


    private void onFinishCommand() {
        switch (currentState) {
            case CALLING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(currentState, CallCommand.FINISH_BY_CALL);
                break;
            default:
                break;
        }
    }


    private void onOtherFinishCommand() {
        switch (currentState) {
            case CALLING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(currentState, CallCommand.FINISH_BY_RECEIVE);
                break;
            default:
                break;
        }
    }


    private void onCancelCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(currentState, CallCommand.CANCEL_DIAL);
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
                observer.onCallStateChange(currentState, CallCommand.OTHER_BUSY);
                break;
            default:
                break;
        }
    }

    private void onTimeoutCommand() {
        switch (currentState) {
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(currentState, CallCommand.CALL_TIMEOUT);
                break;
            default:
                break;
        }
    }


    private void onRefuseCommand() {
        switch (currentState) {
            case INVITING:
            case RINGING:
                currentState = CallState.NORMAL;
                observer.onCallStateChange(currentState, CallCommand.REFUSE);
                break;
            default:
                break;
        }
    }
}
