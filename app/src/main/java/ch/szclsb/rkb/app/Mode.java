package ch.szclsb.rkb.app;

public enum Mode {
    SEND(true,"open"),
    WAITING(true,"terminate"),
    SENDING(true,"disconnect"),
    RECEIVE(false, "connect"),
    RECEIVING(false, "disconnect"),;

    public boolean send;
    private final String actionText;
    Mode(boolean send, String actionText) {
        this.send = send;
        this.actionText = actionText;
    }

    public boolean isSend() {
        return send;
    }

    public String getActionText() {
        return actionText;
    }
}
