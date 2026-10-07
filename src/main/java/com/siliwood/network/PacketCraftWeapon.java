package com.siliwood.network;

import com.siliwood.crafting.WeaponCrafting;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server: "craft weapon with id <name> at the armory workbench".
 * All validation (materials, distance to station) happens server-side.
 * NOTE: Java 8 lambda forbidden in this codebase — use an anonymous ITask.
 */
public class PacketCraftWeapon implements IMessage, IMessageHandler<PacketCraftWeapon, IMessage>
{
    private String weaponId;

    public PacketCraftWeapon() {}

    public PacketCraftWeapon(String weaponId)
    {
        this.weaponId = weaponId;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        int len = buf.readUnsignedShort();
        byte[] b = new byte[len];
        buf.readBytes(b);
        weaponId = new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        byte[] b = weaponId.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(b.length);
        buf.writeBytes(b);
    }

    @Override
    public IMessage onMessage(final PacketCraftWeapon msg, MessageContext ctx)
    {
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        // Forge 1.12: packets arrive on the network thread — hop to the main thread.
        player.serverWorld.addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                WeaponCrafting.tryCraft(player, msg.weaponId);
            }
        });
        return null;
    }
}
