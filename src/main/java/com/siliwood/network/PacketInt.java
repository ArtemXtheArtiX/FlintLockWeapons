package com.siliwood.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Base for tiny packets that only carry an int payload.
 */
public abstract class PacketInt implements IMessage
{
    protected int value;

    protected PacketInt() {}
    protected PacketInt(int value) { this.value = value; }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(value);
    }
}
