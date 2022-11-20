package ch.szclsb.rkb.comm;

import java.util.function.Consumer;

public interface IChannel extends AutoCloseable {
    ChannelState getState();

    void addStateChangeListener(Consumer<ChannelState> listener);
}
