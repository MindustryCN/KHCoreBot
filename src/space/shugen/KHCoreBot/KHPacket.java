package space.shugen.KHCoreBot;


import org.jetbrains.annotations.Nullable;

public class KHPacket<T> {
    public int s;
    public T data;
    @Nullable
    public int sn;
}
