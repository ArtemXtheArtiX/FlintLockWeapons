package com.siliwood.handler;

import com.siliwood.SiliwoodConfig;
import com.siliwood.entity.EntityBullet;
import com.siliwood.item.ItemFirearm;
import com.siliwood.network.PacketHandler;
import com.siliwood.network.PacketStageComplete;
import com.siliwood.util.Effects;
import com.siliwood.util.WeaponNBT;
import com.siliwood.weapon.ReloadStage;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

/**
 * Client-side driver of the staged reload state machine + server-side shooting.
 *
 * Key design: after EVERY completed stage the progress is written into the held
 * stack's NBT and synced to the server. If the player swaps slots mid-reload,
 * the session simply stops — the finished stages remain in NBT and the next
 * right-click resumes from exactly that stage (e.g. powder already poured).
 */
public final class FirearmManager
{
    // ---- client session ----
    private static EntityPlayer user;
    private static ItemStack weapon;
    private static ItemFirearm firearm;
    private static ReloadStage activeStage = null; // stage being performed NOW
    private static int stageTick = 0;
    private static int stageLength = 0;

    private FirearmManager() {}

    // ======================================================================
    //  CLIENT: staged reload
    // ======================================================================

    public static void beginReload(EntityPlayer player, ItemStack stack, ItemFirearm gun)
    {
        user = player; weapon = stack; firearm = gun;
        advanceToNextStage(WeaponNBT.completedStage(stack));
    }

    private static void advanceToNextStage(ReloadStage done)
    {
        WeaponStats s = firearm.stats;
        ReloadStage next = nextStageFor(s, done);
        if (next == null)
        {
            finishReload();
            return;
        }
        if (next == ReloadStage.POUR_POWDER && !ItemFirearm.hasGunpowder(user, powderNeeded()))
        {
            HudOverlay.setHint("§cНужен порох! (" + powderNeeded() + ")");
            stop(true);
            return;
        }
        activeStage = next;
        stageLength = Math.max(1, Math.round(stageTime(s, next) * SiliwoodConfig.reloadTimeMultiplier));
        stageTick = 0;
    }

    private static int powderNeeded()
    {
        return SiliwoodConfig.gunpowderPerPowderCharge * Math.max(1, firearm.stats.powderCost);
    }

    /** Stage route per real-world mechanism family. */
    public static ReloadStage nextStageFor(WeaponStats s, ReloadStage done)
    {
        switch (s.type)
        {
            case MUZZLELOADER:
            case FLINTLOCK_PISTOL:
            case PERCUSSION:
                // 1) насыпать порох 2) закинуть снаряд и протолкнуть шомполом 3) взвести курок
                if (done == ReloadStage.NONE)           return ReloadStage.POUR_POWDER;
                if (done == ReloadStage.POUR_POWDER)    return ReloadStage.RAM_PROJECTILE;
                if (done == ReloadStage.RAM_PROJECTILE) return ReloadStage.CYCLE_ACTION;
                return null;
            case BREAK_ACTION:
                // переломить стволы, заложить заряды, защёлкнуть затвор
                if (done == ReloadStage.NONE)           return ReloadStage.POUR_POWDER;
                if (done == ReloadStage.POUR_POWDER)    return ReloadStage.CYCLE_ACTION;
                return null;
            case NEEDLE_GUN:
            case BOLT_ACTION:
            default:
                // игловая Дрейзе / затворная винтовка: один цикл — дослать патрон и взвести
                if (done == ReloadStage.NONE)           return ReloadStage.CYCLE_ACTION;
                return null;
        }
    }

    private static int stageTime(WeaponStats s, ReloadStage st)
    {
        switch (st)
        {
            case POUR_POWDER:    return s.timePourPowder;
            case RAM_PROJECTILE: return isMuzzleKind(s) ? s.timeLoadBall + s.timeRam : s.timeLoadBall;
            case CYCLE_ACTION:   return s.timeCycle;
            default:             return 10;
        }
    }

    private static boolean isMuzzleKind(WeaponStats s)
    {
        switch (s.type)
        {
            case MUZZLELOADER: case FLINTLOCK_PISTOL: case PERCUSSION: return true;
            default: return false;
        }
    }

    /** Called every client tick from ClientEvents. */
    public static void tickClient(EntityPlayer player)
    {
        if (activeStage == null || firearm == null || weapon == null) return;

        // Abort condition: slot swapped / item changed -> progress stays in NBT.
        ItemStack held = player.getCurrentEquippedItem();
        if (held != weapon)
        {
            stop(true);
            return;
        }

        stageTick++;
        if (stageTick >= stageLength)
            completeStage();
    }

    private static void completeStage()
    {
        ReloadStage finished = activeStage;
        // record locally immediately (client feedback + HUD)
        if (finished == ReloadStage.POUR_POWDER)
            ItemFirearm.consumeGunpowder(user, powderNeeded()); // client mirror
        WeaponNBT.setCompletedStage(weapon, finished);
        // authoritative sync (server re-validates & consumes for real)
        PacketHandler.sendToServer(new PacketStageComplete(finished.id));

        if (nextStageFor(firearm.stats, WeaponNBT.completedStage(weapon)) == null)
            finishReload();
        else
            advanceToNextStage(WeaponNBT.completedStage(weapon));
    }

    /** Server-side application of a stage packet (authoritative). */
    public static void applyStageOnServer(EntityPlayer player, ItemStack stack, ItemFirearm gun, ReloadStage stage)
    {
        if (stage == ReloadStage.POUR_POWDER)
        {
            int need = SiliwoodConfig.gunpowderPerPowderCharge * Math.max(1, gun.stats.powderCost);
            if (!ItemFirearm.hasGunpowder(player, need)) return; // refuse cheat/sync error
            ItemFirearm.consumeGunpowder(player, need);
        }
        WeaponNBT.setCompletedStage(stack, stage);
    }

    /** Server-side "reload fully finished" marker. */
    public static void markLoadedOnServer(ItemStack stack)
    {
        WeaponNBT.setLoaded(stack, true);
        WeaponNBT.setCompletedStage(stack, ReloadStage.NONE);
    }

    private static void finishReload()
    {
        WeaponNBT.setLoaded(weapon, true);
        WeaponNBT.setCompletedStage(weapon, ReloadStage.NONE);
        PacketHandler.sendToServer(new PacketStageComplete(-1)); // -1 => loaded
        HudOverlay.setHint("§aЗаряжено!");
        stop(false);
    }

    public static void stopSession()
    {
        stop(true);
    }

    private static void stop(boolean clearHint)
    {
        user = null; weapon = null; firearm = null;
        activeStage = null; stageTick = 0; stageLength = 0;
        if (clearHint) HudOverlay.clearHint();
    }

    public static boolean isReloading() { return activeStage != null; }

    /** 0..1 progress of the currently running stage (for 3D animation). */
    public static float stageProgress()
    {
        if (activeStage == null || stageLength <= 0) return 0f;
        return Math.min(1f, stageTick / (float) stageLength);
    }

    public static String currentHint()
    {
        if (activeStage == null || firearm == null) return null;
        String label;
        switch (activeStage)
        {
            case POUR_POWDER:    label = "Насыпаем порох…"; break;
            case RAM_PROJECTILE: label = isMuzzleKind(firearm.stats)
                                        ? "Закидываем снаряд, проталкиваем шомполом…"
                                        : "Досылаем патрон…"; break;
            case CYCLE_ACTION:   label = "Передёргиваем затвор / взводим курок…"; break;
            default:             return null;
        }
        int pct = Math.min(100, stageTick * 100 / Math.max(1, stageLength));
        return label + " " + pct + "%";
    }

    // ======================================================================
    //  SERVER: shooting
    // ======================================================================

    public static void shoot(EntityPlayer player, ItemStack stack, ItemFirearm gun)
    {
        if (!WeaponNBT.isLoaded(stack) || WeaponNBT.mode(stack) != 0) return;
        WeaponNBT.setLoaded(stack, false);
        WeaponStats s = gun.stats;
        int pellets = Math.max(1, s.pellets);
        for (int i = 0; i < pellets; i++)
        {
            EntityBullet bullet = new EntityBullet(player.world, player, s, pellets > 1);
            bullet.shootFromRotation(player, player.rotationPitch, player.rotationYaw, 0.0F,
                    s.velocity, pellets > 1 ? s.pelletSpread * 0.35F : s.spread * 0.3F);
            player.world.spawnEntityInWorld(bullet);
        }
        if (stack.getMaxDamage() > 0)
        {
            stack.damageItem(1, player);
            if (stack.getItemDamage() >= stack.getMaxDamage())
            {
                player.addChatComponentMessage(new ChatComponentText("§cОружие разорвало!"), false);
                player.inventory.mainInventory[player.inventory.currentItem] = null;
            }
        }
        Effects.muzzleFlash(player, s.loud);
        player.swingItem();
    }
}
