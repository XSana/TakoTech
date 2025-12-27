package moe.takochan.takotech.client.renderer.graphics.culling;

import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 包围球，用于快速碰撞检测和视锥体剔除。
 * 比 AABB 更简单，但对于非球形物体精度较低。
 */
@SideOnly(Side.CLIENT)
public class BoundingSphere {

    /** 球心 */
    public float centerX, centerY, centerZ;

    /** 半径 */
    public float radius;

    public BoundingSphere() {
        this.radius = 0;
    }

    public BoundingSphere(float centerX, float centerY, float centerZ, float radius) {
        set(centerX, centerY, centerZ, radius);
    }

    public BoundingSphere(Vector3f center, float radius) {
        set(center.x, center.y, center.z, radius);
    }

    /**
     * 从 AABB 创建包围球
     */
    public static BoundingSphere fromAABB(AABB aabb) {
        BoundingSphere sphere = new BoundingSphere();
        sphere.centerX = aabb.getCenterX();
        sphere.centerY = aabb.getCenterY();
        sphere.centerZ = aabb.getCenterZ();
        sphere.radius = aabb.getBoundingSphereRadius();
        return sphere;
    }

    // ==================== 设置方法 ====================

    public BoundingSphere set(float centerX, float centerY, float centerZ, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        return this;
    }

    public BoundingSphere set(Vector3f center, float radius) {
        return set(center.x, center.y, center.z, radius);
    }

    public BoundingSphere set(BoundingSphere other) {
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.centerZ = other.centerZ;
        this.radius = other.radius;
        return this;
    }

    public BoundingSphere setCenter(float x, float y, float z) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        return this;
    }

    public BoundingSphere setCenter(Vector3f center) {
        return setCenter(center.x, center.y, center.z);
    }

    public BoundingSphere setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取球心
     */
    public Vector3f getCenter(Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        dest.set(centerX, centerY, centerZ);
        return dest;
    }

    /**
     * 获取直径
     */
    public float getDiameter() {
        return radius * 2.0f;
    }

    /**
     * 获取体积
     */
    public float getVolume() {
        return (4.0f / 3.0f) * (float) Math.PI * radius * radius * radius;
    }

    /**
     * 获取表面积
     */
    public float getSurfaceArea() {
        return 4.0f * (float) Math.PI * radius * radius;
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return radius > 0;
    }

    // ==================== 变换方法 ====================

    /**
     * 平移球心
     */
    public BoundingSphere translate(float dx, float dy, float dz) {
        centerX += dx;
        centerY += dy;
        centerZ += dz;
        return this;
    }

    /**
     * 均匀缩放
     */
    public BoundingSphere scale(float factor) {
        radius *= factor;
        return this;
    }

    /**
     * 扩展半径以包含指定点
     */
    public BoundingSphere expandToInclude(float x, float y, float z) {
        float dx = x - centerX;
        float dy = y - centerY;
        float dz = z - centerZ;
        float distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > radius * radius) {
            radius = (float) Math.sqrt(distSq);
        }
        return this;
    }

    /**
     * 扩展以包含另一个球
     */
    public BoundingSphere expandToInclude(BoundingSphere other) {
        float dx = other.centerX - centerX;
        float dy = other.centerY - centerY;
        float dz = other.centerZ - centerZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float requiredRadius = dist + other.radius;
        if (requiredRadius > radius) {
            radius = requiredRadius;
        }
        return this;
    }

    // ==================== 碰撞检测 ====================

    /**
     * 判断点是否在球内
     */
    public boolean contains(float x, float y, float z) {
        float dx = x - centerX;
        float dy = y - centerY;
        float dz = z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * 判断点是否在球内（向量版本）
     */
    public boolean contains(Vector3f point) {
        return contains(point.x, point.y, point.z);
    }

    /**
     * 判断是否与另一个球相交
     */
    public boolean intersects(BoundingSphere other) {
        float dx = other.centerX - centerX;
        float dy = other.centerY - centerY;
        float dz = other.centerZ - centerZ;
        float distSq = dx * dx + dy * dy + dz * dz;
        float radiusSum = radius + other.radius;
        return distSq <= radiusSum * radiusSum;
    }

    /**
     * 判断是否与 AABB 相交
     */
    public boolean intersects(AABB aabb) {
        // 找到 AABB 上距离球心最近的点
        float closestX = Math.max(aabb.minX, Math.min(centerX, aabb.maxX));
        float closestY = Math.max(aabb.minY, Math.min(centerY, aabb.maxY));
        float closestZ = Math.max(aabb.minZ, Math.min(centerZ, aabb.maxZ));

        // 计算该点到球心的距离
        float dx = closestX - centerX;
        float dy = closestY - centerY;
        float dz = closestZ - centerZ;

        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * 计算到另一个球的距离（表面间距离）
     * 负值表示相交
     */
    public float distanceTo(BoundingSphere other) {
        float dx = other.centerX - centerX;
        float dy = other.centerY - centerY;
        float dz = other.centerZ - centerZ;
        float centerDist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return centerDist - radius - other.radius;
    }

    /**
     * 计算到点的距离（表面到点）
     * 负值表示点在球内
     */
    public float distanceTo(float x, float y, float z) {
        float dx = x - centerX;
        float dy = y - centerY;
        float dz = z - centerZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz) - radius;
    }

    @Override
    public String toString() {
        return String
            .format("BoundingSphere[center=(%.2f, %.2f, %.2f), radius=%.2f]", centerX, centerY, centerZ, radius);
    }
}
