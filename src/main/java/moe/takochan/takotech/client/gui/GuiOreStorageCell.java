package moe.takochan.takotech.client.gui;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.ContainerOreStorageCell;

@SideOnly(Side.CLIENT)
public class GuiOreStorageCell extends BaseGui<ContainerOreStorageCell> implements INEIGuiHandler {

    public GuiOreStorageCell(ContainerOreStorageCell container) {
        super(container, "测试窗口", 200, 200);
    }

    @Override
    protected void drawComponents(float partialTicks, int mouseX, int mouseY) {
        ItemStack iconStack = new ItemStack(Blocks.iron_ore);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), iconStack, 0, 0);
        RenderHelper.disableStandardItemLighting();
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        fontRendererObj.drawString("Stores ores only!", 0, 16, 0x808080);
        fontRendererObj.drawString("mouseX: " + getAbsoluteMouseX(mouseX), 0, 24, 0x808080);
        fontRendererObj.drawString("Relative mouseX: " + mouseX, 0, 32, 0x808080);
        fontRendererObj.drawString("mouseY: " + getAbsoluteMouseY(mouseY), 0, 40, 0x808080);
        fontRendererObj.drawString("Relative mouseY: " + mouseY, 0, 48, 0x808080);
    }

    // region NEI
    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        // 隐藏NEI界面
        currentVisibility.showNEI = false;
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        return true;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return true;
    }
    // endregion

}
