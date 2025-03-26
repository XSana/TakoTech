package moe.takochan.takotech.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.client.renderer.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.shader.ShaderType;
import moe.takochan.takotech.common.Reference;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public abstract class BaseTakoGui<T extends BaseContainer> extends GuiContainer {

    private final static Minecraft mc = Minecraft.getMinecraft();

    private final static ResourceLocation GUI_TEXTURE = new ResourceLocation(
        Reference.RESOURCE_ROOT_ID,
        "textures/guis/base_gui.png");

    private final T container;

    private final String title;

    private final int windowWidth;
    private final int windowHeight;
    private int titleBarWidth;

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
        this.ySize = windowHeight + 16;

        titleBarWidth = calculateTitleBarWidth();

        super.initGui();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int middleWidth = Math.max(fontRendererObj.getStringWidth(title) + 8, 4);

        // 标题文字居中绘制
//        int textX = guiLeft + 4 + (middleWidth - fontRendererObj.getStringWidth(title)) / 2;
//        int textY = guiTop + 5; // 垂直居中
//        fontRendererObj.drawString(title, textX, textY, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制标题栏
        drawTitleBar(guiLeft, guiTop, windowWidth);
        // 绘制主体
        drawBody(guiLeft, guiTop + 16, windowWidth, windowHeight);
        // 绘制底部
        drawBottom(guiLeft, guiTop + windowHeight - 3, windowWidth);
    }

    @Override
    public void drawDefaultBackground() {
        // 保存当前的 OpenGL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;

        super.drawDefaultBackground();
        int texture = mc.getFramebuffer().framebufferTexture;

        // 创建FBO实例
//        Framebuffer fboBackground = new Framebuffer(screenWidth, screenHeight, true);
//
//        // Shader
//        ShaderProgram shaderProgram = ShaderType.HORIZONTAL_BLUR.get();
//        shaderProgram.use();
//        shaderProgram.setUniform("image", 0);
//        shaderProgram.setUniform("width", screenWidth);
//
//        // 绑定贴图
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
//        // 绑定fbo并绘制
//        fboBackground.bindFramebuffer(true);
//        renderFullScreenQuad(screenWidth, screenHeight);
//        fboBackground.unbindFramebuffer();
//
//        // 解绑Shader
//        ShaderProgram.clear();

        // Shader
//        shaderProgram = ShaderType.VERTICAL_BLUR.get();
//        shaderProgram.use();
//        shaderProgram.setUniform("image", 0);
//        shaderProgram.setUniform("height", screenHeight);
//
//        fboBackground.bindFramebufferTexture();
//        // 绑定贴图
//        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboBackground.framebufferTexture);
//        // 绘制
//        renderFullScreenQuad(screenWidth, screenHeight);
//        ShaderProgram.clear();
        // 恢复之前保存的状态
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        // 释放 FBO 资源
//        fboBackground.deleteFramebuffer();

//        mc.getFramebuffer().bindFramebuffer(true);
    }

    /**
     * 绘制覆盖全屏的四边形，供后处理阶段使用
     *
     * @param width  目标宽度
     * @param height 目标高度
     */
    private void renderFullScreenQuad(int width, int height) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBegin(GL11.GL_QUADS);
        // 左下角
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        // 右下角
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(width, 0.0f);
        // 右上角
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(width, height);
        // 左上角
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, height);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private int calculateTitleBarWidth() {
        int titleWidth = fontRendererObj.getStringWidth(title);
        int padding = 8; // 标题左右留白
        return 3 + Math.max(titleWidth + padding, 4) + 15; // 左侧3像素 + 中间宽度 + 右侧15像素
    }

    private void drawTitleBar(int x, int y, int width) {
        int middleWidth = Math.max(fontRendererObj.getStringWidth(title) + 8, 4);

        // 绘制左侧 (1-3像素)
        drawTexturedModalRect(x, y, 0, 0, 3, 16);

        // 中间拉伸段 (4号像素)
        drawScaledSegment(x + 3, y, 3, 0, middleWidth, 16, 1, 16);

        // 右侧基础部分 (5-20像素中的前16像素)
        drawTexturedModalRect(x + 3 + middleWidth, y, 4, 0, 16, 16);

        // 右侧延伸部分
        int remaining = width - titleBarWidth - 2;
        if (remaining > 0) {
            drawScaledSegment(x + titleBarWidth, y, 20, 0, remaining, 16, 1, 16);
        }

    }

    private void drawBody(int x, int y, int width, int height) {

        // 尺寸约束计算
        int actualWidth = Math.max(width, titleBarWidth); // 保证最小宽度
        int actualHeight = Math.max(height - 3 - 16, 3);  // 最小高度3px

        // 左侧装饰（20,0-20,1）
        drawScaledSegment(x, y, 0, 16, 2, actualHeight, 2, 1);

        // 中间平铺区域（16,0-18,2）
        int middleWidth = actualWidth - 3;
        int segmentWidth = Math.max(3, middleWidth);
        drawScaledSegment(x + 2, y, 0, 17, segmentWidth, 3, 1, 3);
        drawScaledSegment(x + 2, y + 3, 2, 16, segmentWidth, actualHeight - 3, 1, 1);

        // 右侧装饰
        if (actualWidth > titleBarWidth) {
            drawTexturedModalRect(x + actualWidth - 2, y, 1, 17, 3, 3);
            drawScaledSegment(x + actualWidth - 1, y + 3, 3, 16, 2, actualHeight - 3, 2, 1);
        } else {
            drawScaledSegment(x + actualWidth - 1, y, 3, 16, 2, actualHeight, 2, 1);
        }
    }

    private void drawBottom(int x, int y, int width) {
        // 底部左 (16x16)
        drawTexturedModalRect(x, y, 4, 17, 3, 3);
        // 底部中 (动态拉伸)
        drawScaledSegment(x + 3, y - 1, 7, 17, width - 3 - 2, 4, 1, 3);
        // 底部右 (16x16)
        drawTexturedModalRect(x + width - 2, y, 8, 17, 3, 3);
    }

    private void drawScaledSegment(int x, int y, int textureX, int textureY, int width, int height, int textureWidth,
        int textureHeight) {
        float uScale = 1.0F / 256.0F; // 贴图宽度比例
        float vScale = 1.0F / 256.0F; // 贴图高度比例
        float u = textureX * uScale;
        float v = textureY * vScale;
        float u2 = (textureX + textureWidth) * uScale;
        float v2 = (textureY + textureHeight) * vScale;

        drawScaledTexture(x, y, width, height, u, v, u2, v2);
    }

    private void drawScaledTexture(int x, int y, int width, int height, float u, float v, float u2, float v2) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, u, v2);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, u2, v2);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, u2, v);
        tessellator.addVertexWithUV(x, y, this.zLevel, u, v);
        tessellator.draw();
    }

}
