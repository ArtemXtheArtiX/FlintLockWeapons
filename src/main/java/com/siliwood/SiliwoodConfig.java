package com.siliwood;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

/**
 * Global mod configuration (config/siliwood.cfg) plus per-weapon stats file.
 */
public class SiliwoodConfig
{
    public static Configuration main;

    // --- general ---
    public static float reloadTimeMultiplier = 1.0f;   // global speed of staged reload
    public static boolean allowMidReloadSwap = true;   // keep progress when swapping slots
    public static int blackpowderWeaponDamageCap = 20;
    public static boolean bayonetsEnabled = true;
    public static boolean showHudHints = true;         // on-screen stage hints
    public static int hudHintScale = 1;                // 1..3 text scale for hints
    public static boolean loudWeaponsScareMobs = true;

    // --- ammo economy ---
    public static int gunpowderPerPowderCharge = 1;    // gunpowder units consumed at POUR_POWDER
    public static boolean requireBallItem = false;     // if true, need "musket_ball" item to load
    public static int leadPerBall = 1;                 // used by crafting station recipes

    // --- crafting station ---
    public static int stationFuelTicksPerCraft = 200;  // how long one craft takes at the station
    public static boolean stationConsumesMaterials = true;

    // --- performance / optimization ---
    public static boolean enableParticles = true;
    public static int maxActiveProjectiles = 64;       // hard cap; oldest removed beyond it
    public static int projectileDespawnTicks = 100;
    public static boolean renderOptimization = true;   // skip item TESR when not in hand

    private static Logger logger;

    public static void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        main = new Configuration(event.getSuggestedConfigurationFile());
        sync();
    }

    public static void sync()
    {
        main.load();

        String gen = "general";
        reloadTimeMultiplier = clamp(main.getFloat("reloadTimeMultiplier", gen, 1.0f, 0.1f, 10.0f,
                "Global multiplier for every staged reload duration. 2.0 = twice as slow."), 0.1f, 10f);
        allowMidReloadSwap = main.getBoolean("allowMidReloadSwap", gen, true,
                "If true, switching slots mid-reload keeps the completed stages (stored in NBT).");
        bayonetsEnabled = main.getBoolean("bayonetsEnabled", gen, true,
                "Enable Fire/Melee mode switching for weapons with bayonet mounts.");
        showHudHints = main.getBoolean("showHudHints", gen, true,
                "Show on-screen hint of current reload stage while holding a weapon.");
        hudHintScale = Math.max(1, Math.min(3, main.getInt("hudHintScale", gen, 1, 1, 3,
                "Scale of HUD reload hints.")));
        loudWeaponsScareMobs = main.getBoolean("loudWeaponsScareMobs", gen, true,
                "Muzzle flash noise scares nearby mobs like a lightning crack would not, but gives them brief speed.");
        blackpowderWeaponDamageCap = main.getInt("maxWeaponDamage", gen, 20, 1, 1000,
                "Hard cap on damage any configured weapon may have.");

        String ammo = "ammo";
        gunpowderPerPowderCharge = Math.max(0, main.getInt("gunpowderPerPowderCharge", ammo, 1, 0, 16,
                "Gunpowder units consumed during the 'pour powder' stage."));
        requireBallItem = main.getBoolean("requireBallItem", ammo, false,
                "If true, the 'ram projectile' stage consumes a musket_ball item from inventory.");
        leadPerBall = Math.max(1, main.getInt("leadPerBall", ammo, 1, 1, 16,
                "Lead nuggets needed per ball at the crafting station."));

        String st = "station";
        stationFuelTicksPerCraft = Math.max(20, main.getInt("ticksPerCraft", st, 200, 20, 20*60*5,
                "Work-time required for one weapon at the crafting station."));
        stationConsumesMaterials = main.getBoolean("consumesMaterials", st, true,
                "Whether the crafting station consumes input materials.");

        String perf = "performance";
        enableParticles = main.getBoolean("enableParticles", perf, true,
                "Smoke/flame particles. Disable on very weak hardware.");
        maxActiveProjectiles = Math.max(8, main.getInt("maxActiveProjectiles", perf, 64, 8, 512,
                "Server-side cap of live bullets to avoid lag spikes."));
        projectileDespawnTicks = Math.max(20, main.getInt("projectileDespawnTicks", perf, 100, 20, 600));
        renderOptimization = main.getBoolean("renderOptimization", perf, true,
                "Skip 3D weapon rendering when the item is only in an inventory frame far away.");

        if (main.hasChanged()) main.save();
        if (logger != null) logger.info("Siliwood config loaded.");
    }

    private static float clamp(float v, float lo, float hi)
    {
        return Math.max(lo, Math.min(hi, v));
    }
}
