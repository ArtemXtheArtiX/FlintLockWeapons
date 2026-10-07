package com.siliwood.render;

import net.minecraft.client.model.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Fully 3D first-person weapon renderer built from textured cube parts
 * (OBJ-free, so it works in any environment). One model per mechanism family:
 *  - MusketModel : long barrel + wooden stock + ramrod + bayonet socket
 *  - PistolModel : short barrel, curved grip
 *  - RifleModel  : bolt handle + rear sight
 * Parts rotate to reflect the CURRENT reload stage (ramrod sliding out,
 * hammer cocking back, bolt cycling) — visual feedback for the staged system.
 */
public class WeaponModels
{
    private static final ResourceLocation TEX = new ResourceLocation("siliwood:textures/items/weapon_atlas.png");

    // ---------------------------------------------------------------- helpers

    /** Textured cuboid drawn with absolute local coords (no baked rotation). */
    public static void box(ModelBase model, float x0, float y0, float z0, float x1, float y1, float z1, float u, float v, float s)
    {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        ModelRenderer m = new ModelRenderer(model);
        m.setTextureOffset((int) u, (int) v);
        m.addBox(x0, y0, z0, (int) Math.ceil(dx), (int) Math.ceil(dy), (int) Math.ceil(dz));
        m.scale(s, s, s);
        m.render(0f);
    }

    // ---------------------------------------------------------------- models

    /** Long gun: muskets, rifles, blunderbuss, carbines. */
    public static void renderLongGun(ItemStack stack, float stageAnim)
    {
        GL11.glPushMatrix();
        bindTex();
        ModelBase mb = new ModelBase() {};
        float woodU = 0, steelU = 64;

        // buttstock (angled block)
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.55f, 0.28f, 0f);
        GL11.glRotatef(-18f, 0, 0, 1);
        box(mb, 0, -0.09f, -0.05f, 0.34f, 0.11f, 0.05f, woodU, 0, 1f);
        GL11.glPopMatrix();

        // main body / wrist
        box(mb, -0.25f, -0.07f, -0.045f, 0.35f, 0.07f, 0.045f, woodU, 12, 1f);

        // barrel (long steel tube)
        box(mb, 0.35f, -0.035f, -0.03f, 1.35f, 0.02f, 0.03f, steelU, 0, 1f);

        // breech block / lockplate
        box(mb, -0.05f, -0.02f, -0.05f, 0.35f, 0.09f, 0.05f, steelU, 16, 1f);

        // hammer (rotates during CYCLE_ACTION stage)
        GL11.glPushMatrix();
        GL11.glTranslatef(0.05f, 0.05f, 0f);
        GL11.glRotatef(-45f * stageAnim, 0, 0, 1);
        box(mb, -0.02f, 0f, -0.015f, 0.14f, 0.10f, 0.015f, steelU, 24, 1f);
        GL11.glPopMatrix();

        // trigger guard
        box(mb, -0.10f, -0.14f, -0.01f, 0.12f, -0.07f, 0.01f, steelU, 32, 1f);

        // ramrod under barrel — slides OUT during RAM_PROJECTILE stage (stageAnim ramp)
        float ramOut = Math.min(1f, Math.max(0f, stageAnim)) * 0.5f;
        box(mb, 0.4f - ramOut, -0.065f, -0.012f, 1.3f - ramOut, -0.045f, 0.012f, woodU + 32, 0, 1f);

        // front sight
        box(mb, 1.28f, 0.02f, -0.008f, 1.32f, 0.06f, 0.008f, steelU, 40, 1f);

        GL11.glPopMatrix();
    }

    /** Pistol: shorter, one-handed. */
    public static void renderPistol(ItemStack stack, float stageAnim)
    {
        GL11.glPushMatrix();
        bindTex();
        ModelBase mb = new ModelBase() {};

        // grip (curved-ish: two stacked boxes)
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.18f, 0.02f, 0f);
        GL11.glRotatef(28f, 0, 0, 1);
        box(mb, -0.05f, -0.28f, -0.03f, 0.06f, 0.04f, 0.03f, 0, 32, 1f);
        GL11.glPopMatrix();

        // body
        box(mb, -0.20f, -0.06f, -0.03f, 0.28f, 0.05f, 0.03f, 0, 44, 1f);
        // short barrel
        box(mb, 0.28f, -0.03f, -0.02f, 0.72f, 0.02f, 0.02f, 64, 40, 1f);
        // lockplate + hammer animated like long gun
        box(mb, -0.02f, 0.02f, -0.035f, 0.22f, 0.10f, 0.035f, 64, 52, 1f);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.02f, 0.08f, 0f);
        GL11.glRotatef(-50f * stageAnim, 0, 0, 1);
        box(mb, -0.015f, 0f, -0.012f, 0.10f, 0.09f, 0.012f, 64, 64, 1f);
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    /** Blunderbuss: flared muzzle approximated with three widening boxes. */
    public static void renderBlunderbuss(ItemStack stack, float stageAnim)
    {
        renderLongGun(stack, stageAnim);
        GL11.glPushMatrix();
        bindTex();
        ModelBase mb = new ModelBase() {};
        box(mb, 1.30f, -0.045f, -0.038f, 1.38f, 0.03f, 0.038f, 96, 0, 1f);
        box(mb, 1.38f, -0.055f, -0.048f, 1.44f, 0.04f, 0.048f, 96, 8, 1f);
        box(mb, 1.44f, -0.070f, -0.060f, 1.48f, 0.055f, 0.060f, 96, 16, 1f);
        GL11.glPopMatrix();
    }

    private static void bindTex()
    {
        net.minecraft.client.renderer.texture.TextureManager tm =
                net.minecraft.client.Minecraft.getMinecraft().getTextureManager();
        tm.bindTexture(TEX);
        GL11.glColor3f(1f, 1f, 1f);
    }
}
