package com.siliwood.client;

import com.siliwood.SiliwoodMod;
import com.siliwood.crafting.WeaponCrafting;
import com.siliwood.crafting.WeaponRecipes;
import com.siliwood.item.ItemFirearm;
import com.siliwood.network.PacketCraftWeapon;
import com.siliwood.network.PacketHandler;
import com.siliwood.weapon.WeaponRegistry;
import com.siliwood.weapon.WeaponStats;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Armory Workbench station GUI: scrollable list of every configured weapon,
 * material cost preview and a "Собрать" button that sends PacketCraftWeapon.
 * Pure client screen (no container) — cheap and lag-free.
 */
public class GuiWorkbench extends GuiScreen
{
    private final InventoryPlayer inventory;
    private final World world;
    private final BlockPos pos;

    private int scroll = 0;
    private static final int ROW_H = 24;
    private int selected = -1;

    public GuiWorkbench(InventoryPlayer inventory, World world, BlockPos pos)
    {
        this.inventory = inventory;
        this.world = world;
        this.pos = pos;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 + 60, height - 34, 70, 20, "Собрать"));
        buttonList.add(new GuiButton(2, width / 2 + 60, height - 12, 70, 20, I18n.format("gui.siliwood.close")));
        if (WeaponRecipes.get("musket") == null) WeaponRecipes.buildDefaults();
    }

    private List<String> ids()
    {
        List<String> out = new ArrayList<>();
        for (WeaponStats s : WeaponRegistry.all()) out.add(s.id);
        return out;
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 2) { mc.displayGuiScreen(null); return; }
        List<String> all = ids();
        if (button.id == 1 && selected >= 0 && selected < all.size())
        {
            PacketHandler.sendToServer(new PacketCraftWeapon(all.get(selected)));
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0)
        {
            List<String> all = ids();
            int maxScroll = Math.max(0, all.size() - visibleRows());
            scroll = Math.max(0, Math.min(maxScroll, scroll + (d > 0 ? -1 : 1)));
        }
        // click to select a row
        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0)
        {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = Mouse.getEventY() * height / mc.displayHeight;
            int top = guiTop() + 24;
            int idx = (my - top) / ROW_H + scroll;
            if (mx > guiLeft() + 8 && mx < guiLeft() + 220 && idx >= 0 && idx < ids().size())
                selected = idx;
        }
    }

    private int visibleRows() { return Math.max(1, (height - 90) / ROW_H); }
    private int guiLeft() { return width / 2 - 150; }
    private int guiTop() { return 20; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        drawDefaultBackground();
        List<String> all = ids();

        drawCenteredString(fontRendererObj, I18n.format("gui.siliwood.workbench"),
                width / 2, 8, 0xFFFFFF);

        int left = guiLeft(), top = guiTop();
        // panel
        drawRect(left, top, left + 300, top + 24 + visibleRows() * ROW_H + 6, 0x90000000 | 0x202030);

        int y = top + 24;
        for (int i = scroll; i < Math.min(all.size(), scroll + visibleRows()); i++)
        {
            String id = all.get(i);
            ItemFirearm gun = SiliwoodMod.firearm(id);
            boolean craftable = gun != null && hasMatsClient(id);
            if (i == selected)
                drawRect(left + 6, y - 2, left + 220, y + ROW_H - 4, 0x60FFFFFF);
            drawString(fontRendererObj, id, left + 10, y, craftable ? 0xA0FFA0 : 0xFFB0B0);
            // mini cost line
            WeaponRecipes.Cost c = WeaponRecipes.get(id);
            if (c != null)
            {
                StringBuilder sb = new StringBuilder();
                for (ItemStack st : c.inputs)
                    sb.append(st.getItem().getUnlocalizedNameItem().replace("item.", "")).append("x").append(st.stackSize).append(" ");
                drawString(fontRendererObj, trim(sb.toString(), 120), left + 10, y + 11, 0xB0B0B0);
            }
            y += ROW_H;
        }

        // right side: details of selection
        if (selected >= 0 && selected < all.size())
        {
            WeaponStats s = WeaponRegistry.get(all.get(selected));
            if (s != null)
            {
                int dx = left + 228, dy = top + 26;
                dy = drawLine(dy, "Урон: " + s.damage, dx);
                dy = drawLine(dy, "Тип: " + s.type.name, dx);
                dy = drawLine(dy, "Дисперсия: " + s.spread, dx);
                if (s.pellets > 1) dy = drawLine(dy, "Дробь x" + s.pellets, dx);
                dy = drawLine(dy, "Порох->" + s.timePourPowder + "t Мяч->" + s.timeLoadBall
                        + "t Шомпол->" + s.timeRam + "t Затвор->" + s.timeCycle + "t", dx);
                dy = drawLine(dy, "Штык: " + (s.hasBayonetMount ? "да (Огонь/Удар)" : "нет"), dx);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean hasMatsClient(String id)
    {
        WeaponRecipes.Cost c = WeaponRecipes.get(id);
        return c != null && WeaponCrafting.hasMaterials(mc.thePlayer, c);
    }

    private int drawLine(int y, String text, int x)
    {
        drawString(fontRendererObj, text, x, y, 0xE0E0E0);
        return y + 11;
    }

    private static String trim(String s, int w)
    {
        return s.length() <= w ? s : s.substring(0, w) + "…";
    }

    /** Non-pausing so MP-friendly and cheap. */
    @Override
    public boolean doesGuiPauseGame() { return false; }
}
