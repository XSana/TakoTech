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

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.Constants;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.utils.MathUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    // toolbox plus
    private final ItemStack toolboxStack;
    private final List<ItemStack> items = new ArrayList<>();

    // 用于在前景层绘制工具提示时缓存鼠标悬停的物品
    private ItemStack tooltipItem = null;

    private boolean closing;
    private boolean doneClosing;
    private double startAnimation;

    private int selectedItem = -1;
    private boolean keyCycleBeforeL = false;
    private boolean keyCycleBeforeR = false;

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container, ItemStack itemStack) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.toolboxStack = itemStack;
        loadItemsFromNBT();

        this.startAnimation = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
    }

    private void loadItemsFromNBT() {
        NBTTagCompound nbt = this.toolboxStack.getTagCompound();
        if (nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            NBTTagList itemsTagList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            if (itemsTagList != null && itemsTagList.tagCount() > 0) {
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
    public void initGui() {
        super.initGui();
    }


    @Override
    public void onGuiClosed() {
    }

    @Override
    public void drawDefaultBackground() {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (items.isEmpty())
            return;

        ItemStack inHand = Minecraft.getMinecraft().thePlayer.getHeldItem();
        boolean hasAddButton = false;
        int numItems = items.size();


        // 计算动画进度（使用世界总时间，不含部分插值）
        final float OPEN_ANIMATION_LENGTH = 2.5F;
        long currentTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
        float openAnimation = closing
            ? (1F - ((currentTime - (float) startAnimation) / OPEN_ANIMATION_LENGTH))
            : ((currentTime - (float) startAnimation) / OPEN_ANIMATION_LENGTH);
        if (closing && openAnimation <= 0F)
            doneClosing = true;
        float animProgress = MathUtils.clamp_float(openAnimation, 0F, 1F);
        float radiusIn = Math.max(0.1F, 30F * animProgress);
        float radiusOut = radiusIn * 2F;
        float itemRadius = (radiusIn + radiusOut) * 0.5F;
        float animTop = (1F - animProgress) * (height / 2F);

        int x = width / 2;
        int y = height / 2;

        // 依靠鼠标位置确定当前选中的扇区
        double dx = mouseX - x;
        double dy = mouseY - y;
        double a = Math.toDegrees(Math.atan2(dy, dx));
        double d = Math.sqrt(dx * dx + dy * dy);
        float s0 = (((0F - 0.5F) / (float) numItems) + 0.25F) * 360F;
        if (a < s0)
            a += 360D;
        selectedItem = -1;
        for (int i = 0; i < numItems; i++) {
            float s = (((i - 0.5F) / (float) numItems) + 0.25F) * 360F;
            float e = (((i + 0.5F) / (float) numItems) + 0.25F) * 360F;
            if (a >= s && a < e && d >= radiusIn && d < radiusOut) {
                selectedItem = i;
                break;
            }
        }

        // 开始绘制
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glTranslatef(0F, animTop, 0F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        tooltipItem = null;
        boolean hasMouseOver = false;
        // 绘制每个扇形区
        for (int i = 0; i < numItems; i++) {
            float s = (((i - 0.5F) / (float) numItems) + 0.25F) * 360F;
            float e = (((i + 0.5F) / (float) numItems) + 0.25F) * 360F;
            if (selectedItem == i) {
                // 选中扇区使用白色半透明高亮
                drawPieArc(tessellator, x, y, zLevel, radiusIn, radiusOut, s, e, 255, 255, 255, 64);
                hasMouseOver = true;
                tooltipItem = items.get(i);
            } else {
                // 非选中扇区使用黑色半透明
                drawPieArc(tessellator, x, y, zLevel, radiusIn, radiusOut, s, e, 0, 0, 0, 64);
            }
        }

        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (hasMouseOver) {
            if (inHand != null && inHand.stackSize > 0) {
                if (tooltipItem != null && tooltipItem.stackSize > 0) {
//                    drawCenteredString(fontRendererObj, I18n.format("text.toolbelt.swap"),
//                        width / 2, (height - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                } else {
//                    drawCenteredString(fontRendererObj, I18n.format("text.toolbelt.insert"),
//                        width / 2, (height - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                }
            } else if (tooltipItem != null && tooltipItem.stackSize > 0) {
//                drawCenteredString(fontRendererObj, I18n.format("text.toolbelt.extract"),
//                    width / 2, (height - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            }
        }

        // 绘制扇形区域中的物品图标
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < numItems; i++) {
            float angle1 = (((i) / (float) numItems) + 0.25F) * 2F * (float) Math.PI;
            float posX = x - 8 + itemRadius * (float) Math.cos(angle1);
            float posY = y - 8 + itemRadius * (float) Math.sin(angle1);
            ItemStack inSlot = items.get(i);
            if (inSlot != null && inSlot.stackSize > 0) {
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), inSlot, (int) posX, (int) posY);
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), inSlot, (int) posX, (int) posY, "");

            }
        }
        RenderHelper.disableStandardItemLighting();

        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 若存在工具提示物品则绘制其提示文本
        if (tooltipItem != null)
            renderToolTip(tooltipItem, mouseX, mouseY);
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

    /**
     * 取消渲染 HUD 时的准星（仅当当前 GUI 为径向菜单时）。
     * 注意：1.7.10 中订阅事件需通过注册至 MinecraftForge.EVENT_BUS。
     */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS)
            return;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiToolboxPlusSelect) {
            event.setCanceled(true);
        }
    }


    /**
     * 根据给定的坐标设置鼠标位置（转换为屏幕坐标）
     *
     * @param x 新的 x 坐标
     * @param y 新的 y 坐标
     */
    private void setMousePosition(double x, double y) {
        Mouse.setCursorPosition((int) (x * mc.displayWidth / width), (int) (y * mc.displayHeight / width));
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
    private void drawPieArc(Tessellator tessellator, int x, int y, float z,
                            float radiusIn, float radiusOut, float startAngle, float endAngle,
                            int r, int g, int b, int a) {
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
    //endregion
}
