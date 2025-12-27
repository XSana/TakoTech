package moe.takochan.takotech.client.renderer.graphics.batch;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.mesh.DynamicMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexAttribute;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;

/**
 * 3D 世界批量渲染器，用于在 MC 世界中渲染 3D 图元。
 * 直接使用 MC 的 ModelView/Projection 矩阵，坐标为相对于玩家眼睛的世界坐标。
 *
 * <p>
 * 使用示例（在 RenderWorldLastEvent 中）:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 计算相对坐标
 *     double rx = blockX - playerEyeX;
 *     double ry = blockY - playerEyeY;
 *     double rz = blockZ - playerEyeZ;
 *
 *     World3DBatch batch = new World3DBatch();
 *     batch.begin(GL11.GL_LINES);
 *     batch.drawWireBox(rx, ry, rz, 1, 1, 1, r, g, b, a);
 *     batch.end();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class World3DBatch implements AutoCloseable {

    /** 每个顶点的浮点数：3(pos) + 4(color) = 7 */
    private static final int FLOATS_PER_VERTEX = 7;
    /** 默认最大顶点数 */
    private static final int DEFAULT_MAX_VERTICES = 8192;

    /** 顶点格式：位置(3) + 颜色(4) */
    private static final VertexAttribute[] ATTRIBUTES = { VertexAttribute.position3D(0),
        VertexAttribute.colorFloat(12) };
    private static final int STRIDE = FLOATS_PER_VERTEX * 4; // 28 bytes

    private final int maxVertices;
    private final DynamicMesh mesh;

    /** 预分配的顶点数据数组 */
    private final float[] vertexData;
    /** 预分配的索引数据数组 */
    private final int[] indexData;

    /** 当前顶点数据写入位置 */
    private int vertexOffset = 0;
    /** 当前索引数据写入位置 */
    private int indexOffset = 0;
    /** 当前顶点数量 */
    private int vertexCount = 0;

    /** MC 矩阵缓冲 */
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);

    /** 全局透明度 */
    private float alpha = 1.0f;

    /** 当前绘制模式 */
    private int currentDrawMode = GL11.GL_LINES;

    /** 是否正在绘制 */
    private boolean drawing = false;
    private boolean disposed = false;

    public World3DBatch() {
        this(DEFAULT_MAX_VERTICES);
    }

    public World3DBatch(int maxVertices) {
        this.maxVertices = maxVertices;
        this.vertexData = new float[maxVertices * FLOATS_PER_VERTEX];
        this.indexData = new int[maxVertices];
        this.mesh = new DynamicMesh(maxVertices, maxVertices, STRIDE, ATTRIBUTES);
    }

    /**
     * 设置全局透明度
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 开始批量渲染（默认线段模式）
     */
    public void begin() {
        begin(GL11.GL_LINES);
    }

    /**
     * 开始批量渲染
     *
     * @param drawMode GL_LINES, GL_TRIANGLES, GL_QUADS 等
     */
    public void begin(int drawMode) {
        if (drawing) {
            throw new IllegalStateException("World3DBatch.end() must be called before begin()");
        }

        if (!ShaderProgram.isSupported()) {
            return;
        }

        this.currentDrawMode = drawMode;

        // 捕获 MC 当前的矩阵
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        projectionBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);

        // 设置渲染状态
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE); // 关闭面剔除，确保双面可见
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_DEPTH_TEST); // 启用深度测试

        if (drawMode == GL11.GL_LINES) {
            GL11.glLineWidth(2.0f);
        }

        // 重置计数器
        vertexOffset = 0;
        indexOffset = 0;
        vertexCount = 0;

        drawing = true;
    }

    /**
     * 添加一个顶点
     */
    private int addVertex(double x, double y, double z, float r, float g, float b, float a) {
        if (vertexCount >= maxVertices) {
            flush();
        }

        vertexData[vertexOffset++] = (float) x;
        vertexData[vertexOffset++] = (float) y;
        vertexData[vertexOffset++] = (float) z;
        vertexData[vertexOffset++] = r;
        vertexData[vertexOffset++] = g;
        vertexData[vertexOffset++] = b;
        vertexData[vertexOffset++] = a;

        return vertexCount++;
    }

    /**
     * 添加索引
     */
    private void addIndex(int index) {
        indexData[indexOffset++] = index;
    }

    // ==================== 线段绘制 ====================

    /**
     * 绘制一条线段
     */
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b,
        float a) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a);
        int v1 = addVertex(x2, y2, z2, r, g, b, a);
        addIndex(v0);
        addIndex(v1);
    }

    /**
     * 绘制线框方块
     */
    public void drawWireBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面
        drawLine(x, y, z, x2, y, z, r, g, b, a);
        drawLine(x2, y, z, x2, y, z2, r, g, b, a);
        drawLine(x2, y, z2, x, y, z2, r, g, b, a);
        drawLine(x, y, z2, x, y, z, r, g, b, a);
        // 顶面
        drawLine(x, y2, z, x2, y2, z, r, g, b, a);
        drawLine(x2, y2, z, x2, y2, z2, r, g, b, a);
        drawLine(x2, y2, z2, x, y2, z2, r, g, b, a);
        drawLine(x, y2, z2, x, y2, z, r, g, b, a);
        // 连接边
        drawLine(x, y, z, x, y2, z, r, g, b, a);
        drawLine(x2, y, z, x2, y2, z, r, g, b, a);
        drawLine(x2, y, z2, x2, y2, z2, r, g, b, a);
        drawLine(x, y, z2, x, y2, z2, r, g, b, a);
    }

    /**
     * 绘制十字标记
     */
    public void drawCross(double x, double y, double z, double size, float r, float g, float b, float a) {
        double half = size / 2;
        drawLine(x - half, y, z, x + half, y, z, r, g, b, a);
        drawLine(x, y - half, z, x, y + half, z, r, g, b, a);
        drawLine(x, y, z - half, x, y, z + half, r, g, b, a);
    }

    /**
     * 绘制圆（XZ 平面）
     */
    public void drawCircleXZ(double cx, double cy, double cz, double radius, int segments, float r, float g, float b,
        float a) {
        double angleStep = Math.PI * 2 / segments;
        double prevX = cx + radius, prevZ = cz;

        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currX = cx + Math.cos(angle) * radius;
            double currZ = cz + Math.sin(angle) * radius;
            drawLine(prevX, cy, prevZ, currX, cy, currZ, r, g, b, a);
            prevX = currX;
            prevZ = currZ;
        }
    }

    // ==================== 实心图形绘制 ====================

    /**
     * 绘制实心四边形（需要在 GL_TRIANGLES 模式下）
     */
    public void drawQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3,
        double z3, double x4, double y4, double z4, float r, float g, float b, float a) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a);
        int v1 = addVertex(x2, y2, z2, r, g, b, a);
        int v2 = addVertex(x3, y3, z3, r, g, b, a);
        int v3 = addVertex(x4, y4, z4, r, g, b, a);

        // 两个三角形
        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心方块
     */
    public void drawSolidBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面 (Y-)
        drawQuad(x, y, z, x2, y, z, x2, y, z2, x, y, z2, r * 0.5f, g * 0.5f, b * 0.5f, a);
        // 顶面 (Y+)
        drawQuad(x, y2, z2, x2, y2, z2, x2, y2, z, x, y2, z, r, g, b, a);
        // 前面 (Z+)
        drawQuad(x, y, z2, x2, y, z2, x2, y2, z2, x, y2, z2, r * 0.8f, g * 0.8f, b * 0.8f, a);
        // 后面 (Z-)
        drawQuad(x2, y, z, x, y, z, x, y2, z, x2, y2, z, r * 0.8f, g * 0.8f, b * 0.8f, a);
        // 右面 (X+)
        drawQuad(x2, y, z2, x2, y, z, x2, y2, z, x2, y2, z2, r * 0.6f, g * 0.6f, b * 0.6f, a);
        // 左面 (X-)
        drawQuad(x, y, z, x, y, z2, x, y2, z2, x, y2, z, r * 0.6f, g * 0.6f, b * 0.6f, a);
    }

    /**
     * 绘制方块顶面发光效果
     */
    public void drawBlockGlow(double x, double y, double z, float r, float g, float b, float a) {
        // 在方块顶面稍上方绘制发光平面
        double offset = 0.01;
        drawQuad(
            x,
            y + 1 + offset,
            z,
            x + 1,
            y + 1 + offset,
            z,
            x + 1,
            y + 1 + offset,
            z + 1,
            x,
            y + 1 + offset,
            z + 1,
            r,
            g,
            b,
            a);
    }

    /**
     * 绘制方块边缘发光
     */
    public void drawBlockEdgeGlow(double x, double y, double z, double thickness, float r, float g, float b, float a) {
        double t = thickness;
        double y2 = y + 1.01; // 稍高于方块顶面

        // 四条边
        drawQuad(x, y2, z, x + 1, y2, z, x + 1, y2, z + t, x, y2, z + t, r, g, b, a); // 前
        drawQuad(x, y2, z + 1 - t, x + 1, y2, z + 1 - t, x + 1, y2, z + 1, x, y2, z + 1, r, g, b, a); // 后
        drawQuad(x, y2, z + t, x + t, y2, z + t, x + t, y2, z + 1 - t, x, y2, z + 1 - t, r, g, b, a); // 左
        drawQuad(x + 1 - t, y2, z + t, x + 1, y2, z + t, x + 1, y2, z + 1 - t, x + 1 - t, y2, z + 1 - t, r, g, b, a); // 右
    }

    /**
     * 绘制上升粒子柱（垂直面板）
     */
    public void drawRisingBeam(double cx, double y, double cz, double width, double height, float r, float g, float b,
        float a) {
        double hw = width / 2;
        // X 方向面板
        drawQuad(cx - hw, y, cz, cx + hw, y, cz, cx + hw, y + height, cz, cx - hw, y + height, cz, r, g, b, a);
        // Z 方向面板
        drawQuad(cx, y, cz - hw, cx, y, cz + hw, cx, y + height, cz + hw, cx, y + height, cz - hw, r, g, b, a);
    }

    /**
     * 绘制旋转的方块标记
     */
    public void drawRotatingMarker(double cx, double cy, double cz, double size, double rotation, float r, float g,
        float b, float a) {
        double cos = Math.cos(rotation) * size;
        double sin = Math.sin(rotation) * size;

        // 旋转的 X 形
        drawLine(cx - cos, cy, cz - sin, cx + cos, cy, cz + sin, r, g, b, a);
        drawLine(cx - sin, cy, cz + cos, cx + sin, cy, cz - cos, r, g, b, a);
    }

    /**
     * 绘制螺旋上升效果
     */
    public void drawSpiral(double cx, double y, double cz, double radius, double height, int turns, int segments,
        float r, float g, float b, float a) {
        double totalAngle = Math.PI * 2 * turns;
        double angleStep = totalAngle / segments;
        double heightStep = height / segments;

        double prevX = cx + radius, prevY = y, prevZ = cz;

        for (int i = 1; i <= segments; i++) {
            double angle = angleStep * i;
            double currX = cx + Math.cos(angle) * radius;
            double currY = y + heightStep * i;
            double currZ = cz + Math.sin(angle) * radius;

            drawLine(prevX, prevY, prevZ, currX, currY, currZ, r, g, b, a);

            prevX = currX;
            prevY = currY;
            prevZ = currZ;
        }
    }

    /**
     * 绘制菱形标记
     */
    public void drawDiamond(double cx, double cy, double cz, double size, float r, float g, float b, float a) {
        double s = size;
        // 顶部四面
        drawQuad(cx, cy + s, cz, cx + s, cy, cz, cx, cy, cz + s, cx, cy + s, cz, r, g, b, a * 0.9f);
        drawQuad(
            cx,
            cy + s,
            cz,
            cx,
            cy,
            cz + s,
            cx - s,
            cy,
            cz,
            cx,
            cy + s,
            cz,
            r * 0.8f,
            g * 0.8f,
            b * 0.8f,
            a * 0.9f);
        drawQuad(
            cx,
            cy + s,
            cz,
            cx - s,
            cy,
            cz,
            cx,
            cy,
            cz - s,
            cx,
            cy + s,
            cz,
            r * 0.6f,
            g * 0.6f,
            b * 0.6f,
            a * 0.9f);
        drawQuad(
            cx,
            cy + s,
            cz,
            cx,
            cy,
            cz - s,
            cx + s,
            cy,
            cz,
            cx,
            cy + s,
            cz,
            r * 0.7f,
            g * 0.7f,
            b * 0.7f,
            a * 0.9f);
        // 底部四面
        drawQuad(
            cx,
            cy - s,
            cz,
            cx,
            cy,
            cz + s,
            cx + s,
            cy,
            cz,
            cx,
            cy - s,
            cz,
            r * 0.5f,
            g * 0.5f,
            b * 0.5f,
            a * 0.9f);
        drawQuad(
            cx,
            cy - s,
            cz,
            cx - s,
            cy,
            cz,
            cx,
            cy,
            cz + s,
            cx,
            cy - s,
            cz,
            r * 0.4f,
            g * 0.4f,
            b * 0.4f,
            a * 0.9f);
        drawQuad(
            cx,
            cy - s,
            cz,
            cx,
            cy,
            cz - s,
            cx - s,
            cy,
            cz,
            cx,
            cy - s,
            cz,
            r * 0.3f,
            g * 0.3f,
            b * 0.3f,
            a * 0.9f);
        drawQuad(
            cx,
            cy - s,
            cz,
            cx + s,
            cy,
            cz,
            cx,
            cy,
            cz - s,
            cx,
            cy - s,
            cz,
            r * 0.35f,
            g * 0.35f,
            b * 0.35f,
            a * 0.9f);
    }

    // ==================== 刷新和结束 ====================

    /**
     * 刷新当前批次
     */
    public void flush() {
        if (vertexCount == 0) return;

        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        ShaderProgram shader = ShaderType.WORLD_3D.get();
        if (shader == null || !shader.isValid()) return;

        shader.use();

        // 传递 MC 矩阵
        modelViewBuffer.rewind();
        shader.setUniformMatrix4("uModelView", false, modelViewBuffer);
        projectionBuffer.rewind();
        shader.setUniformMatrix4("uProjection", false, projectionBuffer);
        shader.setUniformFloat("uAlpha", alpha);

        mesh.draw(currentDrawMode);

        ShaderProgram.unbind();

        vertexOffset = 0;
        indexOffset = 0;
        vertexCount = 0;
    }

    /**
     * 结束批量渲染
     */
    public void end() {
        if (!drawing) return;

        flush();
        GL20.glUseProgram(0);
        drawing = false;
    }

    public boolean isDrawing() {
        return drawing;
    }

    @Override
    public void close() {
        if (!disposed) {
            drawing = false;
            mesh.close();
            disposed = true;
        }
    }

    public void dispose() {
        close();
    }
}
