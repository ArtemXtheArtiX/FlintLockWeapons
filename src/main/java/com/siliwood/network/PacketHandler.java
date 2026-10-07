package com.siliwood.network;

import com.siliwood.SiliwoodMod;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Simple network wrapper: 2 tiny client->server packets.
 */
public final class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE =
            new SimpleNetworkWrapper(SiliwoodMod.MODID);

    private static int disc = 0;

    private PacketHandler() {}

    public static void init()
    {
        INSTANCE.registerMessage(PacketStageComplete.class, PacketStageComplete.class, disc++, Side.SERVER);
        INSTANCE.registerMessage(PacketToggleMode.class, PacketToggleMode.class, disc++, Side.SERVER);
    }

    public static void sendToServer(net.minecraftforge.fml.common.network.simpleimpl.IMessage msg)
    {
        INSTANCE.sendToServer(msg);
    }
}
