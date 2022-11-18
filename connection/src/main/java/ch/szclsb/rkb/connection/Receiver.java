package ch.szclsb.rkb.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Receiver implements AutoCloseable {
    private final BlockingQueue<Integer> queue;
    private final ExecutorService service;
    private final ByteBuffer buffer;
    private final AtomicReference<SocketChannel> channel;
    private final Consumer<Integer> vkCodeHandler;
    private final Consumer<Throwable> errorHandler;

    public Receiver(Consumer<Integer> vkCodeHandler, Consumer<Throwable> errorHandler) {
        this.queue = new LinkedBlockingQueue<>(64);
        this.service = Executors.newCachedThreadPool();
        this.buffer = ByteBuffer.allocate(4);
        this.channel = new AtomicReference<>();
        this.vkCodeHandler = vkCodeHandler;
        this.errorHandler = errorHandler;
    }

    public void connect(String host, int port) {
        var address = new InetSocketAddress(host, port);
        CompletableFuture.runAsync(() -> {
            try (var clientChannel = SocketChannel.open()) {
                clientChannel.socket().bind(address);
                if (!channel.compareAndSet(null, clientChannel)) {
                    errorHandler.accept(new Exception("already connected"));
                }
                while ((clientChannel.read(buffer)) != -1) {
                    var vkCode = buffer.getInt();
                    queue.add(vkCode);
                }
            } catch (IOException e) {
                errorHandler.accept(e);
            }
        }, service);
        var f = CompletableFuture.runAsync(() -> {
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

    public boolean isConnected() {
        return channel.get() != null;
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
        disconnect();
    }
}
