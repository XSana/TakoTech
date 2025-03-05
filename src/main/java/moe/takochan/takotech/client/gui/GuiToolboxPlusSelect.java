package moe.takochan.takotech.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.common.item.ItemToolboxPlus;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.network.NetworkHandler;
import moe.takochan.takotech.network.ToolboxSelectionPacket;
import moe.takochan.takotech.utils.MathUtils;

public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    // 界面尺寸参数
    private final static float RADIUS_IN = 30F; // 内圈半径
    private final static float RADIUS_OUT = RADIUS_IN * 2F; // 外圈半径
    private final static float ITEM_RADIUS = (RADIUS_IN + RADIUS_OUT) * 0.5F; // 物品显示位置半径
    // 每5度分割一次圆弧
    private final static float PRECISION = 5;

    // 定义默认物品
    private final static ItemStack DEFAULT_ITEM = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);

    // 获取当前游戏
    private final static Minecraft mc = Minecraft.getMinecraft();

    private final ItemStack toolboxStack; // 当前工具箱物品堆
    private final List<ItemStack> items = new ArrayList<>(); // 存储的可选物品列表

    // 用于工具提示的临时存储
    private ItemStack selectedItemStack = null; // 当前鼠标悬停的物品

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container, ItemStack itemStack) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.toolboxStack = itemStack;
        loadItemsFromNBT();
    }

    /**
     * 加载可用工具列表
     */
    private void loadItemsFromNBT() {
        if (toolboxStack != null && toolboxStack.getItem() instanceof ItemToolboxPlus) {
            List<ItemStack> toolItems = ItemToolboxPlus.getToolItems(toolboxStack);
            if (!toolItems.isEmpty()) {
                items.add(DEFAULT_ITEM);
                items.addAll(toolItems);
            } else {
                items.clear();
            }
        }
    }

    /**
     * 绘制背景层（主要绘制逻辑）
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 初始化选中状态为 null
        selectedItemStack = null;

        // 如果没有可选物品则返回
        if (items.isEmpty()) return;

        // 计算选中项
        int xCenter = width / 2;
        int yCenter = height / 2;

        // 计算鼠标相对中心的角度和距离
        double dx = mouseX - xCenter;
        double dy = mouseY - yCenter;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 当前选中的物品索引
        int selectedItem = -1;
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

        // 绘制每个物品对应的扇形区域
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / items.size()) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / items.size()) + 0.25F) * 360F;
            int color;
            // 根据选中状态设置颜色（RGBA格式）
            if (i == selectedItem) {
                color = 0xFFFFDD60;
                selectedItemStack = items.get(i);
            } else {
                color = 0x33333380;
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

            ItemStack toolItemStack = items.get(i);

            if (toolItemStack != null) {
                // 渲染物品图标和效果
                itemRender.renderItemAndEffectIntoGUI(
                    mc.fontRenderer,
                    mc.getTextureManager(),
                    toolItemStack,
                    (int) posX,
                    (int) posY);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
    }

    /**
     * 绘制前景层
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 若存在工具提示物品则绘制其提示文本
        // if (tooltipItem != null) renderToolTip(tooltipItem, mouseX, mouseY);
    }

    /**
     * 处理输入事件
     */
    @Override
    public void handleInput() {
        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                this.handleKeyboardInput();
            }
            // 当选择按键松开时执行选择操作
            if (!Keyboard.isKeyDown(GameSettings.selectTool.getKeyCode())) {
                if (selectedItemStack != null) {
                    NetworkHandler.NETWORK.sendToServer(new ToolboxSelectionPacket(selectedItemStack));
                }
                mc.thePlayer.closeScreen();
            }
        }
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

    /**
     * 绘制扇形区域
     *
     * @param tessellator 顶点缓冲器
     * @param x           圆心X坐标
     * @param y           圆心Y坐标
     * @param radiusIn    内半径
     * @param radiusOut   外半径
     * @param startAngle  起始角度（度数）
     * @param endAngle    结束角度（度数）
     * @param color       颜色（ARGB格式）
     */
    private void drawSector(Tessellator tessellator, int x, int y, float radiusIn, float radiusOut, float startAngle,
        float endAngle, int color) {
        // 解析颜色分量
        int r = (color >> 24) & 0xFF;
        int g = (color >> 16) & 0xFF;
        int b = (color >> 8) & 0xFF;
        int a = color & 0xFF;

        drawPieArc(tessellator, x, y, zLevel, radiusIn, radiusOut, startAngle, endAngle, r, g, b, a);
    }

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

        // 计算需要分割的段数（保证每段不超过PRECISION度）
        int sections = Math.max(1, MathUtils.ceiling_float_int(angle / PRECISION));

        // 角度转换（度数转弧度）
        startAngle = (float) Math.toRadians(startAngle);
        endAngle = (float) Math.toRadians(endAngle);
        angle = endAngle - startAngle;

        // 分段绘制圆弧
        for (int i = 0; i < sections; i++) {
            // 计算当前段的起始和结束角度
            float angle1 = startAngle + (i / (float) sections) * angle;
            float angle2 = startAngle + ((i + 1) / (float) sections) * angle;
            // 计算四个顶点的坐标
            float pos1InX = x + radiusIn * (float) Math.cos(angle1);
            float pos1InY = y + radiusIn * (float) Math.sin(angle1);
            float pos1OutX = x + radiusOut * (float) Math.cos(angle1);
            float pos1OutY = y + radiusOut * (float) Math.sin(angle1);
            float pos2OutX = x + radiusOut * (float) Math.cos(angle2);
            float pos2OutY = y + radiusOut * (float) Math.sin(angle2);
            float pos2InX = x + radiusIn * (float) Math.cos(angle2);
            float pos2InY = y + radiusIn * (float) Math.sin(angle2);

            // 设置颜色并添加四边形顶点
            tessellator.setColorRGBA_F(r / 255F, g / 255F, b / 255F, a / 255F);
            tessellator.addVertex(pos1OutX, pos1OutY, z); // 外圈起点
            tessellator.addVertex(pos1InX, pos1InY, z); // 内圈起点
            tessellator.addVertex(pos2InX, pos2InY, z); // 内圈终点
            tessellator.addVertex(pos2OutX, pos2OutY, z); // 外圈终点
        }
    }
}
