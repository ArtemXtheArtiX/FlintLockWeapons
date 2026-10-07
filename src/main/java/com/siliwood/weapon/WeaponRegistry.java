package com.siliwood.weapon;

import com.siliwood.SiliwoodConfig;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads weapon definitions from config/siliwood/weapons.cfg.
 * Every [[weapon]] block becomes a registered ItemFirearm — so users can add
 * "as many weapons as they want" purely through the config file.
 */
public class WeaponRegistry
{
    private static final Logger LOG = LogManager.getLogger("Siliwood|Weapons");
    private static final Map<String, WeaponStats> ALL = new LinkedHashMap<>();
    private static File cfgFile;

    public static void init(File mainCfg)
    {
        // weapons.cfg lives next to the main config: config/siliwood/weapons.cfg
        File dir = mainCfg.getParentFile();
        File sub = new File(dir, "siliwood");
        if (!sub.exists() && !sub.mkdirs()) sub = dir;
        cfgFile = new File(sub, "weapons.cfg");
        loadDefaults();
        readConfig();
    }

    /** Built-in defaults, mirroring real historical analogues. */
    private static void loadDefaults()
    {
        register(defaultMusket());
        register(defaultBrownBess());
        register(defaultCharleville());
        register(defaultBlunderbuss());
        register(defaultFlintlockPistol());
        register(defaultDuellingPistol());
        register(defaultPercussionRifle());
        register(defaultDreyse());
        register(defaultNeedleCarbine());
        register(defaultSnider());
        register(defaultDoubleShotgun());
        register(defaultSawoffed());
    }

    private static WeaponStats defaultMusket()
    {
        WeaponStats s = new WeaponStats();
        s.id = "musket"; s.type = WeaponType.MUZZLELOADER;
        s.damage = 9; s.velocity = 2.6f; s.spread = 1.5f;
        s.hasBayonetMount = true; s.durability = 300;
        return s;
    }
    private static WeaponStats defaultBrownBess()
    {
        WeaponStats s = new WeaponStats();
        s.id = "brown_bess"; s.type = WeaponType.MUZZLELOADER;
        s.damage = 10; s.velocity = 2.7f; s.spread = 1.3f;
        s.hasBayonetMount = true; s.durability = 340;
        s.timePourPowder = 32; s.timeLoadBall = 26; s.timeRam = 38; s.timeCycle = 16;
        return s;
    }
    private static WeaponStats defaultCharleville()
    {
        WeaponStats s = new WeaponStats();
        s.id = "charleville"; s.type = WeaponType.MUZZLELOADER;
        s.damage = 8.5f; s.velocity = 2.6f; s.spread = 1.4f;
        s.hasBayonetMount = true; s.durability = 280;
        s.timePourPowder = 26; s.timeLoadBall = 22; s.timeRam = 30; s.timeCycle = 12;
        return s;
    }
    private static WeaponStats defaultBlunderbuss()
    {
        WeaponStats s = new WeaponStats();
        s.id = "blunderbuss"; s.type = WeaponType.MUZZLELOADER;
        s.damage = 3.5f; s.pellets = 8; s.pelletSpread = 5f; s.velocity = 2.0f;
        s.hasBayonetMount = false; s.durability = 200;
        s.timePourPowder = 30; s.timeLoadBall = 30; s.timeRam = 40; s.timeCycle = 18;
        return s;
    }
    private static WeaponStats defaultFlintlockPistol()
    {
        WeaponStats s = new WeaponStats();
        s.id = "flintlock_pistol"; s.type = WeaponType.FLINTLOCK_PISTOL;
        s.damage = 7; s.velocity = 2.3f; s.spread = 2.2f;
        s.hasBayonetMount = false; s.durability = 180;
        s.timePourPowder = 28; s.timeLoadBall = 22; s.timeRam = 26; s.timeCycle = 14;
        return s;
    }
    private static WeaponStats defaultDuellingPistol()
    {
        WeaponStats s = new WeaponStats();
        s.id = "dueling_pistol"; s.type = WeaponType.FLINTLOCK_PISTOL;
        s.damage = 12; s.velocity = 3.0f; s.spread = 0.7f;
        s.hasBayonetMount = false; s.durability = 140;
        s.timePourPowder = 34; s.timeLoadBall = 26; s.timeRam = 30; s.timeCycle = 16;
        return s;
    }
    private static WeaponStats defaultPercussionRifle()
    {
        WeaponStats s = new WeaponStats();
        s.id = "percussion_rifle"; s.type = WeaponType.PERCUSSION;
        s.damage = 11; s.velocity = 3.1f; s.spread = 0.8f;
        s.hasBayonetMount = true; s.durability = 320;
        s.timePourPowder = 24; s.timeLoadBall = 24; s.timeRam = 34; s.timeCycle = 12;
        return s;
    }
    private static WeaponStats defaultDreyse()
    {
        WeaponStats s = new WeaponStats();
        s.id = "dreyse_needle_gun"; s.type = WeaponType.NEEDLE_GUN;
        s.damage = 8; s.velocity = 3.2f; s.spread = 0.9f;
        s.hasBayonetMount = true; s.durability = 400;
        s.timeCycle = 26; // single bolt cycle loads the paper cartridge
        return s;
    }
    private static WeaponStats defaultNeedleCarbine()
    {
        WeaponStats s = new WeaponStats();
        s.id = "needle_carbine"; s.type = WeaponType.NEEDLE_GUN;
        s.damage = 7; s.velocity = 2.9f; s.spread = 1.3f;
        s.hasBayonetMount = true; s.durability = 300;
        s.timeCycle = 22;
        return s;
    }
    private static WeaponStats defaultSnider()
    {
        WeaponStats s = new WeaponStats();
        s.id = "snidercarbine"; s.type = WeaponType.BREAK_ACTION;
        s.damage = 10; s.velocity = 3.0f; s.spread = 1.0f;
        s.hasBayonetMount = true; s.durability = 380;
        s.timePourPowder = 20; s.timeCycle = 18;
        return s;
    }
    private static WeaponStats defaultDoubleShotgun()
    {
        WeaponStats s = new WeaponStats();
        s.id = "double_shotgun"; s.type = WeaponType.BREAK_ACTION;
        s.damage = 3f; s.pellets = 6; s.pelletSpread = 4f; s.velocity = 2.2f;
        s.hasBayonetMount = false; s.durability = 240;
        s.timePourPowder = 30; s.timeCycle = 14;
        return s;
    }
    private static WeaponStats defaultSawoffed()
    {
        WeaponStats s = new WeaponStats();
        s.id = "sawed_off"; s.type = WeaponType.BREAK_ACTION;
        s.damage = 2.5f; s.pellets = 7; s.pelletSpread = 7f; s.velocity = 1.8f;
        s.hasBayonetMount = false; s.durability = 120;
        s.timePourPowder = 26; s.timeCycle = 12;
        return s;
    }

    private static void register(WeaponStats s)
    {
        ALL.put(s.id, s);
    }

    public static List<WeaponStats> all()
    {
        return new ArrayList<>(ALL.values());
    }

    public static WeaponStats get(String id)
    {
        return ALL.get(id);
    }

    /** Re-read weapons.cfg (called at server about to start / on command). */
    public static void readConfig()
    {
        Configuration c = new Configuration(cfgFile);
        try
        {
            c.load();
            String cat = "weapons";
            for (String key : c.getCategoryNames())
            {
                if (!key.startsWith(cat + ".weapon")) continue;
                net.minecraftforge.common.config.Property prop =
                        c.get(key, "id", "");
                String id = prop.getString();
                if (id.isEmpty()) continue;
                WeaponStats s = ALL.get(id);
                if (s == null) { s = new WeaponStats(); s.id = id; ALL.put(id, s); }
                s.type = WeaponType.fromName(c.getString(key, "type", s.type.name, "muzzleloader|flintlock_pistol|percussion|needle_gun|bolt_action|break_action"));
                s.damage = Math.min(SiliwoodConfig.blackpowderWeaponDamageCap,
                        c.getFloat(key, "damage", s.damage, 0.1f, 1000f, "Damage per hit (hearts*2 as float)."));
                s.velocity = c.getFloat(key, "velocity", s.velocity, 0.5f, 8f, "Bullet speed.");
                s.spread = c.getFloat(key, "spread", s.spread, 0f, 10f, "Inaccuracy.");
                s.pellets = c.getInt(key, "pellets", s.pellets, 1, 32, "Projectiles per shot (shot).");
                s.pelletSpread = c.getFloat(key, "pelletSpread", s.pelletSpread, 0f, 20f);
                s.ammoPerShot = c.getInt(key, "ammoPerShot", s.ammoPerShot, 1, 8);
                s.timePourPowder = c.getInt(key, "timePourPowder", s.timePourPowder, 0, 6000, "Ticks: pour powder.");
                s.timeLoadBall = c.getInt(key, "timeLoadBall", s.timeLoadBall, 0, 6000, "Ticks: drop ball/paper cartridge.");
                s.timeRam = c.getInt(key, "timeRam", s.timeRam, 0, 6000, "Ticks: ram with ramrod.");
                s.timeCycle = c.getInt(key, "timeCycle", s.timeCycle, 0, 6000, "Ticks: cycle bolt/hammer.");
                s.hasBayonetMount = c.getBoolean(key, "hasBayonetMount", s.hasBayonetMount, "Allows socket bayonet => Fire/Melee modes.");
                s.bayonetDamage = c.getFloat(key, "bayonetDamage", s.bayonetDamage, 1f, 40f);
                s.durability = c.getInt(key, "durability", s.durability, 0, 100000);
                s.powderCost = c.getInt(key, "powderCost", s.powderCost, 0, 16);
                s.projectileItem = c.getString(key, "projectileItem", s.projectileItem, "Optional ammo item registry name.");
                register(s);
            }
            // ensure every default appears in the file too
            for (WeaponStats s : new ArrayList<>(ALL.values())) writeBlock(c, s);
            if (c.hasChanged()) c.save();
        }
        catch (Exception e)
        {
            LOG.error("Failed reading weapons.cfg", e);
        }
        finally
        {
            try { c.save(); } catch (Exception ignored) {}
        }
        LOG.info("Loaded {} weapon definitions.", ALL.size());
    }

    private static void writeBlock(Configuration c, WeaponStats s)
    {
        String key = "weapons.weapon." + s.id;
        c.get(key, "id", s.id);
        c.get(key, "type", s.type.name);
        c.get(key, "damage", s.damage);
        c.get(key, "velocity", s.velocity);
        c.get(key, "spread", s.spread);
        c.get(key, "pellets", s.pellets);
        c.get(key, "ammoPerShot", s.ammoPerShot);
        c.get(key, "timePourPowder", s.timePourPowder);
        c.get(key, "timeLoadBall", s.timeLoadBall);
        c.get(key, "timeRam", s.timeRam);
        c.get(key, "timeCycle", s.timeCycle);
        c.get(key, "hasBayonetMount", s.hasBayonetMount);
        c.get(key, "durability", s.durability);
        c.setCategoryComment("weapons", "Add/remove [[weapon.xxx]] blocks freely; new ids spawn new items automatically.");
    }
}
