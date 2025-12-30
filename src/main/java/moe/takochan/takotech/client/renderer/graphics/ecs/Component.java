package moe.takochan.takotech.client.renderer.graphics.ecs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ECS 组件基类。
 * 组件是纯数据容器，不包含逻辑（逻辑由 System 处理）。
 *
 * <p>
 * 生命周期:
 * </p>
 * <ul>
 * <li>{@link #onAttach(Entity)} - 组件被添加到实体时调用</li>
 * <li>{@link #onDetach()} - 组件从实体移除时调用</li>
 * <li>{@link #onEnable()} - 组件被启用时调用</li>
 * <li>{@link #onDisable()} - 组件被禁用时调用</li>
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
 *     public class HealthComponent extends Component {
 * 
 *         public float maxHealth = 100;
 *         public float currentHealth = 100;
 *
 *         public void takeDamage(float damage) {
 *             currentHealth = Math.max(0, currentHealth - damage);
 *         }
 *     }
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public abstract class Component {

    /** 所属实体 */
    protected Entity entity;

    /** 是否启用 */
    protected boolean enabled = true;

    /**
     * 获取所属实体
     *
     * @return 实体，如果未附加则返回 null
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * 组件是否启用
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
    public Component setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
        return this;
    }

    /**
     * 组件是否处于活动状态（已附加到实体且启用）
     *
     * @return true 活动
     */
    public boolean isActive() {
        return entity != null && enabled && entity.isActiveInHierarchy();
    }

    // ==================== 生命周期 ====================

    /**
     * 组件被添加到实体时调用
     *
     * @param entity 所属实体
     */
    public void onAttach(Entity entity) {
        this.entity = entity;
    }

    /**
     * 组件从实体移除时调用
     */
    public void onDetach() {
        this.entity = null;
    }

    /**
     * 组件被启用时调用
     */
    protected void onEnable() {}

    /**
     * 组件被禁用时调用
     */
    protected void onDisable() {}

    // ==================== 辅助方法 ====================

    /**
     * 获取同一实体上的其他组件
     *
     * @param <T>  组件类型
     * @param type 组件类
     * @return 组件实例，如果不存在则返回 null
     */
    protected <T extends Component> T getComponent(Class<T> type) {
        return entity != null ? entity.getComponent(type) : null;
    }

    /**
     * 检查同一实体是否有指定组件
     *
     * @param type 组件类
     * @return true 存在
     */
    protected boolean hasComponent(Class<? extends Component> type) {
        return entity != null && entity.hasComponent(type);
    }

    /**
     * 获取父实体上的组件
     *
     * @param <T>  组件类型
     * @param type 组件类
     * @return 组件实例，如果不存在则返回 null
     */
    protected <T extends Component> T getComponentInParent(Class<T> type) {
        if (entity == null) return null;
        Entity parent = entity.getParent();
        return parent != null ? parent.getComponent(type) : null;
    }
}
