package ch.szclsb.rkb.comm;

import java.io.IOException;

public interface ISender extends IChannel {
    void open(int port) throws IOException;
    boolean send(int vkCode, boolean up);
    void disconnect() throws IOException;
}
