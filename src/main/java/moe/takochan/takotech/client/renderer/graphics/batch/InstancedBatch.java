package moe.takochan.takotech.client.renderer.graphics.batch;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Matrix4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * GPU Instanced Batch 渲染器。
 * 使用 GL33 的 glDrawElementsInstanced 进行高效批量渲染。
 *
 * <p>
 * 支持的实例数据:
 * </p>
 * <ul>
 * <li>mat4 modelMatrix (location 3-6, 64 bytes)</li>
 * <li>vec4 color (location 7, 16 bytes)</li>
 * <li>vec4 custom (location 8, 16 bytes, 可选)</li>
 * </ul>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * {@code
 * InstancedBatch batch = new InstancedBatch(mesh, 1000);
 *
 * // 每帧
 * batch.begin();
 * for (每个实例) {
 *     batch.addInstance(modelMatrix, color);
 * }
 * batch.end();
 *
 * // 渲染
 * shader.use();
 * batch.render();
 * }
 * </pre>
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Vertex_Rendering#Instancing">OpenGL Instancing</a>
 */
@SideOnly(Side.CLIENT)
public class InstancedBatch implements AutoCloseable {

    /** 是否支持 Instancing */
    private static Boolean supported = null;

    // 实例数据布局 (每实例 96 bytes)
    // mat4 modelMatrix: 64 bytes (location 3-6)
    // vec4 color: 16 bytes (location 7)
    // vec4 custom: 16 bytes (location 8)
    private static final int INSTANCE_STRIDE = 96;
    private static final int MATRIX_OFFSET = 0;
    private static final int COLOR_OFFSET = 64;
    private static final int CUSTOM_OFFSET = 80;

    // Attribute locations
    private static final int ATTR_MODEL_MAT_START = 3;
    private static final int ATTR_COLOR = 7;
    private static final int ATTR_CUSTOM = 8;

    /** 基础网格 */
    private final Mesh baseMesh;

    /** 最大实例数 */
    private final int maxInstances;

    /** 实例数据 VBO */
    private int instanceVbo = 0;

    /** CPU 端实例数据缓冲区 */
    private final ByteBuffer instanceBuffer;

    /** 临时 FloatBuffer 用于矩阵传输 */
    private final FloatBuffer matrixBuffer;

    /** 当前实例数 */
    private int instanceCount = 0;

    /** 是否处于 begin/end 之间 */
    private boolean drawing = false;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 是否已删除 */
    private boolean deleted = false;

    /**
     * 检查是否支持 GPU Instancing
     *
     * @return true 如果支持 GL33 Instancing
     */
    public static boolean isSupported() {
        if (supported == null) {
            try {
                supported = GLContext.getCapabilities().OpenGL33 || GLContext.getCapabilities().GL_ARB_instanced_arrays;
            } catch (Exception e) {
                supported = false;
            }
        }
        return supported;
    }

    /**
     * 创建实例化批处理器
     *
     * @param baseMesh     基础网格（所有实例共享）
     * @param maxInstances 最大实例数量
     */
    public InstancedBatch(Mesh baseMesh, int maxInstances) {
        this.baseMesh = baseMesh;
        this.maxInstances = maxInstances;
        this.instanceBuffer = BufferUtils.createByteBuffer(maxInstances * INSTANCE_STRIDE);
        this.matrixBuffer = BufferUtils.createFloatBuffer(16);
    }

    /**
     * 初始化 GPU 资源（需要在 GL 上下文中调用）
     */
    public void init() {
        if (initialized || !isSupported()) {
            return;
        }

        instanceVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceBuffer.capacity(), GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        initialized = true;
    }

    // ==================== 批处理操作 ====================

    /**
     * 开始添加实例
     */
    public void begin() {
        if (drawing) {
            TakoTechMod.LOG.warn("InstancedBatch.begin() called while already drawing");
            return;
        }

        if (!initialized) {
            init();
        }

        instanceBuffer.clear();
        instanceCount = 0;
        drawing = true;
    }

    /**
     * 添加实例（带模型矩阵和颜色）
     *
     * @param modelMatrix 模型变换矩阵
     * @param r           红色分量 (0-1)
     * @param g           绿色分量 (0-1)
     * @param b           蓝色分量 (0-1)
     * @param a           透明度 (0-1)
     * @return true 如果添加成功
     */
    public boolean addInstance(Matrix4f modelMatrix, float r, float g, float b, float a) {
        return addInstance(modelMatrix, r, g, b, a, 0, 0, 0, 0);
    }

    /**
     * 添加实例（完整版本）
     *
     * @param modelMatrix 模型变换矩阵
     * @param r           红色分量
     * @param g           绿色分量
     * @param b           蓝色分量
     * @param a           透明度
     * @param customX     自定义数据 X
     * @param customY     自定义数据 Y
     * @param customZ     自定义数据 Z
     * @param customW     自定义数据 W
     * @return true 如果添加成功
     */
    public boolean addInstance(Matrix4f modelMatrix, float r, float g, float b, float a, float customX, float customY,
        float customZ, float customW) {

        if (!drawing) {
            TakoTechMod.LOG.warn("InstancedBatch.addInstance() called outside begin/end");
            return false;
        }

        if (instanceCount >= maxInstances) {
            return false;
        }

        int baseOffset = instanceCount * INSTANCE_STRIDE;

        // 写入 mat4 modelMatrix (column-major)
        matrixBuffer.clear();
        modelMatrix.store(matrixBuffer);
        matrixBuffer.flip();

        instanceBuffer.position(baseOffset + MATRIX_OFFSET);
        for (int i = 0; i < 16; i++) {
            instanceBuffer.putFloat(matrixBuffer.get(i));
        }

        // 写入 vec4 color
        instanceBuffer.position(baseOffset + COLOR_OFFSET);
        instanceBuffer.putFloat(r);
        instanceBuffer.putFloat(g);
        instanceBuffer.putFloat(b);
        instanceBuffer.putFloat(a);

        // 写入 vec4 custom
        instanceBuffer.position(baseOffset + CUSTOM_OFFSET);
        instanceBuffer.putFloat(customX);
        instanceBuffer.putFloat(customY);
        instanceBuffer.putFloat(customZ);
        instanceBuffer.putFloat(customW);

        instanceCount++;
        return true;
    }

    /**
     * 添加实例（使用 FloatBuffer 矩阵）
     */
    public boolean addInstance(FloatBuffer modelMatrix, float r, float g, float b, float a) {
        if (!drawing || instanceCount >= maxInstances) {
            return false;
        }

        int baseOffset = instanceCount * INSTANCE_STRIDE;

        // 写入 mat4 modelMatrix
        instanceBuffer.position(baseOffset + MATRIX_OFFSET);
        int pos = modelMatrix.position();
        for (int i = 0; i < 16; i++) {
            instanceBuffer.putFloat(modelMatrix.get(pos + i));
        }

        // 写入 vec4 color
        instanceBuffer.position(baseOffset + COLOR_OFFSET);
        instanceBuffer.putFloat(r);
        instanceBuffer.putFloat(g);
        instanceBuffer.putFloat(b);
        instanceBuffer.putFloat(a);

        // 写入 vec4 custom (默认 0)
        instanceBuffer.position(baseOffset + CUSTOM_OFFSET);
        instanceBuffer.putFloat(0);
        instanceBuffer.putFloat(0);
        instanceBuffer.putFloat(0);
        instanceBuffer.putFloat(0);

        instanceCount++;
        return true;
    }

    /**
     * 结束添加实例并上传数据到 GPU
     */
    public void end() {
        if (!drawing) {
            return;
        }

        drawing = false;

        if (instanceCount == 0) {
            return;
        }

        // 上传实例数据
        instanceBuffer.position(0);
        instanceBuffer.limit(instanceCount * INSTANCE_STRIDE);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        instanceBuffer.limit(instanceBuffer.capacity());
    }

    // ==================== 渲染 ====================

    /**
     * 渲染所有实例
     *
     * @param shader 使用的着色器
     */
    public void render(ShaderProgram shader) {
        if (instanceCount == 0 || !initialized || baseMesh == null || !baseMesh.isValid()) {
            return;
        }

        // 绑定基础网格
        baseMesh.bind();

        // 绑定实例数据 VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);

        // 设置实例属性指针
        setupInstanceAttributes();

        // 执行实例化绘制
        if (baseMesh.hasIndices()) {
            GL31.glDrawElementsInstanced(
                baseMesh.getDrawMode(),
                baseMesh.getElementCount(),
                GL11.GL_UNSIGNED_INT,
                0,
                instanceCount);
        } else {
            GL31.glDrawArraysInstanced(baseMesh.getDrawMode(), 0, baseMesh.getElementCount(), instanceCount);
        }

        // 清理实例属性
        cleanupInstanceAttributes();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        baseMesh.unbind();
    }

    /**
     * 简化渲染（假设 shader 已绑定）
     */
    public void renderSimple() {
        render(null);
    }

    /**
     * 设置实例属性指针
     */
    private void setupInstanceAttributes() {
        // mat4 modelMatrix (4 x vec4, locations 3-6)
        for (int i = 0; i < 4; i++) {
            int loc = ATTR_MODEL_MAT_START + i;
            GL20.glEnableVertexAttribArray(loc);
            GL20.glVertexAttribPointer(loc, 4, GL11.GL_FLOAT, false, INSTANCE_STRIDE, MATRIX_OFFSET + i * 16);
            GL33.glVertexAttribDivisor(loc, 1); // 每实例更新
        }

        // vec4 color (location 7)
        GL20.glEnableVertexAttribArray(ATTR_COLOR);
        GL20.glVertexAttribPointer(ATTR_COLOR, 4, GL11.GL_FLOAT, false, INSTANCE_STRIDE, COLOR_OFFSET);
        GL33.glVertexAttribDivisor(ATTR_COLOR, 1);

        // vec4 custom (location 8)
        GL20.glEnableVertexAttribArray(ATTR_CUSTOM);
        GL20.glVertexAttribPointer(ATTR_CUSTOM, 4, GL11.GL_FLOAT, false, INSTANCE_STRIDE, CUSTOM_OFFSET);
        GL33.glVertexAttribDivisor(ATTR_CUSTOM, 1);
    }

    /**
     * 清理实例属性（重置 divisor）
     */
    private void cleanupInstanceAttributes() {
        for (int i = 0; i < 4; i++) {
            int loc = ATTR_MODEL_MAT_START + i;
            GL33.glVertexAttribDivisor(loc, 0);
            GL20.glDisableVertexAttribArray(loc);
        }

        GL33.glVertexAttribDivisor(ATTR_COLOR, 0);
        GL20.glDisableVertexAttribArray(ATTR_COLOR);

        GL33.glVertexAttribDivisor(ATTR_CUSTOM, 0);
        GL20.glDisableVertexAttribArray(ATTR_CUSTOM);
    }

    // ==================== 查询方法 ====================

    /**
     * 获取当前实例数
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * 获取最大实例数
     */
    public int getMaxInstances() {
        return maxInstances;
    }

    /**
     * 是否可以添加更多实例
     */
    public boolean hasCapacity() {
        return instanceCount < maxInstances;
    }

    /**
     * 获取剩余容量
     */
    public int getRemainingCapacity() {
        return maxInstances - instanceCount;
    }

    /**
     * 是否处于绘制状态
     */
    public boolean isDrawing() {
        return drawing;
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取基础网格
     */
    public Mesh getBaseMesh() {
        return baseMesh;
    }

    // ==================== 资源清理 ====================

    @Override
    public void close() {
        if (instanceVbo != 0 && !deleted) {
            GL15.glDeleteBuffers(instanceVbo);
            instanceVbo = 0;
            deleted = true;
        }
    }

    /**
     * 遗留方法
     */
    public void delete() {
        close();
    }
}
