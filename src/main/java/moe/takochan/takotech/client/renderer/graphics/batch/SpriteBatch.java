package moe.takochan.takotech.client.renderer.graphics.batch;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.mesh.DynamicMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexFormat;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.client.renderer.graphics.state.RenderState;

/**
 * 批量渲染器，用于高效绘制 2D 图元。
 * 支持颜色四边形批量渲染，自动管理 GL 状态。
 *
 * <p>
 * 使用示例:
 * </p>
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     SpriteBatch batch = new SpriteBatch();
 *     batch.setProjectionOrtho(width, height);
 *     batch.begin();
 *     batch.drawQuad(x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
 *     batch.end();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class SpriteBatch implements AutoCloseable {

    /** 每个顶点的浮点数：2(pos) + 4(color) = 6 */
    private static final int FLOATS_PER_VERTEX = 6;
    /** 每个四边形的顶点数 */
    private static final int VERTICES_PER_QUAD = 4;
    /** 每个四边形的索引数 */
    private static final int INDICES_PER_QUAD = 6;
    /** 默认最大四边形数量 */
    private static final int DEFAULT_MAX_QUADS = 256;

    private final int maxQuads;
    private final DynamicMesh mesh;

    /** 预分配的顶点数据数组 */
    private final float[] vertexData;
    /** 预分配的索引数据数组 */
    private final int[] indexData;

    /** 当前顶点数据写入位置 */
    private int vertexOffset = 0;
    /** 当前索引数据写入位置 */
    private int indexOffset = 0;
    /** 当前四边形数量 */
    private int quadCount = 0;

    /** 缓存的投影矩阵 */
    private final FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
    /** 缓存的屏幕尺寸 */
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    /** 是否正在绘制 */
    private boolean drawing = false;
    /** 保存的状态快照 */
    private RenderState.StateSnapshot savedState;

    private boolean disposed = false;

    /**
     * 使用默认容量创建 SpriteBatch
     */
    public SpriteBatch() {
        this(DEFAULT_MAX_QUADS);
    }

    /**
     * 创建指定容量的 SpriteBatch
     *
     * @param maxQuads 最大四边形数量
     */
    public SpriteBatch(int maxQuads) {
        this.maxQuads = maxQuads;
        this.vertexData = new float[maxQuads * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];
        this.indexData = new int[maxQuads * INDICES_PER_QUAD];

        this.mesh = new DynamicMesh(
            maxQuads * VERTICES_PER_QUAD,
            maxQuads * INDICES_PER_QUAD,
            VertexFormat.POSITION_COLOR);
    }

    /**
     * 设置正交投影矩阵
     *
     * @param width  屏幕宽度
     * @param height 屏幕高度
     */
    public void setProjectionOrtho(int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            updateProjectionMatrix(width, height);
            cachedWidth = width;
            cachedHeight = height;
        }
    }

    /**
     * 设置自定义投影矩阵
     *
     * @param matrix 投影矩阵 (16 floats, column-major)
     */
    public void setProjectionMatrix(FloatBuffer matrix) {
        projMatrix.clear();
        projMatrix.put(matrix);
        projMatrix.flip();
        cachedWidth = -1; // 强制下次 setProjectionOrtho 重新计算
        cachedHeight = -1;
    }

    /**
     * 开始批量渲染
     * 保存当前 GL 状态并设置渲染状态
     */
    public void begin() {
        if (drawing) {
            throw new IllegalStateException("SpriteBatch.end() must be called before begin()");
        }

        // 检查 shader 支持
        if (!ShaderProgram.isSupported()) {
            return;
        }

        // 保存当前 GL 状态
        savedState = RenderState.save();

        // 设置渲染状态
        RenderState.setBlendAlpha();
        RenderState.disableDepthTest();
        RenderState.disableTexture2D();
        RenderState.disableCullFace();

        // 重置计数器
        vertexOffset = 0;
        indexOffset = 0;
        quadCount = 0;

        drawing = true;
    }

    /**
     * 添加一个颜色四边形（任意形状）
     *
     * @param x1 顶点1 X
     * @param y1 顶点1 Y
     * @param x2 顶点2 X
     * @param y2 顶点2 Y
     * @param x3 顶点3 X
     * @param y3 顶点3 Y
     * @param x4 顶点4 X
     * @param y4 顶点4 Y
     * @param r  红色分量 (0-1)
     * @param g  绿色分量 (0-1)
     * @param b  蓝色分量 (0-1)
     * @param a  透明度 (0-1)
     */
    public void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r,
        float g, float b, float a) {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before drawing");
        }
        if (quadCount >= maxQuads) {
            flush(); // 自动刷新
        }

        int baseVertex = quadCount * VERTICES_PER_QUAD;

        // 顶点 0
        vertexData[vertexOffset++] = x1;
        vertexData[vertexOffset++] = y1;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 1
        vertexData[vertexOffset++] = x2;
        vertexData[vertexOffset++] = y2;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 2
        vertexData[vertexOffset++] = x3;
        vertexData[vertexOffset++] = y3;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 顶点 3
        vertexData[vertexOffset++] = x4;
        vertexData[vertexOffset++] = y4;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        // 索引 (两个三角形)
        indexData[indexOffset++] = baseVertex;
        indexData[indexOffset++] = baseVertex + 1;
        indexData[indexOffset++] = baseVertex + 2;
        indexData[indexOffset++] = baseVertex;
        indexData[indexOffset++] = baseVertex + 2;
        indexData[indexOffset++] = baseVertex + 3;

        quadCount++;
    }

    /**
     * 添加一个矩形（轴对齐）
     *
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param w 宽度
     * @param h 高度
     * @param r 红色分量 (0-1)
     * @param g 绿色分量 (0-1)
     * @param b 蓝色分量 (0-1)
     * @param a 透明度 (0-1)
     */
    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        drawQuad(x, y, x + w, y, x + w, y + h, x, y + h, r, g, b, a);
    }

    /**
     * 添加一个矩形（使用 RGBA 整数颜色）
     *
     * @param x     左上角 X
     * @param y     左上角 Y
     * @param w     宽度
     * @param h     高度
     * @param color RGBA 颜色 (0xRRGGBBAA)
     */
    public void drawRect(float x, float y, float w, float h, int color) {
        float r = ((color >> 24) & 0xFF) / 255f;
        float g = ((color >> 16) & 0xFF) / 255f;
        float b = ((color >> 8) & 0xFF) / 255f;
        float a = (color & 0xFF) / 255f;
        drawRect(x, y, w, h, r, g, b, a);
    }

    /**
     * 刷新当前批次（提交并绘制）
     */
    public void flush() {
        if (quadCount == 0) return;

        // 更新 mesh 数据
        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        // 使用 shader
        ShaderProgram shader = ShaderType.GUI_COLOR.get();
        if (shader == null || !shader.isValid()) {
            return;
        }
        shader.use();

        // 设置投影矩阵
        projMatrix.rewind();
        shader.setUniformMatrix4("uProjection", false, projMatrix);

        // 绘制
        mesh.draw();

        // 解绑 shader
        ShaderProgram.unbind();

        // 重置计数器
        vertexOffset = 0;
        indexOffset = 0;
        quadCount = 0;
    }

    /**
     * 结束批量渲染
     * 提交绘制并恢复 GL 状态
     */
    public void end() {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before end()");
        }

        // 刷新剩余数据
        flush();

        // 恢复 GL 状态
        if (savedState != null) {
            RenderState.restore(savedState);
            savedState = null;
        }

        drawing = false;
    }

    /**
     * 检查是否正在绘制
     */
    public boolean isDrawing() {
        return drawing;
    }

    /**
     * 获取最大四边形数量
     */
    public int getMaxQuads() {
        return maxQuads;
    }

    /**
     * 获取当前批次中的四边形数量
     */
    public int getQuadCount() {
        return quadCount;
    }

    /**
     * 释放资源（遗留方法）
     */
    public void dispose() {
        close();
    }

    /**
     * 实现 AutoCloseable
     */
    @Override
    public void close() {
        if (!disposed) {
            // 如果正在绘制，只标记为完成，不尝试 flush（可能没有 GL 上下文）
            drawing = false;
            mesh.close();
            disposed = true;
        }
    }

    /**
     * 更新投影矩阵缓存
     */
    private void updateProjectionMatrix(int screenWidth, int screenHeight) {
        float left = 0;
        float right = screenWidth;
        float bottom = screenHeight;
        float top = 0;
        float near = -1;
        float far = 1;

        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = -(far + near) / (far - near);

        projMatrix.clear();
        projMatrix.put(2.0f / (right - left));
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(2.0f / (top - bottom));
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(0);
        projMatrix.put(-2.0f / (far - near));
        projMatrix.put(0);
        projMatrix.put(tx);
        projMatrix.put(ty);
        projMatrix.put(tz);
        projMatrix.put(1);
        projMatrix.flip();
    }
}
