package com.siliwood.entity;

import com.siliwood.SiliwoodConfig;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * Bullet / shot pellet. Extends EntityThrowable for cheap, sync-friendly
 * physics. Optimized: tiny AABB, short lifespan, no client-side work,
 * damage resolved server-side only, global active-bullet cap.
 */
public class EntityBullet extends EntityThrowable
{
    /** Damage carried as raw doubles so save/load keeps behaviour without stats obj. */
    private float bulletDamage = 8f;
    private boolean isPellet = false;
    private int age = 0;

    public EntityBullet(World worldIn)
    {
        super(worldIn);
        setSize(0.1f, 0.1f);
    }

    public EntityBullet(World worldIn, EntityLivingBase shooter, WeaponStats stats, boolean pellet)
    {
        super(worldIn, shooter);
        this.bulletDamage = stats.damage;
        this.isPellet = pellet;
        setSize(pellet ? 0.05f : 0.1f, pellet ? 0.05f : 0.1f);
        this.ignoreParentMaterials = true;
        // spawn slightly ahead of the eyes so the bullet doesn't clip the shooter
        double yawRad = -shooter.rotationYaw / 180.0F * (float) Math.PI;
        setPosition(
                shooter.posX + Math.cos(yawRad) * 0.6,
                shooter.posY + shooter.getEyeHeight() - 0.1,
                shooter.posZ + Math.sin(yawRad) * 0.6);
        prevRotationYaw = rotationYaw = shooter.rotationYaw;
        prevRotationPitch = rotationPitch = shooter.rotationPitch;
    }

    @Override
    protected void onImpact(MovingObjectPosition mop)
    {
        if (worldObj.isRemote)
        {
            setDead();
            return;
        }
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit != null)
        {
            Entity hit = mop.entityHit;
            boolean creativeTarget = hit instanceof EntityPlayer && ((EntityPlayer) hit).capabilities.isCreativeMode;
            if (!creativeTarget && hit instanceof EntityLivingBase)
            {
                hit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), bulletDamage);
                // mild knockback along bullet direction
                hit.motionX += motionX * 0.1;
                hit.motionZ += motionZ * 0.1;
            }
        }
        else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            worldObj.playAuxSFX(2001, new BlockPos(mop.getBlockPos()), 0);
        }
        setDead();
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (worldObj.isRemote) return; // optimization: zero extra client work
        if (++age > SiliwoodConfig.projectileDespawnTicks) setDead();
        if (getEntityData().getBoolean("SW_capKill")) setDead();
    }

    /** Server tick helper: enforce maxActiveProjectiles per world cheaply. */
    public static void enforceCap(World world)
    {
        int count = 0;
        for (Entity e : world.loadedEntityList)
        {
            if (e instanceof EntityBullet)
            {
                if (++count > SiliwoodConfig.maxActiveProjectiles)
                    ((EntityBullet) e).getEntityData().setBoolean("SW_capKill", true);
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag)
    {
        super.writeEntityToNBT(tag);
        tag.setFloat("SW_Dmg", bulletDamage);
        tag.setBoolean("SW_Pellet", isPellet);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag)
    {
        super.readEntityFromNBT(tag);
        bulletDamage = tag.getFloat("SW_Dmg");
        if (bulletDamage <= 0f) bulletDamage = 8f;
        isPellet = tag.getBoolean("SW_Pellet");
    }
}
