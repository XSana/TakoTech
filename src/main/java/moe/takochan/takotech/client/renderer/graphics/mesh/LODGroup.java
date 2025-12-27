package moe.takochan.takotech.client.renderer.graphics.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * LOD (Level of Detail) 组，根据距离选择不同细节级别的 Mesh。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     LODGroup lod = new LODGroup();
 *     lod.addLevel(highDetailMesh, 0, 10); // 0-10 单位距离使用高精度
 *     lod.addLevel(mediumDetailMesh, 10, 50); // 10-50 使用中等精度
 *     lod.addLevel(lowDetailMesh, 50, 200); // 50-200 使用低精度
 *     // 200+ 距离时返回 null（完全剔除）
 *
 *     // 每帧选择
 *     float distSq = calculateDistanceSquared(camera, object);
 *     Mesh mesh = lod.selectLOD(distSq);
 *     if (mesh != null) {
 *         mesh.draw();
 *     }
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class LODGroup {

    /**
     * LOD 级别
     */
    public static class LODLevel {

        /** 此级别使用的 Mesh */
        public final Mesh mesh;

        /** 最小距离（平方） */
        public final float minDistanceSquared;

        /** 最大距离（平方） */
        public final float maxDistanceSquared;

        /** 屏幕覆盖率阈值（可选，0 表示不使用） */
        public float screenCoverageThreshold = 0;

        public LODLevel(Mesh mesh, float minDistance, float maxDistance) {
            this.mesh = mesh;
            this.minDistanceSquared = minDistance * minDistance;
            this.maxDistanceSquared = maxDistance * maxDistance;
        }

        /**
         * 检查指定距离（平方）是否在此级别范围内
         */
        public boolean isInRange(float distanceSquared) {
            return distanceSquared >= minDistanceSquared && distanceSquared < maxDistanceSquared;
        }
    }

    /** LOD 级别列表（按距离排序） */
    private final List<LODLevel> levels = new ArrayList<>();

    /** 是否启用 LOD */
    private boolean enabled = true;

    /** LOD 偏移（正值增加细节，负值减少细节） */
    private float lodBias = 0;

    /** 强制使用的 LOD 级别（-1 表示自动） */
    private int forcedLevel = -1;

    /** 最大剔除距离（平方），超过此距离完全剔除 */
    private float maxCullDistanceSquared = Float.MAX_VALUE;

    /** 当前选择的 LOD 级别（用于调试） */
    private int currentLevel = -1;

    public LODGroup() {}

    // ==================== 配置方法 ====================

    /**
     * 添加 LOD 级别
     *
     * @param mesh        此级别使用的网格
     * @param minDistance 最小距离
     * @param maxDistance 最大距离
     * @return this（链式调用）
     */
    public LODGroup addLevel(Mesh mesh, float minDistance, float maxDistance) {
        levels.add(new LODLevel(mesh, minDistance, maxDistance));
        // 按最小距离排序
        levels.sort((a, b) -> Float.compare(a.minDistanceSquared, b.minDistanceSquared));
        return this;
    }

    /**
     * 添加 LOD 级别（使用 LODLevel 对象）
     */
    public LODGroup addLevel(LODLevel level) {
        levels.add(level);
        levels.sort((a, b) -> Float.compare(a.minDistanceSquared, b.minDistanceSquared));
        return this;
    }

    /**
     * 清除所有 LOD 级别
     */
    public LODGroup clearLevels() {
        levels.clear();
        return this;
    }

    /**
     * 设置是否启用 LOD
     */
    public LODGroup setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * 设置 LOD 偏移
     *
     * @param bias 偏移值（正值=更高细节，负值=更低细节）
     */
    public LODGroup setLODBias(float bias) {
        this.lodBias = bias;
        return this;
    }

    /**
     * 强制使用指定的 LOD 级别
     *
     * @param level 级别索引，-1 表示自动
     */
    public LODGroup setForcedLevel(int level) {
        this.forcedLevel = level;
        return this;
    }

    /**
     * 设置最大剔除距离
     *
     * @param distance 最大距离，超过此距离完全剔除
     */
    public LODGroup setMaxCullDistance(float distance) {
        this.maxCullDistanceSquared = distance * distance;
        return this;
    }

    // ==================== LOD 选择 ====================

    /**
     * 根据距离（平方）选择 LOD
     *
     * @param distanceSquared 到相机的距离平方
     * @return 选择的 Mesh，如果应该剔除则返回 null
     */
    public Mesh selectLOD(float distanceSquared) {
        currentLevel = -1;

        if (!enabled || levels.isEmpty()) {
            return levels.isEmpty() ? null : levels.get(0).mesh;
        }

        // 检查是否超过最大剔除距离
        if (distanceSquared >= maxCullDistanceSquared) {
            return null;
        }

        // 强制级别
        if (forcedLevel >= 0 && forcedLevel < levels.size()) {
            currentLevel = forcedLevel;
            return levels.get(forcedLevel).mesh;
        }

        // 应用 LOD 偏移
        float adjustedDistSq = distanceSquared;
        if (lodBias != 0) {
            // 偏移会影响有效距离
            // 正偏移 = 减小有效距离 = 更高细节
            // 负偏移 = 增大有效距离 = 更低细节
            float factor = 1.0f - lodBias * 0.1f;
            if (factor > 0) {
                adjustedDistSq = distanceSquared * factor * factor;
            }
        }

        // 选择合适的级别
        for (int i = 0; i < levels.size(); i++) {
            LODLevel level = levels.get(i);
            if (level.isInRange(adjustedDistSq)) {
                currentLevel = i;
                return level.mesh;
            }
        }

        // 如果超出所有级别范围，检查是否在最后一个级别的最大距离内
        LODLevel lastLevel = levels.get(levels.size() - 1);
        if (adjustedDistSq < lastLevel.maxDistanceSquared) {
            currentLevel = levels.size() - 1;
            return lastLevel.mesh;
        }

        // 完全剔除
        return null;
    }

    /**
     * 根据距离选择 LOD（非平方版本）
     *
     * @param distance 到相机的距离
     * @return 选择的 Mesh，如果应该剔除则返回 null
     */
    public Mesh selectLODByDistance(float distance) {
        return selectLOD(distance * distance);
    }

    /**
     * 根据屏幕覆盖率选择 LOD
     *
     * @param screenCoverage 物体在屏幕上的覆盖率 (0-1)
     * @return 选择的 Mesh
     */
    public Mesh selectLODByScreenCoverage(float screenCoverage) {
        currentLevel = -1;

        if (!enabled || levels.isEmpty()) {
            return levels.isEmpty() ? null : levels.get(0).mesh;
        }

        // 强制级别
        if (forcedLevel >= 0 && forcedLevel < levels.size()) {
            currentLevel = forcedLevel;
            return levels.get(forcedLevel).mesh;
        }

        // 根据屏幕覆盖率选择（覆盖率越高=越详细）
        for (int i = 0; i < levels.size(); i++) {
            LODLevel level = levels.get(i);
            if (level.screenCoverageThreshold > 0 && screenCoverage >= level.screenCoverageThreshold) {
                currentLevel = i;
                return level.mesh;
            }
        }

        // 默认返回最低细节
        currentLevel = levels.size() - 1;
        return levels.get(levels.size() - 1).mesh;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取 LOD 级别数量
     */
    public int getLevelCount() {
        return levels.size();
    }

    /**
     * 获取指定级别
     */
    public LODLevel getLevel(int index) {
        return levels.get(index);
    }

    /**
     * 获取所有级别（只读）
     */
    public List<LODLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    /**
     * 获取当前选择的级别索引
     *
     * @return 级别索引，-1 表示剔除或未选择
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取 LOD 偏移
     */
    public float getLODBias() {
        return lodBias;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建简单的 3 级 LOD
     *
     * @param high         高细节 Mesh
     * @param medium       中等细节 Mesh
     * @param low          低细节 Mesh
     * @param nearDistance 近距离阈值
     * @param farDistance  远距离阈值
     * @param cullDistance 剔除距离
     * @return LODGroup
     */
    public static LODGroup createSimple(Mesh high, Mesh medium, Mesh low, float nearDistance, float farDistance,
        float cullDistance) {

        return new LODGroup().addLevel(high, 0, nearDistance)
            .addLevel(medium, nearDistance, farDistance)
            .addLevel(low, farDistance, cullDistance)
            .setMaxCullDistance(cullDistance);
    }

    /**
     * 创建 2 级 LOD（高/低）
     */
    public static LODGroup createSimple(Mesh high, Mesh low, float threshold, float cullDistance) {
        return new LODGroup().addLevel(high, 0, threshold)
            .addLevel(low, threshold, cullDistance)
            .setMaxCullDistance(cullDistance);
    }

    @Override
    public String toString() {
        return String.format("LODGroup[levels=%d, enabled=%b, currentLevel=%d]", levels.size(), enabled, currentLevel);
    }
}
