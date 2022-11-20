package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractChannel implements IChannel {
    private final AtomicReference<ChannelState> stateRef;
    private final Set<Consumer<ChannelState>> stateChangeListeners;
    private final ByteBuffer buffer;
    private final ExecutorService service;

    public AbstractChannel() {
        this.stateRef = new AtomicReference<>(ChannelState.DISCONNECTED);
        this.buffer = ByteBuffer.allocate(4);
        this.service = Executors.newCachedThreadPool();
        this.stateChangeListeners = ConcurrentHashMap.newKeySet();
    }

    @Override
    public ChannelState getState() {
        return this.stateRef.get();
    }

    @Override
    public void addStateChangeListener(Consumer<ChannelState> listener) {
        stateChangeListeners.add(listener);
    }

    protected void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable, service);
    }

    protected boolean compareAndSetState(ChannelState expected, ChannelState newState) {
        if (stateRef.compareAndSet(expected, newState)) {
            runAsync(() -> {
                for (var listener: stateChangeListeners) {
                    try {
                        listener.accept(newState);
                    } catch (Exception ignore) {

                    }
                }
            });
            return true;
        }
        return false;
    }

    protected ChannelState updateState(ChannelState newState) {
        var oldState = stateRef.getAndSet(newState);
        runAsync(() -> {
            for (var listener: stateChangeListeners) {
                try {
                    listener.accept(newState);
                } catch (Exception ignore) {

                }
            }
        });
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

    @Override
    public void close() throws Exception {
        service.close();
    }
}
