package ch.szclsb.rkb.comm;

public interface IChannel extends AutoCloseable {
    ChannelState getState();
}
