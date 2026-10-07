package com.siliwood.handler;

import com.siliwood.entity.EntityBullet;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Server-side housekeeping: enforce the active-bullet cap per world once in a
 * while (every 10 ticks) instead of every tick — cheap optimization.
 */
public class ServerEvents
{
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof WorldServer)) return;
        if (event.world.getTotalWorldTime() % 10 != 0) return;
        EntityBullet.enforceCap(event.world);
    }
}
