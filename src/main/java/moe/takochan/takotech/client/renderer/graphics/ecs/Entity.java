package moe.takochan.takotech.client.renderer.graphics.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ECS 实体类。
 * 实体是组件的容器，本身不包含数据或逻辑。
 *
 * <p>
 * 特性:
 * </p>
 * <ul>
 * <li>唯一 ID 标识</li>
 * <li>层级结构（父子关系）</li>
 * <li>启用状态级联传播</li>
 * <li>组件的动态添加/移除</li>
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
 *     Entity player = world.createEntity("Player");
 *     player.addComponent(new TransformComponent());
 *     player.addComponent(new MeshRendererComponent());
 *
 *     TransformComponent transform = player.getComponent(TransformComponent.class);
 *     transform.setPosition(0, 64, 0);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class Entity {

    /** ID 生成器 */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    /** 实体唯一 ID */
    private final int id;

    /** 实体名称 */
    private String name;

    /** 所属世界 */
    private World world;

    /** 是否启用 */
    private boolean enabled = true;

    /** 是否在层级中活动 */
    private boolean activeInHierarchy = true;

    /** 标签 */
    private int tag = 0;

    /** 父实体 */
    private Entity parent;

    /** 子实体列表 */
    private final List<Entity> children = new ArrayList<>();

    /** 子实体只读视图 */
    private final List<Entity> childrenView = Collections.unmodifiableList(children);

    /** 组件映射 */
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    /**
     * 创建实体
     *
     * @param name 实体名称
     */
    public Entity(String name) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name != null ? name : "Entity_" + id;
    }

    /**
     * 创建实体
     */
    public Entity() {
        this(null);
    }

    // ==================== 基本属性 ====================

    /**
     * 获取实体 ID
     *
     * @return 唯一 ID
     */
    public int getId() {
        return id;
    }

    /**
     * 获取实体名称
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置实体名称
     *
     * @param name 名称
     * @return this
     */
    public Entity setName(String name) {
        this.name = name;
        return this;
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
     * 设置所属世界（由 World 内部调用）
     *
     * @param world 世界
     */
    void setWorld(World world) {
        this.world = world;
    }

    /**
     * 获取标签
     *
     * @return 标签值
     */
    public int getTag() {
        return tag;
    }

    /**
     * 设置标签
     *
     * @param tag 标签值
     * @return this
     */
    public Entity setTag(int tag) {
        this.tag = tag;
        return this;
    }

    // ==================== 启用状态 ====================

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
    public Entity setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            updateActiveInHierarchy();
        }
        return this;
    }

    /**
     * 是否在层级中活动
     *
     * @return true 活动
     */
    public boolean isActiveInHierarchy() {
        return activeInHierarchy;
    }

    /**
     * 更新层级活动状态
     */
    private void updateActiveInHierarchy() {
        boolean newActive = enabled && (parent == null || parent.isActiveInHierarchy());
        if (this.activeInHierarchy != newActive) {
            this.activeInHierarchy = newActive;
            // 通知组件
            for (Component component : components.values()) {
                if (component.isEnabled()) {
                    if (newActive) {
                        component.onEnable();
                    } else {
                        component.onDisable();
                    }
                }
            }
            // 递归更新子实体
            for (Entity child : children) {
                child.updateActiveInHierarchy();
            }
        }
    }

    // ==================== 层级管理 ====================

    /**
     * 获取父实体
     *
     * @return 父实体，如果没有则返回 null
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * 设置父实体
     *
     * @param newParent 新父实体，null 表示解除父子关系
     * @return this
     */
    public Entity setParent(Entity newParent) {
        if (this.parent == newParent) {
            return this;
        }

        // 防止循环引用
        if (newParent != null && isAncestorOf(newParent)) {
            throw new IllegalArgumentException("Cannot set parent: would create circular reference");
        }

        // 从旧父节点移除
        if (this.parent != null) {
            this.parent.children.remove(this);
        }

        this.parent = newParent;

        // 添加到新父节点
        if (newParent != null) {
            newParent.children.add(this);
        }

        updateActiveInHierarchy();
        return this;
    }

    /**
     * 添加子实体
     *
     * @param child 子实体
     * @return this
     */
    public Entity addChild(Entity child) {
        if (child != null && child != this) {
            child.setParent(this);
        }
        return this;
    }

    /**
     * 移除子实体
     *
     * @param child 子实体
     * @return this
     */
    public Entity removeChild(Entity child) {
        if (child != null && child.parent == this) {
            child.setParent(null);
        }
        return this;
    }

    /**
     * 获取子实体列表（只读）
     *
     * @return 子实体列表
     */
    public List<Entity> getChildren() {
        return childrenView;
    }

    /**
     * 获取子实体数量
     *
     * @return 数量
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * 检查是否是指定实体的祖先
     *
     * @param entity 实体
     * @return true 是祖先
     */
    public boolean isAncestorOf(Entity entity) {
        Entity current = entity;
        while (current != null) {
            if (current == this) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    // ==================== 组件管理 ====================

    /**
     * 添加组件
     *
     * @param <T>       组件类型
     * @param component 组件实例
     * @return 添加的组件
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T addComponent(T component) {
        if (component == null) {
            return null;
        }

        Class<? extends Component> type = component.getClass();

        // 检查是否已存在同类型组件
        if (components.containsKey(type)) {
            throw new IllegalStateException("Entity already has component of type: " + type.getSimpleName());
        }

        components.put(type, component);
        component.onAttach(this);

        if (activeInHierarchy && component.isEnabled()) {
            component.onEnable();
        }

        return component;
    }

    /**
     * 获取组件
     *
     * @param <T>  组件类型
     * @param type 组件类
     * @return 组件实例，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    /**
     * 获取或添加组件
     *
     * @param <T>  组件类型
     * @param type 组件类
     * @return 组件实例
     */
    public <T extends Component> T getOrAddComponent(Class<T> type) {
        T component = getComponent(type);
        if (component == null) {
            try {
                component = type.newInstance();
                addComponent(component);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create component: " + type.getSimpleName(), e);
            }
        }
        return component;
    }

    /**
     * 检查是否有指定组件
     *
     * @param type 组件类
     * @return true 存在
     */
    public boolean hasComponent(Class<? extends Component> type) {
        return components.containsKey(type);
    }

    /**
     * 移除组件
     *
     * @param <T>  组件类型
     * @param type 组件类
     * @return 移除的组件，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T removeComponent(Class<T> type) {
        Component component = components.remove(type);
        if (component != null) {
            if (activeInHierarchy && component.isEnabled()) {
                component.onDisable();
            }
            component.onDetach();
        }
        return (T) component;
    }

    /**
     * 获取所有组件
     *
     * @return 组件列表
     */
    public List<Component> getAllComponents() {
        return new ArrayList<>(components.values());
    }

    /**
     * 获取组件数量
     *
     * @return 数量
     */
    public int getComponentCount() {
        return components.size();
    }

    // ==================== 查找 ====================

    /**
     * 按名称查找子实体（深度优先）
     *
     * @param name 名称
     * @return 实体，如果不存在则返回 null
     */
    public Entity findByName(String name) {
        for (Entity child : children) {
            if (child.name.equals(name)) {
                return child;
            }
            Entity found = child.findByName(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 按标签查找所有子实体
     *
     * @param tag 标签
     * @return 实体列表
     */
    public List<Entity> findAllByTag(int tag) {
        List<Entity> result = new ArrayList<>();
        findAllByTagRecursive(tag, result);
        return result;
    }

    private void findAllByTagRecursive(int tag, List<Entity> result) {
        for (Entity child : children) {
            if (child.tag == tag) {
                result.add(child);
            }
            child.findAllByTagRecursive(tag, result);
        }
    }

    // ==================== 清理 ====================

    /**
     * 销毁实体及其所有子实体和组件
     */
    public void destroy() {
        // 先销毁子实体
        for (Entity child : new ArrayList<>(children)) {
            child.destroy();
        }
        children.clear();

        // 移除所有组件
        for (Component component : new ArrayList<>(components.values())) {
            if (activeInHierarchy && component.isEnabled()) {
                component.onDisable();
            }
            component.onDetach();
        }
        components.clear();

        // 从父节点移除
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }

        // 从世界移除
        if (world != null) {
            world.removeEntity(this);
            world = null;
        }
    }

    @Override
    public String toString() {
        return "Entity{id=" + id
            + ", name='"
            + name
            + "', components="
            + components.size()
            + ", children="
            + children.size()
            + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
