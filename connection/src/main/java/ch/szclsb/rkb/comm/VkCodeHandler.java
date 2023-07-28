package ch.szclsb.rkb.comm;

@FunctionalInterface
public interface VkCodeHandler {
    void invoke(int vkCode, boolean up) throws Exception;
}
