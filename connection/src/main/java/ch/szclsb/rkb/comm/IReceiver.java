package ch.szclsb.rkb.comm;

import java.util.function.Consumer;

public interface IReceiver extends IChannel {
    void onReceive(Consumer<Integer> vkCodeHandler);
    void connect(String host, int port);
    void disconnect();
}
