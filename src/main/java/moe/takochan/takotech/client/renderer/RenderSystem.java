package moe.takochan.takotech.client.renderer;

import org.lwjgl.opengl.Display;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.batch.BatchConfig;
import moe.takochan.takotech.client.renderer.graphics.batch.SpriteBatch;
import moe.takochan.takotech.client.renderer.graphics.batch.World3DBatch;
import moe.takochan.takotech.client.renderer.graphics.buffer.GlobalUniforms;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.core.RenderContext;
import moe.takochan.takotech.client.renderer.graphics.ecs.World;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleSystem;
import moe.takochan.takotech.client.renderer.graphics.postprocess.PostProcessor;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.client.renderer.graphics.system.ParticleUpdateSystem;

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
    private static boolean spriteBatchInitialized = false;

    /** GUI 相机 - 2D 正交投影，独立于 MC */
    private static Camera guiCamera = null;
    private static boolean guiCameraInitialized = false;

    /** 世界相机 - 3D 透视投影，与 MC 相机同步 */
    private static Camera worldCamera = null;
    private static boolean worldCameraInitialized = false;

    /** 3D 世界批量渲染器 - 用于在世界中渲染 3D 图元 */
    private static World3DBatch world3DBatch = null;
    private static boolean world3DBatchInitialized = false;

    /** 批量渲染器配置 */
    private static BatchConfig batchConfig = null;

    // ==================== ECS ====================

    /** ECS 世界 */
    private static World ecsWorld = null;
    private static boolean ecsWorldInitialized = false;

    /** 渲染上下文 */
    private static RenderContext renderContext = null;
    private static boolean renderContextInitialized = false;

    /** 后处理器 */
    private static PostProcessor postProcessor = null;
    private static boolean postProcessorInitialized = false;

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

        // 检测 GL 硬件能力并初始化批量渲染器配置
        batchConfig = BatchConfig.detect();
        TakoTechMod.LOG.info("BatchConfig: {}", batchConfig.getSummary());

        // 注意：SpriteBatch 延迟初始化，在第一次使用时创建
        // 这是因为在 FML 初始化阶段创建的 VAO/VBO 可能会被 Angelica/Embeddium 污染

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
        spriteBatchInitialized = false;

        // 释放相机
        guiCamera = null;
        guiCameraInitialized = false;
        worldCamera = null;
        worldCameraInitialized = false;

        // 释放 World3DBatch
        if (world3DBatch != null) {
            world3DBatch.close();
            world3DBatch = null;
        }
        world3DBatchInitialized = false;

        // 释放 ECS World
        if (ecsWorld != null) {
            ecsWorld.destroy();
            ecsWorld = null;
        }
        ecsWorldInitialized = false;

        // 释放渲染上下文
        renderContext = null;
        renderContextInitialized = false;

        // 释放后处理器
        if (postProcessor != null) {
            postProcessor.cleanup();
            postProcessor = null;
        }
        postProcessorInitialized = false;

        // 清理 shaders
        ShaderType.cleanupAll();

        // 清理 BatchConfig
        batchConfig = null;

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
     * 注意：SpriteBatch 采用延迟初始化，在第一次调用时创建。
     * 这是为了避免在 FML 初始化阶段创建的 VAO/VBO 被其他 mod 污染。
     *
     * @return SpriteBatch 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static SpriteBatch getSpriteBatch() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化 SpriteBatch（使用 BatchConfig 的推荐值）
        if (!spriteBatchInitialized) {
            int maxQuads = batchConfig != null ? batchConfig.getSpriteMaxQuads() : BatchConfig.DEFAULT_SPRITE_QUADS;
            spriteBatch = new SpriteBatch(maxQuads);
            spriteBatchInitialized = true;
            TakoTechMod.LOG.info("SpriteBatch lazily initialized with {} quads", maxQuads);
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

    /**
     * 获取 GUI 相机（2D 正交投影，独立于 MC）
     * 用于 HUD、菜单等 2D 渲染
     *
     * @return GUI 相机实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static Camera getGuiCamera() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化 GUI 相机
        if (!guiCameraInitialized) {
            guiCamera = Camera.orthographic(1920, 1080, -1000, 1000);
            guiCameraInitialized = true;
            TakoTechMod.LOG.info("GUI Camera lazily initialized");
        }
        return guiCamera;
    }

    /**
     * 获取世界相机（3D 透视投影，与 MC 相机同步）
     * 用于 3D 场景渲染，需要每帧调用 syncFromMinecraft() 同步
     *
     * @return 世界相机实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static Camera getWorldCamera() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化世界相机
        if (!worldCameraInitialized) {
            worldCamera = Camera.perspective(70.0f, 16.0f / 9.0f, 0.05f, 256.0f);
            worldCameraInitialized = true;
            TakoTechMod.LOG.info("World Camera lazily initialized");
        }
        return worldCamera;
    }

    /**
     * 更新世界相机与 MC 同步（每帧调用）
     *
     * @param partialTicks 插值因子
     */
    public static void updateWorldCamera(float partialTicks) {
        Camera camera = getWorldCamera();
        if (camera != null) {
            int width = Display.getWidth();
            int height = Display.getHeight();
            if (width > 0 && height > 0) {
                camera.setAspectRatio((float) width / (float) height);
            }
            camera.syncFromMinecraft(partialTicks);
        }
    }

    /**
     * 获取共享的 World3DBatch 实例
     * 用于在 MC 世界中渲染 3D 图元（线框、方块、粒子等）
     *
     * @return World3DBatch 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static World3DBatch getWorld3DBatch() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化 World3DBatch（使用 BatchConfig 的推荐值）
        if (!world3DBatchInitialized) {
            int maxVerts = batchConfig != null ? batchConfig.getWorld3DMaxVertices()
                : BatchConfig.DEFAULT_WORLD3D_VERTICES;
            world3DBatch = new World3DBatch(maxVerts);
            world3DBatchInitialized = true;
            TakoTechMod.LOG.info("World3DBatch lazily initialized with {} vertices", maxVerts);
        }
        return world3DBatch;
    }

    /**
     * 创建新的 World3DBatch 实例（使用 BatchConfig 推荐值）
     *
     * @return 新的 World3DBatch 实例，如果系统不支持 shader 则返回 null
     */
    public static World3DBatch createWorld3DBatch() {
        if (!isShaderSupported()) {
            return null;
        }
        int maxVerts = batchConfig != null ? batchConfig.getWorld3DMaxVertices() : BatchConfig.DEFAULT_WORLD3D_VERTICES;
        return new World3DBatch(maxVerts);
    }

    /**
     * 创建指定容量的 World3DBatch 实例
     *
     * @param maxVertices 最大顶点数
     * @return 新的 World3DBatch 实例，如果系统不支持 shader 则返回 null
     */
    public static World3DBatch createWorld3DBatch(int maxVertices) {
        if (!isShaderSupported()) {
            return null;
        }
        return new World3DBatch(maxVertices);
    }

    /**
     * 获取批量渲染器配置
     * 可用于查询 GL 硬件限制和自定义批量渲染器容量
     *
     * @return BatchConfig 实例，如果 RenderSystem 未初始化则返回 null
     */
    public static BatchConfig getBatchConfig() {
        return batchConfig;
    }

    // ==================== GPGPU 粒子系统 ====================

    /**
     * 检查当前系统是否支持 GPGPU 粒子系统
     * 需要 OpenGL 4.3+ 支持 Compute Shader 和 SSBO
     *
     * @return true 如果支持 GPGPU 粒子系统
     */
    public static boolean isParticleSystemSupported() {
        return ParticleSystem.isSupported();
    }

    /**
     * 创建 GPGPU 粒子系统
     *
     * @param maxParticles 最大粒子数量
     * @return 新的 ParticleSystem 实例，如果系统不支持则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static ParticleSystem createParticleSystem(int maxParticles) {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isParticleSystemSupported()) {
            TakoTechMod.LOG.warn("GPGPU particle system not supported on this hardware");
            return null;
        }
        ParticleSystem system = new ParticleSystem(maxParticles);
        if (!system.initialize()) {
            TakoTechMod.LOG.error("Failed to initialize particle system");
            return null;
        }
        return system;
    }

    /**
     * 创建默认容量的 GPGPU 粒子系统（10000 粒子）
     *
     * @return 新的 ParticleSystem 实例，如果系统不支持则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static ParticleSystem createParticleSystem() {
        return createParticleSystem(10000);
    }

    // ==================== ECS 接口 ====================

    /**
     * 获取 ECS 世界实例
     * 用于创建和管理实体、组件、系统
     *
     * @return ECS World 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static World getWorld() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化 ECS World
        if (!ecsWorldInitialized) {
            ecsWorld = new World("TakoTechWorld");

            // 注册默认系统
            ecsWorld.registerSystem(new ParticleUpdateSystem());

            ecsWorldInitialized = true;
            TakoTechMod.LOG.info("ECS World lazily initialized");
        }
        return ecsWorld;
    }

    /**
     * 获取渲染上下文
     * 用于传递相机信息和矩阵到着色器
     *
     * @return RenderContext 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static RenderContext getRenderContext() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化渲染上下文
        if (!renderContextInitialized) {
            renderContext = new RenderContext();
            renderContextInitialized = true;
            TakoTechMod.LOG.info("RenderContext lazily initialized");
        }
        return renderContext;
    }

    /**
     * 获取后处理器
     * 用于 Bloom、HDR 等后处理效果
     *
     * @return PostProcessor 实例，如果系统不支持 shader 则返回 null
     * @throws IllegalStateException 如果 RenderSystem 未初始化
     */
    public static PostProcessor getPostProcessor() {
        if (!initialized) {
            throw new IllegalStateException("RenderSystem not initialized. Call RenderSystem.init() first.");
        }
        if (!isShaderSupported()) {
            return null;
        }
        // 延迟初始化后处理器
        if (!postProcessorInitialized) {
            postProcessor = new PostProcessor();
            if (!postProcessor.initialize()) {
                TakoTechMod.LOG.error("Failed to initialize PostProcessor");
                postProcessor = null;
            }
            postProcessorInitialized = true;
            TakoTechMod.LOG.info("PostProcessor lazily initialized");
        }
        return postProcessor;
    }

    /**
     * 更新 ECS 世界（每帧调用）
     *
     * @param deltaTime 时间增量（秒）
     */
    public static void updateWorld(float deltaTime) {
        World world = getWorld();
        if (world != null) {
            world.update(deltaTime);
        }
        GlobalUniforms uniforms = GlobalUniforms.INSTANCE;
        if (uniforms.isAvailable()) {
            uniforms.updateTime(deltaTime);
        }
    }

    /**
     * 更新渲染上下文（每帧调用，在渲染前）
     *
     * @param partialTicks 渲染插值
     */
    public static void updateRenderContext(float partialTicks) {
        RenderContext ctx = getRenderContext();
        if (ctx != null) {
            ctx.syncFromMinecraft(partialTicks);
            GlobalUniforms uniforms = GlobalUniforms.INSTANCE;
            uniforms.init();
            if (uniforms.isAvailable()) {
                uniforms.setProjection(MathUtils.toFloatBuffer(ctx.getProjMatrix()));
                uniforms.setView(MathUtils.toFloatBuffer(ctx.getViewMatrix()));
                uniforms.setScreenSize(Display.getWidth(), Display.getHeight());
                uniforms.bind();
            }
        }
    }
}
