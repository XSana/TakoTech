package moe.takochan.takotech.client.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.client.renderer.shader.Framebuffer;
import moe.takochan.takotech.client.renderer.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.shader.ShaderType;
import moe.takochan.takotech.common.Reference;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.GLU;

@SideOnly(Side.CLIENT)
public abstract class BaseTakoGui<T extends BaseContainer> extends GuiContainer {

    private final static Minecraft mc = Minecraft.getMinecraft();

    private final static ResourceLocation GUI_TEXTURE = new ResourceLocation(Reference.RESOURCE_ROOT_ID,
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
        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;

        // 获取原始纹理ID
        int mcTextureId = mc.getFramebuffer().framebufferTexture;

        // --- Pass 1: 水平模糊 ---
        Framebuffer fboHorizontal = new Framebuffer(screenWidth, screenHeight);
        fboHorizontal.bind();

        // 绑定原始纹理
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mcTextureId);

        // 使用水平模糊 shader
        ShaderProgram horizontalShader = ShaderType.HORIZONTAL_BLUR.get();
        horizontalShader.use();
        horizontalShader.setUniform("image", 0);
        horizontalShader.setUniform("blurScale", 50.0f);

        // 绘制全屏四边形
        // 保存当前状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        // 设置视口、正交投影和其他状态
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // 确保禁用深度测试和启用混合（如果需要）
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 绘制全屏四边形
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(screenWidth, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(screenWidth, screenHeight);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, screenHeight);
        GL11.glEnd();

        // 恢复状态
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        fboHorizontal.unbind();
        ShaderProgram.clear();

        // --- Pass 2: 垂直模糊 ---
        Framebuffer fboVertical = new Framebuffer(screenWidth, screenHeight);
        fboVertical.bind();

        // 将水平模糊结果作为输入
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboHorizontal.getTextureId());

        // 使用垂直模糊 shader
        ShaderProgram verticalShader = ShaderType.VERTICAL_BLUR.get();
        verticalShader.use();
        verticalShader.setUniform("image", 0);
        horizontalShader.setUniform("blurScale", 50.0f);

        // 保存当前状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        // 设置视口、正交投影和其他状态
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // 确保禁用深度测试和启用混合（如果需要）
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 绘制全屏四边形
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(screenWidth, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(screenWidth, screenHeight);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, screenHeight);
        GL11.glEnd();

        // 恢复状态
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        fboVertical.unbind();
        ShaderProgram.clear();

        // --- 最终输出: 绘制到屏幕 ---
        mc.getFramebuffer().bindFramebuffer(false);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboVertical.getTextureId());

        // 直接绘制全屏四边形到屏幕
        // 保存当前状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        // 设置视口、正交投影和其他状态
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        // 确保禁用深度测试和启用混合（如果需要）
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 绘制全屏四边形
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(screenWidth, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(screenWidth, screenHeight);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, screenHeight);
        GL11.glEnd();

        // 恢复状态
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        // 清理 shader 状态（如果需要）
        ShaderProgram.clear();

        fboVertical.delete();
        fboHorizontal.delete();

        //        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        //        GL11.glMatrixMode(GL11.GL_PROJECTION);
        //        GL11.glLoadIdentity();
        //        GL11.glOrtho(0, screenWidth, 0, screenHeight, -1, 1);
        //        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        //        GL11.glLoadIdentity();
        //
        //        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //        fboBackground.bindTexture();        // 绘制四边形
        //        GL11.glBegin(GL11.GL_QUADS);
        //        // 左下角
        //        GL11.glTexCoord2f(0.0f, 0.0f);
        //        GL11.glVertex2f(0.0f, 0.0f);
        //        // 右下角
        //        GL11.glTexCoord2f(1.0f, 0.0f);
        //        GL11.glVertex2f(width, 0.0f);
        //        // 右上角
        //        GL11.glTexCoord2f(1.0f, 1.0f);
        //        GL11.glVertex2f(width, height);
        //        // 左上角
        //        GL11.glTexCoord2f(0.0f, 1.0f);
        //        GL11.glVertex2f(0.0f, height);
        //        GL11.glEnd();

        //        mc.getFramebuffer().bindFramebuffer(true);
        //        err = GL11.glGetError();

        //        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(screenWidth * screenHeight * 4);
        //        byteBuffer.order(ByteOrder.nativeOrder());
        //        fboBackground.bindTexture();
        //        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        //
        //        BufferedImage image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        //        byteBuffer.rewind();
        //
        //        for (int y = 0; y < height; y++) {
        //            for (int x = 0; x < width; x++) {
        //                int i = (x + (width * y)) * 4;
        //                // 注意取出无符号字节
        //                int r = byteBuffer.get(i) & 0xFF;
        //                int g = byteBuffer.get(i + 1) & 0xFF;
        //                int b = byteBuffer.get(i + 2) & 0xFF;
        //                int a = byteBuffer.get(i + 3) & 0xFF;
        //                // 组合成ARGB像素值
        //                int pixel = ((a << 24) | (r << 16) | (g << 8) | b);
        //                // 翻转y坐标（OpenGL与BufferedImage的坐标系差异）
        //                image.setRGB(x, height - y - 1, pixel);
        //            }
        //        }
        //        try {
        //
        //            ImageIO.write(image, "PNG", new File("D://texture.png"));
        //        } catch (Exception e) {
        //        }
        //
        //        int err = GL11.glGetError();
        //        if (err != 0) {
        //            String s1 = GLU.gluErrorString(err);
        //            TakoTechMod.LOG.error("OpenGL error: " + s1);
        //        }

        //        fboBackground.unbind();

        //        Framebuffer outFboBackground = new Framebuffer(screenWidth, screenHeight);
        //        // Shader
        //        ShaderProgram shaderProgram = ShaderType.HORIZONTAL_BLUR.get();
        //        shaderProgram.use();
        //        shaderProgram.setUniform("image", fboBackground.getTextureId());
        //        shaderProgram.setUniform("width", screenWidth);
        //        // 绑定fbo并绘制
        //        outFboBackground.bind();
        //        fboBackground.bindTexture();
        //        fboBackground.renderToScreen();
        //        outFboBackground.unbind();
        //        // 解绑Shader
        //        ShaderProgram.clear();
        //
        //
        //        // Shader
        //        shaderProgram = ShaderType.VERTICAL_BLUR.get();
        //        shaderProgram.use();
        //        shaderProgram.setUniform("image", outFboBackground.getTextureId());
        //        //        shaderProgram.setUniform("height", screenHeight);
        //        // 绘制
        //        outFboBackground.bindTexture();
        //        outFboBackground.renderToScreen();
        //        ShaderProgram.clear();
        //
        //        // 释放 FBO 资源
        //        outFboBackground.delete();
        //        fboBackground.delete();
        //
        //        mc.getFramebuffer()
        //            .bindFramebuffer(true);
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
