package moe.takochan.takotech.client.renderer.graphics.batch;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;

/**
 * 批量渲染器配置。
 * 提供基于 OpenGL 硬件能力的默认值，同时支持用户自定义。
 *
 * <p>
 * OpenGL 相关查询：
 * </p>
 * <ul>
 * <li>GL_MAX_ELEMENTS_VERTICES - 推荐的最大顶点数</li>
 * <li>GL_MAX_ELEMENTS_INDICES - 推荐的最大索引数</li>
 * </ul>
 *
 * <p>
 * 使用示例：
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 获取推荐配置
 *     BatchConfig config = BatchConfig.detect();
 *
 *     // 使用推荐值创建批量渲染器
 *     SpriteBatch batch = new SpriteBatch(config.getRecommendedSpriteQuads());
 *     World3DBatch worldBatch = new World3DBatch(config.getRecommendedWorld3DVertices());
 *
 *     // 或使用自定义值
 *     config.setSpriteMaxQuads(1024);
 *     config.setWorld3DMaxVertices(32768);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class BatchConfig {

    // ==================== GL 硬件限制 ====================

    /** GL 推荐的最大顶点数 (glDrawElements) */
    private int glMaxVertices;

    /** GL 推荐的最大索引数 (glDrawElements) */
    private int glMaxIndices;

    /** 是否支持 GL 1.2+ (GL_MAX_ELEMENTS_* 查询) */
    private boolean supportsGL12;

    // ==================== 用户配置 ====================

    /** SpriteBatch 最大四边形数（用户覆盖值，-1 表示使用推荐值） */
    private int spriteMaxQuads = -1;

    /** World3DBatch 最大顶点数（用户覆盖值，-1 表示使用推荐值） */
    private int world3DMaxVertices = -1;

    /** InstancedBatch 最大实例数（用户覆盖值，-1 表示使用推荐值） */
    private int instancedMaxInstances = -1;

    // ==================== 默认值常量 ====================

    /** SpriteBatch 默认四边形数 */
    public static final int DEFAULT_SPRITE_QUADS = 256;

    /** SpriteBatch 最小四边形数 */
    public static final int MIN_SPRITE_QUADS = 64;

    /** SpriteBatch 最大四边形数 */
    public static final int MAX_SPRITE_QUADS = 65536;

    /** World3DBatch 默认顶点数 */
    public static final int DEFAULT_WORLD3D_VERTICES = 8192;

    /** World3DBatch 最小顶点数 */
    public static final int MIN_WORLD3D_VERTICES = 1024;

    /** World3DBatch 最大顶点数 */
    public static final int MAX_WORLD3D_VERTICES = 262144;

    /** InstancedBatch 默认实例数 */
    public static final int DEFAULT_INSTANCED_INSTANCES = 1024;

    /** InstancedBatch 最小实例数 */
    public static final int MIN_INSTANCED_INSTANCES = 128;

    /** InstancedBatch 最大实例数 */
    public static final int MAX_INSTANCED_INSTANCES = 65536;

    // ==================== 单例 ====================

    private static BatchConfig instance;

    private BatchConfig() {}

    /**
     * 获取全局配置实例
     */
    public static BatchConfig getInstance() {
        if (instance == null) {
            instance = detect();
        }
        return instance;
    }

    /**
     * 检测 GL 硬件能力并创建配置
     */
    public static BatchConfig detect() {
        BatchConfig config = new BatchConfig();
        config.queryGLCapabilities();
        return config;
    }

    /**
     * 查询 OpenGL 硬件能力
     */
    private void queryGLCapabilities() {
        try {
            // 检查 GL 1.2 支持（GL_MAX_ELEMENTS_* 是 GL 1.2 引入的）
            supportsGL12 = GLContext.getCapabilities().OpenGL12;

            if (supportsGL12) {
                glMaxVertices = GL11.glGetInteger(GL12.GL_MAX_ELEMENTS_VERTICES);
                glMaxIndices = GL11.glGetInteger(GL12.GL_MAX_ELEMENTS_INDICES);

                TakoTechMod.LOG.info(
                    "[BatchConfig] GL Limits: MAX_ELEMENTS_VERTICES={}, MAX_ELEMENTS_INDICES={}",
                    glMaxVertices,
                    glMaxIndices);
            } else {
                // 不支持 GL 1.2，使用保守默认值
                glMaxVertices = 65536;
                glMaxIndices = 65536;
                TakoTechMod.LOG.info("[BatchConfig] GL 1.2 not supported, using default limits");
            }

            // 查询其他有用的 GL 限制
            int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            TakoTechMod.LOG.info("[BatchConfig] GL_MAX_TEXTURE_SIZE={}", maxTextureSize);

        } catch (Exception e) {
            TakoTechMod.LOG.warn("[BatchConfig] Failed to query GL capabilities: {}", e.getMessage());
            glMaxVertices = 65536;
            glMaxIndices = 65536;
            supportsGL12 = false;
        }
    }

    // ==================== GL 限制查询 ====================

    /**
     * 获取 GL 推荐的最大顶点数
     */
    public int getGLMaxVertices() {
        return glMaxVertices;
    }

    /**
     * 获取 GL 推荐的最大索引数
     */
    public int getGLMaxIndices() {
        return glMaxIndices;
    }

    /**
     * 是否支持 GL 1.2+
     */
    public boolean supportsGL12() {
        return supportsGL12;
    }

    // ==================== 推荐值计算 ====================

    /**
     * 获取 SpriteBatch 推荐的最大四边形数
     * 基于 GL_MAX_ELEMENTS_INDICES 计算
     */
    public int getRecommendedSpriteQuads() {
        // 每个四边形 6 个索引
        int maxByIndices = glMaxIndices / 6;
        // 每个四边形 4 个顶点
        int maxByVertices = glMaxVertices / 4;

        int recommended = Math.min(maxByIndices, maxByVertices);

        // 限制在合理范围内
        return clamp(recommended, MIN_SPRITE_QUADS, MAX_SPRITE_QUADS);
    }

    /**
     * 获取 World3DBatch 推荐的最大顶点数
     */
    public int getRecommendedWorld3DVertices() {
        // World3DBatch 主要受顶点数限制
        int recommended = Math.min(glMaxVertices, glMaxIndices);

        // 限制在合理范围内
        return clamp(recommended, MIN_WORLD3D_VERTICES, MAX_WORLD3D_VERTICES);
    }

    /**
     * 获取 InstancedBatch 推荐的最大实例数
     */
    public int getRecommendedInstancedInstances() {
        // 实例数主要受显存限制，这里使用保守估计
        // 假设每个实例 64 字节，限制在 4MB 以内
        int maxByMemory = (4 * 1024 * 1024) / 64;
        return clamp(maxByMemory, MIN_INSTANCED_INSTANCES, MAX_INSTANCED_INSTANCES);
    }

    // ==================== 用户配置 ====================

    /**
     * 设置 SpriteBatch 最大四边形数
     *
     * @param maxQuads 最大四边形数，-1 表示使用推荐值
     */
    public void setSpriteMaxQuads(int maxQuads) {
        if (maxQuads == -1) {
            this.spriteMaxQuads = -1;
        } else {
            this.spriteMaxQuads = clamp(maxQuads, MIN_SPRITE_QUADS, MAX_SPRITE_QUADS);
        }
    }

    /**
     * 获取 SpriteBatch 最大四边形数（考虑用户配置）
     */
    public int getSpriteMaxQuads() {
        if (spriteMaxQuads > 0) {
            return spriteMaxQuads;
        }
        return getRecommendedSpriteQuads();
    }

    /**
     * 设置 World3DBatch 最大顶点数
     *
     * @param maxVertices 最大顶点数，-1 表示使用推荐值
     */
    public void setWorld3DMaxVertices(int maxVertices) {
        if (maxVertices == -1) {
            this.world3DMaxVertices = -1;
        } else {
            this.world3DMaxVertices = clamp(maxVertices, MIN_WORLD3D_VERTICES, MAX_WORLD3D_VERTICES);
        }
    }

    /**
     * 获取 World3DBatch 最大顶点数（考虑用户配置）
     */
    public int getWorld3DMaxVertices() {
        if (world3DMaxVertices > 0) {
            return world3DMaxVertices;
        }
        return getRecommendedWorld3DVertices();
    }

    /**
     * 设置 InstancedBatch 最大实例数
     *
     * @param maxInstances 最大实例数，-1 表示使用推荐值
     */
    public void setInstancedMaxInstances(int maxInstances) {
        if (maxInstances == -1) {
            this.instancedMaxInstances = -1;
        } else {
            this.instancedMaxInstances = clamp(maxInstances, MIN_INSTANCED_INSTANCES, MAX_INSTANCED_INSTANCES);
        }
    }

    /**
     * 获取 InstancedBatch 最大实例数（考虑用户配置）
     */
    public int getInstancedMaxInstances() {
        if (instancedMaxInstances > 0) {
            return instancedMaxInstances;
        }
        return getRecommendedInstancedInstances();
    }

    /**
     * 重置所有用户配置为推荐值
     */
    public void resetToRecommended() {
        spriteMaxQuads = -1;
        world3DMaxVertices = -1;
        instancedMaxInstances = -1;
    }

    // ==================== 工具方法 ====================

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 获取配置摘要字符串
     */
    public String getSummary() {
        return String.format(
            "BatchConfig[GL: vertices=%d, indices=%d | Sprite: %d quads | World3D: %d verts | Instanced: %d instances]",
            glMaxVertices,
            glMaxIndices,
            getSpriteMaxQuads(),
            getWorld3DMaxVertices(),
            getInstancedMaxInstances());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
