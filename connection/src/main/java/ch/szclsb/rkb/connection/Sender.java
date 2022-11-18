package ch.szclsb.rkb.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Sender implements AutoCloseable {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final ByteBuffer buffer;
    private final AtomicReference<ServerSocketChannel> server;
    private final AtomicReference<SocketChannel> channel;
    private final Consumer<Throwable> errorHandler;

    public Sender(Consumer<Throwable> errorHandler) {
        this.queue = new LinkedBlockingQueue<>(64);
        this.service = Executors.newCachedThreadPool();
        this.buffer = ByteBuffer.allocate(4);
        this.server = new AtomicReference<>();
        this.channel = new AtomicReference<>();
        this.errorHandler = errorHandler;
    }

    public void open(int port) {
        var address = new InetSocketAddress("localhost", port);
        // incoming connections
        CompletableFuture.runAsync(() -> {
            try (var serverChannel = ServerSocketChannel.open()) {
                serverChannel.socket().bind(address);
                if (server.compareAndSet(null, serverChannel)) {
                    while (!service.isTerminated()) {
                        var client = serverChannel.accept();
                        if (!this.channel.compareAndSet(null, client)) {
                            errorHandler.accept(new Exception("already connected"));
                        }
                    }
                }
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        }, service);
        CompletableFuture.runAsync(() -> {
            try {
                while (!service.isTerminated()) {
                    var vkCode = queue.take();
                    // todo block until channel is available
                    var c = channel.get();
                    if (c != null) {
                        buffer.putInt(vkCode);
                        channel.get().write(buffer);
                    } else {
                        errorHandler.accept(new NotYetConnectedException());
                    }
                }
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        }, service);
    }

    public boolean isListening() {
        return server.get() != null;
    }

    public boolean isConnected() {
        return channel.get() != null;
    }

    public void send(int vkCode) {
        queue.add(vkCode);
    }

    public void disconnect() throws IOException {
        var c = channel.getAndSet(null);
        if (c != null) {
            c.close();
        }
    }

    @Override
    public void close() throws Exception {
        service.close();
        var sc = server.getAndSet(null);
        if (sc != null) {
            sc.close();
        }
        disconnect();
    }
}
