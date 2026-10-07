package com.siliwood.render;

import com.siliwood.handler.FirearmManager;
import com.siliwood.item.ItemFirearm;
import com.siliwood.util.WeaponNBT;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.AbstractClientPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Renders the held weapon as a real 3D model (see WeaponModels) in first
 * person, animated by the current reload stage. Optimization: nothing is
 * drawn unless a firearm is actually in hand; no per-frame allocation except
 * one float computation.
 */
public class RenderWeaponFP
{
    public static void register()
    {
        MinecraftForge.EVENT_BUS.register(new RenderWeaponFP());
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack == null || !(stack.getItem() instanceof ItemFirearm)) return;
        ItemFirearm gun = (ItemFirearm) stack.getItem();

        // cancel vanilla hand render for our items
        event.setCanceled(true);

        AbstractClientPlayer player = event.getClientPlayer();
        GL11.glPushMatrix();

        // proper item lighting so textured 3D faces aren't pitch black
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();

        float bob = (float) Math.sin(player.ticksExisted * 0.1f) * 0.02f;
        // position like a typical FPS weapon at bottom-right of screen
        GlStateManager.translate(0.56f, -0.52f + bob, -0.72f);
        GlStateManager.rotate(-player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * event.getPartialTicks(), 1, 0, 0);

        // walking sway
        float sway = (float) Math.cos(player.ticksExisted * 0.08f) * 0.03f;
        GlStateManager.translate(sway, sway * 0.5f, 0f);

        // scale to screen-space-ish units
        GlStateManager.scale(0.45f, 0.45f, 0.45f);
        GlStateManager.rotate(-45f, 0, 1, 0);
        GlStateManager.rotate(-10f, 0, 0, 1);

        // stage animation: hammer/bolt progress 0..1 while cycling; ramrod slide during RAM
        float anim = computeStageAnim(gun, stack);

        switch (gun.stats.type)
        {
            case FLINTLOCK_PISTOL:
                WeaponModels.renderPistol(stack, anim);
                break;
            case MUZZLELOADER:
                if ("blunderbuss".equals(gun.stats.id)) WeaponModels.renderBlunderbuss(stack, anim);
                else WeaponModels.renderLongGun(stack, anim);
                break;
            default:
                WeaponModels.renderLongGun(stack, anim);
        }

        // bayonet blade when in melee mode
        if (gun.stats.hasBayonetMount && WeaponNBT.mode(stack) == 1)
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(1.35f, 0.0f, 0f);
            WeaponModels.box(new net.minecraft.client.model.ModelBase() {}, 0, -0.01f, -0.06f, 0.5f, 0.03f, 0.06f, 112, 0, 1f);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }

    private static float computeStageAnim(ItemFirearm gun, ItemStack stack)
    {
        if (!FirearmManager.isReloading())
        {
            // loaded => hammer fully cocked
            return WeaponNBT.isLoaded(stack) ? 1f : 0f;
        }
        return FirearmManager.stageProgress();
    }
}
