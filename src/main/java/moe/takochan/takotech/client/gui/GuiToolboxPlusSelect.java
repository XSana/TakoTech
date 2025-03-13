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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.network.NetworkHandler;
import moe.takochan.takotech.network.PacketToolboxSelected;

@SideOnly(Side.CLIENT)
public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    // 物品显示位置半径
    public final static float ITEM_RADIUS = (RADIUS_IN + RADIUS_OUT) * 0.5F;
    // 获取当前游戏实例
    private final static Minecraft mc = Minecraft.getMinecraft();
    // 默认物品实例
    private final static ItemStack DEFAULT_ITEM = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);

    // 存储的可选物品列表
    private final List<ToolData> items = new ArrayList<>();
    // 关联的容器实例
    private final ContainerToolboxPlusSelect container;
    // 当前选中的物品索引
    private int selectIndex = -1;

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.container = container;

    }

    /**
     * 初始化 GUI
     */
    @Override
    public void initGui() {
        super.initGui();
        EntityPlayer player = container.getPlayer();
        loadItems(player.inventory.getCurrentItem());
    }

    /**
     * 绘制背景层（主要绘制逻辑）
     *
     * @param partialTicks 部分刻（用于动画效果）
     * @param mouseX       鼠标 X 坐标
     * @param mouseY       鼠标 Y 坐标
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

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
        selectIndex = -1;
        int n = items.size();
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / n) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / n) + 0.25F) * 360F;

            if (angle < sectorStart) angle += 360;
            if (angle >= sectorStart && angle < sectorEnd && distance >= RADIUS_IN && distance < RADIUS_OUT) {
                selectIndex = i;
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
                    int color = (i == selectIndex) ? 0xFFFFDD60 : 0x33333380;
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

            ItemStack toolItemStack = items.get(i)
                .getItemStack();

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
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
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
                if (selectIndex != -1) {
                    int slot = items.get(selectIndex)
                        .getSlot();
                    NetworkHandler.NETWORK.sendToServer(new PacketToolboxSelected(slot));
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
     * 加载物品到 GUI 中
     * 
     * @param stack 玩家当前手持的物品
     */
    private void loadItems(ItemStack stack) {
        items.clear();
        if (ItemToolboxPlus.isMetaGeneratedTool(stack)) {
            this.items.add(new ToolData(-1, DEFAULT_ITEM));
        }
        final List<ToolData> list = getGTTools(stack);
        if (list == null || list.isEmpty()) return;
        this.items.addAll(list);
    }

    /**
     * 获取 GT 工具箱中的工具
     * 
     * @param stack 玩家当前手持的物品
     * @return 工具列表，如果不存在则返回 null
     */
    public List<ToolData> getGTTools(ItemStack stack) {
        return ItemToolboxPlus.getToolbox(stack)
            .map(toolbox -> {
                if (toolbox.getItem() instanceof ItemToolboxPlus) {
                    return ItemToolboxPlus.getToolItems(toolbox);
                }
                return null;
            })
            .orElse(null);
    }
}
