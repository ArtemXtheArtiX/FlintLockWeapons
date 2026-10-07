package com.siliwood.network;

import com.siliwood.util.WeaponNBT;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server: toggle Fire/Melee (bayonet) mode of the held weapon.
 */
public class PacketToggleMode implements IMessage, IMessageHandler<PacketToggleMode, IMessage>
{
    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    @Override
    public IMessage onMessage(PacketToggleMode msg, MessageContext ctx)
    {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player.getCurrentEquippedItem() != null)
        {
            int m = WeaponNBT.mode(player.getCurrentEquippedItem()) == 0 ? 1 : 0;
            WeaponNBT.setMode(player.getCurrentEquippedItem(), m);
        }
        return null;
    }
}
