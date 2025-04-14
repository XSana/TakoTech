package moe.takochan.takotech.client.gui;

import java.awt.Rectangle;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.client.renderer.graphics.framebuffer.Framebuffer;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.common.Reference;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@SideOnly(Side.CLIENT)
public abstract class BaseGui<T extends BaseContainer> extends GuiContainer {

    // 静态资源与常量
    protected final static Minecraft mc = Minecraft.getMinecraft();
    protected final static ResourceLocation GUI_TEXTURE = new ResourceLocation(
        Reference.RESOURCE_ROOT_ID,
        "textures/guis/base_gui.png");
    protected final static float TEXTURE_ATLAS_SIZE = 256.0f;

    // FBO 渲染状态
    private static int VAO = -1;
    private Framebuffer[] fboBlur;
    private int fboWidth = -1;
    private int fboHeight = -1;

    // 动画
    private final static int GAUSSIAN_BLUR_ITERATIONS = 5 * 2;
    private final static float BLUR_ANIMATION_DURATION_MS = 600.0f;
    private final static float OPEN_ANIMATION_DURATION_MS = 500.0f;
    private final float maxBlurScale;
    private final long openTime;

    // GUI 相关字段
    private final static int TITLE_BAR_HEIGHT = 16;
    private final static int BODY_PADDING = 4;

    private final int guiWidth;
    private final int guiHeight;
    private final T container;
    private final String title;
    private int titleBarWidth;
    private int screenWidth = -1;
    private int screenHeight = -1;

    public BaseGui(T container, String title, int guiWidth, int guiHeight) {
        this(container, title, guiWidth, guiHeight, 1.0f);
    }

    public BaseGui(T container, String title, int guiWidth, int guiHeight, float maxBlurScale) {
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
     * @param mouseX       鼠标在 GUI 主体区域内的相对 X 坐标
     * @param mouseY       鼠标在 GUI 主体区域内的相对 Y 坐标
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
     * @param mouseX 鼠标在 GUI 主体区域内的相对 X 坐标
     * @param mouseY 鼠标在 GUI 主体区域内的相对 Y 坐标
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
        return new Rectangle(0, 0, guiWidth - BODY_PADDING * 2, guiHeight - TITLE_BAR_HEIGHT - BODY_PADDING * 2);
    }

    /**
     * 将鼠标的屏幕绝对 X 坐标转换为 GUI 主体区域内的相对 X 坐标。
     *
     * @param mouseX 鼠标的屏幕绝对 X 坐标
     * @return 相对于 GUI 主体区域左上角的 X 坐标
     */
    protected int getRelativeMouseX(int mouseX) {
        return mouseX - (guiLeft + BODY_PADDING);
    }

    /**
     * 将鼠标的屏幕绝对 Y 坐标转换为 GUI 主体区域内的相对 Y 坐标。
     *
     * @param mouseY 鼠标的屏幕绝对 Y 坐标
     * @return 相对于 GUI 主体区域左上角的 Y 坐标
     */
    protected int getRelativeMouseY(int mouseY) {
        return mouseY - (guiTop + TITLE_BAR_HEIGHT + BODY_PADDING);
    }

    /**
     * 将 GUI 主体区域内的相对 X 坐标转换为屏幕绝对 X 坐标。
     */
    protected int getAbsoluteMouseX(int relativeX) {
        return relativeX + (guiLeft + BODY_PADDING);
    }

    /**
     * 将 GUI 主体区域内的相对 Y 坐标转换为屏幕绝对 Y 坐标。
     */
    protected int getAbsoluteMouseY(int relativeY) {
        return relativeY + (guiTop + TITLE_BAR_HEIGHT + BODY_PADDING);
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
        this.ySize = guiHeight + TITLE_BAR_HEIGHT;
        super.initGui();
        // 记录屏幕尺寸
        screenWidth = mc.displayWidth;
        screenHeight = mc.displayHeight;
        // 计算标题栏宽度
        this.titleBarWidth = calculateTitleBarWidth();
        // 初始化或重建 FBO
        this.updateFBO();
        // 初始化 VAO
        this.initQuad();

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
        // 获取MC当前缓冲帧的纹理ID
        int mainFboTexture = mc.getFramebuffer().framebufferTexture;
        // 计算模糊度
        float blurScale = getDynamicBlurScale();
        Framebuffer blurFbo = applyGaussianBlur(mainFboTexture, blurScale);
        // 输出最终模糊结果到默认帧缓冲
        mc.getFramebuffer()
            .bindFramebuffer(true);
        GL11.glViewport(0, 0, screenWidth, screenHeight);
        drawFullscreenQuadWithTexture(blurFbo.getTextureId());
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
        drawBody(0, TITLE_BAR_HEIGHT, guiWidth, guiHeight);

        // 绘制组件
        GL11.glPushMatrix();
        transformToContentArea();
        drawComponents(partialTicks, getRelativeMouseX(mouseX), getRelativeMouseY(mouseY));
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
        drawForeground(getRelativeMouseX(mouseX), getRelativeMouseY(mouseY));
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
        float elapsed = (System.currentTimeMillis() - openTime) / BLUR_ANIMATION_DURATION_MS;
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
        float elapsed = (System.currentTimeMillis() - openTime) / OPEN_ANIMATION_DURATION_MS;
        float t = Math.min(elapsed, 1.0f); // 归一化时间 0~1
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3); // 缓入效果
        return 0.8f + 0.2f * eased; // 初始缩放0.8，过渡到1.0
    }

    // endregion

    // region OpenGL相关

    private Framebuffer applyGaussianBlur(int sceneTextureId, float blurScale) {
        boolean horizontal = true;
        boolean firstIteration = true;

        ShaderProgram shader = ShaderType.BLUR.get();
        shader.use();
        shader.setUniformInt("mainTexture", 0);
        shader.setUniformFloat("blurScale", blurScale);

        for (int i = 0; i < GAUSSIAN_BLUR_ITERATIONS; ++i) {
            Framebuffer targetFbo = fboBlur[horizontal ? 0 : 1];
            targetFbo.bind();

            shader.setUniformFloat("isHorizontal", !horizontal ? 0 : 1);

            GL11.glViewport(0, 0, targetFbo.getWidth(), targetFbo.getHeight());
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(
                GL11.GL_TEXTURE_2D,
                firstIteration ? sceneTextureId : fboBlur[!horizontal ? 0 : 1].getTextureId());

            GL30.glBindVertexArray(VAO);
            GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);

            targetFbo.unbind();

            horizontal = !horizontal;
            if (firstIteration) firstIteration = false;
        }

        ShaderProgram.clear();

        return fboBlur[!horizontal ? 0 : 1];
    }

    /**
     * 根据屏幕尺寸更新或创建 FBO。
     */
    private void updateFBO() {
        // 检查是否需要重建 FBO
        if (fboWidth != screenWidth || fboHeight != screenHeight) {
            // 删除旧的 FBO
            deleteFBO();
            // 更新 FBO 尺寸
            fboWidth = screenWidth;
            fboHeight = screenHeight;
            // 创建新的 FBO
            fboBlur = new Framebuffer[] { new Framebuffer(fboWidth, fboHeight), new Framebuffer(fboWidth, fboHeight) };
        }
    }

    /**
     * 删除所有 FBO 缓冲资源。
     */
    private void deleteFBO() {
        if (fboBlur != null) {
            for (Framebuffer fbo : fboBlur) {
                if (fbo != null) fbo.delete();
            }
        }
        fboBlur = null;
    }

    private void initQuad() {
        if (VAO == -1) {
            final float[] vertices = new float[] { -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 1.0f,
                0.0f, 1.0f, 1.0f, 1.0f, 1.0f };
            final int[] indices = { 0, 1, 2, 0, 2, 3 };
            final FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length)
                .put(vertices);
            verticesBuffer.flip();
            final IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.length)
                .put(indices);
            indicesBuffer.flip();

            VAO = GL30.glGenVertexArrays();
            final int vbo = GL15.glGenBuffers();
            final int ibo = GL15.glGenBuffers();

            GL30.glBindVertexArray(VAO);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
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
        // 计算UV
        float uScale = 1.0F / TEXTURE_ATLAS_SIZE; // 贴图宽度比例
        float vScale = 1.0F / TEXTURE_ATLAS_SIZE; // 贴图高度比例
        float u = textureX * uScale;
        float v = textureY * vScale;
        float u2 = (textureX + textureWidth) * uScale;
        float v2 = (textureY + textureHeight) * vScale;
        // 绘制
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, u, v2);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, u2, v2);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, u2, v);
        tessellator.addVertexWithUV(x, y, this.zLevel, u, v);
        tessellator.draw();
    }

    /**
     * 将给定的纹理绘制为一个全屏矩形
     *
     * @param textureId 要绑定的纹理 ID（OpenGL 纹理对象）
     */
    protected void drawFullscreenQuadWithTexture(int textureId) {
        // 绑定Shader
        ShaderProgram shader = ShaderType.SIMPLE.get();
        shader.use();
        shader.setUniformInt("mainTexture", 0);
        // 绑定纹理
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        // 绘制全屏矩形
        GL30.glBindVertexArray(VAO);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);

        ShaderProgram.clear();
    }

    /**
     * 将 OpenGL 坐标原点移动到 GUI 的内容区域左上角。 内容区域是去除标题栏和边框之后的内部矩形区域。 通常配合绘制组件内容使用。
     */
    protected void transformToContentArea() {
        GL11.glTranslatef(BODY_PADDING, 16 + BODY_PADDING, 0);
    }

    // endregion

}
