package moe.takochan.takotech.client.renderer.graphics.particle;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * GPGPU 粒子系统。
 * 整合 Buffer、Compute、Emitter、Renderer 实现完整的粒子系统。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 创建粒子系统
 *     ParticleSystem particles = new ParticleSystem(10000);
 *
 *     // 添加发射器
 *     particles.addEmitter(
 *         new ParticleEmitter().setPosition(0, 64, 0)
 *             .setShape(EmitterShape.SPHERE, 1.0f)
 *             .setRate(100)
 *             .setLifetime(1.0f, 2.0f)
 *             .setColor(1, 0.5f, 0, 1));
 *
 *     // 每帧更新和渲染
 *     particles.update(deltaTime);
 *     particles.render(viewMatrix, projMatrix, cameraPos);
 *
 *     // 清理
 *     particles.cleanup();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleSystem {

    /** 最大粒子数量 */
    private final int maxParticles;

    /** 粒子缓冲区 */
    private ParticleBuffer buffer;

    /** 计算着色器调度器 */
    private ParticleCompute compute;

    /** 渲染器 */
    private ParticleRenderer renderer;

    /** 发射器列表 */
    private final List<ParticleEmitter> emitters = new ArrayList<>();

    /** 全局力场列表 */
    private final List<ParticleForce> globalForces = new ArrayList<>();

    /** 系统位置 */
    private float posX = 0, posY = 0, posZ = 0;

    /** 系统时间（秒） */
    private float systemTime = 0;

    /** 是否暂停 */
    private boolean paused = false;

    /** 是否循环播放 */
    private boolean looping = true;

    /** 系统持续时间（秒），0 表示无限 */
    private float duration = 0;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 是否启用 GPU 计算 */
    private boolean gpuComputeEnabled = true;

    /** 粒子纹理 ID */
    private int textureId = 0;

    /** 渲染混合模式 */
    private ParticleRenderer.BlendMode blendMode = ParticleRenderer.BlendMode.ALPHA;

    /** 渲染模式 */
    private ParticleRenderer.RenderMode renderMode = ParticleRenderer.RenderMode.BILLBOARD_QUAD;

    /** 全局碰撞模式 */
    private CollisionMode collisionMode = CollisionMode.NONE;

    /** 全局碰撞响应 */
    private CollisionResponse collisionResponse = CollisionResponse.KILL;

    /** 碰撞弹性 */
    private float bounciness = 0.5f;

    /** 待设置的内置 Mesh 类型（延迟到 initialize 后应用） */
    private ParticleRenderer.BuiltinMesh pendingBuiltinMesh = null;

    /** 待设置的自定义 Mesh 数据 */
    private float[] pendingMeshVertices = null;
    private int[] pendingMeshIndices = null;

    /**
     * 创建粒子系统
     *
     * @param maxParticles 最大粒子数量
     */
    public ParticleSystem(int maxParticles) {
        this.maxParticles = maxParticles;
    }

    /**
     * 初始化粒子系统
     *
     * @return true 初始化成功
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        // 检查 GPU 计算支持
        gpuComputeEnabled = ShaderProgram.isComputeShaderSupported() && ShaderProgram.isSSBOSupported();

        if (!gpuComputeEnabled) {
            TakoTechMod.LOG.error("ParticleSystem: GPU compute not supported, particle system disabled");
            return false;
        }

        try {
            // 创建粒子缓冲区
            buffer = new ParticleBuffer(maxParticles);
            buffer.initialize();

            // 创建计算着色器调度器
            if (gpuComputeEnabled) {
                compute = new ParticleCompute();
                if (!compute.initialize()) {
                    TakoTechMod.LOG.error("ParticleSystem: Compute shader initialization failed");
                    cleanup();
                    return false;
                }
            }

            // 创建渲染器
            renderer = new ParticleRenderer();
            if (!renderer.initialize()) {
                TakoTechMod.LOG.error("ParticleSystem: Renderer initialization failed");
                cleanup();
                return false;
            }

            // 配置渲染器实例属性
            renderer.setupInstancedAttributes(buffer.getSsboId());

            // 应用延迟设置的 Mesh
            if (pendingBuiltinMesh != null) {
                renderer.setBuiltinMesh(pendingBuiltinMesh);
                pendingBuiltinMesh = null;
            } else if (pendingMeshVertices != null && pendingMeshIndices != null) {
                renderer.setMesh(pendingMeshVertices, pendingMeshIndices);
                pendingMeshVertices = null;
                pendingMeshIndices = null;
            }

            initialized = true;
            TakoTechMod.LOG.info(
                "ParticleSystem: Initialized with {} max particles, GPU compute: {}",
                maxParticles,
                gpuComputeEnabled);
            return true;

        } catch (Exception e) {
            TakoTechMod.LOG.error("ParticleSystem: Initialization failed", e);
            cleanup();
            return false;
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 更新粒子系统
     *
     * @param deltaTime 时间增量（秒）
     */
    public void update(float deltaTime) {
        if (!initialized || paused) {
            return;
        }

        // 更新系统时间
        systemTime += deltaTime;

        // 检查持续时间
        if (duration > 0 && systemTime >= duration) {
            if (looping) {
                systemTime = 0;
                for (ParticleEmitter emitter : emitters) {
                    emitter.reset();
                }
            } else {
                paused = true;
                return;
            }
        }

        // 发射新粒子
        for (ParticleEmitter emitter : emitters) {
            if (emitter.isEmitting()) {
                int emitCount = emitter.calculateEmitCount(deltaTime);
                if (emitCount > 0) {
                    emitParticles(emitter, emitCount);
                }
            }
        }

        // 更新粒子物理
        if (gpuComputeEnabled && compute != null) {
            // GPU 计算
            List<ParticleForce> allForces = gatherAllForces();

            // 获取碰撞参数（优先使用第一个 emitter 的设置，否则使用系统默认值）
            CollisionMode effectiveCollisionMode = collisionMode;
            CollisionResponse effectiveCollisionResponse = collisionResponse;
            float effectiveBounciness = bounciness;
            float effectiveBounceChance = 1.0f;
            float effectiveBounceSpread = 0.0f;
            float planeNX = 0, planeNY = 1, planeNZ = 0, planeD = 0;

            if (!emitters.isEmpty()) {
                ParticleEmitter firstEmitter = emitters.get(0);
                if (firstEmitter.getCollisionMode() != CollisionMode.NONE) {
                    effectiveCollisionMode = firstEmitter.getCollisionMode();
                    effectiveCollisionResponse = firstEmitter.getCollisionResponse();
                    effectiveBounciness = firstEmitter.getBounciness();
                    effectiveBounceChance = firstEmitter.getBounceChance();
                    effectiveBounceSpread = firstEmitter.getBounceSpread();
                    planeNX = firstEmitter.getCollisionPlaneNX();
                    planeNY = firstEmitter.getCollisionPlaneNY();
                    planeNZ = firstEmitter.getCollisionPlaneNZ();
                    planeD = firstEmitter.getCollisionPlaneD();
                }
            }

            compute.dispatchUpdateWithPlane(
                buffer,
                deltaTime,
                allForces,
                effectiveCollisionMode,
                effectiveCollisionResponse,
                effectiveBounciness,
                effectiveBounceChance,
                effectiveBounceSpread,
                planeNX,
                planeNY,
                planeNZ,
                planeD);
        }
    }

    /**
     * 渲染粒子
     *
     * @param viewMatrix 视图矩阵 (16 floats)
     * @param projMatrix 投影矩阵 (16 floats)
     * @param cameraPos  相机位置 [x, y, z]
     */
    public void render(float[] viewMatrix, float[] projMatrix, float[] cameraPos) {
        if (!initialized || renderer == null) {
            return;
        }

        renderer.setBlendMode(blendMode);
        renderer.setRenderMode(renderMode);
        renderer.render(buffer, viewMatrix, projMatrix, cameraPos, textureId);
    }

    /**
     * 使用 Minecraft 相机渲染
     *
     * @param partialTicks 渲染插值
     */
    public void renderWithMinecraftCamera(float partialTicks) {
        if (!initialized || renderer == null) {
            return;
        }

        renderer.setBlendMode(blendMode);
        renderer.setRenderMode(renderMode);
        renderer.renderWithMinecraftCamera(buffer, partialTicks);
    }

    // ==================== 发射器管理 ====================

    /**
     * 添加发射器
     *
     * @param emitter 发射器
     * @return this
     */
    public ParticleSystem addEmitter(ParticleEmitter emitter) {
        if (emitter != null) {
            // 应用系统位置偏移
            float ex = emitter.getPositionX() + posX;
            float ey = emitter.getPositionY() + posY;
            float ez = emitter.getPositionZ() + posZ;
            emitter.setPosition(ex, ey, ez);
            emitters.add(emitter);
        }
        return this;
    }

    /**
     * 移除发射器
     *
     * @param emitter 发射器
     * @return this
     */
    public ParticleSystem removeEmitter(ParticleEmitter emitter) {
        emitters.remove(emitter);
        return this;
    }

    /**
     * 清除所有发射器
     *
     * @return this
     */
    public ParticleSystem clearEmitters() {
        emitters.clear();
        return this;
    }

    /**
     * 获取发射器数量
     */
    public int getEmitterCount() {
        return emitters.size();
    }

    /**
     * 获取所有发射器（只读）
     */
    public List<ParticleEmitter> getEmitters() {
        return emitters;
    }

    // ==================== 力场管理 ====================

    /**
     * 添加全局力场
     *
     * @param force 力场
     * @return this
     */
    public ParticleSystem addForce(ParticleForce force) {
        if (force != null) {
            globalForces.add(force);
        }
        return this;
    }

    /**
     * 添加重力
     *
     * @param strength 重力强度（向下为负）
     * @return this
     */
    public ParticleSystem addGravity(float strength) {
        return addForce(ParticleForce.gravity(0, strength, 0));
    }

    /**
     * 清除所有全局力场
     *
     * @return this
     */
    public ParticleSystem clearForces() {
        globalForces.clear();
        return this;
    }

    // ==================== 系统控制 ====================

    /**
     * 设置系统位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this
     */
    public ParticleSystem setPosition(float x, float y, float z) {
        float dx = x - this.posX;
        float dy = y - this.posY;
        float dz = z - this.posZ;

        this.posX = x;
        this.posY = y;
        this.posZ = z;

        // 更新所有发射器位置
        for (ParticleEmitter emitter : emitters) {
            emitter.setPosition(emitter.getPositionX() + dx, emitter.getPositionY() + dy, emitter.getPositionZ() + dz);
        }

        return this;
    }

    /**
     * 播放
     *
     * @return this
     */
    public ParticleSystem play() {
        paused = false;
        return this;
    }

    /**
     * 暂停
     *
     * @return this
     */
    public ParticleSystem pause() {
        paused = true;
        return this;
    }

    /**
     * 停止并重置
     *
     * @return this
     */
    public ParticleSystem stop() {
        paused = true;
        systemTime = 0;
        buffer.clear();
        for (ParticleEmitter emitter : emitters) {
            emitter.reset();
        }
        return this;
    }

    /**
     * 重新开始
     *
     * @return this
     */
    public ParticleSystem restart() {
        stop();
        play();
        return this;
    }

    /**
     * 设置是否暂停
     *
     * @param paused true 暂停
     * @return this
     */
    public ParticleSystem setPaused(boolean paused) {
        this.paused = paused;
        return this;
    }

    /**
     * 设置是否循环
     *
     * @param looping true 循环
     * @return this
     */
    public ParticleSystem setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    /**
     * 设置持续时间
     *
     * @param duration 持续时间（秒），0 表示无限
     * @return this
     */
    public ParticleSystem setDuration(float duration) {
        this.duration = duration;
        return this;
    }

    // ==================== 渲染设置 ====================

    /**
     * 设置纹理
     *
     * @param textureId OpenGL 纹理 ID
     * @return this
     */
    public ParticleSystem setTexture(int textureId) {
        this.textureId = textureId;
        return this;
    }

    /**
     * 设置混合模式
     *
     * @param mode 混合模式
     * @return this
     */
    public ParticleSystem setBlendMode(ParticleRenderer.BlendMode mode) {
        this.blendMode = mode;
        return this;
    }

    /**
     * 设置渲染模式
     *
     * @param mode 渲染模式
     * @return this
     */
    public ParticleSystem setRenderMode(ParticleRenderer.RenderMode mode) {
        this.renderMode = mode;
        return this;
    }

    /**
     * 设置内置几何体（用于 MESH 渲染模式）
     *
     * @param type 内置几何体类型
     * @return this
     */
    public ParticleSystem setBuiltinMesh(ParticleRenderer.BuiltinMesh type) {
        if (renderer != null) {
            renderer.setBuiltinMesh(type);
        } else {
            // 延迟设置
            pendingBuiltinMesh = type;
            pendingMeshVertices = null;
            pendingMeshIndices = null;
        }
        return this;
    }

    /**
     * 设置自定义几何体（用于 MESH 渲染模式）
     *
     * @param vertices 顶点数据（每顶点 6 floats: pos.xyz + normal.xyz）
     * @param indices  索引数据
     * @return this
     */
    public ParticleSystem setMesh(float[] vertices, int[] indices) {
        if (renderer != null) {
            renderer.setMesh(vertices, indices);
        } else {
            // 延迟设置
            pendingMeshVertices = vertices;
            pendingMeshIndices = indices;
            pendingBuiltinMesh = null;
        }
        return this;
    }

    // ==================== 碰撞设置 ====================

    /**
     * 设置碰撞
     *
     * @param mode     碰撞模式
     * @param response 碰撞响应
     * @return this
     */
    public ParticleSystem setCollision(CollisionMode mode, CollisionResponse response) {
        this.collisionMode = mode;
        this.collisionResponse = response;
        return this;
    }

    /**
     * 设置碰撞弹性
     *
     * @param bounciness 弹性系数 (0-1)
     * @return this
     */
    public ParticleSystem setBounciness(float bounciness) {
        this.bounciness = bounciness;
        return this;
    }

    // ==================== 状态查询 ====================

    /**
     * 是否暂停
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 是否循环
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * 获取系统时间
     */
    public float getSystemTime() {
        return systemTime;
    }

    /**
     * 获取最大粒子数
     */
    public int getMaxParticles() {
        return maxParticles;
    }

    /**
     * 获取当前存活粒子数（近似值）
     */
    public int getAliveParticleCount() {
        if (buffer != null) {
            return buffer.readAtomicCounter();
        }
        return 0;
    }

    /**
     * 是否启用 GPU 计算
     */
    public boolean isGpuComputeEnabled() {
        return gpuComputeEnabled;
    }

    /**
     * 获取粒子缓冲区
     */
    public ParticleBuffer getBuffer() {
        return buffer;
    }

    /**
     * 获取渲染器
     */
    public ParticleRenderer getRenderer() {
        return renderer;
    }

    // ==================== 手动发射 ====================

    /**
     * 手动发射粒子
     *
     * @param count 发射数量
     */
    public void emit(int count) {
        if (emitters.isEmpty()) {
            return;
        }
        emitParticles(emitters.get(0), count);
    }

    /**
     * 从指定发射器发射粒子
     *
     * @param emitterIndex 发射器索引
     * @param count        发射数量
     */
    public void emit(int emitterIndex, int count) {
        if (emitterIndex >= 0 && emitterIndex < emitters.size()) {
            emitParticles(emitters.get(emitterIndex), count);
        }
    }

    // ==================== 清理 ====================

    /**
     * 清理所有资源
     */
    public void cleanup() {
        if (buffer != null) {
            buffer.cleanup();
            buffer = null;
        }
        if (compute != null) {
            compute.cleanup();
            compute = null;
        }
        if (renderer != null) {
            renderer.cleanup();
            renderer = null;
        }
        emitters.clear();
        globalForces.clear();
        initialized = false;
    }

    // ==================== 内部方法 ====================

    /**
     * 发射粒子
     */
    private void emitParticles(ParticleEmitter emitter, int count) {
        if (gpuComputeEnabled && compute != null) {
            // GPU 发射
            compute.dispatchEmit(buffer, emitter, count, systemTime);
        }
    }

    /**
     * 收集所有力场
     */
    private List<ParticleForce> gatherAllForces() {
        List<ParticleForce> allForces = new ArrayList<>(globalForces);

        // 添加发射器的力场
        for (ParticleEmitter emitter : emitters) {
            allForces.addAll(emitter.getForces());
        }

        return allForces;
    }

    /**
     * CPU 更新粒子（回退方案）
     * 注意：这是简化版本，实际应用中应该使用 GPU 计算
     */
    private void updateParticlesCPU(float deltaTime) {
        // CPU 回退实现非常慢，仅用于不支持 compute shader 的情况
        // 实际实现需要读取 SSBO 数据、更新、再写回
        // 这里仅作为占位符，实际游戏中应该警告并禁用粒子系统
        TakoTechMod.LOG.trace("ParticleSystem: CPU update fallback (not implemented)");
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 检查是否支持 GPGPU 粒子系统
     *
     * @return true 支持
     */
    public static boolean isSupported() {
        return ShaderProgram.isComputeShaderSupported() && ShaderProgram.isSSBOSupported();
    }

    /**
     * 创建简单粒子系统
     *
     * @param maxParticles 最大粒子数
     * @param emitter      发射器
     * @return 粒子系统
     */
    public static ParticleSystem create(int maxParticles, ParticleEmitter emitter) {
        ParticleSystem system = new ParticleSystem(maxParticles);
        system.addEmitter(emitter);
        return system;
    }
}
