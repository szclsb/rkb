package ch.szclsb.rkb.app;

public enum Mode {
    SEND("open"),
    RECEIVE("connect");

    private String actionText;
    Mode(String actionText) {
        this.actionText = actionText;
    }

    public String getActionText() {
        return actionText;
    }
}
