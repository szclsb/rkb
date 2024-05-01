package ch.szclsb.rkb.comm;

import java.util.function.Consumer;

public interface IChannel {
    ChannelState getState();

    void addStateChangeListener(Consumer<ChannelState> listener);
}
