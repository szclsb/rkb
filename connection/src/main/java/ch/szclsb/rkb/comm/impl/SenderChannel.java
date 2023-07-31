package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.ISender;
import ch.szclsb.rkb.comm.VkCodeEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SenderChannel extends AbstractChannel implements ISender {
    private final BlockingQueue<VkCodeEvent> queue;
    private final ByteBuffer buffer;
    private volatile ServerSocketChannel serverSocketChannel;
    private volatile SocketChannel channel;

    public SenderChannel() {
        this.queue = new ArrayBlockingQueue<>(255);
        this.buffer = ByteBuffer.allocate(4);
    }

    @Override
    public void open(int port) throws IOException {
        if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.WAITING)) {
            this.serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            Thread.ofVirtual().start(() -> {
                while (ChannelState.WAITING.equals(getState())) {
                    try {
                        this.channel = serverSocketChannel.accept();
                        queue.clear();
                        if (compareAndSetState(ChannelState.WAITING, ChannelState.CONNECTED)) {
                            while (ChannelState.CONNECTED.equals(getState())) {
                                try {
                                    var event = queue.take();
                                    if (event.vkCode() > 0) {  // negative vk code to exit
                                        buffer.clear();
                                        buffer.putInt(event.vkCode() * (event.up() ? -1 : 1));  // send key press as positive vkCode, send key release as negative vkCode
                                        buffer.flip();
                                        channel.write(buffer);
                                    }
                                } catch (IOException e) {
                                    setState(ChannelState.WAITING);
                                }
                            }
                        }
                    } catch (InterruptedException | IOException e) {
                        setState(ChannelState.DISCONNECTED);
                    } finally {
                        try {
                            channel.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean send(int vkCode, boolean up) {
        if (ChannelState.CONNECTED.equals(getState())) {
            return queue.offer(new VkCodeEvent(vkCode, up));
        }
        return false;
    }

    @Override
    public void disconnect() throws IOException {
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.WAITING)) {
            channel.close();  // interrupt socket if currently writing
            queue.clear();
            queue.add(STOP_EVENT);  // interrupt waiting queue
        }
    }

    @Override
    public void close() throws Exception {
        disconnect();
        if (serverSocketChannel != null) {
            serverSocketChannel.close();  // interrupt server if currently waiting
        }
    }
}
