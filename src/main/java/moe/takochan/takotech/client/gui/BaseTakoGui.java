package moe.takochan.takotech.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.client.renderer.shader.Framebuffer;
import moe.takochan.takotech.client.renderer.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.shader.ShaderType;
import moe.takochan.takotech.common.Reference;

@SideOnly(Side.CLIENT)
public abstract class BaseTakoGui<T extends BaseContainer> extends GuiContainer {

    // 静态资源与常量
    private final static Minecraft mc = Minecraft.getMinecraft();
    private final static ResourceLocation GUI_TEXTURE = new ResourceLocation(
        Reference.RESOURCE_ROOT_ID,
        "textures/guis/base_gui.png");

    // FBO 渲染状态
    private Framebuffer fboDownscale;
    private Framebuffer fboHorizontal;
    private Framebuffer fboVertical;
    private int fboWidth = -1;
    private int fboHeight = -1;

    // 动画
    private final float maxBlurScale;
    private long openTime;

    // GUI 相关字段
    private final int guiWidth;
    private final int guiHeight;
    private final T container;
    private final String title;
    private int titleBarWidth;
    private int screenWidth = -1;
    private int screenHeight = -1;

    public BaseTakoGui(T container, String title, int guiWidth, int guiHeight) {
        this(container, title, guiWidth, guiHeight, 1.0f);
    }

    public BaseTakoGui(T container, String title, int guiWidth, int guiHeight, float maxBlurScale) {
        super(container);
        this.container = container;
        this.title = title;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.maxBlurScale = maxBlurScale;
    }

    public T getContainer() {
        return container;
    }

    public String getTitle() {
        return title;
    }

    public int getGuiWidth() {
        return guiWidth;
    }

    public int getGuiHeight() {
        return guiHeight;
    }

    @Override
    public void initGui() {
        // 初始化 GUI
        this.xSize = guiWidth;
        this.ySize = guiHeight + 16;
        super.initGui();
        // 记录屏幕尺寸
        screenWidth = mc.displayWidth;
        screenHeight = mc.displayHeight;
        // 计算标题栏宽度
        this.titleBarWidth = calculateTitleBarWidth();
        // 初始化或重建 FBO
        this.updateFBO();
        // 记录打开时间
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void onGuiClosed() {
        this.deleteFBO();
        super.onGuiClosed();
    }

    @Override
    public void drawDefaultBackground() {

        // 计算模糊度
        float blurScale = getDynamicBlurScale();
        // 绘制黑色半透明背景
        this.drawGradientRect(0, 0, this.width, this.height, 0x33101010, 0x4C101010);
        // 获取MC当前缓冲帧的纹理ID
        int mainFboTexture = mc.getFramebuffer().framebufferTexture;
        // 降采样
        fboDownscale.bind();
        drawFullscreenQuadWithTexture(fboWidth, fboHeight, mainFboTexture);
        fboDownscale.unbind();
        // 水平模糊
        applyShaderPass(fboHorizontal, fboDownscale.getTextureId(), ShaderType.HORIZONTAL_BLUR, blurScale);
        // 垂直模糊
        applyShaderPass(fboVertical, fboHorizontal.getTextureId(), ShaderType.VERTICAL_BLUR, blurScale);
        // 最终输出，绘制到当前缓冲帧
        // 将MC的当前缓冲帧绑定为当前FBO
        mc.getFramebuffer()
            .bindFramebuffer(true);
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        // 绘制全屏矩形
        drawFullscreenQuadWithTexture(fboWidth, fboHeight, fboVertical.getTextureId());
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制标题栏
        drawTitleBar(guiLeft, guiTop, guiWidth);
        // 绘制主体
        drawBody(guiLeft, guiTop + 16, guiWidth, guiHeight);
        // 绘制底部
        drawBottom(guiLeft, guiTop + guiHeight - 3, guiWidth);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int middleWidth = Math.max(fontRendererObj.getStringWidth(title) + 8, 4);

        // 标题文字居中绘制
        // int textX = guiLeft + 4 + (middleWidth - fontRendererObj.getStringWidth(title)) / 2;
        // int textY = guiTop + 5; // 垂直居中
        // fontRendererObj.drawString(title, textX, textY, 0x404040);
    }

    private float getDynamicBlurScale() {
        float elapsed = (System.currentTimeMillis() - openTime) / 600.0f;
        float t = Math.min(elapsed, 1.0f); // 正规化时间
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
        return eased * maxBlurScale;
    }

    private void applyShaderPass(Framebuffer targetFBO, int textureId, ShaderType shaderType, float blurScale) {

        // 绑定目标FBO
        targetFBO.bind();
        // 应用着色器
        ShaderProgram shader = shaderType.get();
        shader.use();
        shader.setUniform("image", 0);
        shader.setUniform("blurScale", blurScale);
        shader.setUniform("texSize", (float) screenWidth, (float) screenHeight);
        // 绘制全屏矩形
        drawFullscreenQuadWithTexture(screenWidth, screenHeight, textureId);
        // 清除着色器
        ShaderProgram.clear();
        // 解绑FBO
        targetFBO.unbind();
    }

    private void drawFullscreenQuadWithTexture(int width, int height, int textureId) {
        // 设置矩阵
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        // 绑定纹理
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        // 绘制全屏矩形
        drawFullscreenQuad(width, height);
        // 恢复矩阵
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void updateFBO() {
        // 计算新的 FBO 尺寸
        int newFboWidth = screenWidth / 2;
        int newFboHeight = screenHeight / 2;
        // 检查是否需要重建 FBO
        if (fboWidth != newFboWidth || fboHeight != newFboHeight) {
            // 删除旧的 FBO
            deleteFBO();
            // 更新 FBO 尺寸
            fboWidth = newFboWidth;
            fboHeight = newFboHeight;
            // 创建新的 FBO
            fboDownscale = new Framebuffer(fboWidth, fboHeight);
            fboHorizontal = new Framebuffer(fboWidth, fboHeight);
            fboVertical = new Framebuffer(fboWidth, fboHeight);
        }
    }

    private void deleteFBO() {
        if (fboDownscale != null) fboDownscale.delete();
        if (fboHorizontal != null) fboHorizontal.delete();
        if (fboVertical != null) fboVertical.delete();
        fboDownscale = fboHorizontal = fboVertical = null;
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
        int actualHeight = Math.max(height - 3 - 16, 3); // 最小高度3px

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

    private void drawFullscreenQuad(int width, int height) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorOpaque_I(0xFFFFFF);
        tess.addVertexWithUV(0, height, 0, 0.0, 0.0);
        tess.addVertexWithUV(width, height, 0, 1.0, 0.0);
        tess.addVertexWithUV(width, 0, 0, 1.0, 1.0);
        tess.addVertexWithUV(0, 0, 0, 0.0, 1.0);
        tess.draw();
    }

}
