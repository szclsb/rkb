package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.ISender;
import ch.szclsb.rkb.comm.VkCodeEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class SenderChannel extends AbstractChannel implements ISender {
    private final BlockingQueue<VkCodeEvent> queue;
    private final ByteBuffer buffer;
    private volatile Thread serverThread;

    public SenderChannel() {
        this.queue = new ArrayBlockingQueue<>(255);
        this.buffer = ByteBuffer.allocate(4);
    }

    @Override
    public synchronized void open(int port) throws IOException {
        if (compareAndSetState(ChannelState.WAITING, ChannelState.DISCONNECTED)) {
            this.serverThread = Thread.ofVirtual().start(() -> {
                try (var socket = ServerSocketChannel.open().bind(new InetSocketAddress(port))) {
                    while (ChannelState.WAITING.equals(getState())) {
                        try (var channel = socket.accept()) {
                            queue.clear();
                            if (compareAndSetState(ChannelState.CONNECTED, ChannelState.WAITING)) {
                                while (ChannelState.CONNECTED.equals(getState())) {
                                    try {
                                        var event = queue.poll(3L, TimeUnit.SECONDS);
                                        if (event == null) {
                                            transmit(channel, HEARTBEAT_EVENT);
                                        } else if (event.vkCode() > 0) {  // negative vk code to exit
                                            transmit(channel, event);
                                        } else {
                                            setState(ChannelState.WAITING);
                                        }
                                    } catch (IOException e) {
                                        setState(ChannelState.WAITING);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    setState(ChannelState.DISCONNECTED);
                }
            });
        }
    }

    private void transmit(SocketChannel channel, VkCodeEvent event) throws IOException {
        buffer.clear();
        buffer.putInt(event.vkCode() * (event.up() ? -1 : 1));  // send key press as positive vkCode, send key release as negative vkCode
        buffer.flip();
        channel.write(buffer);
    }

    @Override
    public boolean send(int vkCode, boolean up) {
        if (ChannelState.CONNECTED.equals(getState())) {
            return queue.offer(new VkCodeEvent(vkCode, up));
        }
        return false;
    }

    @Override
    public void disconnect() {
        queue.clear();
        queue.offer(STOP_EVENT);
    }

    @Override
    public synchronized void terminate() {
        if (serverThread != null) {
            this.serverThread.interrupt();
        }
    }
}
