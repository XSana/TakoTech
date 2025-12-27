package moe.takochan.takotech.client.renderer.graphics.scene;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.camera.Frustum;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 场景类，管理场景图和渲染。
 */
@SideOnly(Side.CLIENT)
public class Scene {

    /** 场景名称 */
    private String name;

    /** 根节点 */
    private final SceneNode root;

    /** 主相机 */
    private Camera mainCamera;

    /** 可见渲染器列表（每帧重建） */
    private final List<MeshRenderer> visibleRenderers = new ArrayList<>();

    /** 统计信息 */
    private int totalNodes = 0;
    private int visibleNodes = 0;
    private int culledNodes = 0;

    // ==================== 构造函数 ====================

    public Scene() {
        this("Scene");
    }

    public Scene(String name) {
        this.name = name;
        this.root = new SceneNode("Root");
    }

    // ==================== 基本属性 ====================

    public String getName() {
        return name;
    }

    public Scene setName(String name) {
        this.name = name;
        return this;
    }

    public SceneNode getRoot() {
        return root;
    }

    // ==================== 相机 ====================

    public Camera getMainCamera() {
        return mainCamera;
    }

    public Scene setMainCamera(Camera camera) {
        this.mainCamera = camera;
        return this;
    }

    // ==================== 节点管理便捷方法 ====================

    /**
     * 添加节点到根节点
     */
    public Scene addNode(SceneNode node) {
        root.addChild(node);
        return this;
    }

    /**
     * 从根节点移除节点
     */
    public Scene removeNode(SceneNode node) {
        root.removeChild(node);
        return this;
    }

    /**
     * 按名称查找节点
     */
    public SceneNode findNode(String name) {
        return root.findByName(name);
    }

    /**
     * 按标签查找所有节点
     */
    public List<SceneNode> findNodesByTag(int tag) {
        return root.findAllByTag(tag);
    }

    // ==================== 更新 ====================

    /**
     * 更新场景（每帧调用）
     *
     * @param deltaTime 帧时间（秒）
     */
    public void update(float deltaTime) {
        root.update(deltaTime);
    }

    // ==================== 渲染 ====================

    /**
     * 收集可见渲染器（执行视锥体剔除）
     */
    public void collectVisibleRenderers() {
        visibleRenderers.clear();
        totalNodes = 0;
        visibleNodes = 0;
        culledNodes = 0;

        Frustum frustum = mainCamera != null ? mainCamera.getFrustum() : null;

        root.traverseActive(node -> {
            totalNodes++;

            // 视锥体剔除
            if (frustum != null) {
                AABB bounds = node.getWorldBounds();
                if (bounds.isValid() && !frustum.intersectsAABB(bounds)) {
                    culledNodes++;
                    return false; // 跳过子节点
                }
            }

            visibleNodes++;

            // 收集渲染器
            MeshRenderer renderer = node.getRenderer();
            if (renderer != null && renderer.shouldRender()) {
                visibleRenderers.add(renderer);
            }

            return true; // 继续遍历子节点
        });
    }

    /**
     * 渲染场景
     *
     * @param shader 使用的着色器程序
     */
    public void render(ShaderProgram shader) {
        if (mainCamera == null || shader == null) {
            return;
        }

        // 收集可见渲染器
        collectVisibleRenderers();

        // 设置相机矩阵
        shader.use();
        shader.setUniformMatrix4("uProjection", false, mainCamera.getProjectionMatrixBuffer());
        shader.setUniformMatrix4("uView", false, mainCamera.getViewMatrixBuffer());

        // 渲染所有可见对象
        for (MeshRenderer renderer : visibleRenderers) {
            renderer.render(shader);
        }

        ShaderProgram.unbind();
    }

    /**
     * 获取可见渲染器列表（在 collectVisibleRenderers 后有效）
     */
    public List<MeshRenderer> getVisibleRenderers() {
        return visibleRenderers;
    }

    /**
     * 按排序优先级排序渲染器
     */
    public void sortRenderers() {
        visibleRenderers.sort((a, b) -> Integer.compare(a.getSortingOrder(), b.getSortingOrder()));
    }

    /**
     * 渲染场景（带排序）
     */
    public void renderSorted(ShaderProgram shader) {
        if (mainCamera == null || shader == null) {
            return;
        }

        collectVisibleRenderers();
        sortRenderers();

        shader.use();
        shader.setUniformMatrix4("uProjection", false, mainCamera.getProjectionMatrixBuffer());
        shader.setUniformMatrix4("uView", false, mainCamera.getViewMatrixBuffer());

        for (MeshRenderer renderer : visibleRenderers) {
            renderer.render(shader);
        }

        ShaderProgram.unbind();
    }

    // ==================== 统计信息 ====================

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getVisibleNodes() {
        return visibleNodes;
    }

    public int getCulledNodes() {
        return culledNodes;
    }

    public int getVisibleRendererCount() {
        return visibleRenderers.size();
    }

    /**
     * 获取统计摘要
     */
    public String getStats() {
        return String.format(
            "Scene[%s]: nodes=%d, visible=%d, culled=%d, renderers=%d",
            name,
            totalNodes,
            visibleNodes,
            culledNodes,
            visibleRenderers.size());
    }

    // ==================== 清理 ====================

    /**
     * 清空场景
     */
    public void clear() {
        root.removeAllChildren();
        visibleRenderers.clear();
        totalNodes = visibleNodes = culledNodes = 0;
    }

    @Override
    public String toString() {
        return String.format("Scene[%s, camera=%s]", name, mainCamera != null ? "set" : "null");
    }
}
