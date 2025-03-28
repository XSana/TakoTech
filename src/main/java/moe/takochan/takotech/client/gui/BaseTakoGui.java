package moe.takochan.takotech.client.gui;

import java.awt.Rectangle;

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
    protected final static Minecraft mc = Minecraft.getMinecraft();
    protected final static ResourceLocation GUI_TEXTURE = new ResourceLocation(
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
    private final long openTime;

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
        this.openTime = System.currentTimeMillis();
    }

    // region 抽象方法

    /**
     * 绘制 GUI 的主体区域内容，如按钮、图标、物品槽等。
     * <p>
     * 在此方法中，所有绘制操作的坐标都是相对于 GUI 的主体区域及其边框的左上角。 可调用 {@link #getContentArea()} 获取该区域的大小，用于布局或对齐计算。
     * <br>
     * 鼠标坐标为屏幕的绝对位置，如需在该区域内进行鼠标悬浮或点击判断，请手动转换为相对坐标。
     * </p>
     *
     * @param partialTicks 渲染补间时间
     * @param mouseX       鼠标屏幕坐标 X（绝对坐标）
     * @param mouseY       鼠标屏幕坐标 Y（绝对坐标）
     */
    protected abstract void drawComponents(float partialTicks, int mouseX, int mouseY);

    /**
     * 绘制 GUI 主体区域的前景内容，例如文本、状态图标、数值等。
     * <p>
     * 本方法中的所有绘制坐标也均相对于 GUI 主体区域及边框的左上角。 可通过 {@link #getContentArea()} 获取该区域的宽度和高度，用于排版和对齐。
     * <br>
     * 鼠标坐标为屏幕绝对位置，如需绘制悬浮提示等交互内容，请将其转换为相对坐标后处理。
     * </p>
     *
     * @param mouseX 鼠标屏幕坐标 X（绝对坐标）
     * @param mouseY 鼠标屏幕坐标 Y（绝对坐标）
     */
    protected abstract void drawForeground(int mouseX, int mouseY);

    // endregion

    // region Getter 方法

    /**
     * @return 当前容器对象
     */
    public T getContainer() {
        return container;
    }

    /**
     * @return GUI 标题文本
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return GUI 宽度（像素）
     */
    public int getGuiWidth() {
        return guiWidth;
    }

    /**
     * @return GUI 高度（像素）
     */
    public int getGuiHeight() {
        return guiHeight;
    }

    /**
     * @return 可绘制区域相对坐标矩形（去除边框与标题栏）
     */
    public Rectangle getContentArea() {
        return new Rectangle(0, 0, guiWidth - 6, guiHeight - 16 - 6);
    }

    /**
     * 将鼠标的屏幕绝对 X 坐标转换为 GUI 主体区域内的相对 X 坐标。
     *
     * @param mouseX 鼠标的屏幕绝对 X 坐标
     * @return 相对于 GUI 主体区域左上角的 X 坐标
     */
    protected int getRelativeMouseX(int mouseX) {
        return mouseX - (guiLeft + 3); // 主体区域起始 X：含边框偏移
    }

    /**
     * 将鼠标的屏幕绝对 Y 坐标转换为 GUI 主体区域内的相对 Y 坐标。
     *
     * @param mouseY 鼠标的屏幕绝对 Y 坐标
     * @return 相对于 GUI 主体区域左上角的 Y 坐标
     */
    protected int getRelativeMouseY(int mouseY) {
        return mouseY - (guiTop + 16 + 3); // 主体区域起始 Y：标题栏 + 上边框偏移
    }

    // endregion

    // region GUI生命周期

    /**
     * 初始化 GUI
     */
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
    }

    /**
     * GUI 关闭事件
     */
    @Override
    public void onGuiClosed() {
        // 删除 FBO
        this.deleteFBO();
        // 关闭 GUI
        super.onGuiClosed();
    }

    // endregion

    // region GUI主绘制逻辑

    /**
     * 绘制模糊背景和半透明遮罩。
     */
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

    /**
     * 绘制 GUI 主体区域（含背景、边框、组件）。
     *
     * @param partialTicks 渲染补间
     * @param mouseX       鼠标 X
     * @param mouseY       鼠标 Y
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        float scale = getOpenAnimationScale();
        float offsetX = guiLeft + guiWidth / 2.0f;
        float offsetY = guiTop + guiHeight / 2.0f;

        GL11.glPushMatrix();
        // 把原点移到中心
        GL11.glTranslatef(offsetX, offsetY, 0);
        // 以中心缩放
        GL11.glScalef(scale, scale, 1.0f);
        // 再把原点移回左上角
        GL11.glTranslatef(-guiWidth / 2.0f, -guiHeight / 2.0f, 0);

        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制标题栏
        drawTitleBar(0, 0, guiWidth);
        // 绘制主体
        drawBody(0, 16, guiWidth, guiHeight - 16);

        // 绘制组件
        GL11.glPushMatrix();
        transformToContentArea();
        drawComponents(partialTicks, mouseX, mouseY);
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    /**
     * 绘制 GUI 前景（标题文字与前景图形）。
     *
     * @param mouseX 鼠标 X
     * @param mouseY 鼠标 Y
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        float scale = getOpenAnimationScale();
        float offsetX = guiWidth / 2.0f;
        float offsetY = guiHeight / 2.0f;

        GL11.glPushMatrix();

        // 缩放同步 GUI
        GL11.glTranslatef(offsetX, offsetY, 0);
        GL11.glScalef(scale, scale, 1.0f);
        GL11.glTranslatef(-guiWidth / 2.0f, -guiHeight / 2.0f, 0);

        fontRendererObj.drawString(title, 6, 5, 0x404040);

        // 绘制前景
        GL11.glPushMatrix();
        transformToContentArea();
        drawForeground(mouseX, mouseY);
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    // endregion

    // region 动画与模糊计算

    /**
     * 动态计算模糊强度（基于打开时间和最大模糊）。
     *
     * @return 当前帧模糊强度
     */
    private float getDynamicBlurScale() {
        float elapsed = (System.currentTimeMillis() - openTime) / 600.0f;
        float t = Math.min(elapsed, 1.0f); // 正规化时间
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3);
        return eased * maxBlurScale;
    }

    /**
     * 动态计算缩放动画的当前缩放比例。
     *
     * @return 当前缩放系数（0.8 ~ 1.0）
     */
    private float getOpenAnimationScale() {
        float elapsed = (System.currentTimeMillis() - openTime) / 500.0f;
        float t = Math.min(elapsed, 1.0f); // 归一化时间 0~1
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3); // 缓入效果
        return 0.8f + 0.2f * eased; // 初始缩放0.8，过渡到1.0
    }

    // endregion

    // region FBO 相关

    /**
     * 应用模糊着色器，并绘制到目标 FBO。
     *
     * @param targetFBO  目标帧缓冲对象
     * @param textureId  输入纹理 ID
     * @param shaderType 着色器类型（水平 / 垂直）
     * @param blurScale  模糊强度
     */
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

    /**
     * 根据屏幕尺寸更新或创建 FBO。
     */
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

    /**
     * 删除所有 FBO 缓冲资源。
     */
    private void deleteFBO() {
        if (fboDownscale != null) fboDownscale.delete();
        if (fboHorizontal != null) fboHorizontal.delete();
        if (fboVertical != null) fboVertical.delete();
        fboDownscale = fboHorizontal = fboVertical = null;
    }

    // endregion

    // region 绘制 GUI 背景结构

    /**
     * 根据标题宽度计算整个标题栏宽度。
     *
     * @return 标题栏总宽度（含左右边距）
     */
    private int calculateTitleBarWidth() {
        int titleWidth = fontRendererObj.getStringWidth(title);
        return 16 + Math.max(titleWidth - 12, 4) + 16;
    }

    /**
     * 绘制顶部标题栏。
     *
     * @param x     左上角 X
     * @param y     左上角 Y
     * @param width 总宽度
     */
    private void drawTitleBar(int x, int y, int width) {
        int middleWidth = Math.max(fontRendererObj.getStringWidth(title) - 9, 4);

        // 绘制左侧
        drawTexturedModalRect(x, y, 0, 0, 16, 16);

        // 中间拉伸段
        drawScaledSegment(x + 16, y, 16, 0, 16, 16, middleWidth, 16);

        // 右侧基础部分
        drawTexturedModalRect(x + 16 + middleWidth, y, 32, 0, 16, 16);

        // 右侧延伸部分
        int remaining = width - titleBarWidth - 2;
        if (remaining > 0) {
            drawScaledSegment(x + titleBarWidth, y, 48, 0, 16, 16, remaining, 16);
        }

    }

    /**
     * 绘制 GUI 主体背景区域（含左右装饰与内容平铺区域）。
     *
     * @param x      左上角 X
     * @param y      左上角 Y
     * @param width  总宽度
     * @param height 总高度
     */
    private void drawBody(int x, int y, int width, int height) {
        final int tileSize = 16;

        // 九宫格贴图起始坐标
        final int texX = 16;
        final int texY = 16;

        final int centerWidth = width - tileSize * 2;
        final int centerHeight = height - tileSize * 2;

        // 贴图坐标
        final int leftU = 0;
        final int centerU = texX;
        final int rightU = texX * 2;

        final int topV = texY;
        final int centerV = texY + tileSize;
        final int bottomV = texY + tileSize * 2;

        // 四角（不拉伸）
        drawTexturedModalRect(x, y, leftU, topV, tileSize, tileSize); // 左上
        drawTexturedModalRect(x, y + tileSize + centerHeight, leftU, bottomV, tileSize, tileSize); // 左下
        drawTexturedModalRect(x + tileSize + centerWidth, y, rightU, topV, tileSize, tileSize); // 右上
        drawTexturedModalRect(
            x + tileSize + centerWidth,
            y + tileSize + centerHeight,
            rightU,
            bottomV,
            tileSize,
            tileSize); // 右下

        // 四边（横/竖向拉伸）
        drawScaledSegment(x + tileSize, y, centerU, topV, tileSize, tileSize, centerWidth, tileSize); // 上边
        drawScaledSegment(x, y + tileSize, leftU, centerV, tileSize, tileSize, tileSize, centerHeight); // 左边
        drawScaledSegment(
            x + tileSize,
            y + tileSize + centerHeight,
            centerU,
            bottomV,
            tileSize,
            tileSize,
            centerWidth,
            tileSize); // 下边
        drawScaledSegment(
            x + tileSize + centerWidth,
            y + tileSize,
            rightU,
            centerV,
            tileSize,
            tileSize,
            tileSize,
            centerHeight); // 右边

        // 中心（平铺拉抻）
        drawScaledSegment(x + tileSize, y + tileSize, centerU, centerV, tileSize, tileSize, centerWidth, centerHeight);
    }

    // endregion

    // region 渲染贴图辅助方法

    /**
     * 绘制可缩放的贴图段，用于 GUI 拉伸区域。
     *
     * @param x             屏幕绘制位置的 X 坐标
     * @param y             屏幕绘制位置的 Y 坐标
     * @param textureX      贴图中起始 X 坐标（像素）
     * @param textureY      贴图中起始 Y 坐标（像素）
     * @param width         要绘制的目标宽度
     * @param height        要绘制的目标高度
     * @param textureWidth  原始贴图区域的宽度
     * @param textureHeight 原始贴图区域的高度
     */
    protected void drawScaledSegment(int x, int y, int textureX, int textureY, int textureWidth, int textureHeight,
        int width, int height) {
        float uScale = 1.0F / 256.0F; // 贴图宽度比例
        float vScale = 1.0F / 256.0F; // 贴图高度比例
        float u = textureX * uScale;
        float v = textureY * vScale;
        float u2 = (textureX + textureWidth) * uScale;
        float v2 = (textureY + textureHeight) * vScale;

        drawScaledTexture(x, y, width, height, u, v, u2, v2);
    }

    /**
     * 实际执行贴图坐标映射并绘制一个矩形。
     *
     * @param x      左上角屏幕坐标 X
     * @param y      左上角屏幕坐标 Y
     * @param width  要绘制的目标宽度
     * @param height 要绘制的目标高度
     * @param u      贴图左上角 U 坐标（0~1）
     * @param v      贴图左上角 V 坐标（0~1）
     * @param u2     贴图右下角 U 坐标（0~1）
     * @param v2     贴图右下角 V 坐标（0~1）
     */
    protected void drawScaledTexture(int x, int y, int width, int height, float u, float v, float u2, float v2) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, u, v2);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, u2, v2);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, u2, v);
        tessellator.addVertexWithUV(x, y, this.zLevel, u, v);
        tessellator.draw();
    }

    /**
     * 将给定的纹理绘制为一个全屏矩形，自动设置投影和纹理绑定。
     *
     * @param width     当前目标区域的宽度（通常为 FBO 尺寸）
     * @param height    当前目标区域的高度
     * @param textureId 要绑定的纹理 ID（OpenGL 纹理对象）
     */
    protected void drawFullscreenQuadWithTexture(int width, int height, int textureId) {
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

    /**
     * 绘制一个满屏矩形（UV 坐标固定为 (0,0)-(1,1)），使用当前绑定纹理。
     *
     * @param width  绘制宽度
     * @param height 绘制高度
     */
    protected void drawFullscreenQuad(int width, int height) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorOpaque_I(0xFFFFFF);
        tess.addVertexWithUV(0, height, 0, 0.0, 0.0);
        tess.addVertexWithUV(width, height, 0, 1.0, 0.0);
        tess.addVertexWithUV(width, 0, 0, 1.0, 1.0);
        tess.addVertexWithUV(0, 0, 0, 0.0, 1.0);
        tess.draw();
    }

    /**
     * 将 OpenGL 坐标原点移动到 GUI 的内容区域左上角。 内容区域是去除标题栏和边框之后的内部矩形区域。 通常配合绘制组件内容使用。
     */
    protected void transformToContentArea() {
        GL11.glTranslatef(3, 16 + 3, 0);
    }

    // endregion

}
