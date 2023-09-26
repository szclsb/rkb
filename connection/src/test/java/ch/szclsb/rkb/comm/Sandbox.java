package ch.szclsb.rkb.comm;

import ch.szclsb.rkb.comm.impl.ReceiverChannel;
import ch.szclsb.rkb.comm.impl.SenderChannel;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public interface Sandbox {
    void play(SenderChannel sender, ReceiverChannel receiver,
              CountDownLatch connectLatch, CountDownLatch sendLatch) throws Exception;
}
