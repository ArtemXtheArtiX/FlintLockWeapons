package com.siliwood.item;

import com.siliwood.SiliwoodConfig;
import com.siliwood.SiliwoodMod;
import com.siliwood.handler.FirearmManager;
import com.siliwood.network.PacketHandler;
import com.siliwood.network.PacketToggleMode;
import com.siliwood.util.WeaponNBT;
import com.siliwood.weapon.ReloadStage;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * One class for every blackpowder weapon. Behaviour (reload stages, timing,
 * bayonet modes) is driven entirely by its WeaponStats loaded from config.
 */
public class ItemFirearm extends Item
{
    public final WeaponStats stats;

    public ItemFirearm(WeaponStats stats)
    {
        this.stats = stats;
        setRegistryName(stats.id);
        setUnlocalizedName(SiliwoodMod.MODID + "." + stats.id);
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
        if (stats.durability > 0) setMaxDamage(stats.durability);
        setNoRepair();
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced)
    {
        ReloadStage done = WeaponNBT.completedStage(stack);
        if (WeaponNBT.isLoaded(stack))
            lines.add(EnumChatFormatting.GREEN + "Заряжено — ПКМ: выстрел");
        else if (done != ReloadStage.NONE)
            lines.add(EnumChatFormatting.GOLD + "Прерванная перезарядка: «" + stageName(done) + "» — ПКМ продолжить");
        else
            lines.add(EnumChatFormatting.GRAY + "Не заряжено — ПКМ: поэтапная перезарядка");

        if (stats.hasBayonetMount && SiliwoodConfig.bayonetsEnabled)
            lines.add(EnumChatFormatting.AQUA + "Режим: " +
                    (WeaponNBT.mode(stack) == 0 ? EnumChatFormatting.RED + "Огонь"
                    : EnumChatFormatting.YELLOW + "Удар") +
                    EnumChatFormatting.GRAY + "  [F] сменить");
    }

    public static String stageName(ReloadStage s)
    {
        switch (s)
        {
            case POUR_POWDER:    return "порох засыпан";
            case RAM_PROJECTILE: return "снаряд протолкнут";
            case CYCLE_ACTION:   return "затвор передёрнут";
            default:             return "нет";
        }
    }

    // ------------------------------------------------------------- interactions

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
        if (!world.isRemote)
        {
            // Server: shooting only. Reload runs client-driven and syncs each
            // completed stage via PacketStageComplete.
            if (WeaponNBT.mode(stack) == 0 && WeaponNBT.isLoaded(stack))
                FirearmManager.shoot(player, stack, this);
            return stack;
        }

        if (WeaponNBT.mode(stack) == 1 && stats.hasBayonetMount)
            return stack; // melee mode: right-click does nothing

        if (WeaponNBT.isLoaded(stack))
        {
            // fire handled server-side through vanilla right-click propagation
            return stack;
        }

        if (!FirearmManager.isReloading())
            FirearmManager.beginReload(player, stack, this);

        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft)
    {
        // Releasing the button does NOT cancel reload stages — the state machine
        // keeps running in ClientTickHandler; progress lives in NBT anyway.
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.BLOCK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return 72000;
    }

    /** Bayonet melee handling (mode == 1). */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, net.minecraft.entity.Entity entity)
    {
        if (!stats.hasBayonetMount || !SiliwoodConfig.bayonetsEnabled)
            return false;
        if (WeaponNBT.mode(stack) != 1)
            return false;
        if (!player.world.isRemote && entity instanceof EntityLivingBase)
        {
            DamageSource src = DamageSource.causePlayerDamage(player).setDamageBypassesArmor();
            entity.attackEntityFrom(src, stats.bayonetDamage);
            if (stack.getMaxDamage() > 0) stack.damageItem(1, player);
        }
        return true;
    }

    /** Toggle fire/melee; called client-side from KeyHandler, synced to server. */
    public void toggleMode(EntityPlayer player, ItemStack stack)
    {
        if (!stats.hasBayonetMount || !SiliwoodConfig.bayonetsEnabled) return;
        int m = WeaponNBT.mode(stack) == 0 ? 1 : 0;
        WeaponNBT.setMode(stack, m);
        if (player.world.isRemote)
        {
            player.addChatComponentMessage(new ChatComponentText(
                    m == 0 ? EnumChatFormatting.RED + "Режим: ОГОНЬ"
                           : EnumChatFormatting.YELLOW + "Режим: УДАР (штык)"), false);
            PacketHandler.sendToServer(new PacketToggleMode());
        }
    }

    // ----------------------------------------------------------- ammo helpers

    public static boolean hasGunpowder(EntityPlayer player, int count)
    {
        if (count <= 0) return true;
        int have = 0;
        for (ItemStack s : player.inventory.mainInventory)
            if (s != null && s.getItem() == Items.GUNPOWDER) have += s.stackSize;
        return have >= count;
    }

    public static void consumeGunpowder(EntityPlayer player, int count)
    {
        for (int i = 0; i < player.inventory.mainInventory.size() && count > 0; i++)
        {
            ItemStack s = player.inventory.mainInventory.get(i);
            if (s != null && s.getItem() == Items.GUNPOWDER)
            {
                int take = Math.min(count, s.stackSize);
                count -= take;
                s.stackSize -= take;
                if (s.stackSize <= 0) player.inventory.mainInventory.set(i, null);
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    @SideOnly(Side.CLIENT)
    public void registerModel()
    {
        ModelLoader.setCustomModelResourceLocation(this, 0,
                new ModelResourceLocation(SiliwoodMod.MODID + ":" + stats.id, "inventory"));
    }
}
