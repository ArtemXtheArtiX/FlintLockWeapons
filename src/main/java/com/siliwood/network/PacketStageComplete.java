package com.siliwood.network;

import com.siliwood.handler.FirearmManager;
import com.siliwood.item.ItemFirearm;
import com.siliwood.util.WeaponNBT;
import com.siliwood.weapon.ReloadStage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server: "I finished reload stage <id>" (or -1 = fully loaded).
 * The server re-validates gunpowder and writes the same progress into its copy
 * of the stack, so both sides agree even after an interrupted reload.
 */
public class PacketStageComplete implements IMessage, IMessageHandler<PacketStageComplete, IMessage>
{
    private int stageId;

    public PacketStageComplete() {}

    public PacketStageComplete(int stageId)
    {
        this.stageId = stageId;
    }

    @Override
    public void fromBytes(ByteBuf buf) { stageId = buf.readInt(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeInt(stageId); }

    @Override
    public IMessage onMessage(PacketStageComplete msg, MessageContext ctx)
    {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !(held.getItem() instanceof ItemFirearm)) return null;
        ItemFirearm gun = (ItemFirearm) held.getItem();

        if (msg.stageId < 0)
        {
            FirearmManager.markLoadedOnServer(held);
        }
        else
        {
            FirearmManager.applyStageOnServer(player, held, gun, ReloadStage.fromId(msg.stageId));
        }
        // sync NBT back to client (hotbar is part of the player inventory container)
        player.sendContainerAndContentsToPlayer(player.inventoryContainer,
                player.inventoryContainer.getInventory());
        return null;
    }
}
