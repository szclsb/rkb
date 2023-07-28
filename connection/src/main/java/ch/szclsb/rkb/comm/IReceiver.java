package ch.szclsb.rkb.comm;

public interface IReceiver extends IChannel {
    void addVkCodeListener(VkCodeHandler listener);

    void connect(String host, int port);

    void disconnect();
}
