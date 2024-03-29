package ch.szclsb.rkb.comm;

import ch.szclsb.rkb.comm.impl.ReceiverChannel;
import ch.szclsb.rkb.comm.impl.SenderChannel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IT")
public class CommIT {
    private final String host = "localhost";
    private final int port = 9797;

    private void prepare(Sandbox sandbox,
                         Consumer<List<ChannelState>> senderStateConsumer,
                         Consumer<List<ChannelState>> receiverStateConsumer) throws Exception {
        var connectLatch = new CountDownLatch(1);
        var sendLatch = new CountDownLatch(2);
        var senderStateList = new ArrayList<ChannelState>();
        var receiverStateList = new ArrayList<ChannelState>();
//        Consumer<Throwable> errorHandler = t -> fail(t.getMessage());
        try (var sender = new SenderChannel()) {
            try (var receiver = new ReceiverChannel()) {
                sender.addStateChangeListener(state -> {
                    senderStateList.add(state);
//                    System.out.printf("sender: %s\n", state.name());
                    try {
                        switch (state) {
                            case WAITING -> connectLatch.countDown();
                            case CONNECTED -> sendLatch.countDown();
                        }
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
                receiver.addStateChangeListener(state -> {
                    receiverStateList.add(state);
//                    System.out.printf("receiver: %s\n", state.name());
                    try {
                        if (state.equals(ChannelState.CONNECTED)) {
                            sendLatch.countDown();
                        }
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
                sandbox.play(sender, receiver, connectLatch, sendLatch);
            }
        }
        senderStateConsumer.accept(senderStateList);
        receiverStateConsumer.accept(receiverStateList);
    }

    private Sandbox handshake(VkCodeHandler handler, Sandbox sandbox) {
        return (sender, receiver, connectLatch, sendLatch) -> {
            sender.open(port);
            if (!connectLatch.await(5, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
            receiver.connect(host, port, handler);
            if (!sendLatch.await(5, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
            sandbox.play(sender, receiver, connectLatch, sendLatch);
        };
    }

    private Consumer<List<ChannelState>> assertStates(ChannelState... expectedStates) {
        return states -> assertArrayEquals(Stream.of(expectedStates).toArray(ChannelState[]::new),
                states.toArray(ChannelState[]::new));
    }

    @Test
    @Disabled
    public void testComm() throws Exception {
        var vkCodes = List.of(
                new VkCodeEvent(127, false),
                new VkCodeEvent(127, true),
                new VkCodeEvent(65, false),
                new VkCodeEvent(65, true)
        );
        var queue = new LinkedBlockingQueue<VkCodeEvent>(5);
        prepare(handshake((vkCode1, up) -> {
                            try {
                                queue.offer(new VkCodeEvent(vkCode1, up), 200, TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                fail(e.getMessage());
                            }
                        }, (sender, receiver, connectLatch, sendLatch) -> {
                            vkCodes.forEach(vkCode -> sender.send(vkCode.vkCode(), vkCode.up()));
                            vkCodes.forEach(vkCode -> {
                                try {
                                    var receivedCode = queue.poll(5, TimeUnit.SECONDS);
                                    assertEquals(vkCode, receivedCode);
                                } catch (Exception e) {
                                    fail(e.getMessage());
                                }
                            });
                        }
                ), assertStates(ChannelState.WAITING,
                        ChannelState.CONNECTED,
                        ChannelState.DISCONNECTED),
                assertStates(ChannelState.CONNECTING,
                        ChannelState.CONNECTED,
                        ChannelState.DISCONNECTED)
        );
    }
}
