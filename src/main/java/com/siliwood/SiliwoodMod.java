package com.siliwood;

import com.siliwood.crafting.BlockWorkbench;
import com.siliwood.handler.ClientEvents;
import com.siliwood.handler.HudOverlay;
import com.siliwood.handler.KeyHandler;
import com.siliwood.handler.ServerEvents;
import com.siliwood.item.ItemFirearm;
import com.siliwood.network.PacketHandler;
import com.siliwood.weapon.WeaponRegistry;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Siliwood — кремниевые (исторические) оружия для Minecraft 1.12.2.
 *
 *  • Поэтапная перезарядка (порох → снаряд/шомпол → затвор), прогресс хранится
 *    в NBT оружия: смена слота не сбрасывает уже пройденные этапы.
 *  • Любое количество видов оружия добавляется конфигом config/siliwood/weapons.cfg.
 *  • Штыковые крепления → режимы «Огонь / Удар» + HUD-подсказки.
 *  • Верстак оружейника (станция создания оружия за материалы).
 *  • Полностью 3D-рендер оружия в руках, текстуры до 32x32, упор на оптимизацию.
 */
@Mod(modid = SiliwoodMod.MODID, name = SiliwoodMod.NAME, version = SiliwoodMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]")
public class SiliwoodMod
{
    public static final String MODID = "siliwood";
    public static final String NAME = "Siliwood — Silicon & Blackpowder";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static SiliwoodMod instance;

    @SidedProxy(clientSide = "com.siliwood.ClientProxy", serverSide = "com.siliwood.Proxy")
    public static Proxy proxy;

    /** Creative tab for all mod content. */
    public static final CreativeTabs TAB = new CreativeTabs(MODID)
    {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack getTabIconItem()
        {
            return new ItemStack(ARMORY_WORKBENCH);
        }
    };

    // Registered content (populated during registry events / preInit).
    public static BlockWorkbench ARMORY_WORKBENCH;
    private static final List<ItemFirearm> FIREARMS = new ArrayList<>();

    public SiliwoodMod()
    {
        // Event-bested classes must be constructed once so @SubscribeEvent works.
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ======================================================================
    //  Lifecycle
    // ======================================================================

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        SiliwoodConfig.preInit(event);
        WeaponRegistry.init(SiliwoodConfig.main.getConfigFile());
        com.siliwood.crafting.WeaponRecipes.buildDefaults();
        PacketHandler.init();

        ARMORY_WORKBENCH = new BlockWorkbench();
        GameRegistry.register(ARMORY_WORKBENCH);
        GameRegistry.register(new ItemBlockWorkbench(ARMORY_WORKBENCH)
                .setRegistryName(ARMORY_WORKBENCH.getRegistryName()));

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

        MinecraftForge.EVENT_BUS.register(new ServerEvents());
        if (event.getSide().isClient())
        {
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
            MinecraftForge.EVENT_BUS.register(new KeyHandler());
            HudOverlay.register();
        }
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        com.siliwood.crafting.WeaponRecipes.registerVanillaRecipes();
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit();
    }

    // ======================================================================
    //  Registry events: one ItemFirearm per configured weapon definition
    // ======================================================================

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event)
    {
        for (WeaponStats stats : WeaponRegistry.all())
        {
            ItemFirearm gun = new ItemFirearm(stats);
            gun.setCreativeTab(TAB);
            event.getRegistry().register(gun);
            FIREARMS.add(gun);
        }
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> event)
    {
        // Blocks are registered eagerly in preInit via GameRegistry.register;
        // this handler exists so Forge's vanilla registry ordering is satisfied
        // for the item-block pairing above.
    }

    /** Client model registration — weapons use a simple builtin inventory model. */
    public static void registerItemModels()
    {
        for (ItemFirearm gun : FIREARMS)
        {
            ModelLoader.setCustomModelResourceLocation(gun, 0,
                    new ModelResourceLocation(gun.getRegistryName(), "inventory"));
        }
        if (ARMORY_WORKBENCH != null)
        {
            Item item = Item.getItemFromBlock(ARMORY_WORKBENCH);
            if (item != null)
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(ARMORY_WORKBENCH.getRegistryName(), "inventory"));
        }
    }

    /** Helper to fetch a registered firearm by config id (used by GUI/commands). */
    public static ItemFirearm firearm(String id)
    {
        for (ItemFirearm f : FIREARMS)
            if (f.stats.id.equals(id)) return f;
        return null;
    }

    // ======================================================================
    //  GUI handler (workbench station)
    // ======================================================================

    public static class GuiHandler implements IGuiHandler
    {
        public static final int GUI_WORKBENCH = 1;

        @Override
        public Object getServerGuiElement(int ID, net.minecraft.entity.player.EntityPlayer player,
                                          net.minecraft.world.World world, int x, int y, int z)
        {
            return null; // container-less GUI (pure inventory-driven crafting menu)
        }

        @Override
        public Object getClientGuiElement(int ID, net.minecraft.entity.player.EntityPlayer player,
                                          net.minecraft.world.World world, int x, int y, int z)
        {
            if (ID == GUI_WORKBENCH) return new com.siliwood.client.GuiWorkbench(player.inventory, world, new net.minecraft.util.math.BlockPos(x, y, z));
            return null;
        }
    }

    // ======================================================================
    //  Item form of the workbench block
    // ======================================================================

    public static class ItemBlockWorkbench extends net.minecraft.item.ItemBlock
    {
        public ItemBlockWorkbench(Block block)
        {
            super(block);
            setRegistryName(block.getRegistryName());
            setUnlocalizedName(SiliwoodMod.MODID + ".armory_workbench");
            setCreativeTab(TAB);
        }
    }
}
