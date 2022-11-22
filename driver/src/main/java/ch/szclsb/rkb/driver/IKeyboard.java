package ch.szclsb.rkb.driver;

import java.util.function.BiConsumer;

public interface IKeyboard {
    /**
     * Adds a Windows virtual key listener.
     *
     * @param listener - Listener first arg is Windows virtual key code
     *                 second args is <code>false</code> if key is pressed, otherwise <code>true</code> if key is released
     */
    void addVkCodeListener(BiConsumer<Integer, Boolean> listener);

    /**
     * Simulates a key press or release.
     *
     * @param vkCode - Windows virtual key code
     * @param up - <code>false</code> if key is pressed, <code>true</code> if key is released
     */
    void invoke(int vkCode, boolean up) throws Throwable;

    /**
     * Starts scanning the Windows virtual key.
     * This method intercepts system keys, so ALT+TAB and other system shortcuts won't work.
     */
    void scan() throws Throwable;

    /**
     * Stops scanning the pressed and released keystrokes.
     */
    void stop() throws Throwable;

}
