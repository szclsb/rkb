package ch.szclsb.rkb.comm;

import ch.szclsb.rkb.comm.impl.VkCodeHandler;

import java.util.function.Consumer;

public interface IReceiver extends IChannel {
    void addVkCodeListener(VkCodeHandler listener);

    void connect(String host, int port);

    void disconnect();
}
