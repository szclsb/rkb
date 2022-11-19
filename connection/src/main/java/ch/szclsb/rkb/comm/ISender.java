package ch.szclsb.rkb.comm;

public interface ISender extends IChannel {
    void open(int port);
    void send(int vkCode);
    void disconnect();
}
