package moe.takochan.takotech.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.utils.MathUtils;

public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    private final static float RADIUS_IN = 30F;
    private final static float RADIUS_OUT = RADIUS_IN * 2F;
    private final static float ITEM_RADIUS = (RADIUS_IN + RADIUS_OUT) * 0.5F;

    private static final Minecraft mc = Minecraft.getMinecraft();
    // toolbox plus
    private final ItemStack toolboxStack;
    private final List<ItemStack> items = new ArrayList<>();

    // 用于在前景层绘制工具提示时缓存鼠标悬停的物品
    private ItemStack tooltipItem = null;

    private int selectedItem = -1;

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container, ItemStack itemStack) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.toolboxStack = itemStack;
        loadItemsFromNBT();
    }

    private void loadItemsFromNBT() {
        NBTTagCompound nbt = this.toolboxStack.getTagCompound();
        if (nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            NBTTagList itemsTagList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            if (itemsTagList != null && itemsTagList.tagCount() > 0) {
                items.add(toolboxStack);
                for (int i = 0; i < itemsTagList.tagCount(); i++) {
                    ItemStack itemStack = ItemStack.loadItemStackFromNBT(itemsTagList.getCompoundTagAt(i));
                    if (itemStack != null && itemStack.getItem() instanceof MetaGeneratedTool) {
                        items.add(itemStack);
                    }
                }
            }
        } else {
            items.clear();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (items.isEmpty()) return;

        // 计算选中项
        int xCenter = width / 2;
        int yCenter = height / 2;
        double dx = mouseX - xCenter;
        double dy = mouseY - yCenter;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double distance = Math.sqrt(dx * dx + dy * dy);

        selectedItem = -1;
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / items.size()) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / items.size()) + 0.25F) * 360F;

            if (angle < sectorStart) angle += 360;
            if (angle >= sectorStart && angle < sectorEnd && distance >= RADIUS_IN && distance < RADIUS_OUT) {
                selectedItem = i;
                break;
            }
        }

        // 开始绘制
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // 绘制扇形区域
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / items.size()) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / items.size()) + 0.25F) * 360F;
            int color;
            if (i == selectedItem) {
                color = 0xFFFFFF40;
                tooltipItem = items.get(i);
            } else {
                color = 0x00000040;
                tooltipItem = null;
            }
            drawSector(tess, xCenter, yCenter, RADIUS_IN, RADIUS_OUT, sectorStart, sectorEnd, color);
        }

        tess.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // 绘制物品图标
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < items.size(); i++) {
            float angleRad = (((i) / (float) items.size()) + 0.25F) * 2F * (float) Math.PI;
            float posX = xCenter - 8 + ITEM_RADIUS * (float) Math.cos(angleRad);
            float posY = yCenter - 8 + ITEM_RADIUS * (float) Math.sin(angleRad);

            ItemStack stack = items.get(i);
            if (stack != null) {
                itemRender
                    .renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, (int) posX, (int) posY);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 若存在工具提示物品则绘制其提示文本
        // if (tooltipItem != null) renderToolTip(tooltipItem, mouseX, mouseY);
    }

    @Override
    public void handleInput() {
        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                this.handleKeyboardInput();
            }
            if (!Keyboard.isKeyDown(GameSettings.selectTool.getKeyCode())) {
                // NetworkHandler.NETWORK.sendToServer(new ToolboxSelectionPacket(selectedItem));
                mc.thePlayer.closeScreen();
            }
        }
    }

    // region NEI
    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
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

    private void drawSector(Tessellator tessellator, int x, int y, float radiusIn, float radiusOut, float startAngle,
        float endAngle, int color) {
        int r = (color >> 24) & 0xFF;
        int g = (color >> 16) & 0xFF;
        int b = (color >> 8) & 0xFF;
        int a = color & 0xFF;

        drawPieArc(tessellator, x, y, zLevel, radiusIn, radiusOut, startAngle, endAngle, r, g, b, a);
    }

    private static final float PRECISION = 5;

    /**
     * 绘制圆环扇形（Pie Arc）
     *
     * @param tessellator 用于绘制的 Tessellator 实例
     * @param x           圆心 x 坐标
     * @param y           圆心 y 坐标
     * @param z           z 轴高度
     * @param radiusIn    内半径
     * @param radiusOut   外半径
     * @param startAngle  起始角度（度数）
     * @param endAngle    结束角度（度数）
     * @param r           红色通道值
     * @param g           绿色通道值
     * @param b           蓝色通道值
     * @param a           透明度
     */
    private void drawPieArc(Tessellator tessellator, int x, int y, float z, float radiusIn, float radiusOut,
        float startAngle, float endAngle, int r, int g, int b, int a) {
        float angle = endAngle - startAngle;
        int sections = Math.max(1, MathUtils.ceiling_float_int(angle / PRECISION));
        startAngle = (float) Math.toRadians(startAngle);
        endAngle = (float) Math.toRadians(endAngle);
        angle = endAngle - startAngle;
        for (int i = 0; i < sections; i++) {
            float angle1 = startAngle + (i / (float) sections) * angle;
            float angle2 = startAngle + ((i + 1) / (float) sections) * angle;
            float pos1InX = x + radiusIn * (float) Math.cos(angle1);
            float pos1InY = y + radiusIn * (float) Math.sin(angle1);
            float pos1OutX = x + radiusOut * (float) Math.cos(angle1);
            float pos1OutY = y + radiusOut * (float) Math.sin(angle1);
            float pos2OutX = x + radiusOut * (float) Math.cos(angle2);
            float pos2OutY = y + radiusOut * (float) Math.sin(angle2);
            float pos2InX = x + radiusIn * (float) Math.cos(angle2);
            float pos2InY = y + radiusIn * (float) Math.sin(angle2);

            tessellator.setColorRGBA_F(r / 255F, g / 255F, b / 255F, a / 255F);
            tessellator.addVertex(pos1OutX, pos1OutY, z);
            tessellator.addVertex(pos1InX, pos1InY, z);
            tessellator.addVertex(pos2InX, pos2InY, z);
            tessellator.addVertex(pos2OutX, pos2OutY, z);
        }
    }
}
