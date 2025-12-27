package moe.takochan.takotech.client.renderer.graphics.scene;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;
import moe.takochan.takotech.client.renderer.graphics.material.Material;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 网格渲染器组件，将 Mesh 和 Material 绑定到 SceneNode。
 */
@SideOnly(Side.CLIENT)
public class MeshRenderer {

    /** 所属节点 */
    private SceneNode node;

    /** 网格 */
    private Mesh mesh;

    /** 材质 */
    private Material material;

    /** 本地空间包围盒 */
    private final AABB localBounds = new AABB();

    /** 是否可见 */
    private boolean visible = true;

    /** 是否投射阴影（预留） */
    private boolean castShadows = true;

    /** 是否接收阴影（预留） */
    private boolean receiveShadows = true;

    /** 渲染排序优先级（越小越先渲染） */
    private int sortingOrder = 0;

    /** 渲染层（位掩码） */
    private int renderLayer = 1;

    // ==================== 构造函数 ====================

    public MeshRenderer() {}

    public MeshRenderer(Mesh mesh) {
        setMesh(mesh);
    }

    public MeshRenderer(Mesh mesh, Material material) {
        setMesh(mesh);
        setMaterial(material);
    }

    // ==================== 节点关联 ====================

    /**
     * 获取所属节点
     */
    public SceneNode getNode() {
        return node;
    }

    /**
     * 设置所属节点（由 SceneNode 调用）
     */
    void setNode(SceneNode node) {
        this.node = node;
    }

    // ==================== Mesh ====================

    public Mesh getMesh() {
        return mesh;
    }

    public MeshRenderer setMesh(Mesh mesh) {
        this.mesh = mesh;
        updateLocalBounds();
        if (node != null) {
            node.invalidateBounds();
        }
        return this;
    }

    // ==================== Material ====================

    public Material getMaterial() {
        return material;
    }

    public MeshRenderer setMaterial(Material material) {
        this.material = material;
        return this;
    }

    // ==================== 包围盒 ====================

    /**
     * 获取本地空间包围盒
     */
    public AABB getLocalBounds() {
        return localBounds;
    }

    /**
     * 手动设置本地包围盒（当无法自动计算时）
     */
    public MeshRenderer setLocalBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        localBounds.set(minX, minY, minZ, maxX, maxY, maxZ);
        if (node != null) {
            node.invalidateBounds();
        }
        return this;
    }

    /**
     * 更新本地包围盒
     * 目前需要手动设置，未来可从 Mesh 顶点数据自动计算
     */
    private void updateLocalBounds() {
        // Mesh 类目前没有提供顶点数据访问，需要手动设置包围盒
        // 或者在子类/工厂方法中计算
    }

    // ==================== 可见性 ====================

    public boolean isVisible() {
        return visible;
    }

    public MeshRenderer setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * 判断是否应该渲染
     */
    public boolean shouldRender() {
        return visible && mesh != null && mesh.isValid();
    }

    // ==================== 阴影设置（预留） ====================

    public boolean isCastShadows() {
        return castShadows;
    }

    public MeshRenderer setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }

    public boolean isReceiveShadows() {
        return receiveShadows;
    }

    public MeshRenderer setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }

    // ==================== 排序 ====================

    public int getSortingOrder() {
        return sortingOrder;
    }

    public MeshRenderer setSortingOrder(int sortingOrder) {
        this.sortingOrder = sortingOrder;
        return this;
    }

    public int getRenderLayer() {
        return renderLayer;
    }

    public MeshRenderer setRenderLayer(int renderLayer) {
        this.renderLayer = renderLayer;
        return this;
    }

    // ==================== 渲染 ====================

    /**
     * 渲染网格
     *
     * @param shader 使用的着色器程序
     */
    public void render(ShaderProgram shader) {
        if (!shouldRender()) {
            return;
        }

        // 应用材质
        if (material != null && shader != null) {
            material.apply(shader);
        }

        // 设置模型矩阵
        if (node != null && shader != null) {
            shader.setUniformMatrix4(
                "uModel",
                false,
                moe.takochan.takotech.client.renderer.graphics.math.MathUtils.getMatrixBuffer(
                    node.getTransform()
                        .getWorldMatrix()));
        }

        // 绘制
        mesh.draw();
    }

    /**
     * 简单渲染（不设置 uniforms）
     */
    public void renderSimple() {
        if (shouldRender()) {
            mesh.draw();
        }
    }

    @Override
    public String toString() {
        return String.format(
            "MeshRenderer[mesh=%s, material=%s, visible=%b]",
            mesh != null ? "valid" : "null",
            material != null ? material.getRenderMode() : "null",
            visible);
    }
}
