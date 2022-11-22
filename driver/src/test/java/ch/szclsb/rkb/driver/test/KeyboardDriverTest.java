package ch.szclsb.rkb.driver.test;

import ch.szclsb.rkb.driver.Code;
import ch.szclsb.rkb.driver.impl.KeyboardDriver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IT")
public class KeyboardDriverTest {
    @Test
    public void testScan() throws Exception {
        var queue = new LinkedBlockingDeque<Code>(10);
        CompletableFuture.runAsync(() -> {
            try (var keyboard = KeyboardDriver.getInstance()) {
                keyboard.addVkCodeListener((vkCode, up) -> {
                    try {
                        if (vkCode.equals(27)) {
                            keyboard.stop();  // stop if ESC pressed
                        } else {
                            queue.add(new Code(vkCode, up));
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
                var scanner = CompletableFuture.runAsync(() -> {
                    try {
                        keyboard.scan();   // calls SetWindowsHookExW
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
                Thread.sleep(500);  // dirty wait until LowLevelKeyboardProc has been hooked by windows
                keyboard.invoke(64, false);  // invoke a pressed
                keyboard.invoke(64, true);   // invoke a released
                keyboard.invoke(27, false);  // invoke ESC pressed
                scanner.get(500, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).get(5, TimeUnit.SECONDS);

        var code1 = queue.poll(20, TimeUnit.MILLISECONDS);
        var code2 = queue.poll(20, TimeUnit.MILLISECONDS);
        var size = queue.size();
        assertEquals(64, code1.vk());
        assertFalse(code1.up());
        assertEquals(64, code2.vk());
        assertTrue(code2.up());
        assertEquals(0, size);
    }
}
