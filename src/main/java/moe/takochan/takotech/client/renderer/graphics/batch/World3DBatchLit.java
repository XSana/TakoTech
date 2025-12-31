package moe.takochan.takotech.client.renderer.graphics.batch;

import java.nio.FloatBuffer;

import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.lighting.MCLightingHelper;
import moe.takochan.takotech.client.renderer.graphics.mesh.DynamicMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexAttribute;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;

/**
 * 3D 世界批量渲染器（带 MC 光照支持）。
 * 扩展 World3DBatch，支持采样 MC 的 lightmap 纹理实现真实光照效果。
 *
 * <p>
 * 特性：
 * </p>
 * <ul>
 * <li>自动从世界坐标查询光照值</li>
 * <li>支持日夜循环</li>
 * <li>支持方块光源（火把、熔岩等）</li>
 * <li>可选的光照开关</li>
 * </ul>
 *
 * <p>
 * 使用示例（在 RenderWorldLastEvent 中）:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     World3DBatchLit batch = RenderSystem.getWorld3DBatchLit();
 *     batch.setWorld(Minecraft.getMinecraft().theWorld);
 *     batch.setLightingEnabled(true);
 *
 *     double rx = blockX - playerEyeX;
 *     double ry = blockY - playerEyeY;
 *     double rz = blockZ - playerEyeZ;
 *
 *     batch.begin(GL11.GL_TRIANGLES);
 *     batch.drawSolidBox(rx, ry, rz, 1, 1, 1, 1.0f, 0.5f, 0.2f, 1.0f);
 *     batch.end();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class World3DBatchLit implements AutoCloseable {

    /** 每个顶点的浮点数：3(pos) + 4(color) + 2(lightCoord) + 3(normal) = 12 */
    private static final int FLOATS_PER_VERTEX = 12;
    /** 默认最大顶点数 */
    private static final int DEFAULT_MAX_VERTICES = 8192;

    /**
     * 顶点格式：位置(3) + 颜色(4) + 光照坐标(2) + 法线(3)
     * MC 1.7.10 右手坐标系: +X=东, +Y=上, +Z=南(朝向玩家)
     */
    private static final VertexAttribute[] ATTRIBUTES = { VertexAttribute.position3D(0), // offset 0, 12 bytes
        VertexAttribute.colorFloat(12), // offset 12, 16 bytes
        VertexAttribute.lightCoord(28), // offset 28, 8 bytes
        VertexAttribute.normal(36) // offset 36, 12 bytes
    };
    private static final int STRIDE = FLOATS_PER_VERTEX * 4; // 48 bytes

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

    /** MC 世界引用（用于光照查询） */
    private World world = null;

    /** 是否启用 MC 光照 */
    private boolean lightingEnabled = true;

    /** 玩家眼睛位置（用于世界坐标转换） */
    private double playerEyeX = 0;
    private double playerEyeY = 0;
    private double playerEyeZ = 0;

    /** 默认光照坐标（当禁用光照时使用） */
    private float defaultLightU = 1.0f;
    private float defaultLightV = 1.0f;

    /** 光照强度乘数 (default 1.0) */
    private float lightIntensity = 1.0f;

    /** 最小亮度下限 (default 0.1) */
    private float minBrightness = 0.1f;

    /** 是否启用基于法线的方向光着色 */
    private boolean normalShadingEnabled = true;

    public World3DBatchLit() {
        this(DEFAULT_MAX_VERTICES);
    }

    public World3DBatchLit(int maxVertices) {
        this.maxVertices = maxVertices;
        this.vertexData = new float[maxVertices * FLOATS_PER_VERTEX];
        this.indexData = new int[maxVertices];
        this.mesh = new DynamicMesh(maxVertices, maxVertices, STRIDE, ATTRIBUTES);
    }

    // ==================== 配置方法 ====================

    /**
     * 设置世界引用（用于光照查询）
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * 设置玩家眼睛位置（用于将相对坐标转换为世界坐标）
     */
    public void setPlayerEyePosition(double x, double y, double z) {
        this.playerEyeX = x;
        this.playerEyeY = y;
        this.playerEyeZ = z;
    }

    /**
     * 启用/禁用 MC 光照
     */
    public void setLightingEnabled(boolean enabled) {
        this.lightingEnabled = enabled;
    }

    /**
     * 获取光照是否启用
     */
    public boolean isLightingEnabled() {
        return lightingEnabled;
    }

    /**
     * 设置全局透明度
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 设置默认光照坐标（当禁用光照或无法查询时使用）
     *
     * @param blockLight 方块光照级别 (0-15)
     * @param skyLight   天空光照级别 (0-15)
     */
    public void setDefaultLight(int blockLight, int skyLight) {
        this.defaultLightU = Math.max(0, Math.min(15, blockLight)) / 15.0f;
        this.defaultLightV = Math.max(0, Math.min(15, skyLight)) / 15.0f;
    }

    /**
     * 设置光照强度乘数
     *
     * @param intensity 强度乘数 (default 1.0, 范围建议 0.5-2.0)
     */
    public void setLightIntensity(float intensity) {
        this.lightIntensity = Math.max(0, intensity);
    }

    /**
     * 设置最小亮度下限（防止完全黑暗）
     *
     * @param minBrightness 最小亮度 (default 0.1, 范围 0-1)
     */
    public void setMinBrightness(float minBrightness) {
        this.minBrightness = Math.max(0, Math.min(1, minBrightness));
    }

    /**
     * 启用/禁用基于法线的方向光着色
     * 启用时，面的亮度会根据法线方向和模拟太阳光方向计算
     *
     * @param enabled 是否启用 (default true)
     */
    public void setNormalShadingEnabled(boolean enabled) {
        this.normalShadingEnabled = enabled;
    }

    /**
     * 获取法线着色是否启用
     */
    public boolean isNormalShadingEnabled() {
        return normalShadingEnabled;
    }

    // ==================== 批量渲染控制 ====================

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
            throw new IllegalStateException("World3DBatchLit.end() must be called before begin()");
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

        // 保存 GL 状态
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LINE_BIT
                | GL11.GL_TEXTURE_BIT);

        // 设置渲染状态
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        // Face culling done in shader using normal direction
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true); // Enable depth write for proper occlusion
        GL11.glEnable(GL11.GL_DEPTH_TEST);

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
     * 添加一个顶点（带光照坐标和法线）
     *
     * @param nx 法线 X 分量 (MC右手坐标系: +X=东)
     * @param ny 法线 Y 分量 (MC右手坐标系: +Y=上)
     * @param nz 法线 Z 分量 (MC右手坐标系: +Z=南/朝向玩家)
     */
    private int addVertex(double x, double y, double z, float r, float g, float b, float a, float lightU, float lightV,
        float nx, float ny, float nz) {
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
        vertexData[vertexOffset++] = lightU;
        vertexData[vertexOffset++] = lightV;
        vertexData[vertexOffset++] = nx;
        vertexData[vertexOffset++] = ny;
        vertexData[vertexOffset++] = nz;

        return vertexCount++;
    }

    /**
     * 获取指定相对坐标位置的光照坐标
     */
    private float[] getLightAt(double rx, double ry, double rz) {
        if (!lightingEnabled || world == null) {
            return new float[] { defaultLightU, defaultLightV };
        }

        // 转换为世界坐标
        int wx = (int) Math.floor(rx + playerEyeX);
        int wy = (int) Math.floor(ry + playerEyeY);
        int wz = (int) Math.floor(rz + playerEyeZ);

        return MCLightingHelper.getLightmapCoords(world, wx, wy, wz);
    }

    /**
     * 添加索引
     */
    private void addIndex(int index) {
        indexData[indexOffset++] = index;
    }

    // ==================== 线段绘制 ====================

    /**
     * 绘制一条线段（自动计算光照）
     * 线段使用默认法线 (0, 1, 0)
     */
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b,
        float a) {
        if (!drawing) return;

        float[] light1 = getLightAt(x1, y1, z1);
        float[] light2 = getLightAt(x2, y2, z2);

        // Lines use default up normal
        int v0 = addVertex(x1, y1, z1, r, g, b, a, light1[0], light1[1], 0, 1, 0);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, light2[0], light2[1], 0, 1, 0);
        addIndex(v0);
        addIndex(v1);
    }

    /**
     * 绘制一条线段（手动指定光照）
     * 线段使用默认法线 (0, 1, 0)
     */
    public void drawLineWithLight(double x1, double y1, double z1, double x2, double y2, double z2, float r, float g,
        float b, float a, float lightU, float lightV) {
        if (!drawing) return;

        // Lines use default up normal
        int v0 = addVertex(x1, y1, z1, r, g, b, a, lightU, lightV, 0, 1, 0);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, lightU, lightV, 0, 1, 0);
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
     * 绘制实心四边形（需要在 GL_TRIANGLES 模式下，带法线）
     * 法线遵循 MC 右手坐标系: +X=东, +Y=上, +Z=南
     */
    public void drawQuad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3,
        double z3, double x4, double y4, double z4, float r, float g, float b, float a, float nx, float ny, float nz) {
        if (!drawing) return;

        // 计算四边形中心的光照
        double cx = (x1 + x2 + x3 + x4) / 4;
        double cy = (y1 + y2 + y3 + y4) / 4;
        double cz = (z1 + z2 + z3 + z4) / 4;
        float[] light = getLightAt(cx, cy, cz);

        int v0 = addVertex(x1, y1, z1, r, g, b, a, light[0], light[1], nx, ny, nz);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, light[0], light[1], nx, ny, nz);
        int v2 = addVertex(x3, y3, z3, r, g, b, a, light[0], light[1], nx, ny, nz);
        int v3 = addVertex(x4, y4, z4, r, g, b, a, light[0], light[1], nx, ny, nz);

        // 两个三角形
        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心四边形（手动指定光照和法线）
     */
    public void drawQuadWithLight(double x1, double y1, double z1, double x2, double y2, double z2, double x3,
        double y3, double z3, double x4, double y4, double z4, float r, float g, float b, float a, float lightU,
        float lightV, float nx, float ny, float nz) {
        if (!drawing) return;

        int v0 = addVertex(x1, y1, z1, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v1 = addVertex(x2, y2, z2, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v2 = addVertex(x3, y3, z3, r, g, b, a, lightU, lightV, nx, ny, nz);
        int v3 = addVertex(x4, y4, z4, r, g, b, a, lightU, lightV, nx, ny, nz);

        addIndex(v0);
        addIndex(v1);
        addIndex(v2);
        addIndex(v0);
        addIndex(v2);
        addIndex(v3);
    }

    /**
     * 绘制实心方块（每个面使用中心光照和正确法线）
     * 顶点顺序为 CCW（逆时针），从外部观察时为正面。
     * 法线遵循 MC 右手坐标系: +X=东, +Y=上, +Z=南(朝向玩家)
     */
    public void drawSolidBox(double x, double y, double z, double w, double h, double d, float r, float g, float b,
        float a) {
        double x2 = x + w, y2 = y + h, z2 = z + d;

        // 底面 (Y-) - normal (0, -1, 0) pointing down
        drawQuad(x, y, z2, x2, y, z2, x2, y, z, x, y, z, r * 0.5f, g * 0.5f, b * 0.5f, a, 0, -1, 0);
        // 顶面 (Y+) - normal (0, 1, 0) pointing up
        drawQuad(x, y2, z, x2, y2, z, x2, y2, z2, x, y2, z2, r, g, b, a, 0, 1, 0);
        // 前面 (Z+) - normal (0, 0, 1) pointing south (toward player)
        drawQuad(x, y2, z2, x2, y2, z2, x2, y, z2, x, y, z2, r * 0.8f, g * 0.8f, b * 0.8f, a, 0, 0, 1);
        // 后面 (Z-) - normal (0, 0, -1) pointing north
        drawQuad(x2, y2, z, x, y2, z, x, y, z, x2, y, z, r * 0.8f, g * 0.8f, b * 0.8f, a, 0, 0, -1);
        // 右面 (X+) - normal (1, 0, 0) pointing east
        drawQuad(x2, y2, z2, x2, y2, z, x2, y, z, x2, y, z2, r * 0.6f, g * 0.6f, b * 0.6f, a, 1, 0, 0);
        // 左面 (X-) - normal (-1, 0, 0) pointing west
        drawQuad(x, y2, z, x, y2, z2, x, y, z2, x, y, z, r * 0.6f, g * 0.6f, b * 0.6f, a, -1, 0, 0);
    }

    /**
     * 绘制方块顶面发光效果
     */
    public void drawBlockGlow(double x, double y, double z, float r, float g, float b, float a) {
        double offset = 0.01;
        // Top face glow - normal pointing up (0, 1, 0)
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
            a,
            0,
            1,
            0);
    }

    /**
     * 绘制方块边缘发光
     */
    public void drawBlockEdgeGlow(double x, double y, double z, double thickness, float r, float g, float b, float a) {
        double t = thickness;
        double y2 = y + 1.01;

        // All edge quads are on top face - normal pointing up (0, 1, 0)
        drawQuad(x, y2, z, x + 1, y2, z, x + 1, y2, z + t, x, y2, z + t, r, g, b, a, 0, 1, 0);
        drawQuad(x, y2, z + 1 - t, x + 1, y2, z + 1 - t, x + 1, y2, z + 1, x, y2, z + 1, r, g, b, a, 0, 1, 0);
        drawQuad(x, y2, z + t, x + t, y2, z + t, x + t, y2, z + 1 - t, x, y2, z + 1 - t, r, g, b, a, 0, 1, 0);
        drawQuad(
            x + 1 - t,
            y2,
            z + t,
            x + 1,
            y2,
            z + t,
            x + 1,
            y2,
            z + 1 - t,
            x + 1 - t,
            y2,
            z + 1 - t,
            r,
            g,
            b,
            a,
            0,
            1,
            0);
    }

    /**
     * 绘制上升粒子柱（垂直面板）
     */
    public void drawRisingBeam(double cx, double y, double cz, double width, double height, float r, float g, float b,
        float a) {
        double hw = width / 2;
        // XY plane facing +Z (south)
        drawQuad(cx - hw, y, cz, cx + hw, y, cz, cx + hw, y + height, cz, cx - hw, y + height, cz, r, g, b, a, 0, 0, 1);
        // YZ plane facing +X (east)
        drawQuad(cx, y, cz - hw, cx, y, cz + hw, cx, y + height, cz + hw, cx, y + height, cz - hw, r, g, b, a, 1, 0, 0);
    }

    /**
     * 绘制旋转的方块标记
     */
    public void drawRotatingMarker(double cx, double cy, double cz, double size, double rotation, float r, float g,
        float b, float a) {
        double cos = Math.cos(rotation) * size;
        double sin = Math.sin(rotation) * size;

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

    // ==================== 刷新和结束 ====================

    /**
     * 刷新当前批次
     */
    public void flush() {
        if (vertexCount == 0) return;

        mesh.updateData(vertexData, vertexOffset, indexData, indexOffset);

        ShaderProgram shader = ShaderType.WORLD_3D_LIT.get();
        if (shader == null || !shader.isValid()) return;

        shader.use();

        // 传递 MC 矩阵
        modelViewBuffer.rewind();
        shader.setUniformMatrix4("uModelView", false, modelViewBuffer);
        projectionBuffer.rewind();
        shader.setUniformMatrix4("uProjection", false, projectionBuffer);
        shader.setUniformFloat("uAlpha", alpha);

        // 设置光照参数
        shader.setUniformBool("uUseLighting", lightingEnabled);
        shader.setUniformFloat("uLightIntensity", lightIntensity);
        shader.setUniformFloat("uMinBrightness", minBrightness);
        shader.setUniformBool("uUseNormalShading", normalShadingEnabled);

        if (lightingEnabled) {
            // 绑定 MC lightmap 纹理
            MCLightingHelper.bindLightmap(MCLightingHelper.LIGHTMAP_TEXTURE_SLOT);
            shader.setUniformInt("uLightmap", MCLightingHelper.LIGHTMAP_TEXTURE_SLOT);
        }

        mesh.draw(currentDrawMode);

        if (lightingEnabled) {
            MCLightingHelper.unbindLightmap(MCLightingHelper.LIGHTMAP_TEXTURE_SLOT);
        }

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

        // 恢复 GL 状态
        GL11.glPopAttrib();

        // 恢复默认纹理单元
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

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
