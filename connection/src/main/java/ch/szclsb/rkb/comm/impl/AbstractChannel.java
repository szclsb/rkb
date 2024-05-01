package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IChannel;
import ch.szclsb.rkb.comm.VkCodeEvent;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractChannel implements IChannel {
    public static final VkCodeEvent STOP_EVENT = new VkCodeEvent(-1, false);
    public static final VkCodeEvent HEARTBEAT_EVENT = new VkCodeEvent(0, false);
    private final AtomicReference<ChannelState> state;
    private final Collection<Consumer<ChannelState>> listeners;

    public AbstractChannel() {
        this.state = new AtomicReference<>(ChannelState.DISCONNECTED);
        this.listeners = new ConcurrentLinkedDeque<>();
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
        Thread.ofVirtual().start(() -> listeners.forEach(c -> c.accept(state)));
    }

    protected boolean compareAndSetState(ChannelState state, ChannelState expected) {
        if (this.state.compareAndSet(expected, state)) {
            Thread.ofVirtual().start(() -> listeners.forEach(c -> c.accept(state)));
            return true;
        }
        return false;
    }
}
