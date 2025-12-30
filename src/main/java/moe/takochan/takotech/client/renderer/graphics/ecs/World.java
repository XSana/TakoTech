package moe.takochan.takotech.client.renderer.graphics.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;

/**
 * ECS 世界容器。
 * 管理所有实体和系统的生命周期。
 *
 * <p>
 * 功能:
 * </p>
 * <ul>
 * <li>创建和销毁实体</li>
 * <li>注册和管理系统</li>
 * <li>按组件类型查询实体</li>
 * <li>每帧更新所有系统</li>
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
 *     // 创建世界
 *     World world = new World("GameWorld");
 *
 *     // 注册系统
 *     world.registerSystem(new PhysicsSystem());
 *     world.registerSystem(new RenderSystem());
 *
 *     // 创建实体
 *     Entity player = world.createEntity("Player");
 *     player.addComponent(new TransformComponent());
 *
 *     // 每帧更新
 *     world.update(deltaTime);
 *
 *     // 清理
 *     world.destroy();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class World {

    /** 世界名称 */
    private final String name;

    /** 实体映射 (ID -> Entity) */
    private final Map<Integer, Entity> entities = new HashMap<>();

    /** 系统列表（按优先级排序） */
    private final List<GameSystem> systems = new ArrayList<>();

    /** 系统是否需要重新排序 */
    private boolean systemsDirty = false;

    /** 待添加的实体（延迟添加） */
    private final List<Entity> pendingAdd = new ArrayList<>();

    /** 待移除的实体（延迟移除） */
    private final List<Entity> pendingRemove = new ArrayList<>();

    /** 是否正在更新 */
    private boolean updating = false;

    /**
     * 创建世界
     *
     * @param name 世界名称
     */
    public World(String name) {
        this.name = name != null ? name : "World";
    }

    /**
     * 创建世界
     */
    public World() {
        this("World");
    }

    /**
     * 获取世界名称
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    // ==================== 实体管理 ====================

    /**
     * 创建新实体
     *
     * @param name 实体名称
     * @return 新实体
     */
    public Entity createEntity(String name) {
        Entity entity = new Entity(name);
        addEntity(entity);
        return entity;
    }

    /**
     * 创建新实体
     *
     * @return 新实体
     */
    public Entity createEntity() {
        return createEntity(null);
    }

    /**
     * 添加实体到世界
     *
     * @param entity 实体
     */
    public void addEntity(Entity entity) {
        if (entity == null || entity.getWorld() == this) {
            return;
        }

        // 如果已在其他世界，先移除
        if (entity.getWorld() != null) {
            entity.getWorld()
                .removeEntity(entity);
        }

        if (updating) {
            // 延迟添加
            pendingAdd.add(entity);
        } else {
            entities.put(entity.getId(), entity);
            entity.setWorld(this);
        }
    }

    /**
     * 从世界移除实体
     *
     * @param entity 实体
     */
    public void removeEntity(Entity entity) {
        if (entity == null || entity.getWorld() != this) {
            return;
        }

        if (updating) {
            // 延迟移除
            pendingRemove.add(entity);
        } else {
            entities.remove(entity.getId());
            entity.setWorld(null);
        }
    }

    /**
     * 通过 ID 获取实体
     *
     * @param id 实体 ID
     * @return 实体，如果不存在则返回 null
     */
    public Entity getEntity(int id) {
        return entities.get(id);
    }

    /**
     * 通过名称获取实体（第一个匹配）
     *
     * @param name 实体名称
     * @return 实体，如果不存在则返回 null
     */
    public Entity getEntityByName(String name) {
        for (Entity entity : entities.values()) {
            if (entity.getName()
                .equals(name)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 获取所有实体
     *
     * @return 实体列表
     */
    public List<Entity> getAllEntities() {
        return new ArrayList<>(entities.values());
    }

    /**
     * 获取实体数量
     *
     * @return 数量
     */
    public int getEntityCount() {
        return entities.size();
    }

    // ==================== 组件查询 ====================

    /**
     * 查询带有指定组件的所有活动实体
     *
     * @param <T>           组件类型
     * @param componentType 组件类
     * @return 实体列表
     */
    public <T extends Component> List<Entity> getEntitiesWith(Class<T> componentType) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (entity.isActiveInHierarchy() && entity.hasComponent(componentType)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 查询带有多个指定组件的所有活动实体
     *
     * @param componentTypes 组件类数组
     * @return 实体列表
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWith(Class<? extends Component>... componentTypes) {
        if (componentTypes == null || componentTypes.length == 0) {
            return Collections.emptyList();
        }

        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (!entity.isActiveInHierarchy()) {
                continue;
            }

            boolean hasAll = true;
            for (Class<? extends Component> type : componentTypes) {
                if (!entity.hasComponent(type)) {
                    hasAll = false;
                    break;
                }
            }

            if (hasAll) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 查询带有指定标签的所有活动实体
     *
     * @param tag 标签
     * @return 实体列表
     */
    public List<Entity> getEntitiesByTag(int tag) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (entity.isActiveInHierarchy() && entity.getTag() == tag) {
                result.add(entity);
            }
        }
        return result;
    }

    // ==================== 系统管理 ====================

    /**
     * 注册系统
     *
     * @param <T>    系统类型
     * @param system 系统实例
     * @return 系统实例
     */
    public <T extends GameSystem> T registerSystem(T system) {
        if (system == null) {
            return null;
        }

        // 检查是否已注册同类型系统
        for (GameSystem s : systems) {
            if (s.getClass() == system.getClass()) {
                TakoTechMod.LOG.warn(
                    "World: System {} already registered",
                    system.getClass()
                        .getSimpleName());
                return null;
            }
        }

        systems.add(system);
        system.setWorld(this);
        systemsDirty = true;

        system.onInit();

        TakoTechMod.LOG.info(
            "World: Registered system {} with priority {}",
            system.getClass()
                .getSimpleName(),
            system.getPriority());

        return system;
    }

    /**
     * 移除系统
     *
     * @param systemClass 系统类
     */
    public void unregisterSystem(Class<? extends GameSystem> systemClass) {
        GameSystem toRemove = null;
        for (GameSystem system : systems) {
            if (system.getClass() == systemClass) {
                toRemove = system;
                break;
            }
        }

        if (toRemove != null) {
            toRemove.onDestroy();
            toRemove.setWorld(null);
            systems.remove(toRemove);
        }
    }

    /**
     * 获取系统
     *
     * @param <T>         系统类型
     * @param systemClass 系统类
     * @return 系统实例，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T extends GameSystem> T getSystem(Class<T> systemClass) {
        for (GameSystem system : systems) {
            if (system.getClass() == systemClass) {
                return (T) system;
            }
        }
        return null;
    }

    /**
     * 获取所有系统
     *
     * @return 系统列表
     */
    public List<GameSystem> getAllSystems() {
        return new ArrayList<>(systems);
    }

    /**
     * 对系统进行排序（内部使用）
     */
    void sortSystems() {
        systemsDirty = true;
    }

    // ==================== 更新 ====================

    /**
     * 更新世界（每帧调用）
     *
     * @param deltaTime 时间增量（秒）
     */
    public void update(float deltaTime) {
        // 排序系统
        if (systemsDirty) {
            Collections.sort(systems);
            systemsDirty = false;
        }

        // 处理延迟添加/移除
        processPending();

        // 更新所有启用的系统
        updating = true;
        try {
            for (GameSystem system : systems) {
                if (system.isEnabled()) {
                    try {
                        system.update(deltaTime);
                    } catch (Exception e) {
                        TakoTechMod.LOG.error(
                            "World: Error updating system {}",
                            system.getClass()
                                .getSimpleName(),
                            e);
                    }
                }
            }
        } finally {
            updating = false;
        }

        // 处理更新期间产生的延迟操作
        processPending();
    }

    /**
     * 处理延迟添加/移除的实体
     */
    private void processPending() {
        // 处理待添加
        if (!pendingAdd.isEmpty()) {
            for (Entity entity : pendingAdd) {
                entities.put(entity.getId(), entity);
                entity.setWorld(this);
            }
            pendingAdd.clear();
        }

        // 处理待移除
        if (!pendingRemove.isEmpty()) {
            for (Entity entity : pendingRemove) {
                entities.remove(entity.getId());
                entity.setWorld(null);
            }
            pendingRemove.clear();
        }
    }

    // ==================== 清理 ====================

    /**
     * 销毁世界及其所有实体和系统
     */
    public void destroy() {
        // 销毁所有系统
        for (GameSystem system : new ArrayList<>(systems)) {
            try {
                system.onDestroy();
            } catch (Exception e) {
                TakoTechMod.LOG.error(
                    "World: Error destroying system {}",
                    system.getClass()
                        .getSimpleName(),
                    e);
            }
            system.setWorld(null);
        }
        systems.clear();

        // 销毁所有实体
        for (Entity entity : new ArrayList<>(entities.values())) {
            entity.destroy();
        }
        entities.clear();

        pendingAdd.clear();
        pendingRemove.clear();

        TakoTechMod.LOG.info("World: {} destroyed", name);
    }

    @Override
    public String toString() {
        return "World{name='" + name + "', entities=" + entities.size() + ", systems=" + systems.size() + "}";
    }
}
