package com.siliwood.util;

import com.siliwood.weapon.ReloadStage;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * NBT helpers storing staged reload progress inside the weapon stack, so that
 * cancelling a reload (slot swap / deselect) preserves completed stages.
 *
 * Keys:
 *   SW_Stage  : int id of last COMPLETED stage (0 none,1 powder,2 rammed,3 cycled)
 *   SW_Mode   : 0 = fire, 1 = melee(bayonet)
 *   SW_Loaded : 1 when ready to shoot
 */
public final class WeaponNBT
{
    public static final String TAG_STAGE  = "SW_Stage";
    public static final String TAG_MODE   = "SW_Mode";
    public static final String TAG_LOADED = "SW_Loaded";
    public static final String TAG_USES   = "SW_UsesLeft"; // optional durability mirror

    private WeaponNBT() {}

    public static NBTTagCompound tag(ItemStack stack)
    {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    /** Last completed stage (not the in-progress one). */
    public static ReloadStage completedStage(ItemStack stack)
    {
        return ReloadStage.fromId(tag(stack).getInteger(TAG_STAGE));
    }

    public static void setCompletedStage(ItemStack stack, ReloadStage stage)
    {
        tag(stack).setInteger(TAG_STAGE, stage.id);
    }

    public static boolean isLoaded(ItemStack stack)
    {
        return tag(stack).getBoolean(TAG_LOADED);
    }

    public static void setLoaded(ItemStack stack, boolean loaded)
    {
        tag(stack).setBoolean(TAG_LOADED, loaded);
    }

    public static int mode(ItemStack stack)
    {
        return tag(stack).getInteger(TAG_MODE); // 0 fire, 1 melee
    }

    public static void setMode(ItemStack stack, int m)
    {
        tag(stack).setInteger(TAG_MODE, m);
    }

    public static void resetReload(ItemStack stack)
    {
        NBTTagCompound t = tag(stack);
        t.setInteger(TAG_STAGE, ReloadStage.NONE.id);
        t.setBoolean(TAG_LOADED, false);
    }
}
