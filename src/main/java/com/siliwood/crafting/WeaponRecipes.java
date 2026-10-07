package com.siliwood.crafting;

import com.siliwood.SiliwoodConfig;
import com.siliwood.weapon.WeaponRegistry;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Material costs for the Armory Workbench. Every weapon id known to the
 * registry (including user-added config weapons) gets a default cost derived
 * from its stats; specific overrides may be added here or via config later.
 */
public final class WeaponRecipes
{
    public static class Cost
    {
        public final ItemStack[] inputs;
        public final int workTicks;

        public Cost(int workTicks, ItemStack... inputs)
        {
            this.workTicks = workTicks;
            this.inputs = inputs;
        }
    }

    private static final Map<String, Cost> BY_WEAPON = new LinkedHashMap<>();

    private WeaponRecipes() {}

    public static void buildDefaults()
    {
        BY_WEAPON.clear();
        for (WeaponStats s : WeaponRegistry.all())
            BY_WEAPON.put(s.id, defaultCost(s));
    }

    private static Cost defaultCost(WeaponStats s)
    {
        boolean longGun = !s.id.contains("pistol") && !s.id.contains("sawed");
        List<ItemStack> in = new ArrayList<>();
        in.add(new ItemStack(Items.IRON_INGOT, longGun ? 5 : 2));          // ствол/механизм
        in.add(new ItemStack(Items.STICK, longGun ? 3 : 1));               // ложе (дерево)
        if (s.hasBayonetMount) in.add(new ItemStack(Items.IRON_INGOT, 1)); // штык
        if (s.type.name.equals("needle_gun") || s.type.name.equals("bolt_action"))
            in.add(new ItemStack(Items.REDSTONE, 2));                      // пружины затвора
        in.add(new ItemStack(Items.GUNPOWDER, Math.max(1, s.powderCost))); // испытательный заряд
        in.add(new ItemStack(Items.FLINT, 1));                             // кремень
        return new Cost(SiliwoodConfig.stationFuelTicksPerCraft + (longGun ? 100 : 0),
                in.toArray(new ItemStack[0]));
    }

    /** Cost for a weapon id; falls back to a generic kit so config weapons always craftable. */
    public static Cost get(String weaponId)
    {
        Cost c = BY_WEAPON.get(weaponId);
        if (c != null) return c;
        WeaponStats s = WeaponRegistry.get(weaponId);
        return s != null ? defaultCost(s) : null;
    }

    /** Register vanilla crafting recipes too (shaped, simple), optional but handy. */
    public static void registerVanillaRecipes()
    {
        Item musket = itemFor("musket");
        if (musket != null)
        {
            GameRegistry.addShapedRecipe(
                    new ItemStack(musket),
                    "siliwood:musket",
                    "  I", " FP", "IW ",
                    'I', Items.IRON_INGOT,
                    'F', Items.FLINT,
                    'P', Items.GUNPOWDER,
                    'W', Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS));
        }
    }

    private static Item itemFor(String id)
    {
        return GameRegistry.findItem("siliwood", id);
    }

    public static List<String> weaponIds()
    {
        return new ArrayList<>(BY_WEAPON.keySet());
    }
}
