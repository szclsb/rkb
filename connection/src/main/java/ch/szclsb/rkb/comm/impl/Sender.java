package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ISender;
import ch.szclsb.rkb.comm.ChannelState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Sender extends AbstractChannel implements ISender {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final Consumer<Throwable> errorHandler;
    private volatile ServerSocketChannel serverChannel;
    private volatile SocketChannel channel;

    public Sender(Consumer<Throwable> errorHandler, Consumer<ChannelState> stateChangeListener) {
        super(stateChangeListener);
        this.queue = new LinkedBlockingDeque<>(64);
        this.service = Executors.newCachedThreadPool();
        this.errorHandler = errorHandler;
    }

    @Override
    public void open(int port) {
        var address = new InetSocketAddress("localhost", port);
        // incoming connections
        CompletableFuture.runAsync(() -> {
            //todo: use NIO Selector
            if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.WAITING)) {
                try (var serverChannel = ServerSocketChannel.open()) {
                    serverChannel.bind(address);
                    this.serverChannel = serverChannel;
                    while (getState() == ChannelState.WAITING) {
                        try (var clientChannel = serverChannel.accept()) {
                            if (compareAndSetState(ChannelState.WAITING, ChannelState.CONNECTED)) {
                                this.channel = clientChannel;
                                while (!service.isTerminated()) {
                                    var vkCode = queue.take();
                                    writeToChannel(vkCode, clientChannel);
                                }
                            }
                        } catch (IOException e) {
                            errorHandler.accept(e);
                        }
                        updateState(ChannelState.WAITING);
                    }
                } catch (Exception e) {
                    errorHandler.accept(e);
                }
                updateState(ChannelState.DISCONNECTED);
            } else {
                errorHandler.accept(new Exception("already connected"));
            }
        }, service);
    }

    @Override
    public void send(int vkCode) {
        queue.add(vkCode);
    }

    @Override
    public void disconnect() {
        var state = getState();
        if (state == ChannelState.CONNECTED) {
            try {
                this.channel.close();
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        }
        if (state != ChannelState.DISCONNECTED) {
            try {
                this.serverChannel.close();
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        service.close();
        disconnect();
    }
}
