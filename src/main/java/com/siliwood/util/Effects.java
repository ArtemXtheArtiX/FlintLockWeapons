package com.siliwood.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Cheap muzzle-flash / smoke effects. All particle spawning is guarded by
 * SiliwoodConfig.enableParticles for weak hardware.
 */
public final class Effects
{
    private Effects() {}

    public static void muzzleFlash(EntityPlayer player, boolean loud)
    {
        World w = player.world;
        if (w.isRemote) return; // spawn via aux/sfx + world particles below

        double yawRad = -player.rotationYaw / 180.0F * (float) Math.PI;
        double pitchRad = -player.rotationPitch / 180.0F * (float) Math.PI;
        double dx = Math.cos(pitchRad) * Math.cos(yawRad);
        double dy = Math.sin(pitchRad);
        double dz = Math.cos(pitchRad) * Math.sin(yawRad);
        double bx = player.posX + dx * 0.9;
        double by = player.posY + player.getEyeHeight() + dy * 0.9 - 0.05;
        double bz = player.posZ + dz * 0.9;

        if (com.siliwood.SiliwoodConfig.enableParticles && w instanceof World)
        {
            // flame puff
            for (int i = 0; i < 6; i++)
                w.spawnParticle("smoke", bx + rnd(), by + rnd(), bz + rnd(), dx * 0.4, dy * 0.4, dz * 0.4);
            for (int i = 0; i < 4; i++)
                w.spawnParticle("flame", bx + rnd(), by + rnd(), bz + rnd(), 0, 0.05, 0);
        }
        if (loud)
        {
            // loud bang: like a far thunder crack — scares mobs in radius
            w.playSoundEffect(bx, by, bz, "random.explode", 1.2f, 0.7f + w.rand.nextFloat() * 0.3f);
            scareNearbyMobs(w, player);
        }
        else
        {
            w.playSoundEffect(bx, by, bz, "random.fizz", 0.6f, 1.0f);
        }
    }

    private static double rnd() { return (Math.random() - 0.5) * 0.12; }

    private static void scareNearbyMobs(World w, EntityPlayer player)
    {
        if (!com.siliwood.SiliwoodConfig.loudWeaponsScareMobs) return;
        BlockPos p = player.getPosition();
        for (net.minecraft.entity.Entity entity : w.getEntitiesWithinAABBExcludingEntity(
                player, player.getEntityBoundingBox().expand(16.0, 8.0, 16.0)))
        {
            if (entity instanceof net.minecraft.entity.EntityLiving)
            {
                ((net.minecraft.entity.EntityLiving) entity).addPotionEffect(
                        new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.moveSlowdown.id, 40, 1));
            }
        }
    }
}
