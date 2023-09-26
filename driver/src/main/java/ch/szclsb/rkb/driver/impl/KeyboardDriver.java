package ch.szclsb.rkb.driver.impl;

import ch.szclsb.rkb.driver.IKeyboard;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static java.lang.foreign.ValueLayout.*;

public class KeyboardDriver implements IKeyboard, AutoCloseable {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOADER = SymbolLookup.loaderLookup();
    private static final KeyboardDriver instance = new KeyboardDriver();
    private static final Set<BiConsumer<Integer, Boolean>> vkCodeListeners = ConcurrentHashMap.newKeySet();

    public static KeyboardDriver getInstance() {
        return instance;
    }

    private static MemorySegment loadSymbol(String name) {
        return LOADER.find(name).orElseThrow(() -> new UnsatisfiedLinkError("unable to find symbol " + name));
    }

    /*
     * C function pointer method, must be a static method to be invoked as an upcall stub.
     */
    private static void upcall(int vkCode, boolean up) {
        // todo run async
        for (var listener : vkCodeListeners) {
            listener.accept(vkCode, up);
        }
    }

    private final Arena session;
    private final MemorySegment upcallStub;
    private final MethodHandle invokeNative;
    private final MethodHandle scanNative;
    private final MethodHandle stopNative;

    private KeyboardDriver() {
        this.session = Arena.ofShared();

        var dir = System.getProperty("user.dir");
        System.load(dir + "/build-native/Debug/rkb_native.dll");
        this.invokeNative = LINKER.downcallHandle(loadSymbol("invoke"), FunctionDescriptor.ofVoid(JAVA_INT, JAVA_BOOLEAN));
        this.scanNative = LINKER.downcallHandle(loadSymbol("scan"), FunctionDescriptor.ofVoid(ADDRESS));
        this.stopNative = LINKER.downcallHandle(loadSymbol("stop"), FunctionDescriptor.ofVoid());

        try {
            var descriptor = FunctionDescriptor.ofVoid(JAVA_INT, JAVA_BOOLEAN);
            var methodHandle = MethodHandles.lookup().findStatic(KeyboardDriver.class, "upcall", descriptor.toMethodType());
            this.upcallStub = LINKER.upcallStub(methodHandle, descriptor, session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addVkCodeListener(BiConsumer<Integer, Boolean> listener) {
        vkCodeListeners.add(listener);
    }


    @Override
    public void invoke(int vkCode, boolean up) throws Throwable {
        invokeNative.invoke(vkCode, up);
    }

    @Override
    public void scan() throws Throwable {
        scanNative.invoke(upcallStub);
    }

    @Override
    public void stop() throws Throwable {
        stopNative.invoke();
    }

    @Override
    public void close() throws Exception {
        this.session.close();
    }
}
