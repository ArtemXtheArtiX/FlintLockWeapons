package com.siliwood.handler;

import com.siliwood.item.ItemFirearm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Key binding "F" — switch Fire/Melee mode on bayonet weapons.
 */
public class KeyHandler
{
    public static KeyBinding toggleMode;

    public static void register()
    {
        toggleMode = new KeyBinding("key.siliwood.togglemode", Keyboard.KEY_F, "key.categories.siliwood");
        ClientRegistry.registerKeyBinding(toggleMode);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (toggleMode.isPressed())
        {
            Minecraft mc = Minecraft.getMinecraft();
            ItemStack held = mc.thePlayer != null ? mc.thePlayer.getCurrentEquippedItem() : null;
            if (held != null && held.getItem() instanceof ItemFirearm)
            {
                ((ItemFirearm) held.getItem()).toggleMode(mc.thePlayer, held);
            }
        }
    }
}
