package com.siliwood.handler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Small on-screen hint while holding a weapon: current reload stage, mode
 * (Fire/Melee) and quick keys. Cheap: renders 1-2 strings only when needed.
 */
public class HudOverlay
{
    private static String hint = null;
    private static long hintExpireMs = 0;

    public static void register()
    {
        MinecraftForge.EVENT_BUS.register(new HudOverlay());
    }

    public static void setHint(String text)
    {
        hint = text;
        hintExpireMs = System.currentTimeMillis() + 2500L;
    }

    public static void clearHint()
    {
        hint = null;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!com.siliwood.SiliwoodConfig.showHudHints) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return; // don't draw over GUIs

        boolean drewSomething = false;
        GL11.glPushMatrix();
        float scale = Math.max(1, Math.min(3, com.siliwood.SiliwoodConfig.hudHintScale));
        int x = event.resolution.getScaledWidth() / 2;
        int y = event.resolution.getScaledHeight() - 62;

        // live reload progress line
        String live = FirearmManager.isReloading() ? FirearmManager.currentHint() : null;
        if (live != null)
        {
            drawCentered(mc, live, x, y, scale, 0xFFFFFF);
            drewSomething = true;
        }

        // transient hint (e.g. "Заряжено!", "Нужен порох!")
        if (hint != null)
        {
            if (System.currentTimeMillis() > hintExpireMs) hint = null;
            else
            {
                drawCentered(mc, hint, x, y + (drewSomething ? 12 : 0), scale, 0xFFE0A0);
                drewSomething = true;
            }
        }

        // mode reminder for bayonet weapons held in hand
        net.minecraft.item.ItemStack held = mc.thePlayer != null ? mc.thePlayer.getCurrentEquippedItem() : null;
        if (held != null && held.getItem() instanceof com.siliwood.item.ItemFirearm)
        {
            com.siliwood.item.ItemFirearm gun = (com.siliwood.item.ItemFirearm) held.getItem();
            String modeLine;
            if (gun.stats.hasBayonetMount && com.siliwood.SiliwoodConfig.bayonetsEnabled)
            {
                int m = com.siliwood.util.WeaponNBT.mode(held);
                modeLine = (m == 0 ? "\u00a7c\u25c9 Огонь" : "\u00a7e\u2694 Удар")
                        + "  \u00a77[F] режим"
                        + (com.siliwood.util.WeaponNBT.isLoaded(held) ? "  \u00a7a[ПКМ] огонь!" : "");
            }
            else
            {
                modeLine = com.siliwood.util.WeaponNBT.isLoaded(held)
                        ? "\u00a7aЗаряжено — [ПКМ] выстрел"
                        : "\u00a77Удерживайте [ПКМ] для перезарядки";
            }
            drawCentered(mc, modeLine, x, y - (drewSomething ? 12 : 0) - 12, scale, 0xBBBBBB);
        }
        GL11.glPopMatrix();
    }

    private static void drawCentered(Minecraft mc, String text, int x, int y, float scale, int color)
    {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glScalef(scale, scale, 1f);
        int w = mc.fontRendererObj.getStringWidth(text);
        mc.fontRendererObj.drawStringWithShadow(text, -w / 2f, 0, color);
        GL11.glPopMatrix();
    }
}
