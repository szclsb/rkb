package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IReceiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Receiver extends AbstractChannel implements IReceiver {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final Consumer<Throwable> errorHandler;
    private final Set<VkCodeHandler> listeners;

    private volatile SocketChannel channel;

    public Receiver(Consumer<Throwable> errorHandler, Consumer<ChannelState> stateChangeListener) {
        super(stateChangeListener);
        this.queue = new LinkedBlockingDeque<>(64);
        this.service = Executors.newCachedThreadPool();
        this.errorHandler = errorHandler;
        this.listeners = ConcurrentHashMap.newKeySet();

        CompletableFuture.runAsync(() -> {
            try {
                int vkCode;
                while ((vkCode = queue.take()) != 0) {
                    for (var listener : listeners) {
                        try {
                            listener.invoke(vkCode);
                        } catch (Exception e) {
                            errorHandler.accept(e);
                        }
                    }
                }
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        }, service);
    }

    @Override
    public void addVkCodeListener(VkCodeHandler listener) {
        listeners.add(listener);
    }

    @Override
    public void connect(String host, int port) {
        var address = new InetSocketAddress(host, port);
        CompletableFuture.runAsync(() -> {
            if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.CONNECTING)) {
                try (var clientChannel = SocketChannel.open()) {
                    clientChannel.connect(address);
                    this.channel = clientChannel;
                    if (compareAndSetState(ChannelState.CONNECTING, ChannelState.CONNECTED)) {
                        readFromChannel(queue::put, clientChannel);
                    }
                } catch (Exception e) {
                    errorHandler.accept(e);
                } finally {
                    updateState(ChannelState.DISCONNECTED);
                }
            } else {
                errorHandler.accept(new Exception("already connected"));
            }
        }, service);
    }

    @Override
    public void disconnect() {
        compareAndSetState(ChannelState.CONNECTING, ChannelState.TERMINATING);
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.TERMINATING)) {
            try {
                channel.close();
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        disconnect();
        queue.clear();
        queue.add(0);
        service.close();
    }
}
