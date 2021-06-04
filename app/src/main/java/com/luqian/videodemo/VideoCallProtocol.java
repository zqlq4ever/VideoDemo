package com.luqian.videodemo;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

public class VideoCallProtocol {

    enum CallCommand {

        kInvite(101),   // 发起呼叫
        kRing(102),     // 回复铃响
        kOk(103),       // 回话接通
        kBye(104),      // 挂断回话
        kCancel(105),   // 取消呼叫
        kRequestTimeout(106),   // 超时无回应
        kBusyHere(107);         // 呼叫正忙

        private final int value;

        CallCommand(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    enum CallState {
        kStable,
        kInviting,
        kRinging,
        kCalling,    // 通话中
        kReceiveInviting,
    }

    enum CallRole {
        kSender,
        kReceiver,
    }

    interface CallStateObserver {
        void onStateChange(CallState state, CallRole role, CallCommand reasonCommand, CallRole commandSource);

        void sendMessage(String msg);
    }

    public static class CallConfig {
        CallConfig() {
        }

        public int invitTimeout = 5000;    // invite 超时时间
        public int ringTimeout = 10000;     // ring 超时时间
    }

    private CallState currentState = CallState.kStable;
    private CallRole currentRole = CallRole.kSender;
    private final CallStateObserver observer;
    private final CallConfig config;
    private final Handler timerHandler;     //  定时器
    private Runnable timerRunnable;

    public VideoCallProtocol(CallStateObserver observer, CallConfig config) {
        this.observer = observer;
        this.config = config;
        timerHandler = new Handler();
    }

    public CallState getCurrentState() {
        return currentState;
    }

    //  发起会话
    public void startCall() {
        sendInvite();
        currentState = CallState.kInviting;
        currentRole = CallRole.kSender;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                currentState = CallState.kStable;
                observer.onStateChange(currentState, currentRole, CallCommand.kRequestTimeout, CallRole.kSender);
            }
        };
        timerHandler.postDelayed(timerRunnable, config.invitTimeout);
    }

    //  接收会话
    public void receiveCall() {
        currentState = CallState.kCalling;
        timerHandler.removeCallbacks(timerRunnable);   // 关闭ring定时器
        sendOk();
        observer.onStateChange(currentState, currentRole, CallCommand.kOk, CallRole.kReceiver);
    }

    //  取消会话(未接通时)
    public void cancelCall() {
        sendCancel();
        currentState = CallState.kStable;
        timerHandler.removeCallbacks(timerRunnable);    // 关闭ring定时器
        observer.onStateChange(currentState, currentRole, CallCommand.kCancel, currentRole);
    }

    //  结束会话
    public void finishCall() {
        sendBye();
        currentState = CallState.kStable;
        observer.onStateChange(currentState, currentRole, CallCommand.kBye, currentRole);
    }

    private void sendInvite() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kInvite.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendRing() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kRing.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendOk() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kOk.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendBye() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kBye.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendCancel() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kCancel.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendBusyHere() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kBusyHere.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendTimeout() {
        JSONObject invite = new JSONObject();
        try {
            invite.putOpt("command", CallCommand.kRequestTimeout.getValue());
            sendMessage(invite);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(JSONObject msg) {
        observer.sendMessage(msg.toString());
    }

    public void onMessage(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            int command = obj.optInt("command");
            if (command == CallCommand.kInvite.getValue()) {
                onReceiveInvite();
            } else if (command == CallCommand.kRing.getValue()) {
                onReceiveRing();
            } else if (command == CallCommand.kOk.getValue()) {
                onReceiveOk();
            } else if (command == CallCommand.kBye.getValue()) {
                onReceiveBye();
            } else if (command == CallCommand.kCancel.getValue()) {
                onReceiveCancel();
            } else if (command == CallCommand.kBusyHere.getValue()) {
                onReceiveBusyHere();
            } else if (command == CallCommand.kRequestTimeout.getValue()) {
                onReceiveRequestTimeout();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onReceiveInvite() {
        switch (currentState) {
            case kStable: {
                currentRole = CallRole.kReceiver;
                currentState = CallState.kReceiveInviting;
                sendRing();
                currentState = CallState.kRinging;
                // Ring定时器;只由接受方去做此超时判断
                timerRunnable = new Runnable() {
                    @Override
                    public void run() {
                        sendTimeout();
                        currentState = CallState.kStable;
                        observer.onStateChange(currentState, currentRole, CallCommand.kRequestTimeout, CallRole.kReceiver);
                    }
                };
                timerHandler.postDelayed(timerRunnable, config.ringTimeout);
                //
                observer.onStateChange(currentState, currentRole, CallCommand.kRing, CallRole.kReceiver);
            }
            break;
            default:
                sendBusyHere();
        }
    }

    private void onReceiveRing() {
        switch (currentState) {
            case kInviting:
                currentState = CallState.kRinging;
                timerHandler.removeCallbacks(timerRunnable);    //  关闭超时定时器
                observer.onStateChange(currentState, currentRole, CallCommand.kRing, CallRole.kReceiver);
                break;
            default:
                break;
        }
    }

    private void onReceiveOk() {
        switch (currentState) {
            case kRinging:
                currentState = CallState.kCalling;
                observer.onStateChange(currentState, currentRole, CallCommand.kOk, CallRole.kReceiver);
                break;
            default:
                break;
        }
    }

    private void onReceiveBye() {
        switch (currentState) {
            case kCalling:
                currentState = CallState.kStable;
                observer.onStateChange(currentState, currentRole,
                        CallCommand.kBye, currentRole == CallRole.kSender ? CallRole.kReceiver : CallRole.kSender);
                break;
            default:
                break;
        }
    }

    private void onReceiveCancel() {
        switch (currentState) {
            case kRinging:
                if (currentRole == CallRole.kReceiver) {
                    timerHandler.removeCallbacks(timerRunnable);    //  关闭ring定时器
                }
                currentState = CallState.kStable;
                observer.onStateChange(currentState, currentRole, CallCommand.kCancel,
                        currentRole == CallRole.kSender ? CallRole.kReceiver : CallRole.kSender);
                break;
        }
    }

    private void onReceiveBusyHere() {
        switch (currentState) {
            case kInviting:
                currentState = CallState.kStable;
                observer.onStateChange(currentState, currentRole, CallCommand.kBusyHere,
                        currentRole == CallRole.kSender ? CallRole.kReceiver : CallRole.kSender);
                break;
            default:
                break;
        }
    }

    private void onReceiveRequestTimeout() {
        switch (currentState) {
            case kRinging:
                currentState = CallState.kStable;
                observer.onStateChange(currentState, currentRole, CallCommand.kRequestTimeout, CallRole.kReceiver);
                break;
            default:
                break;
        }
    }
}
