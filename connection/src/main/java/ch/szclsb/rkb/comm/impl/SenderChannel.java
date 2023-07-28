package ch.szclsb.rkb.comm.impl;

import ch.szclsb.rkb.comm.ChannelState;
import ch.szclsb.rkb.comm.ISender;
import ch.szclsb.rkb.comm.VkCodeEvent;

import java.io.IOException;
import java.lang.invoke.DirectMethodHandle$Holder;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class SenderChannel extends AbstractChannel implements ISender {
    private final BlockingQueue<VkCodeEvent> queue;
    private final ByteBuffer buffer;
    private volatile ServerSocket serverSocket;
    private volatile Socket socket;

    public SenderChannel() {
        this.queue = new ArrayBlockingQueue<>(255);
        this.buffer = ByteBuffer.allocate(4);
    }

    @Override
    public void open(int port) throws IOException {
        if (compareAndSetState(ChannelState.DISCONNECTED, ChannelState.WAITING)) {
            this.serverSocket = new ServerSocket(port);
            Thread.ofVirtual().start(() -> {
                while (ChannelState.WAITING.equals(getState())) {
                    try {
                        this.socket = serverSocket.accept();
                        queue.clear();
                        if (compareAndSetState(ChannelState.WAITING, ChannelState.CONNECTED)) {
                            while (ChannelState.CONNECTED.equals(getState())) {
                                try {
                                    var event = queue.take();
                                    if (event.vkCode() > 0) {  // negative vk code to exit
                                        buffer.clear();
                                        buffer.putInt(event.vkCode() * (event.up() ? -1 : 1));  // send key press as positive vkCode, send key release as negative vkCode
                                        buffer.flip();
                                        socket.getChannel().write(buffer);
                                    }
                                } catch (IOException e) {
                                    setState(ChannelState.WAITING);
                                }
                            }
                        }
                    } catch (InterruptedException | IOException e) {
                        setState(ChannelState.DISCONNECTED);
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
        socket.close();  // interrupt socket if currently writing
        if (compareAndSetState(ChannelState.CONNECTED, ChannelState.WAITING)) {
            queue.clear();
            queue.add(new VkCodeEvent(-1, false));  // interrupt waiting queue
        }
    }

    @Override
    public void close() throws Exception {
        socket.close();  // interrupt socket if currently writing
        serverSocket.close();  // interrupt server if currently waiting
    }
}
