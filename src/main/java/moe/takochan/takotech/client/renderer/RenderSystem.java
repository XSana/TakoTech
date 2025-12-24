package moe.takochan.takotech.client.renderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.batch.SpriteBatch;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;

/**
 * 渲染系统入口点。
 * 负责初始化和管理渲染资源，提供全局渲染服务。
 *
 * <p>
 * 初始化示例（在 ClientProxy.init 中调用）:
 * </p>
 * 
 * <pre>
 * {@code
 * RenderSystem.init();
 * }
 * </pre>
 *
 * <p>
 * 使用示例:
 * </p>
 * 
 * <pre>
 * {@code
 * if (RenderSystem.isShaderSupported()) {
 *     SpriteBatch batch = RenderSystem.getSpriteBatch();
 *     batch.setProjectionOrtho(width, height);
 *     batch.begin();
 *     batch.drawRect(x, y, w, h, r, g, b, a);
 *     batch.end();
 * }
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class RenderSystem {

    private static boolean initialized = false;
    private static SpriteBatch spriteBatch = null;

    private RenderSystem() {}

    /**
     * 初始化渲染系统
     * 应在 ClientProxy.init 中调用
     */
    public static void init() {
        if (initialized) {
            TakoTechMod.LOG.warn("RenderSystem already initialized");
            return;
        }

        TakoTechMod.LOG.info("Initializing RenderSystem...");

        // 检查 shader 支持
        if (!ShaderProgram.isSupported()) {
            TakoTechMod.LOG.warn("Shaders not supported, RenderSystem will use fallback rendering");
            initialized = true;
            return;
        }

        // 注册 shaders
        ShaderType.register();

        // 创建共享的 SpriteBatch
        spriteBatch = new SpriteBatch();

        initialized = true;
        TakoTechMod.LOG.info("RenderSystem initialized successfully");
    }

    /**
     * 关闭渲染系统，释放资源
     * 应在 mod 卸载时调用
     */
    public static void shutdown() {
        if (!initialized) return;

        TakoTechMod.LOG.info("Shutting down RenderSystem...");

        // 释放 SpriteBatch
        if (spriteBatch != null) {
            spriteBatch.close();
            spriteBatch = null;
        }

        // 清理 shaders
        ShaderType.cleanupAll();

        initialized = false;
        TakoTechMod.LOG.info("RenderSystem shutdown complete");
    }

    /**
     * 检查渲染系统是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 检查当前系统是否支持 Shader
     */
    public static boolean isShaderSupported() {
        return ShaderProgram.isSupported();
    }

    /**
     * 获取共享的 SpriteBatch 实例
     * 适用于大多数 2D 渲染场景
     *
     * @return SpriteBatch 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static SpriteBatch getSpriteBatch() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        return spriteBatch;
    }

    /**
     * 创建新的 SpriteBatch 实例
     * 适用于需要独立批次管理的场景
     *
     * @return 新的 SpriteBatch 实例，如果系统不支持 shader 则返回 null
     */
    public static SpriteBatch createSpriteBatch() {
        if (!isShaderSupported()) {
            return null;
        }
        return new SpriteBatch();
    }

    /**
     * 创建指定容量的 SpriteBatch 实例
     *
     * @param maxQuads 最大四边形数量
     * @return 新的 SpriteBatch 实例，如果系统不支持 shader 则返回 null
     */
    public static SpriteBatch createSpriteBatch(int maxQuads) {
        if (!isShaderSupported()) {
            return null;
        }
        return new SpriteBatch(maxQuads);
    }
}
