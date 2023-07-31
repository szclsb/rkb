package ch.szclsb.rkb.comm;

import java.io.IOException;

public interface IReceiver extends IChannel {
    void connect(String host, int port, VkCodeHandler listener) throws IOException;

    void disconnect() throws IOException;
}
