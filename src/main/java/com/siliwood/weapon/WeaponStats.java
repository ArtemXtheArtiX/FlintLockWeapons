package com.siliwood.weapon;

/**
 * Immutable stat block for one weapon definition. Instances are built from
 * the config file (config/siliwood/weapons.cfg), so users can add unlimited
 * weapon variants without touching code.
 */
public class WeaponStats
{
    public String id = "musket";
    public WeaponType type = WeaponType.MUZZLELOADER;

    // --- ballistics ---
    public float damage = 9.0f;          // hearts of damage on direct hit
    public float velocity = 2.6f;        // projectile speed multiplier
    public float spread = 1.4f;          // inaccuracy (like bow pull variance)
    public int pellets = 1;              // >1 => shotgun scatter
    public float pelletSpread = 3.0f;    // scatter per pellet for shot
    public int ammoPerShot = 1;          // rounds consumed from inventory per fire
    public boolean loud = true;          // scares nearby mobs / particles & sound

    // --- reload timing (ticks, scaled by config global multiplier) ---
    public int timePourPowder = 30;      // насыпать порох
    public int timeLoadBall = 25;        // закинуть снаряд
    public int timeRam = 35;             // протолкнуть шомполом
    public int timeCycle = 15;           // передёрнуть затвор / взвести курок

    // --- bayonet ---
    public boolean hasBayonetMount = false;
    public float bayonetDamage = 6.0f;   // melee damage in "melee" mode
    public float bayonetReach = 0.8f;    // extra attack reach

    // --- misc ---
    public int durability = 250;         // uses before break (0 = infinite)
    public float weight = 1.0f;          // reserved: movement penalty scaling
    public int powderCost = 1;           // gunpowder units consumed per reload cycle
    public String projectileItem = "";   // item used as ammunition ("" = generic ball/round)

    public int stageTime(ReloadStage stage)
    {
        switch (stage)
        {
            case POUR_POWDER:   return timePourPowder;
            case RAM_PROJECTILE:return timeLoadBall + timeRam;
            case CYCLE_ACTION:  return timeCycle;
            default:            return 0;
        }
    }
}
