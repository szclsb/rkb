package ch.szclsb.rkb.comm.impl;

@FunctionalInterface
public interface VkCodeHandler {
    void invoke(int vkCode) throws Exception;
}
