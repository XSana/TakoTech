package moe.takochan.takotech.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.common.Reference;

public abstract class BaseTakoGui<T extends BaseContainer> extends GuiContainer {

    private final static Minecraft mc = Minecraft.getMinecraft();

    private final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        Reference.RESOURCE_ROOT_ID,
        "textures/guis/base_gui.png");

    private final T container;

    private final String title;
    private final int windowWidth; // 窗口宽度
    private final int windowHeight; // 窗口高度

    public BaseTakoGui(T container, String title, int windowWidth, int windowHeight) {
        super(container);
        this.container = container;
        this.title = title;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public T getContainer() {
        return container;
    }

    public String getTitle() {
        return title;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    @Override
    public void initGui() {
        this.xSize = windowWidth;
        this.ySize = windowHeight;

        super.initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制标题栏
        drawTitleBar(guiLeft, guiTop, windowWidth, 16);

        // 绘制主体
        drawBody(guiLeft, guiTop + 16, windowWidth, windowHeight - 32);

        // 绘制底部
        drawBottom(guiLeft, guiTop + windowHeight - 16, windowWidth, 16);
    }

    private void drawTitleBar(int x, int y, int width, int height) {
        // 标题栏左 (16x16)
        drawTexturedModalRect(x, y, 0, 0, 16, 16);
        // 标题栏中 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x + 16, y, 16, 0, width - 32, 16, 16, 16);
        // 标题栏右 (16x16)
        drawTexturedModalRect(x + width - 16, y, 32, 0, 16, 16);
    }

    private void drawBody(int x, int y, int width, int height) {
        // 主体左 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x, y, 0, 16, 16, height, 16, 16);
        // 主体中 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x + 16, y, 16, 16, width - 32, height, 16, 16);
        // 主体右 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x + width - 16, y, 32, 16, 16, height, 16, 16);
    }

    private void drawBottom(int x, int y, int width, int height) {
        // 底部左 (16x16)
        drawTexturedModalRect(x, y, 0, 32, 16, 16);
        // 底部中 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x + 16, y, 16, 32, width - 32, 16, 16, 16);
        // 底部右 (16x16)
        drawTexturedModalRect(x + width - 16, y, 32, 32, 16, 16);
    }

    private void drawModalRectWithCustomSizedTexture(int x, int y, int textureX, int textureY, int width, int height,
        int textureWidth, int textureHeight) {
        float uScale = 1.0F / 256.0F; // 贴图宽度比例
        float vScale = 1.0F / 256.0F; // 贴图高度比例
        float u = textureX * uScale;
        float v = textureY * vScale;
        float u2 = (textureX + textureWidth) * uScale;
        float v2 = (textureY + textureHeight) * vScale;

        // 绘制动态拉伸部分
        drawTexturedModalRect(x, y, width, height, u, v, u2, v2);
    }

    private void drawTexturedModalRect(int x, int y, int width, int height, float u, float v, float u2, float v2) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, u, v2);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, u2, v2);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, u2, v);
        tessellator.addVertexWithUV(x, y, this.zLevel, u, v);
        tessellator.draw();
    }
}
