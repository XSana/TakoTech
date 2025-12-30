package moe.takochan.takotech.client.renderer.graphics.ecs;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ECS 系统基类。
 * 系统负责处理特定类型组件的逻辑。
 *
 * <p>
 * 系统特性:
 * </p>
 * <ul>
 * <li>按优先级排序执行</li>
 * <li>可查询带有特定组件的实体</li>
 * <li>支持启用/禁用</li>
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
 *     public class MovementSystem extends GameSystem {
 * 
 *         public MovementSystem() {
 *             super(100); // 优先级 100
 *         }
 *
 *         &#64;Override
 *         public void update(float deltaTime) {
 *             for (Entity entity : getEntitiesWith(TransformComponent.class, VelocityComponent.class)) {
 *                 TransformComponent transform = entity.getComponent(TransformComponent.class);
 *                 VelocityComponent velocity = entity.getComponent(VelocityComponent.class);
 *                 transform.translate(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public abstract class GameSystem implements Comparable<GameSystem> {

    /** 所属世界 */
    protected World world;

    /** 执行优先级（越小越先执行） */
    protected int priority;

    /** 是否启用 */
    protected boolean enabled = true;

    /**
     * 创建系统
     */
    public GameSystem() {
        this(0);
    }

    /**
     * 创建系统
     *
     * @param priority 执行优先级
     */
    public GameSystem(int priority) {
        this.priority = priority;
    }

    /**
     * 设置所属世界（由 World 内部调用）
     *
     * @param world 世界
     */
    void setWorld(World world) {
        this.world = world;
    }

    /**
     * 获取所属世界
     *
     * @return 世界
     */
    public World getWorld() {
        return world;
    }

    /**
     * 获取执行优先级
     *
     * @return 优先级
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 设置执行优先级
     *
     * @param priority 优先级
     */
    public void setPriority(int priority) {
        this.priority = priority;
        if (world != null) {
            world.sortSystems();
        }
    }

    /**
     * 是否启用
     *
     * @return true 启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置启用状态
     *
     * @param enabled true 启用
     * @return this
     */
    public GameSystem setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // ==================== 生命周期 ====================

    /**
     * 系统初始化时调用
     */
    public void onInit() {}

    /**
     * 系统被移除时调用
     */
    public void onDestroy() {}

    /**
     * 每帧更新
     *
     * @param deltaTime 时间增量（秒）
     */
    public abstract void update(float deltaTime);

    // ==================== 辅助方法 ====================

    /**
     * 查询带有指定组件的所有活动实体
     *
     * @param <T>           组件类型
     * @param componentType 组件类
     * @return 实体列表
     */
    protected <T extends Component> List<Entity> getEntitiesWith(Class<T> componentType) {
        return world != null ? world.getEntitiesWith(componentType) : java.util.Collections.emptyList();
    }

    /**
     * 查询带有多个指定组件的所有活动实体
     *
     * @param componentTypes 组件类数组
     * @return 实体列表
     */
    @SafeVarargs
    protected final List<Entity> getEntitiesWith(Class<? extends Component>... componentTypes) {
        return world != null ? world.getEntitiesWith(componentTypes) : java.util.Collections.emptyList();
    }

    /**
     * 获取所有活动实体
     *
     * @return 实体列表
     */
    protected List<Entity> getAllEntities() {
        return world != null ? world.getAllEntities() : java.util.Collections.emptyList();
    }

    @Override
    public int compareTo(GameSystem other) {
        return Integer.compare(this.priority, other.priority);
    }
}
