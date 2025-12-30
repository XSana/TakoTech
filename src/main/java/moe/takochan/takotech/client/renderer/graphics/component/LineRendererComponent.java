package moe.takochan.takotech.client.renderer.graphics.component;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.core.RenderContext;
import moe.takochan.takotech.client.renderer.graphics.ecs.Component;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;
import moe.takochan.takotech.client.renderer.graphics.mesh.DynamicMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexFormat;
import moe.takochan.takotech.client.renderer.graphics.particle.Gradient;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;

/**
 * 线条渲染器组件。
 * 渲染面向相机的线条（billboard 技术），支持渐变颜色和宽度。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Entity trail = world.createEntity("Trail");
 *     trail.addComponent(new TransformComponent());
 *
 *     LineRendererComponent line = trail.addComponent(new LineRendererComponent());
 *     line.setWidth(0.1f, 0.01f); // 起点到终点宽度渐变
 *     line.setColorGradient(Gradient.fire());
 *     line.addPoint(new Vector3f(0, 0, 0));
 *     line.addPoint(new Vector3f(5, 2, 3));
 *     line.addPoint(new Vector3f(10, 1, 6));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class LineRendererComponent extends Component {

    /** 默认最大顶点数 */
    private static final int DEFAULT_MAX_VERTICES = 1024;
    /** 默认最大索引数 */
    private static final int DEFAULT_MAX_INDICES = 2048;

    /** 每个顶点的浮点数 (pos3 + color4 = 7) */
    private static final int FLOATS_PER_VERTEX = 7;

    /** 线条点列表 */
    private final List<Vector3f> points = new ArrayList<>();

    /** 起点宽度 */
    private float startWidth = 0.1f;

    /** 终点宽度 */
    private float endWidth = 0.1f;

    /** 颜色渐变 */
    private Gradient colorGradient;

    /** 是否使用世界空间坐标 */
    private boolean useWorldSpace = true;

    /** 是否闭合 (首尾相连) */
    private boolean loop = false;

    /** 是否可见 */
    private boolean visible = true;

    /** 全局透明度 */
    private float alpha = 1.0f;

    /** 发光强度（用于 Bloom） */
    private float emissive = 1.0f;

    /** 动态 Mesh */
    private DynamicMesh lineMesh;

    /** 顶点数据缓冲 */
    private float[] vertexData;

    /** 索引数据缓冲 */
    private int[] indexData;

    /** 是否需要重建 Mesh */
    private boolean meshDirty = true;

    /** 最大顶点数 */
    private final int maxVertices;

    /** 最大索引数 */
    private final int maxIndices;

    // ==================== 构造函数 ====================

    /**
     * 创建线条渲染器组件
     */
    public LineRendererComponent() {
        this(DEFAULT_MAX_VERTICES, DEFAULT_MAX_INDICES);
    }

    /**
     * 创建线条渲染器组件
     *
     * @param maxVertices 最大顶点数
     * @param maxIndices  最大索引数
     */
    public LineRendererComponent(int maxVertices, int maxIndices) {
        this.maxVertices = maxVertices;
        this.maxIndices = maxIndices;
        this.colorGradient = Gradient.white();
    }

    // ==================== 生命周期 ====================

    @Override
    public void onAttach(moe.takochan.takotech.client.renderer.graphics.ecs.Entity entity) {
        super.onAttach(entity);
        initializeMesh();
    }

    @Override
    public void onDetach() {
        cleanup();
        super.onDetach();
    }

    /**
     * 初始化 Mesh
     */
    private void initializeMesh() {
        if (lineMesh != null) {
            return;
        }

        // 创建顶点格式: position3D + colorFloat
        VertexFormat format = VertexFormat.POSITION_3D_COLOR;

        lineMesh = new DynamicMesh(maxVertices, maxIndices, format);
        vertexData = new float[maxVertices * FLOATS_PER_VERTEX];
        indexData = new int[maxIndices];
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (lineMesh != null) {
            lineMesh.delete();
            lineMesh = null;
        }
        vertexData = null;
        indexData = null;
    }

    // ==================== 点管理 ====================

    /**
     * 添加点
     *
     * @param point 点坐标
     * @return this
     */
    public LineRendererComponent addPoint(Vector3f point) {
        points.add(new Vector3f(point));
        meshDirty = true;
        return this;
    }

    /**
     * 添加点
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this
     */
    public LineRendererComponent addPoint(float x, float y, float z) {
        points.add(new Vector3f(x, y, z));
        meshDirty = true;
        return this;
    }

    /**
     * 设置点列表
     *
     * @param newPoints 新的点列表
     * @return this
     */
    public LineRendererComponent setPoints(List<Vector3f> newPoints) {
        points.clear();
        for (Vector3f p : newPoints) {
            points.add(new Vector3f(p));
        }
        meshDirty = true;
        return this;
    }

    /**
     * 更新指定索引的点
     *
     * @param index 索引
     * @param point 新坐标
     * @return this
     */
    public LineRendererComponent setPoint(int index, Vector3f point) {
        if (index >= 0 && index < points.size()) {
            points.get(index)
                .set(point);
            meshDirty = true;
        }
        return this;
    }

    /**
     * 更新指定索引的点
     *
     * @param index 索引
     * @param x     X 坐标
     * @param y     Y 坐标
     * @param z     Z 坐标
     * @return this
     */
    public LineRendererComponent setPoint(int index, float x, float y, float z) {
        if (index >= 0 && index < points.size()) {
            points.get(index)
                .set(x, y, z);
            meshDirty = true;
        }
        return this;
    }

    /**
     * 清除所有点
     *
     * @return this
     */
    public LineRendererComponent clearPoints() {
        points.clear();
        meshDirty = true;
        return this;
    }

    /**
     * 获取点数量
     *
     * @return 点数量
     */
    public int getPointCount() {
        return points.size();
    }

    /**
     * 获取点列表（只读）
     *
     * @return 点列表
     */
    public List<Vector3f> getPoints() {
        return new ArrayList<>(points);
    }

    // ==================== 属性设置 ====================

    /**
     * 设置线条宽度
     *
     * @param startWidth 起点宽度
     * @param endWidth   终点宽度
     * @return this
     */
    public LineRendererComponent setWidth(float startWidth, float endWidth) {
        this.startWidth = startWidth;
        this.endWidth = endWidth;
        meshDirty = true;
        return this;
    }

    /**
     * 设置统一宽度
     *
     * @param width 宽度
     * @return this
     */
    public LineRendererComponent setWidth(float width) {
        return setWidth(width, width);
    }

    /**
     * 获取起点宽度
     *
     * @return 起点宽度
     */
    public float getStartWidth() {
        return startWidth;
    }

    /**
     * 获取终点宽度
     *
     * @return 终点宽度
     */
    public float getEndWidth() {
        return endWidth;
    }

    /**
     * 设置颜色渐变
     *
     * @param gradient 渐变
     * @return this
     */
    public LineRendererComponent setColorGradient(Gradient gradient) {
        this.colorGradient = gradient != null ? gradient : Gradient.white();
        meshDirty = true;
        return this;
    }

    /**
     * 获取颜色渐变
     *
     * @return 渐变
     */
    public Gradient getColorGradient() {
        return colorGradient;
    }

    /**
     * Set start color (creates a two-color gradient)
     *
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return this
     */
    public LineRendererComponent setStartColor(float r, float g, float b, float a) {
        float[] endColor = colorGradient != null ? colorGradient.evaluate(1.0f) : new float[] { 1, 1, 1, 1 };
        this.colorGradient = Gradient.twoColor(r, g, b, a, endColor[0], endColor[1], endColor[2], endColor[3]);
        meshDirty = true;
        return this;
    }

    /**
     * Set end color (creates a two-color gradient)
     *
     * @param r red (0-1)
     * @param g green (0-1)
     * @param b blue (0-1)
     * @param a alpha (0-1)
     * @return this
     */
    public LineRendererComponent setEndColor(float r, float g, float b, float a) {
        float[] startColor = colorGradient != null ? colorGradient.evaluate(0.0f) : new float[] { 1, 1, 1, 1 };
        this.colorGradient = Gradient.twoColor(startColor[0], startColor[1], startColor[2], startColor[3], r, g, b, a);
        meshDirty = true;
        return this;
    }

    /**
     * Set both start and end colors
     *
     * @param sr start red
     * @param sg start green
     * @param sb start blue
     * @param sa start alpha
     * @param er end red
     * @param eg end green
     * @param eb end blue
     * @param ea end alpha
     * @return this
     */
    public LineRendererComponent setColors(float sr, float sg, float sb, float sa, float er, float eg, float eb,
        float ea) {
        this.colorGradient = Gradient.twoColor(sr, sg, sb, sa, er, eg, eb, ea);
        meshDirty = true;
        return this;
    }

    /**
     * 设置是否使用世界空间
     *
     * @param useWorldSpace true 使用世界空间
     * @return this
     */
    public LineRendererComponent setUseWorldSpace(boolean useWorldSpace) {
        this.useWorldSpace = useWorldSpace;
        return this;
    }

    /**
     * 是否使用世界空间
     *
     * @return true 使用世界空间
     */
    public boolean isUseWorldSpace() {
        return useWorldSpace;
    }

    /**
     * 设置是否闭合
     *
     * @param loop true 闭合
     * @return this
     */
    public LineRendererComponent setLoop(boolean loop) {
        this.loop = loop;
        meshDirty = true;
        return this;
    }

    /**
     * 是否闭合
     *
     * @return true 闭合
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * 设置可见性
     *
     * @param visible true 可见
     * @return this
     */
    public LineRendererComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public LineRendererComponent setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public LineRendererComponent setEmissive(float emissive) {
        this.emissive = emissive;
        return this;
    }

    /**
     * 是否可见
     *
     * @return true 可见
     */
    public boolean isVisible() {
        return visible;
    }

    // ==================== 渲染 ====================

    /**
     * 重建 Mesh（生成面向相机的三角形条带）
     *
     * @param cameraX 相机 X 坐标
     * @param cameraY 相机 Y 坐标
     * @param cameraZ 相机 Z 坐标
     */
    public void rebuildMesh(float cameraX, float cameraY, float cameraZ) {
        if (points.size() < 2) {
            return;
        }

        initializeMesh();

        int numPoints = points.size();
        int numSegments = loop ? numPoints : numPoints - 1;

        // 检查缓冲区大小
        int requiredVertices = numPoints * 2;
        int requiredIndices = numSegments * 6;

        if (requiredVertices > maxVertices || requiredIndices > maxIndices) {
            return; // 超出限制
        }

        int vertexIndex = 0;
        int indexIndex = 0;

        // 获取变换组件
        TransformComponent transform = getComponent(TransformComponent.class);
        float offsetX = 0, offsetY = 0, offsetZ = 0;
        if (!useWorldSpace && transform != null) {
            offsetX = transform.getX();
            offsetY = transform.getY();
            offsetZ = transform.getZ();
        }

        // 生成顶点
        for (int i = 0; i < numPoints; i++) {
            Vector3f current = points.get(i);
            float t = numPoints > 1 ? (float) i / (numPoints - 1) : 0;

            // 计算宽度
            float width = startWidth + (endWidth - startWidth) * t;
            float halfWidth = width * 0.5f;

            // 计算颜色
            float[] color = colorGradient.evaluate(t);

            // 计算世界坐标
            float px = current.x + offsetX;
            float py = current.y + offsetY;
            float pz = current.z + offsetZ;

            // 计算线段方向
            Vector3f dir = new Vector3f();
            if (i == 0) {
                Vector3f next = points.get(i + 1);
                dir.set(next.x - current.x, next.y - current.y, next.z - current.z);
            } else if (i == numPoints - 1) {
                Vector3f prev = points.get(i - 1);
                dir.set(current.x - prev.x, current.y - prev.y, current.z - prev.z);
            } else {
                Vector3f prev = points.get(i - 1);
                Vector3f next = points.get(i + 1);
                dir.set(next.x - prev.x, next.y - prev.y, next.z - prev.z);
            }

            // 归一化方向
            float dirLen = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
            if (dirLen > 0.0001f) {
                dir.x /= dirLen;
                dir.y /= dirLen;
                dir.z /= dirLen;
            }

            // 计算从当前点到相机的向量
            float toCamera_x = cameraX - px;
            float toCamera_y = cameraY - py;
            float toCamera_z = cameraZ - pz;

            // 计算侧向量 (dir × toCamera)
            float side_x = dir.y * toCamera_z - dir.z * toCamera_y;
            float side_y = dir.z * toCamera_x - dir.x * toCamera_z;
            float side_z = dir.x * toCamera_y - dir.y * toCamera_x;

            // 归一化侧向量
            float sideLen = (float) Math.sqrt(side_x * side_x + side_y * side_y + side_z * side_z);
            if (sideLen > 0.0001f) {
                side_x /= sideLen;
                side_y /= sideLen;
                side_z /= sideLen;
            }

            // 左顶点
            int baseIdx = vertexIndex * FLOATS_PER_VERTEX;
            vertexData[baseIdx] = px - side_x * halfWidth;
            vertexData[baseIdx + 1] = py - side_y * halfWidth;
            vertexData[baseIdx + 2] = pz - side_z * halfWidth;
            vertexData[baseIdx + 3] = color[0];
            vertexData[baseIdx + 4] = color[1];
            vertexData[baseIdx + 5] = color[2];
            vertexData[baseIdx + 6] = color[3];
            vertexIndex++;

            // 右顶点
            baseIdx = vertexIndex * FLOATS_PER_VERTEX;
            vertexData[baseIdx] = px + side_x * halfWidth;
            vertexData[baseIdx + 1] = py + side_y * halfWidth;
            vertexData[baseIdx + 2] = pz + side_z * halfWidth;
            vertexData[baseIdx + 3] = color[0];
            vertexData[baseIdx + 4] = color[1];
            vertexData[baseIdx + 5] = color[2];
            vertexData[baseIdx + 6] = color[3];
            vertexIndex++;
        }

        // 生成索引 (三角形条带转三角形列表)
        for (int i = 0; i < numSegments; i++) {
            int idx = i * 2;
            int nextIdx = ((i + 1) % numPoints) * 2;

            // 如果不是闭合且是最后一段，使用下一个顶点对
            if (!loop && i == numSegments - 1) {
                nextIdx = (i + 1) * 2;
            }

            // 第一个三角形
            indexData[indexIndex++] = idx;
            indexData[indexIndex++] = idx + 1;
            indexData[indexIndex++] = nextIdx;

            // 第二个三角形
            indexData[indexIndex++] = nextIdx;
            indexData[indexIndex++] = idx + 1;
            indexData[indexIndex++] = nextIdx + 1;
        }

        // 更新 Mesh
        lineMesh.updateData(vertexData, vertexIndex * FLOATS_PER_VERTEX, indexData, indexIndex);

        meshDirty = false;
    }

    /**
     * 使用渲染上下文渲染
     *
     * @param ctx 渲染上下文
     */
    public void render(RenderContext ctx) {
        if (!visible || !enabled || points.size() < 2) {
            return;
        }

        // 获取相机位置
        float camX = ctx.getCameraX();
        float camY = ctx.getCameraY();
        float camZ = ctx.getCameraZ();

        // 重建 Mesh（每帧都需要，因为相机可能移动）
        rebuildMesh(camX, camY, camZ);

        if (lineMesh == null || !lineMesh.isValid()) {
            return;
        }

        // 绘制
        ShaderProgram shader = ShaderType.LINE.get();
        if (shader == null || !shader.isValid()) {
            return;
        }

        shader.use();
        shader.setUniformMatrix4("uModelView", false, MathUtils.toFloatBuffer(ctx.getViewMatrix()));
        shader.setUniformMatrix4("uProjection", false, MathUtils.toFloatBuffer(ctx.getProjMatrix()));
        shader.setUniformFloat("uAlpha", alpha);
        shader.setUniformFloat("uEmissive", emissive);
        lineMesh.draw();
        ShaderProgram.unbind();
    }

    /**
     * 使用着色器渲染
     *
     * @param shader  着色器
     * @param cameraX 相机 X
     * @param cameraY 相机 Y
     * @param cameraZ 相机 Z
     */
    public void render(ShaderProgram shader, float cameraX, float cameraY, float cameraZ) {
        if (!visible || !enabled || points.size() < 2) {
            return;
        }

        // 重建 Mesh
        rebuildMesh(cameraX, cameraY, cameraZ);

        if (lineMesh == null || !lineMesh.isValid()) {
            return;
        }

        // 绘制
        lineMesh.draw();
    }

    /**
     * 判断是否应该渲染
     *
     * @return true 应该渲染
     */
    public boolean shouldRender() {
        return visible && enabled && points.size() >= 2;
    }

    /**
     * 标记需要重建 Mesh
     */
    public void markDirty() {
        meshDirty = true;
    }

    @Override
    public String toString() {
        return String.format(
            "LineRendererComponent[points=%d, width=%.2f->%.2f, visible=%b]",
            points.size(),
            startWidth,
            endWidth,
            visible);
    }
}
