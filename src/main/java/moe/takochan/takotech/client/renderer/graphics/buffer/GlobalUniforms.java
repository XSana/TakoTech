package moe.takochan.takotech.client.renderer.graphics.buffer;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Matrix4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 全局 Uniforms (binding = 0)，所有 shader 共享。
 *
 * <p>
 * std140 布局:
 * </p>
 *
 * <pre>
 * layout(std140, binding = 0) uniform GlobalUniforms {
 *     mat4 uProjection;    // offset 0,   size 64
 *     mat4 uView;          // offset 64,  size 64
 *     vec4 uScreenSize;    // offset 128, size 16 [width, height, 1/width, 1/height]
 *     vec4 uTime;          // offset 144, size 16 [totalTime, deltaTime, frameCount, 0]
 * };                       // total: 160 bytes
 * </pre>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * {@code
 * // 每帧开始时更新
 * GlobalUniforms.INSTANCE.setScreenSize(width, height);
 * GlobalUniforms.INSTANCE.updateTime(deltaTime);
 * GlobalUniforms.INSTANCE.setFromCamera(camera);
 * GlobalUniforms.INSTANCE.bind();
 *
 * // 或者手动设置矩阵
 * GlobalUniforms.INSTANCE.setProjection(projMatrix);
 * GlobalUniforms.INSTANCE.setView(viewMatrix);
 * GlobalUniforms.INSTANCE.upload();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class GlobalUniforms {

    /** 单例实例 */
    public static final GlobalUniforms INSTANCE = new GlobalUniforms();

    /** Binding point */
    public static final int BINDING_POINT = 0;

    /** Uniform block 名称 */
    public static final String BLOCK_NAME = "GlobalUniforms";

    // std140 布局偏移量
    private static final int OFFSET_PROJECTION = 0;
    private static final int OFFSET_VIEW = 64;
    private static final int OFFSET_SCREEN_SIZE = 128;
    private static final int OFFSET_TIME = 144;

    /** UBO 大小 */
    private static final int BUFFER_SIZE = 160;

    /** 底层 UBO */
    private UniformBuffer ubo;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 累计时间（秒） */
    private float totalTime = 0;

    /** 帧计数 */
    private int frameCount = 0;

    /** 屏幕尺寸缓存 */
    private float screenWidth = 1;
    private float screenHeight = 1;

    private GlobalUniforms() {
        // 私有构造函数
    }

    /**
     * 初始化 UBO（在 OpenGL 上下文可用时调用）
     */
    public void init() {
        if (initialized) return;

        if (!UniformBuffer.isSupported()) {
            return;
        }

        ubo = new UniformBuffer(BUFFER_SIZE);
        initialized = true;

        // 设置默认值
        setScreenSize(1, 1);
        setProjection(new Matrix4f()); // 单位矩阵
        setView(new Matrix4f());
    }

    /**
     * 判断是否可用
     */
    public boolean isAvailable() {
        return initialized && ubo != null && ubo.isValid();
    }

    /**
     * 将 GlobalUniforms 绑定到 binding point 0
     */
    public void bind() {
        if (ubo != null) {
            ubo.bind(BINDING_POINT);
        }
    }

    /**
     * 解绑
     */
    public void unbind() {
        UniformBuffer.unbind(BINDING_POINT);
    }

    /**
     * 将 shader 的 GlobalUniforms block 绑定到正确的 binding point
     *
     * @param shader 要绑定的 shader program
     */
    public void bindToShader(ShaderProgram shader) {
        if (shader != null && shader.isValid()) {
            shader.bindUniformBlock(BLOCK_NAME, BINDING_POINT);
        }
    }

    // ==================== 设置方法 ====================

    /**
     * 设置投影矩阵
     */
    public void setProjection(Matrix4f projection) {
        if (ubo != null) {
            ubo.setMatrix4(OFFSET_PROJECTION, projection);
        }
    }

    /**
     * 设置投影矩阵（从 FloatBuffer）
     */
    public void setProjection(FloatBuffer projection) {
        if (ubo != null) {
            ubo.setMatrix4(OFFSET_PROJECTION, projection);
        }
    }

    /**
     * 设置视图矩阵
     */
    public void setView(Matrix4f view) {
        if (ubo != null) {
            ubo.setMatrix4(OFFSET_VIEW, view);
        }
    }

    /**
     * 设置视图矩阵（从 FloatBuffer）
     */
    public void setView(FloatBuffer view) {
        if (ubo != null) {
            ubo.setMatrix4(OFFSET_VIEW, view);
        }
    }

    /**
     * 从 Camera 设置投影和视图矩阵
     */
    public void setFromCamera(Camera camera) {
        if (camera == null || ubo == null) return;

        ubo.beginBatch();
        ubo.setMatrix4Batch(OFFSET_PROJECTION, camera.getProjectionMatrix());
        ubo.setMatrix4Batch(OFFSET_VIEW, camera.getViewMatrix());
        ubo.endBatch();
    }

    /**
     * 设置屏幕尺寸
     *
     * @param width  屏幕宽度
     * @param height 屏幕高度
     */
    public void setScreenSize(float width, float height) {
        this.screenWidth = width;
        this.screenHeight = height;

        if (ubo != null) {
            float invWidth = width > 0 ? 1.0f / width : 0;
            float invHeight = height > 0 ? 1.0f / height : 0;
            ubo.setVec4(OFFSET_SCREEN_SIZE, width, height, invWidth, invHeight);
        }
    }

    /**
     * 设置屏幕尺寸（整数版本）
     */
    public void setScreenSize(int width, int height) {
        setScreenSize((float) width, (float) height);
    }

    /**
     * 更新时间（每帧调用）
     *
     * @param deltaTime 帧时间（秒）
     */
    public void updateTime(float deltaTime) {
        totalTime += deltaTime;
        frameCount++;

        if (ubo != null) {
            ubo.setVec4(OFFSET_TIME, totalTime, deltaTime, (float) frameCount, 0);
        }
    }

    /**
     * 重置时间
     */
    public void resetTime() {
        totalTime = 0;
        frameCount = 0;
    }

    // ==================== 查询方法 ====================

    public float getTotalTime() {
        return totalTime;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public float getScreenWidth() {
        return screenWidth;
    }

    public float getScreenHeight() {
        return screenHeight;
    }

    // ==================== 清理 ====================

    /**
     * 释放资源
     */
    public void dispose() {
        if (ubo != null) {
            ubo.close();
            ubo = null;
        }
        initialized = false;
    }
}
