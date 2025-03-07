package moe.takochan.takotech.client.gui;

import static moe.takochan.takotech.utils.SectorVertexUtils.DEFAULT_SECTOR_VERTEX_DATA;
import static moe.takochan.takotech.utils.SectorVertexUtils.RADIUS_IN;
import static moe.takochan.takotech.utils.SectorVertexUtils.RADIUS_OUT;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.item.tool.ContainerToolbox;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.network.NetworkHandler;
import moe.takochan.takotech.network.PacketToolboxSelection;
import moe.takochan.takotech.utils.CommonUtils;

@SideOnly(Side.CLIENT)
public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    public final static float ITEM_RADIUS = (RADIUS_IN + RADIUS_OUT) * 0.5F; // 物品显示位置半径

    // 定义默认物品
    private final static ItemStack DEFAULT_ITEM = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);

    // 获取当前游戏
    private final static Minecraft mc = Minecraft.getMinecraft();

    private final ItemStack handItemStack; // 当前工具箱物品堆
    private final List<ItemStack> items = new ArrayList<>(); // 存储的可选物品列表

    // 用于工具提示的临时存储
    private ItemStack selectedItemStack = null; // 当前鼠标悬停的物品

    private final ContainerToolboxPlusSelect container;

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.container = container;
        this.handItemStack = this.container.getPlayer().inventory.getCurrentItem();
        loadItemsFromNBT();
    }

    /**
     * 加载可用工具列表
     */
    private void loadItemsFromNBT() {
        if (handItemStack == null) return;

        if (handItemStack.getItem() instanceof ItemToolboxPlus) {
            items.addAll(ItemToolboxPlus.getToolItems(handItemStack));
        } else if (handItemStack.getItem() instanceof MetaGeneratedTool) {
            NBTTagCompound nbt = CommonUtils.openNbtData(handItemStack);
            if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) {
                ItemStack toolbox = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);
                CommonUtils.openNbtData(toolbox)
                    .setTag(
                        NBTConstants.TOOLBOX_ITEMS,
                        nbt.getTagList(NBTConstants.TOOLBOX_DATA, Constants.NBT.TAG_COMPOUND));
                items.add(DEFAULT_ITEM);
                items.addAll(ItemToolboxPlus.getToolItems(toolbox));
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
        int n = items.size();
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / n) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / n) + 0.25F) * 360F;

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

        // 使用预计算数据绘制扇区
        if (n <= 9) {
            List<List<float[][]>> sectors = DEFAULT_SECTOR_VERTEX_DATA.get(n);
            if (sectors != null) {
                for (int i = 0; i < n; i++) {
                    int color = (i == selectedItem) ? 0xFFFFDD60 : 0x33333380;
                    int r = (color >> 24) & 0xFF;
                    int g = (color >> 16) & 0xFF;
                    int b = (color >> 8) & 0xFF;
                    int a = color & 0xFF;
                    tess.setColorRGBA_F(r / 255F, g / 255F, b / 255F, a / 255F);

                    List<float[][]> segments = sectors.get(i);
                    for (float[][] segment : segments) {
                        float[] pos1Out = segment[0];
                        float[] pos1In = segment[1];
                        float[] pos2In = segment[2];
                        float[] pos2Out = segment[3];

                        tess.addVertex(xCenter + pos1Out[0], yCenter + pos1Out[1], zLevel);
                        tess.addVertex(xCenter + pos1In[0], yCenter + pos1In[1], zLevel);
                        tess.addVertex(xCenter + pos2In[0], yCenter + pos2In[1], zLevel);
                        tess.addVertex(xCenter + pos2Out[0], yCenter + pos2Out[1], zLevel);
                    }
                    if (i == selectedItem) selectedItemStack = items.get(i);
                }
            }
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
                    NetworkHandler.NETWORK.sendToServer(new PacketToolboxSelection(selectedItemStack));
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
}
