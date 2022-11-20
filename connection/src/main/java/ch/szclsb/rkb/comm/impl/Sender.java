package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ISender;
import ch.szclsb.rkb.comm.ChannelState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Sender extends AbstractChannel implements ISender {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final Consumer<Throwable> errorHandler;

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
            if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.STARTING)) {
                try (var serverChannel = ServerSocketChannel.open()) {
                    serverChannel.bind(address);
                    updateState(ChannelState.WAITING);
                    while (getState() == ChannelState.WAITING) {
                        // client connection
                        try (var clientChannel = serverChannel.accept()) {
                            if (compareAndSetState(ChannelState.WAITING, ChannelState.CONNECTING)) {
                                queue.clear();
                                // todo: validate client
                                if (compareAndSetState(ChannelState.CONNECTING, ChannelState.CONNECTED)) {
                                    int vkCode;
                                    while ((vkCode = queue.take()) != 0) {
                                        writeToChannel(vkCode, clientChannel);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            errorHandler.accept(e);
                        } finally {
                            compareAndSetState(ChannelState.DISCONNECTING, ChannelState.WAITING);
                        }
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
    public void send(int vkCode) {
        // vk 1 to 254  windows vkCodes;
        // vk (-1) to (-254)  windows vkCodes;
        if (vkCode == 0 || vkCode < -254 || vkCode > 254) {
            throw new IllegalArgumentException("invalid vk code " + vkCode);
        }
        queue.add(vkCode);
    }

    @Override
    public void disconnect() {
        compareAndSetState(ChannelState.CONNECTING, ChannelState.DISCONNECTING); // abort during client validation
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.DISCONNECTING)) {
            queue.clear();
            queue.add(0);
        }
    }

    @Override
    public void stop() {
        compareAndSetState(ChannelState.CONNECTING, ChannelState.TERMINATING); // abort during client validation
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.TERMINATING)) {
            queue.clear();
            queue.add(0);
        }
    }

    @Override
    public void close() throws Exception {
        stop();
        service.close();
    }
}
