package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractChannel implements IChannel {
    private final AtomicReference<ChannelState> state;
    private final List<Consumer<ChannelState>> listeners;

    public AbstractChannel() {
        this.state = new AtomicReference<>(ChannelState.DISCONNECTED);
        this.listeners = new ArrayList<>();
    }

    @Override
    public ChannelState getState() {
        return state.get();
    }

    @Override
    public void addStateChangeListener(Consumer<ChannelState> listener) {
        listeners.add(listener);
    }

    protected void setState(ChannelState state) {
        this.state.set(state);
        Thread.ofVirtual().start(() -> {
            listeners.forEach(c -> c.accept(state));
        });
    }

    protected boolean compareAndSetState(ChannelState expected, ChannelState state) {
        if (this.state.compareAndSet(expected, state)) {
            Thread.ofVirtual().start(() -> {
                listeners.forEach(c -> c.accept(state));
            });
            return true;
        }
        return false;
    }
}
