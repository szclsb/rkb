package ch.szclsb.rkb.comm;

import ch.szclsb.rkb.comm.impl.Receiver;
import ch.szclsb.rkb.comm.impl.Sender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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
        var blocker = new CountDownLatch(1);
        Consumer<Throwable> errorHandler = t -> fail(t.getMessage());
        var sender = new Sender(errorHandler, state -> {
            System.out.printf("sender: %s\n", state.name());
        });
        var receiver = new Receiver(errorHandler, state -> {
            System.out.printf("receiver: %s\n", state.name());
        });
        sender.open(port);
        receiver.onReceive(vkCode1 -> {
            assertEquals(vkCode, vkCode1);
            try {
                blocker.countDown();
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        receiver.connect(host, port);
        Thread.sleep(200);
        sender.send(vkCode);
        blocker.await(2, TimeUnit.SECONDS);
        receiver.close();
        sender.close();
    }
}
