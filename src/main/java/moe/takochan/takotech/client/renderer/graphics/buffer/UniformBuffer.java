package moe.takochan.takotech.client.renderer.graphics.buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;

/**
 * Uniform Buffer Object (UBO) 封装类。
 * 使用 std140 布局规则，保证跨平台兼容性。
 *
 * <p>
 * std140 布局规则简要：
 * </p>
 * <ul>
 * <li>float: 4 bytes, 对齐 4</li>
 * <li>vec2: 8 bytes, 对齐 8</li>
 * <li>vec3: 12 bytes, 对齐 16</li>
 * <li>vec4: 16 bytes, 对齐 16</li>
 * <li>mat4: 64 bytes (4 x vec4), 对齐 16</li>
 * </ul>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     UniformBuffer ubo = new UniformBuffer(160); // GlobalUniforms 大小
 *     ubo.setMatrix4(0, projectionMatrix); // offset 0
 *     ubo.setMatrix4(64, viewMatrix); // offset 64
 *     ubo.setVec4(128, width, height, 1 / width, 1 / height); // offset 128
 *     ubo.bind(0); // 绑定到 binding point 0
 * }
 * </pre>
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Uniform_Buffer_Object">OpenGL UBO Wiki</a>
 */
@SideOnly(Side.CLIENT)
public class UniformBuffer implements AutoCloseable {

    /** 是否支持 UBO */
    private static Boolean supported = null;

    /** Buffer ID */
    private int id;

    /** 缓冲区大小（字节） */
    private final int size;

    /** CPU 端缓冲区 */
    private final ByteBuffer buffer;

    /** 临时 FloatBuffer 用于矩阵传输 */
    private final FloatBuffer matrixBuffer;

    /** 是否已删除 */
    private boolean deleted = false;

    /**
     * 检查当前系统是否支持 UBO
     *
     * @return true 如果支持 UBO (GL31)
     */
    public static boolean isSupported() {
        if (supported == null) {
            try {
                supported = GLContext.getCapabilities().GL_ARB_uniform_buffer_object
                    || GLContext.getCapabilities().OpenGL31;
            } catch (Exception e) {
                supported = false;
            }
        }
        return supported;
    }

    /**
     * 创建指定大小的 Uniform Buffer
     *
     * @param size 缓冲区大小（字节），必须符合 std140 对齐规则
     */
    public UniformBuffer(int size) {
        this.size = size;
        this.buffer = BufferUtils.createByteBuffer(size);
        this.matrixBuffer = BufferUtils.createFloatBuffer(16);

        if (!isSupported()) {
            TakoTechMod.LOG.warn("UBO not supported on this system");
            this.id = 0;
            return;
        }

        this.id = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    // ==================== 绑定方法 ====================

    /**
     * 将此 UBO 绑定到指定的 binding point
     *
     * @param bindingPoint 绑定点（0-N，与 shader 中的 layout(binding=N) 对应）
     */
    public void bind(int bindingPoint) {
        if (id != 0) {
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, id);
        }
    }

    /**
     * 将此 UBO 的一部分绑定到指定的 binding point
     *
     * @param bindingPoint 绑定点
     * @param offset       起始偏移（必须是 GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT 的倍数）
     * @param length       绑定长度
     */
    public void bindRange(int bindingPoint, int offset, int length) {
        if (id != 0) {
            GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, bindingPoint, id, offset, length);
        }
    }

    /**
     * 解绑指定 binding point 的 UBO
     *
     * @param bindingPoint 绑定点
     */
    public static void unbind(int bindingPoint) {
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, 0);
    }

    // ==================== 数据设置方法 (std140 布局) ====================

    /**
     * 设置 mat4 (64 bytes, 对齐 16)
     *
     * @param offset 字节偏移量（必须是 16 的倍数）
     * @param matrix 矩阵
     */
    public void setMatrix4(int offset, Matrix4f matrix) {
        matrixBuffer.clear();
        matrix.store(matrixBuffer);
        matrixBuffer.flip();

        buffer.position(offset);
        for (int i = 0; i < 16; i++) {
            buffer.putFloat(matrixBuffer.get(i));
        }

        uploadRange(offset, 64);
    }

    /**
     * 设置 mat4 从 FloatBuffer
     *
     * @param offset       字节偏移量
     * @param matrixBuffer 包含矩阵数据的 FloatBuffer (16 floats)
     */
    public void setMatrix4(int offset, FloatBuffer matrixBuffer) {
        buffer.position(offset);
        int pos = matrixBuffer.position();
        for (int i = 0; i < 16; i++) {
            buffer.putFloat(matrixBuffer.get(pos + i));
        }

        uploadRange(offset, 64);
    }

    /**
     * 设置 vec4 (16 bytes, 对齐 16)
     *
     * @param offset 字节偏移量（必须是 16 的倍数）
     */
    public void setVec4(int offset, float x, float y, float z, float w) {
        buffer.position(offset);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(w);

        uploadRange(offset, 16);
    }

    /**
     * 设置 vec4 从 Vector4f
     */
    public void setVec4(int offset, Vector4f vec) {
        setVec4(offset, vec.x, vec.y, vec.z, vec.w);
    }

    /**
     * 设置 vec3 (12 bytes 数据, 占用 16 bytes 因为对齐)
     *
     * @param offset 字节偏移量（必须是 16 的倍数）
     */
    public void setVec3(int offset, float x, float y, float z) {
        buffer.position(offset);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        // 不写入 padding，但上传时包含

        uploadRange(offset, 16); // std140 中 vec3 占用 16 bytes
    }

    /**
     * 设置 vec3 从 Vector3f
     */
    public void setVec3(int offset, Vector3f vec) {
        setVec3(offset, vec.x, vec.y, vec.z);
    }

    /**
     * 设置 vec2 (8 bytes, 对齐 8)
     *
     * @param offset 字节偏移量（必须是 8 的倍数）
     */
    public void setVec2(int offset, float x, float y) {
        buffer.position(offset);
        buffer.putFloat(x);
        buffer.putFloat(y);

        uploadRange(offset, 8);
    }

    /**
     * 设置 float (4 bytes, 对齐 4)
     *
     * @param offset 字节偏移量（必须是 4 的倍数）
     */
    public void setFloat(int offset, float value) {
        buffer.position(offset);
        buffer.putFloat(value);

        uploadRange(offset, 4);
    }

    /**
     * 设置 int (4 bytes, 对齐 4)
     *
     * @param offset 字节偏移量（必须是 4 的倍数）
     */
    public void setInt(int offset, int value) {
        buffer.position(offset);
        buffer.putInt(value);

        uploadRange(offset, 4);
    }

    /**
     * 设置 bool (作为 int, 4 bytes)
     *
     * @param offset 字节偏移量
     */
    public void setBool(int offset, boolean value) {
        setInt(offset, value ? 1 : 0);
    }

    // ==================== 批量上传 ====================

    /**
     * 上传整个缓冲区到 GPU
     */
    public void upload() {
        if (id == 0) return;

        buffer.position(0);
        buffer.limit(size);

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    /**
     * 上传指定范围到 GPU
     *
     * @param offset 起始偏移
     * @param length 长度
     */
    private void uploadRange(int offset, int length) {
        if (id == 0) return;

        buffer.position(offset);
        buffer.limit(offset + length);

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, id);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

        buffer.limit(size);
    }

    // ==================== 批量设置（无立即上传） ====================

    /**
     * 开始批量设置模式
     * 在此模式下，setXxx 方法不会立即上传，需要调用 endBatch() 完成上传
     */
    private boolean batchMode = false;
    private int batchMinOffset = Integer.MAX_VALUE;
    private int batchMaxOffset = 0;

    /**
     * 开始批量设置
     */
    public void beginBatch() {
        batchMode = true;
        batchMinOffset = Integer.MAX_VALUE;
        batchMaxOffset = 0;
    }

    /**
     * 结束批量设置并上传修改的范围
     */
    public void endBatch() {
        if (!batchMode) return;
        batchMode = false;

        if (batchMinOffset < batchMaxOffset) {
            uploadRange(batchMinOffset, batchMaxOffset - batchMinOffset);
        }
    }

    /**
     * 批量模式下记录修改范围
     */
    private void markDirty(int offset, int length) {
        if (batchMode) {
            batchMinOffset = Math.min(batchMinOffset, offset);
            batchMaxOffset = Math.max(batchMaxOffset, offset + length);
        }
    }

    /**
     * 批量设置 mat4（不立即上传）
     */
    public void setMatrix4Batch(int offset, Matrix4f matrix) {
        matrixBuffer.clear();
        matrix.store(matrixBuffer);
        matrixBuffer.flip();

        buffer.position(offset);
        for (int i = 0; i < 16; i++) {
            buffer.putFloat(matrixBuffer.get(i));
        }

        if (batchMode) {
            markDirty(offset, 64);
        } else {
            uploadRange(offset, 64);
        }
    }

    /**
     * 批量设置 vec4（不立即上传）
     */
    public void setVec4Batch(int offset, float x, float y, float z, float w) {
        buffer.position(offset);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(w);

        if (batchMode) {
            markDirty(offset, 16);
        } else {
            uploadRange(offset, 16);
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取缓冲区 ID
     */
    public int getId() {
        return id;
    }

    /**
     * 获取缓冲区大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return id != 0 && !deleted;
    }

    // ==================== 资源清理 ====================

    @Override
    public void close() {
        if (id != 0 && !deleted) {
            GL15.glDeleteBuffers(id);
            id = 0;
            deleted = true;
        }
    }

    /**
     * 遗留方法，建议使用 close()
     */
    public void delete() {
        close();
    }
}
