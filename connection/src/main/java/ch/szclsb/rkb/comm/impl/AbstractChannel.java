package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractChannel implements IChannel {
    private final AtomicReference<ChannelState> stateRef;
    private final Consumer<ChannelState> stateChangeListener;
    private final ByteBuffer buffer;

    public AbstractChannel(Consumer<ChannelState> stateChangeListener) {
        this.stateRef = new AtomicReference<>(ChannelState.DISCONNECTED);
        this.stateChangeListener = stateChangeListener;
        this.buffer = ByteBuffer.allocate(4);
    }

    public ChannelState getState() {
        return this.stateRef.get();
    }

    protected boolean compareAndSetState(ChannelState expected, ChannelState newState) {
        if (stateRef.compareAndSet(expected, newState)) {
            stateChangeListener.accept(newState);
            return true;
        }
        return false;
    }

    protected ChannelState updateState(ChannelState newState) {
        var oldState = stateRef.getAndSet(newState);
        stateChangeListener.accept(newState);
        return oldState;
    }

    protected void writeToChannel(int vkCode, SocketChannel channel) throws IOException {
        buffer.putInt(vkCode);
        buffer.flip();
        channel.write(buffer);
    }

    protected void readFromChannel(VkCodeHandler handler, SocketChannel channel) throws Exception {
        int size;
        while ((size = channel.read(buffer)) != -1) {
            buffer.rewind();
            //todo improve
            if (size == 4) {
                var vkCode = buffer.getInt();
                handler.invoke(vkCode);
            }
        }
    }
}
