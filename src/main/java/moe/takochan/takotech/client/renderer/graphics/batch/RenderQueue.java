package moe.takochan.takotech.client.renderer.graphics.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.material.Material;
import moe.takochan.takotech.client.renderer.graphics.scene.MeshRenderer;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 渲染队列，用于排序和分类渲染对象。
 *
 * <p>
 * 支持三种队列类型:
 * </p>
 * <ul>
 * <li>OPAQUE: 不透明物体，从前往后排序（减少 overdraw）</li>
 * <li>TRANSPARENT: 透明物体，从后往前排序（正确混合）</li>
 * <li>OVERLAY: UI/特效层，按 sortingOrder 排序</li>
 * </ul>
 *
 * <p>
 * 排序策略:
 * </p>
 * <ul>
 * <li>首先按 Shader ID 分组（减少 shader 切换）</li>
 * <li>然后按纹理 ID 分组（减少纹理切换）</li>
 * <li>最后按距离或 sortingOrder 排序</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class RenderQueue {

    /**
     * 队列类型
     */
    public enum QueueType {

        /** 不透明物体（从前往后） */
        OPAQUE(1000),
        /** 透明物体（从后往前） */
        TRANSPARENT(2000),
        /** UI/特效层 */
        OVERLAY(3000);

        private final int basePriority;

        QueueType(int basePriority) {
            this.basePriority = basePriority;
        }

        public int getBasePriority() {
            return basePriority;
        }
    }

    /**
     * 渲染项，包含渲染器和排序所需的缓存数据
     */
    public static class RenderItem {

        public final MeshRenderer renderer;
        public final int shaderId;
        public final int textureId;
        public final int sortingOrder;
        public float distanceSquared;

        public RenderItem(MeshRenderer renderer) {
            this.renderer = renderer;
            Material mat = renderer.getMaterial();
            this.shaderId = 0; // TODO: 从材质获取 shader ID
            this.textureId = mat != null ? mat.getTextureId() : 0;
            this.sortingOrder = renderer.getSortingOrder();
            this.distanceSquared = 0;
        }

        public void setDistanceSquared(float distSq) {
            this.distanceSquared = distSq;
        }
    }

    /** 各队列的渲染项列表 */
    private final Map<QueueType, List<RenderItem>> queues = new EnumMap<>(QueueType.class);

    /** 相机位置（用于距离排序） */
    private float cameraX, cameraY, cameraZ;

    /** 是否需要重新排序 */
    private boolean dirty = true;

    /** 统计信息 */
    private int totalItems = 0;
    private int opaqueCount = 0;
    private int transparentCount = 0;
    private int overlayCount = 0;

    public RenderQueue() {
        for (QueueType type : QueueType.values()) {
            queues.put(type, new ArrayList<>());
        }
    }

    // ==================== 相机设置 ====================

    /**
     * 设置相机位置（用于距离排序）
     */
    public void setCameraPosition(float x, float y, float z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
        dirty = true;
    }

    // ==================== 添加渲染项 ====================

    /**
     * 清空所有队列
     */
    public void clear() {
        for (List<RenderItem> queue : queues.values()) {
            queue.clear();
        }
        totalItems = opaqueCount = transparentCount = overlayCount = 0;
        dirty = true;
    }

    /**
     * 添加渲染器到适当的队列
     *
     * @param renderer  渲染器
     * @param queueType 队列类型
     */
    public void add(MeshRenderer renderer, QueueType queueType) {
        if (renderer == null || !renderer.shouldRender()) {
            return;
        }

        RenderItem item = new RenderItem(renderer);

        // 计算到相机的距离
        if (renderer.getNode() != null) {
            var transform = renderer.getNode()
                .getTransform();
            var worldMatrix = transform.getWorldMatrix();
            float dx = worldMatrix.m30 - cameraX;
            float dy = worldMatrix.m31 - cameraY;
            float dz = worldMatrix.m32 - cameraZ;
            item.setDistanceSquared(dx * dx + dy * dy + dz * dz);
        }

        queues.get(queueType)
            .add(item);
        totalItems++;

        switch (queueType) {
            case OPAQUE:
                opaqueCount++;
                break;
            case TRANSPARENT:
                transparentCount++;
                break;
            case OVERLAY:
                overlayCount++;
                break;
        }

        dirty = true;
    }

    /**
     * 自动分类添加渲染器
     * 根据材质的 alpha 值判断队列类型
     */
    public void addAuto(MeshRenderer renderer) {
        if (renderer == null || !renderer.shouldRender()) {
            return;
        }

        // 根据 sortingOrder 判断队列类型
        int order = renderer.getSortingOrder();
        QueueType type;

        if (order >= QueueType.OVERLAY.getBasePriority()) {
            type = QueueType.OVERLAY;
        } else if (order >= QueueType.TRANSPARENT.getBasePriority()) {
            type = QueueType.TRANSPARENT;
        } else {
            type = QueueType.OPAQUE;
        }

        add(renderer, type);
    }

    // ==================== 排序 ====================

    /**
     * 对所有队列进行排序
     */
    public void sort() {
        if (!dirty) {
            return;
        }

        // 不透明队列：Shader -> Texture -> 距离（从近到远）
        queues.get(QueueType.OPAQUE)
            .sort(OPAQUE_COMPARATOR);

        // 透明队列：距离（从远到近）-> Shader -> Texture
        queues.get(QueueType.TRANSPARENT)
            .sort(TRANSPARENT_COMPARATOR);

        // Overlay 队列：sortingOrder
        queues.get(QueueType.OVERLAY)
            .sort(OVERLAY_COMPARATOR);

        dirty = false;
    }

    /** 不透明物体比较器：Shader -> Texture -> 距离（近到远） */
    private static final Comparator<RenderItem> OPAQUE_COMPARATOR = (a, b) -> {
        // 首先按 shader 分组
        int cmp = Integer.compare(a.shaderId, b.shaderId);
        if (cmp != 0) return cmp;

        // 然后按纹理分组
        cmp = Integer.compare(a.textureId, b.textureId);
        if (cmp != 0) return cmp;

        // 最后按距离（近到远，减少 overdraw）
        return Float.compare(a.distanceSquared, b.distanceSquared);
    };

    /** 透明物体比较器：距离（远到近） */
    private static final Comparator<RenderItem> TRANSPARENT_COMPARATOR = (a, b) -> {
        // 首先按距离排序（远到近，正确的透明混合）
        int cmp = Float.compare(b.distanceSquared, a.distanceSquared);
        if (cmp != 0) return cmp;

        // 然后按 shader 分组
        cmp = Integer.compare(a.shaderId, b.shaderId);
        if (cmp != 0) return cmp;

        // 最后按纹理分组
        return Integer.compare(a.textureId, b.textureId);
    };

    /** Overlay 比较器：sortingOrder */
    private static final Comparator<RenderItem> OVERLAY_COMPARATOR = Comparator.comparingInt(item -> item.sortingOrder);

    // ==================== 渲染 ====================

    /**
     * 渲染指定队列
     *
     * @param queueType 队列类型
     * @param shader    使用的着色器
     */
    public void render(QueueType queueType, ShaderProgram shader) {
        if (dirty) {
            sort();
        }

        List<RenderItem> queue = queues.get(queueType);
        for (RenderItem item : queue) {
            item.renderer.render(shader);
        }
    }

    /**
     * 渲染所有队列（按顺序：OPAQUE -> TRANSPARENT -> OVERLAY）
     *
     * @param shader 使用的着色器
     */
    public void renderAll(ShaderProgram shader) {
        if (dirty) {
            sort();
        }

        // 渲染不透明物体
        for (RenderItem item : queues.get(QueueType.OPAQUE)) {
            item.renderer.render(shader);
        }

        // 渲染透明物体
        for (RenderItem item : queues.get(QueueType.TRANSPARENT)) {
            item.renderer.render(shader);
        }

        // 渲染 Overlay
        for (RenderItem item : queues.get(QueueType.OVERLAY)) {
            item.renderer.render(shader);
        }
    }

    /**
     * 获取指定队列的渲染项列表（只读）
     */
    public List<RenderItem> getQueue(QueueType queueType) {
        if (dirty) {
            sort();
        }
        return Collections.unmodifiableList(queues.get(queueType));
    }

    // ==================== 统计信息 ====================

    public int getTotalItems() {
        return totalItems;
    }

    public int getOpaqueCount() {
        return opaqueCount;
    }

    public int getTransparentCount() {
        return transparentCount;
    }

    public int getOverlayCount() {
        return overlayCount;
    }

    /**
     * 获取统计摘要
     */
    public String getStats() {
        return String.format(
            "RenderQueue[total=%d, opaque=%d, transparent=%d, overlay=%d]",
            totalItems,
            opaqueCount,
            transparentCount,
            overlayCount);
    }
}
