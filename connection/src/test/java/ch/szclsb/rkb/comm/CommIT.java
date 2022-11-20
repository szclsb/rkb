package ch.szclsb.rkb.comm;

import ch.szclsb.rkb.comm.impl.Receiver;
import ch.szclsb.rkb.comm.impl.Sender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("IT")
public class CommIT {
    private final String host = "localhost";
    private final int port = 9797;

    @Test
    public void testComm() throws Exception {
        var vkCode = 127;
        var connectLatch = new CountDownLatch(1);
        var sendLatch = new CountDownLatch(2);
        var assertExchanger = new Exchanger<Integer>();
        Consumer<Throwable> errorHandler = t -> fail(t.getMessage());
        try (var sender = new Sender(errorHandler)) {
            try (var receiver = new Receiver(errorHandler)) {
                sender.addStateChangeListener(state -> {
                    System.out.printf("sender: %s\n", state.name());
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
                    System.out.printf("receiver: %s\n", state.name());
                    try {
                        if (state.equals(ChannelState.CONNECTED)) {
                            sendLatch.countDown();
                        }
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
                receiver.addVkCodeListener(vkCode1 -> {
                    try {
                        assertExchanger.exchange(vkCode1, 5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
                sender.open(port);
                if (!connectLatch.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException();
                }
                receiver.connect(host, port);
                if (!sendLatch.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException();
                }
                sender.send(vkCode);
                int receivedCode = assertExchanger.exchange(null, 5, TimeUnit.SECONDS);
                assertEquals(vkCode, receivedCode);
            }
        }
    }
}
