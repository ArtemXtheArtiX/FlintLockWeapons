package com.siliwood.crafting;

import com.siliwood.SiliwoodConfig;
import com.siliwood.SiliwoodMod;
import com.siliwood.item.ItemFirearm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.math.BlockPos;

/**
 * Server-side authority for crafting weapons at the Armory Workbench.
 * Validates: player near a workbench block, has all materials, then consumes
 * them (unless config disables consumption) and gives the weapon with a fresh
 * (empty) reload-progress NBT state.
 */
public final class WeaponCrafting
{
    private WeaponCrafting() {}

    public static boolean isNearWorkbench(EntityPlayer player)
    {
        BlockPos p = player.getPosition();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++)
                    if (player.world.getBlockState(p.add(dx, dy, dz)).getBlock()
                            == SiliwoodMod.ARMORY_WORKBENCH)
                        return true;
        return false;
    }

    public static boolean hasMaterials(EntityPlayer player, WeaponRecipes.Cost cost)
    {
        for (ItemStack need : cost.inputs)
        {
            int have = 0;
            for (ItemStack inv : player.inventory.mainInventory)
                if (inv != null && isItemEqual(inv, need)) have += inv.stackSize;
            if (have < need.stackSize) return false;
        }
        return true;
    }

    private static boolean isItemEqual(ItemStack a, ItemStack b)
    {
        return a.getItem() == b.getItem()
                && (a.getItemDamage() == b.getItemDamage() || b.getMetadata() == Short.MAX_VALUE);
    }

    public static void consumeMaterials(EntityPlayer player, WeaponRecipes.Cost cost)
    {
        if (!SiliwoodConfig.stationConsumesMaterials) return;
        for (ItemStack need : cost.inputs)
        {
            int left = need.stackSize;
            for (int i = 0; i < player.inventory.mainInventory.size() && left > 0; i++)
            {
                ItemStack inv = player.inventory.mainInventory.get(i);
                if (inv != null && isItemEqual(inv, need))
                {
                    int take = Math.min(left, inv.stackSize);
                    left -= take;
                    inv.stackSize -= take;
                    if (inv.stackSize <= 0) player.inventory.mainInventory.set(i, null);
                }
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    /** Called on the server main thread from PacketCraftWeapon. */
    public static void tryCraft(EntityPlayer player, String weaponId)
    {
        if (!isNearWorkbench(player))
        {
            player.addChatComponentMessage(new ChatComponentText(
                    "§cНужен верстак оружейника рядом."), false);
            return;
        }
        ItemFirearm gun = SiliwoodMod.firearm(weaponId);
        WeaponRecipes.Cost cost = WeaponRecipes.get(weaponId);
        if (gun == null || cost == null)
        {
            player.addChatComponentMessage(new ChatComponentText(
                    "§cНеизвестное оружие: " + weaponId), false);
            return;
        }
        if (!hasMaterials(player, cost))
        {
            player.addChatComponentMessage(new ChatComponentText(
                    "§cНе хватает материалов для «" + gun.stats.id + "»."), false);
            return;
        }
        consumeMaterials(player, cost);
        ItemStack out = new ItemStack(gun);
        // fresh stack: no stages completed, unloaded, fire mode
        out.setItemDamage(0);
        if (!player.inventory.addItemStackToInventory(out))
            player.dropItem(out, false);
        player.addChatComponentMessage(new ChatComponentText(
                "§aСобрано: " + gun.stats.id), false);
        player.inventoryContainer.detectAndSendChanges();
    }
}
