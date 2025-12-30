package moe.takochan.takotech.client.renderer.graphics.system;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.component.ParticleSystemComponent;
import moe.takochan.takotech.client.renderer.graphics.ecs.Entity;
import moe.takochan.takotech.client.renderer.graphics.ecs.GameSystem;

/**
 * 粒子更新系统。
 * 每帧更新所有 ParticleSystemComponent。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     World world = new World("GameWorld");
 *     world.registerSystem(new ParticleUpdateSystem());
 *
 *     // 创建粒子实体
 *     Entity fire = world.createEntity("Fire");
 *     fire.addComponent(new TransformComponent())
 *         .setPosition(0, 64, 0);
 *     fire.addComponent(new ParticleSystemComponent(ParticlePresets.createFire(1.0f)));
 *
 *     // 每帧更新（ParticleUpdateSystem 会自动更新所有粒子）
 *     world.update(deltaTime);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleUpdateSystem extends GameSystem {

    /** 默认优先级（在渲染系统之前执行） */
    public static final int DEFAULT_PRIORITY = 100;

    /**
     * 创建粒子更新系统
     */
    public ParticleUpdateSystem() {
        super(DEFAULT_PRIORITY);
    }

    /**
     * 创建粒子更新系统
     *
     * @param priority 执行优先级
     */
    public ParticleUpdateSystem(int priority) {
        super(priority);
    }

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getEntitiesWith(ParticleSystemComponent.class);

        for (Entity entity : entities) {
            ParticleSystemComponent ps = entity.getComponent(ParticleSystemComponent.class);
            if (ps != null && ps.isEnabled()) {
                ps.update(deltaTime);
            }
        }
    }

    @Override
    public void onDestroy() {
        // 清理所有粒子系统
        if (world != null) {
            List<Entity> entities = getEntitiesWith(ParticleSystemComponent.class);
            for (Entity entity : entities) {
                ParticleSystemComponent ps = entity.getComponent(ParticleSystemComponent.class);
                if (ps != null) {
                    ps.cleanup();
                }
            }
        }
    }
}
