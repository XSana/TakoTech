package moe.takochan.takotech.client.renderer.graphics.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.core.RenderContext;
import moe.takochan.takotech.client.renderer.graphics.ecs.Component;
import moe.takochan.takotech.client.renderer.graphics.ecs.Entity;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleSystem;

/**
 * 粒子系统组件。
 * 将 ParticleSystem 包装为 ECS 组件，自动同步位置和生命周期。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Entity fireEffect = world.createEntity("Fire");
 *     fireEffect.addComponent(new TransformComponent())
 *         .setPosition(5, 64, 5);
 *
 *     ParticleSystem fire = ParticlePresets.createFire(1.0f);
 *     fireEffect.addComponent(new ParticleSystemComponent(fire));
 *
 *     // 更新和渲染由 System 自动处理
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleSystemComponent extends Component {

    /** 内部粒子系统 */
    private ParticleSystem particleSystem;

    /** 是否自动初始化 */
    private boolean autoInitialize = true;

    /** 是否自动同步位置 */
    private boolean syncPosition = true;

    /** 上一帧的位置（用于检测变化） */
    private float lastPosX, lastPosY, lastPosZ;

    // ==================== 构造函数 ====================

    /**
     * 创建空的粒子系统组件
     */
    public ParticleSystemComponent() {}

    /**
     * 创建带粒子系统的组件
     *
     * @param particleSystem 粒子系统
     */
    public ParticleSystemComponent(ParticleSystem particleSystem) {
        this.particleSystem = particleSystem;
    }

    /**
     * 创建带粒子系统的组件
     *
     * @param particleSystem 粒子系统
     * @param autoInitialize 是否自动初始化
     */
    public ParticleSystemComponent(ParticleSystem particleSystem, boolean autoInitialize) {
        this.particleSystem = particleSystem;
        this.autoInitialize = autoInitialize;
    }

    // ==================== 生命周期 ====================

    @Override
    public void onAttach(Entity entity) {
        super.onAttach(entity);

        if (particleSystem != null && autoInitialize && !particleSystem.isInitialized()) {
            particleSystem.initialize();
        }

        // 同步初始位置
        syncPositionFromTransform();
    }

    @Override
    public void onDetach() {
        cleanup();
        super.onDetach();
    }

    @Override
    protected void onEnable() {
        if (particleSystem != null) {
            particleSystem.play();
        }
    }

    @Override
    protected void onDisable() {
        if (particleSystem != null) {
            particleSystem.pause();
        }
    }

    // ==================== 粒子系统访问 ====================

    /**
     * 获取粒子系统
     *
     * @return 粒子系统
     */
    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }

    /**
     * 设置粒子系统
     *
     * @param particleSystem 粒子系统
     * @return this
     */
    public ParticleSystemComponent setParticleSystem(ParticleSystem particleSystem) {
        // 清理旧系统
        if (this.particleSystem != null && this.particleSystem != particleSystem) {
            this.particleSystem.cleanup();
        }

        this.particleSystem = particleSystem;

        // 如果已经附加且需要自动初始化
        if (entity != null && autoInitialize && particleSystem != null && !particleSystem.isInitialized()) {
            particleSystem.initialize();
        }

        return this;
    }

    /**
     * 是否自动初始化
     *
     * @return true 自动初始化
     */
    public boolean isAutoInitialize() {
        return autoInitialize;
    }

    /**
     * 设置是否自动初始化
     *
     * @param autoInitialize true 自动初始化
     * @return this
     */
    public ParticleSystemComponent setAutoInitialize(boolean autoInitialize) {
        this.autoInitialize = autoInitialize;
        return this;
    }

    /**
     * 是否自动同步位置
     *
     * @return true 自动同步
     */
    public boolean isSyncPosition() {
        return syncPosition;
    }

    /**
     * 设置是否自动同步位置
     *
     * @param syncPosition true 自动同步
     * @return this
     */
    public ParticleSystemComponent setSyncPosition(boolean syncPosition) {
        this.syncPosition = syncPosition;
        return this;
    }

    // ==================== 更新与渲染 ====================

    /**
     * 更新粒子系统
     *
     * @param deltaTime 时间增量（秒）
     */
    public void update(float deltaTime) {
        if (particleSystem == null || !enabled) {
            return;
        }

        // 同步位置
        if (syncPosition) {
            syncPositionFromTransform();
        }

        // 更新粒子系统
        particleSystem.update(deltaTime);
    }

    /**
     * 渲染粒子系统
     *
     * @param ctx 渲染上下文
     */
    public void render(RenderContext ctx) {
        if (particleSystem == null || !enabled) {
            return;
        }

        particleSystem.render(ctx.getViewMatrix(), ctx.getProjMatrix(), ctx.getCameraPos());
    }

    /**
     * 使用 Minecraft 相机渲染
     *
     * @param partialTicks 渲染插值
     */
    public void renderWithMinecraftCamera(float partialTicks) {
        if (particleSystem == null || !enabled) {
            return;
        }

        particleSystem.renderWithMinecraftCamera(partialTicks);
    }

    /**
     * 从变换组件同步位置
     */
    private void syncPositionFromTransform() {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null || particleSystem == null) {
            return;
        }

        float x = transform.getX();
        float y = transform.getY();
        float z = transform.getZ();

        // 检查位置是否变化
        if (x != lastPosX || y != lastPosY || z != lastPosZ) {
            particleSystem.setPosition(x, y, z);
            lastPosX = x;
            lastPosY = y;
            lastPosZ = z;
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 播放粒子系统
     *
     * @return this
     */
    public ParticleSystemComponent play() {
        if (particleSystem != null) {
            particleSystem.play();
        }
        return this;
    }

    /**
     * 暂停粒子系统
     *
     * @return this
     */
    public ParticleSystemComponent pause() {
        if (particleSystem != null) {
            particleSystem.pause();
        }
        return this;
    }

    /**
     * 停止粒子系统
     *
     * @return this
     */
    public ParticleSystemComponent stop() {
        if (particleSystem != null) {
            particleSystem.stop();
        }
        return this;
    }

    /**
     * 重置粒子系统
     * 注意：停止后重新播放
     *
     * @return this
     */
    public ParticleSystemComponent reset() {
        if (particleSystem != null) {
            particleSystem.stop();
            particleSystem.play();
        }
        return this;
    }

    /**
     * 判断是否正在播放
     *
     * @return true 正在播放
     */
    public boolean isPlaying() {
        return particleSystem != null && !particleSystem.isPaused();
    }

    /**
     * 判断是否应该渲染
     *
     * @return true 应该渲染
     */
    public boolean shouldRender() {
        return enabled && particleSystem != null && particleSystem.isInitialized();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (particleSystem != null) {
            particleSystem.cleanup();
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ParticleSystemComponent[initialized=%b, playing=%b, emitters=%d]",
            particleSystem != null && particleSystem.isInitialized(),
            isPlaying(),
            particleSystem != null ? particleSystem.getEmitterCount() : 0);
    }
}
