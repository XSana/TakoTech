package moe.takochan.takotech.client.renderer.graphics.particle;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 粒子计算着色器调度器。
 * 管理粒子物理模拟的计算着色器。
 *
 * <p>
 * 职责:
 * </p>
 * <ul>
 * <li>加载和编译计算着色器</li>
 * <li>调度粒子更新计算</li>
 * <li>管理力场 Uniform</li>
 * <li>处理内存屏障</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class ParticleCompute {

    /** 粒子更新着色器 */
    private ShaderProgram updateShader;

    /** 粒子发射着色器 */
    private ShaderProgram emitShader;

    /** 工作组大小 */
    private static final int WORK_GROUP_SIZE = 256;

    /** 最大力场数量 */
    private static final int MAX_FORCES = 16;

    /** 力场数据缓冲 (每个力场 12 个 float) */
    private final float[] forceData = new float[MAX_FORCES * 12];

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 粒子 SSBO 绑定点 */
    public static final int PARTICLE_SSBO_BINDING = 0;

    /** 计数器绑定点 */
    public static final int COUNTER_BINDING = 1;

    /**
     * 创建粒子计算器
     */
    public ParticleCompute() {}

    /**
     * 初始化（延迟加载着色器）
     *
     * @return true 初始化成功
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        if (!ShaderProgram.isComputeShaderSupported()) {
            TakoTechMod.LOG.warn("ParticleCompute: Compute shader not supported");
            return false;
        }

        try {
            // 加载粒子更新着色器
            updateShader = ShaderProgram.createCompute("takotech", "shaders/particle/particle_update.comp");
            if (updateShader == null || !updateShader.isValid()) {
                TakoTechMod.LOG.warn("ParticleCompute: Failed to create update shader, using fallback");
                updateShader = createFallbackUpdateShader();
            }

            // 加载粒子发射着色器
            emitShader = ShaderProgram.createCompute("takotech", "shaders/particle/particle_emit.comp");
            if (emitShader == null || !emitShader.isValid()) {
                TakoTechMod.LOG.warn("ParticleCompute: Failed to create emit shader, using fallback");
                emitShader = createFallbackEmitShader();
            }

            if (updateShader == null || !updateShader.isValid() || emitShader == null || !emitShader.isValid()) {
                TakoTechMod.LOG.error("ParticleCompute: Failed to initialize compute shaders");
                return false;
            }

            initialized = true;
            TakoTechMod.LOG.info("ParticleCompute: Initialized successfully");
            return true;

        } catch (Exception e) {
            TakoTechMod.LOG.error("ParticleCompute: Initialization failed", e);
            return false;
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /** 随机种子计数器（确定性随机） */
    private int randomSeedCounter = 0;

    /**
     * 调度粒子更新
     *
     * @param buffer            粒子缓冲区
     * @param deltaTime         时间增量（秒）
     * @param forces            力场列表
     * @param collisionMode     碰撞模式
     * @param collisionResponse 碰撞响应
     * @param bounciness        弹性系数
     * @param bounceChance      弹跳概率 (0-1)
     * @param bounceSpread      弹跳扩散角度 (度)
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness, float bounceChance,
        float bounceSpread) {

        if (!initialized || updateShader == null) {
            return;
        }

        int particleCount = buffer.getMaxParticles();
        if (particleCount == 0) {
            return;
        }

        // 重置原子计数器，这样每帧重新计数存活粒子
        buffer.resetAtomicCounter(0);

        updateShader.use();

        // 确定性随机种子（每帧递增，确保噪声平滑）
        randomSeedCounter++;

        // 设置 Uniform
        setUniform("uDeltaTime", deltaTime);
        setUniform("uParticleCount", particleCount);
        setUniform("uCollisionMode", collisionMode.getId());
        setUniform("uCollisionResponse", collisionResponse.getId());
        setUniform("uBounciness", bounciness);
        setUniform("uBounceChance", bounceChance);
        setUniform("uBounceSpread", bounceSpread);
        setUniform("uRandomSeed", randomSeedCounter);

        // 设置默认碰撞平面 (Y=0 地面)
        // 注意: 这个方法不接收碰撞平面参数，使用默认值
        // 如需自定义平面，请使用带 emitter 参数的 dispatchUpdate 方法
        setUniform("uCollisionPlane", 0.0f, 1.0f, 0.0f, 0.0f);

        // 设置力场数据
        int forceCount = Math.min(forces != null ? forces.size() : 0, MAX_FORCES);
        setUniform("uForceCount", forceCount);

        if (forceCount > 0) {
            for (int i = 0; i < forceCount; i++) {
                ParticleForce force = forces.get(i);
                float[] data = force.toFloatArray();
                System.arraycopy(data, 0, forceData, i * 12, 12);
            }
            // 上传力场数组到 uniform
            setUniformArray("uForces", forceData, forceCount * 12);
        }

        // 绑定 SSBO
        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        // 计算工作组数量
        int workGroups = (particleCount + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;

        // 调度计算
        updateShader.dispatch(workGroups, 1, 1);

        // 内存屏障
        ShaderProgram.memoryBarrierSSBO();

        ShaderProgram.unbind();
    }

    /**
     * 调度粒子更新（简化版，向后兼容）
     *
     * @param buffer            粒子缓冲区
     * @param deltaTime         时间增量（秒）
     * @param forces            力场列表
     * @param collisionMode     碰撞模式
     * @param collisionResponse 碰撞响应
     * @param bounciness        弹性系数
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness) {
        dispatchUpdate(buffer, deltaTime, forces, collisionMode, collisionResponse, bounciness, 1.0f, 0.0f);
    }

    /**
     * 调度粒子更新（简化版）
     *
     * @param buffer    粒子缓冲区
     * @param deltaTime 时间增量
     * @param emitter   发射器（获取力场和碰撞设置）
     */
    public void dispatchUpdate(ParticleBuffer buffer, float deltaTime, ParticleEmitter emitter) {
        dispatchUpdateWithPlane(
            buffer,
            deltaTime,
            emitter.getForces(),
            emitter.getCollisionMode(),
            emitter.getCollisionResponse(),
            emitter.getBounciness(),
            emitter.getBounceChance(),
            emitter.getBounceSpread(),
            emitter.getCollisionPlaneNX(),
            emitter.getCollisionPlaneNY(),
            emitter.getCollisionPlaneNZ(),
            emitter.getCollisionPlaneD());
    }

    /**
     * 调度粒子更新（带碰撞平面参数）
     */
    public void dispatchUpdateWithPlane(ParticleBuffer buffer, float deltaTime, List<ParticleForce> forces,
        CollisionMode collisionMode, CollisionResponse collisionResponse, float bounciness, float bounceChance,
        float bounceSpread, float planeNX, float planeNY, float planeNZ, float planeD) {

        if (!initialized || updateShader == null) {
            return;
        }

        int particleCount = buffer.getMaxParticles();
        if (particleCount == 0) {
            return;
        }

        buffer.resetAtomicCounter(0);
        updateShader.use();
        randomSeedCounter++;

        setUniform("uDeltaTime", deltaTime);
        setUniform("uParticleCount", particleCount);
        setUniform("uCollisionMode", collisionMode.getId());
        setUniform("uCollisionResponse", collisionResponse.getId());
        setUniform("uBounciness", bounciness);
        setUniform("uBounceChance", bounceChance);
        setUniform("uBounceSpread", bounceSpread);
        setUniform("uRandomSeed", randomSeedCounter);
        setUniform("uCollisionPlane", planeNX, planeNY, planeNZ, planeD);

        int forceCount = Math.min(forces != null ? forces.size() : 0, MAX_FORCES);
        setUniform("uForceCount", forceCount);

        if (forceCount > 0) {
            for (int i = 0; i < forceCount; i++) {
                ParticleForce force = forces.get(i);
                float[] data = force.toFloatArray();
                System.arraycopy(data, 0, forceData, i * 12, 12);
            }
            setUniformArray("uForces", forceData, forceCount * 12);
        }

        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        int workGroups = (particleCount + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;
        updateShader.dispatch(workGroups, 1, 1);
        ShaderProgram.memoryBarrierSSBO();
        ShaderProgram.unbind();
    }

    /**
     * 发射新粒子（使用 compute shader）
     *
     * @param buffer   粒子缓冲区
     * @param emitter  发射器
     * @param count    发射数量
     * @param baseTime 基础时间偏移
     */
    public void dispatchEmit(ParticleBuffer buffer, ParticleEmitter emitter, int count, float baseTime) {
        if (!initialized || emitShader == null || count <= 0) {
            return;
        }

        emitShader.use();

        // 设置发射器参数
        setUniform("uEmitCount", count);
        setUniform("uMaxParticles", buffer.getMaxParticles());
        setUniform("uBaseTime", baseTime);
        setUniform("uEmitterPos", emitter.getPositionX(), emitter.getPositionY(), emitter.getPositionZ());
        setUniform(
            "uShapeType",
            emitter.getShape()
                .getId());
        setUniform("uShapeParam1", emitter.getShapeParam1());
        setUniform("uShapeParam2", emitter.getShapeParam2());
        setUniform("uShapeParam3", emitter.getShapeParam3());
        setUniform("uLifetimeMin", emitter.getLifetimeMin());
        setUniform("uLifetimeMax", emitter.getLifetimeMax());
        setUniform("uVelocity", emitter.getVelocityX(), emitter.getVelocityY(), emitter.getVelocityZ());
        setUniform("uSpeed", emitter.getSpeed());
        setUniform("uSizeMin", emitter.getSizeMin());
        setUniform("uSizeMax", emitter.getSizeMax());
        setUniform("uColor", emitter.getColorR(), emitter.getColorG(), emitter.getColorB(), emitter.getColorA());
        setUniform("uParticleType", emitter.getParticleType());
        setUniform("uRotationMin", 0.0f);
        setUniform("uRotationMax", 0.0f);
        setUniform("uAngularVelMin", 0.0f);
        setUniform("uAngularVelMax", 0.0f);

        // 绑定 SSBO
        buffer.bindToCompute(PARTICLE_SSBO_BINDING);
        buffer.bindAtomicCounter(COUNTER_BINDING);

        // 计算工作组数量
        int workGroups = (count + WORK_GROUP_SIZE - 1) / WORK_GROUP_SIZE;

        // 调度计算
        emitShader.dispatch(workGroups, 1, 1);

        // 内存屏障
        ShaderProgram.memoryBarrierSSBO();

        ShaderProgram.unbind();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (updateShader != null) {
            updateShader.close();
            updateShader = null;
        }
        if (emitShader != null) {
            emitShader.close();
            emitShader = null;
        }
        initialized = false;
    }

    // ==================== 内部方法 ====================

    /**
     * 设置 float uniform
     */
    private void setUniform(String name, float value) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    /**
     * 设置 int uniform
     */
    private void setUniform(String name, int value) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    /**
     * 设置 vec3 uniform
     */
    private void setUniform(String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * 设置 vec4 uniform
     */
    private void setUniform(String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    /**
     * 设置 float 数组 uniform
     */
    private void setUniformArray(String name, float[] data, int count) {
        int location = GL20.glGetUniformLocation(getCurrentProgramId(), name);
        if (location >= 0) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(count);
            buffer.put(data, 0, count);
            buffer.flip();
            GL20.glUniform1(location, buffer);
        }
    }

    /**
     * 获取当前绑定的程序 ID
     */
    private int getCurrentProgramId() {
        return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    }

    // ==================== 回退着色器 ====================

    /**
     * 创建回退更新着色器（基本物理）
     */
    private ShaderProgram createFallbackUpdateShader() {
        String source = "#version 430 core\n" + "\n"
            + "layout(local_size_x = 256) in;\n"
            + "\n"
            + "struct Particle {\n"
            + "    vec4 position;  // xyz: pos, w: life\n"
            + "    vec4 velocity;  // xyz: vel, w: maxLife\n"
            + "    vec4 color;\n"
            + "    vec4 params;    // x: size, y: rot, z: type, w: angVel\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 0) buffer ParticleBuffer {\n"
            + "    Particle particles[];\n"
            + "};\n"
            + "\n"
            + "layout(binding = 1) uniform atomic_uint aliveCount;\n"
            + "\n"
            + "uniform float uDeltaTime;\n"
            + "uniform int uParticleCount;\n"
            + "uniform int uForceCount;\n"
            + "uniform vec3 uGravity;\n"
            + "\n"
            + "void main() {\n"
            + "    uint idx = gl_GlobalInvocationID.x;\n"
            + "    if (idx >= uParticleCount) return;\n"
            + "    \n"
            + "    Particle p = particles[idx];\n"
            + "    \n"
            + "    // Check if alive\n"
            + "    if (p.position.w <= 0.0) return;\n"
            + "    \n"
            + "    // Update lifetime\n"
            + "    p.position.w -= uDeltaTime;\n"
            + "    \n"
            + "    if (p.position.w <= 0.0) {\n"
            + "        // Particle dead\n"
            + "        p.position.w = 0.0;\n"
            + "        particles[idx] = p;\n"
            + "        return;\n"
            + "    }\n"
            + "    \n"
            + "    // Apply gravity\n"
            + "    p.velocity.xyz += uGravity * uDeltaTime;\n"
            + "    \n"
            + "    // Update position\n"
            + "    p.position.xyz += p.velocity.xyz * uDeltaTime;\n"
            + "    \n"
            + "    // Update rotation\n"
            + "    p.params.y += p.params.w * uDeltaTime;\n"
            + "    \n"
            + "    // 计算存活粒子\n"
            + "    atomicCounterIncrement(aliveCount);\n"
            + "    \n"
            + "    particles[idx] = p;\n"
            + "}\n";

        return ShaderProgram.createComputeFromSource(source);
    }

    /**
     * 创建回退发射着色器
     */
    private ShaderProgram createFallbackEmitShader() {
        String source = "#version 430 core\n" + "\n"
            + "layout(local_size_x = 256) in;\n"
            + "\n"
            + "struct Particle {\n"
            + "    vec4 position;\n"
            + "    vec4 velocity;\n"
            + "    vec4 color;\n"
            + "    vec4 params;\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 0) buffer ParticleBuffer {\n"
            + "    Particle particles[];\n"
            + "};\n"
            + "\n"
            + "uniform int uEmitCount;\n"
            + "uniform vec3 uEmitterPos;\n"
            + "uniform float uLifetimeMin;\n"
            + "uniform float uLifetimeMax;\n"
            + "uniform vec3 uVelocity;\n"
            + "uniform float uSizeMin;\n"
            + "uniform float uSizeMax;\n"
            + "uniform vec4 uColor;\n"
            + "uniform int uParticleType;\n"
            + "uniform float uBaseTime;\n"
            + "\n"
            + "// 简单伪随机\n"
            + "uint hash(uint x) {\n"
            + "    x += (x << 10u);\n"
            + "    x ^= (x >> 6u);\n"
            + "    x += (x << 3u);\n"
            + "    x ^= (x >> 11u);\n"
            + "    x += (x << 15u);\n"
            + "    return x;\n"
            + "}\n"
            + "\n"
            + "float random(uint seed) {\n"
            + "    return float(hash(seed)) / 4294967295.0;\n"
            + "}\n"
            + "\n"
            + "void main() {\n"
            + "    uint idx = gl_GlobalInvocationID.x;\n"
            + "    if (idx >= uEmitCount) return;\n"
            + "    \n"
            + "    // 找到一个空闲槽位\n"
            + "    uint slot = idx; // 简化：直接使用索引\n"
            + "    \n"
            + "    uint seed = idx + uint(uBaseTime * 1000.0);\n"
            + "    \n"
            + "    Particle p;\n"
            + "    p.position = vec4(uEmitterPos, mix(uLifetimeMin, uLifetimeMax, random(seed)));\n"
            + "    p.velocity = vec4(uVelocity, p.position.w);\n"
            + "    p.color = uColor;\n"
            + "    p.params = vec4(mix(uSizeMin, uSizeMax, random(seed + 1u)), 0.0, float(uParticleType), 0.0);\n"
            + "    \n"
            + "    particles[slot] = p;\n"
            + "}\n";

        return ShaderProgram.createComputeFromSource(source);
    }

    /**
     * 获取工作组大小
     */
    public static int getWorkGroupSize() {
        return WORK_GROUP_SIZE;
    }
}
