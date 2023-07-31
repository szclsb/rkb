package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ReceiverChannel extends AbstractChannel implements IReceiver {
    private final BlockingQueue<VkCodeEvent> queue;
    private final ByteBuffer buffer;
    private volatile SocketChannel channel;

    public ReceiverChannel() {
        this.queue = new ArrayBlockingQueue<>(255);
        this.buffer = ByteBuffer.allocate(4);
    }

    @Override
    public void connect(String host, int port, VkCodeHandler listener) throws IOException {
        if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.CONNECTING)) {
            var address = new InetSocketAddress(host, port);
            // consumer thread
            Thread.ofVirtual().start(() -> {
                try {
                    queue.clear();
                    boolean run = true;
                    while (run) {
                        var event = queue.take();
                        if (event.vkCode() >= 0) {
                            listener.invoke(event.vkCode(), event.up());
                        } else {
                            run = false;
                        }
                    }
                } catch (InterruptedException ie) {
                    try {
                        close();
                    } catch (Exception ignore) {
                    }
                }
            });
            // worker thread
            Thread.ofVirtual().start(() -> {
                try {
                    this.channel = SocketChannel.open(address);
                    if (compareAndSetState(ChannelState.CONNECTING, ChannelState.CONNECTED)) {
                        int size;
                        while ((size = channel.read(buffer)) != -1) {
                            buffer.rewind();
                            //todo improve
                            if (size == 4) {
                                var value = buffer.getInt();
                                queue.offer(new VkCodeEvent(Math.abs(value), value < 0));
                            }
                        }
                    }
                } catch (IOException e) {
                } finally {
                    try {
                        setState(ChannelState.DISCONNECTED);
                        queue.clear();
                        queue.offer(STOP_EVENT);
                        channel.close();
                    } catch (IOException ignore) {
                    }
                }
            });
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.DISCONNECTED)) {
            channel.close();  // interrupt socket if currently writing
        }
    }

    @Override
    public void close() throws Exception {
        disconnect();
    }
}
