package moe.takochan.takotech.client.renderer.graphics.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;

/**
 * 场景图节点，支持层级结构和组件系统。
 */
@SideOnly(Side.CLIENT)
public class SceneNode {

    /** 节点名称（调试用） */
    private String name;

    /** 变换组件 */
    private final Transform transform;

    /** 父节点 */
    private SceneNode parent;

    /** 子节点列表 */
    private final List<SceneNode> children = new ArrayList<>();

    /** 子节点的只读视图 */
    private final List<SceneNode> childrenView = Collections.unmodifiableList(children);

    /** 是否启用 */
    private boolean enabled = true;

    /** 是否在场景中可见（自身及所有父节点都启用） */
    private boolean activeInHierarchy = true;

    /** 世界空间包围盒（用于剔除） */
    private final AABB worldBounds = new AABB();

    /** 包围盒是否有效 */
    private boolean boundsDirty = true;

    /** 渲染器组件（可选） */
    private MeshRenderer renderer;

    /** 节点标签（用于分类和查找） */
    private int tag = 0;

    // ==================== 构造函数 ====================

    public SceneNode() {
        this.name = "Node";
        this.transform = new Transform();
    }

    public SceneNode(String name) {
        this.name = name;
        this.transform = new Transform();
    }

    // ==================== 基本属性 ====================

    public String getName() {
        return name;
    }

    public SceneNode setName(String name) {
        this.name = name;
        return this;
    }

    public Transform getTransform() {
        return transform;
    }

    public int getTag() {
        return tag;
    }

    public SceneNode setTag(int tag) {
        this.tag = tag;
        return this;
    }

    // ==================== 启用/禁用 ====================

    public boolean isEnabled() {
        return enabled;
    }

    public SceneNode setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            updateActiveInHierarchy();
        }
        return this;
    }

    public boolean isActiveInHierarchy() {
        return activeInHierarchy;
    }

    private void updateActiveInHierarchy() {
        boolean newActive = enabled && (parent == null || parent.isActiveInHierarchy());
        if (activeInHierarchy != newActive) {
            activeInHierarchy = newActive;
            // 递归更新子节点
            for (SceneNode child : children) {
                child.updateActiveInHierarchy();
            }
        }
    }

    // ==================== 层级管理 ====================

    public SceneNode getParent() {
        return parent;
    }

    /**
     * 获取子节点列表（只读）
     */
    public List<SceneNode> getChildren() {
        return childrenView;
    }

    /**
     * 获取子节点数量
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * 获取指定索引的子节点
     */
    public SceneNode getChild(int index) {
        return children.get(index);
    }

    /**
     * 添加子节点
     */
    public SceneNode addChild(SceneNode child) {
        if (child == null || child == this) {
            return this;
        }

        // 检查是否会形成循环
        if (isDescendantOf(child)) {
            throw new IllegalArgumentException("Cannot add ancestor as child (would create cycle)");
        }

        // 从原父节点移除
        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        child.parent = this;
        children.add(child);

        // 更新变换层级
        child.transform.setParent(this.transform);
        child.updateActiveInHierarchy();
        child.invalidateBounds();

        return this;
    }

    /**
     * 移除子节点
     */
    public SceneNode removeChild(SceneNode child) {
        if (child != null && children.remove(child)) {
            child.parent = null;
            child.transform.setParent(null);
            child.updateActiveInHierarchy();
        }
        return this;
    }

    /**
     * 移除所有子节点
     */
    public SceneNode removeAllChildren() {
        for (SceneNode child : children) {
            child.parent = null;
            child.transform.setParent(null);
            child.updateActiveInHierarchy();
        }
        children.clear();
        return this;
    }

    /**
     * 从父节点移除自身
     */
    public SceneNode removeFromParent() {
        if (parent != null) {
            parent.removeChild(this);
        }
        return this;
    }

    /**
     * 判断是否是指定节点的后代
     */
    public boolean isDescendantOf(SceneNode ancestor) {
        SceneNode node = parent;
        while (node != null) {
            if (node == ancestor) {
                return true;
            }
            node = node.parent;
        }
        return false;
    }

    /**
     * 判断是否是根节点
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 获取根节点
     */
    public SceneNode getRoot() {
        SceneNode node = this;
        while (node.parent != null) {
            node = node.parent;
        }
        return node;
    }

    // ==================== 渲染器组件 ====================

    public MeshRenderer getRenderer() {
        return renderer;
    }

    public SceneNode setRenderer(MeshRenderer renderer) {
        this.renderer = renderer;
        if (renderer != null) {
            renderer.setNode(this);
        }
        invalidateBounds();
        return this;
    }

    public boolean hasRenderer() {
        return renderer != null;
    }

    // ==================== 包围盒 ====================

    /**
     * 获取世界空间包围盒
     */
    public AABB getWorldBounds() {
        if (boundsDirty) {
            updateWorldBounds();
        }
        return worldBounds;
    }

    /**
     * 标记包围盒需要更新
     */
    public void invalidateBounds() {
        boundsDirty = true;
        // 向上传播
        if (parent != null) {
            parent.invalidateBounds();
        }
    }

    private void updateWorldBounds() {
        worldBounds.reset();

        // 如果有渲染器，使用其包围盒
        if (renderer != null) {
            AABB localBounds = renderer.getLocalBounds();
            if (localBounds != null && localBounds.isValid()) {
                // 将本地包围盒变换到世界空间
                // 简化处理：取 8 个顶点变换后重新计算 AABB
                transformAABBToWorld(localBounds, worldBounds);
            }
        }

        // 合并所有子节点的包围盒
        for (SceneNode child : children) {
            if (child.isActiveInHierarchy()) {
                AABB childBounds = child.getWorldBounds();
                if (childBounds.isValid()) {
                    worldBounds.expandToInclude(childBounds);
                }
            }
        }

        boundsDirty = false;
    }

    /**
     * 将本地 AABB 变换到世界空间
     */
    private void transformAABBToWorld(AABB local, AABB world) {
        org.lwjgl.util.vector.Matrix4f worldMatrix = transform.getWorldMatrix();

        // 变换 8 个顶点
        float[] corners = { local.minX, local.minY, local.minZ, local.maxX, local.minY, local.minZ, local.minX,
            local.maxY, local.minZ, local.maxX, local.maxY, local.minZ, local.minX, local.minY, local.maxZ, local.maxX,
            local.minY, local.maxZ, local.minX, local.maxY, local.maxZ, local.maxX, local.maxY, local.maxZ };

        for (int i = 0; i < 8; i++) {
            float x = corners[i * 3];
            float y = corners[i * 3 + 1];
            float z = corners[i * 3 + 2];

            // 手动矩阵变换
            float wx = worldMatrix.m00 * x + worldMatrix.m10 * y + worldMatrix.m20 * z + worldMatrix.m30;
            float wy = worldMatrix.m01 * x + worldMatrix.m11 * y + worldMatrix.m21 * z + worldMatrix.m31;
            float wz = worldMatrix.m02 * x + worldMatrix.m12 * y + worldMatrix.m22 * z + worldMatrix.m32;

            world.expandToInclude(wx, wy, wz);
        }
    }

    // ==================== 遍历 ====================

    /**
     * 遍历访问器接口
     */
    @FunctionalInterface
    public interface NodeVisitor {

        /**
         * 访问节点
         *
         * @param node 当前节点
         * @return true 继续遍历子节点，false 跳过子节点
         */
        boolean visit(SceneNode node);
    }

    /**
     * 深度优先遍历（前序）
     */
    public void traverse(NodeVisitor visitor) {
        if (visitor.visit(this)) {
            for (SceneNode child : children) {
                child.traverse(visitor);
            }
        }
    }

    /**
     * 仅遍历活动节点
     */
    public void traverseActive(NodeVisitor visitor) {
        if (!activeInHierarchy) {
            return;
        }
        if (visitor.visit(this)) {
            for (SceneNode child : children) {
                child.traverseActive(visitor);
            }
        }
    }

    // ==================== 查找 ====================

    /**
     * 按名称查找子节点（深度优先）
     */
    public SceneNode findByName(String name) {
        if (this.name.equals(name)) {
            return this;
        }
        for (SceneNode child : children) {
            SceneNode found = child.findByName(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 按标签查找所有节点
     */
    public List<SceneNode> findAllByTag(int tag) {
        List<SceneNode> result = new ArrayList<>();
        traverse(node -> {
            if (node.tag == tag) {
                result.add(node);
            }
            return true;
        });
        return result;
    }

    // ==================== 更新 ====================

    /**
     * 每帧更新（子类可重写）
     *
     * @param deltaTime 帧时间（秒）
     */
    public void update(float deltaTime) {
        // 默认实现：更新所有子节点
        for (SceneNode child : children) {
            if (child.isActiveInHierarchy()) {
                child.update(deltaTime);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("SceneNode[%s, children=%d, enabled=%b]", name, children.size(), enabled);
    }
}
