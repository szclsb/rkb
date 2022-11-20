package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;

@FunctionalInterface
public interface ChannelStateListener {
    void accept(ChannelState state) throws Exception;
}
