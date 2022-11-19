package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.IReceiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Receiver extends AbstractChannel implements IReceiver {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final Consumer<Throwable> errorHandler;

    private volatile SocketChannel channel;

    public Receiver(Consumer<Throwable> errorHandler, Consumer<ChannelState> stateChangeListener) {
        super(stateChangeListener);
        this.queue = new LinkedBlockingDeque<>(64);
        this.service = Executors.newCachedThreadPool();
        this.errorHandler = errorHandler;
    }

    @Override
    public void onReceive(Consumer<Integer> vkCodeHandler) {
        CompletableFuture.runAsync(() -> {
            try {
                while (!service.isTerminated()) {
                    var vkCode = queue.take();
                    vkCodeHandler.accept(vkCode);
                }
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        }, service);
    }

    @Override
    public void connect(String host, int port) {
        var address = new InetSocketAddress(host, port);
        CompletableFuture.runAsync(() -> {
            if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.CONNECTED)) {
                try (var clientChannel = SocketChannel.open()) {
                    clientChannel.connect(address);
                    this.channel = clientChannel;
                    readFromChannel(queue::put, clientChannel);
                    disconnect();
                } catch (Exception e) {
                    errorHandler.accept(e);
                }
            } else {
                errorHandler.accept(new Exception("already connected"));
            }
        }, service);
    }

    @Override
    public void disconnect() {
        if (updateState(ChannelState.DISCONNECTED) != ChannelState.DISCONNECTED) {
            try {
                channel.close();
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
