package moe.takochan.takotech.client.renderer.graphics.culling;

import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 轴对齐包围盒 (Axis-Aligned Bounding Box)。
 * 用于快速碰撞检测和视锥体剔除。
 */
@SideOnly(Side.CLIENT)
public class AABB {

    /** 最小点 */
    public float minX, minY, minZ;

    /** 最大点 */
    public float maxX, maxY, maxZ;

    public AABB() {
        reset();
    }

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 从中心点和半尺寸创建
     */
    public static AABB fromCenterExtent(float cx, float cy, float cz, float extentX, float extentY, float extentZ) {
        return new AABB(cx - extentX, cy - extentY, cz - extentZ, cx + extentX, cy + extentY, cz + extentZ);
    }

    /**
     * 从中心点和统一半尺寸创建
     */
    public static AABB fromCenterExtent(float cx, float cy, float cz, float extent) {
        return fromCenterExtent(cx, cy, cz, extent, extent, extent);
    }

    // ==================== 设置方法 ====================

    public AABB set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    public AABB set(AABB other) {
        this.minX = other.minX;
        this.minY = other.minY;
        this.minZ = other.minZ;
        this.maxX = other.maxX;
        this.maxY = other.maxY;
        this.maxZ = other.maxZ;
        return this;
    }

    /**
     * 重置为无效状态（用于累积扩展）
     */
    public AABB reset() {
        minX = minY = minZ = Float.POSITIVE_INFINITY;
        maxX = maxY = maxZ = Float.NEGATIVE_INFINITY;
        return this;
    }

    // ==================== 扩展方法 ====================

    /**
     * 扩展包围盒以包含指定点
     */
    public AABB expandToInclude(float x, float y, float z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
        return this;
    }

    /**
     * 扩展包围盒以包含指定点（向量版本）
     */
    public AABB expandToInclude(Vector3f point) {
        return expandToInclude(point.x, point.y, point.z);
    }

    /**
     * 扩展包围盒以包含另一个包围盒
     */
    public AABB expandToInclude(AABB other) {
        minX = Math.min(minX, other.minX);
        minY = Math.min(minY, other.minY);
        minZ = Math.min(minZ, other.minZ);
        maxX = Math.max(maxX, other.maxX);
        maxY = Math.max(maxY, other.maxY);
        maxZ = Math.max(maxZ, other.maxZ);
        return this;
    }

    /**
     * 向外扩展指定距离
     */
    public AABB expand(float amount) {
        minX -= amount;
        minY -= amount;
        minZ -= amount;
        maxX += amount;
        maxY += amount;
        maxZ += amount;
        return this;
    }

    /**
     * 平移包围盒
     */
    public AABB translate(float dx, float dy, float dz) {
        minX += dx;
        minY += dy;
        minZ += dz;
        maxX += dx;
        maxY += dy;
        maxZ += dz;
        return this;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取中心点 X
     */
    public float getCenterX() {
        return (minX + maxX) * 0.5f;
    }

    /**
     * 获取中心点 Y
     */
    public float getCenterY() {
        return (minY + maxY) * 0.5f;
    }

    /**
     * 获取中心点 Z
     */
    public float getCenterZ() {
        return (minZ + maxZ) * 0.5f;
    }

    /**
     * 获取中心点
     */
    public Vector3f getCenter(Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        dest.set(getCenterX(), getCenterY(), getCenterZ());
        return dest;
    }

    /**
     * 获取 X 方向尺寸
     */
    public float getSizeX() {
        return maxX - minX;
    }

    /**
     * 获取 Y 方向尺寸
     */
    public float getSizeY() {
        return maxY - minY;
    }

    /**
     * 获取 Z 方向尺寸
     */
    public float getSizeZ() {
        return maxZ - minZ;
    }

    /**
     * 获取半尺寸（extent）
     */
    public Vector3f getExtent(Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        dest.set(getSizeX() * 0.5f, getSizeY() * 0.5f, getSizeZ() * 0.5f);
        return dest;
    }

    /**
     * 获取最大维度尺寸
     */
    public float getMaxSize() {
        return Math.max(getSizeX(), Math.max(getSizeY(), getSizeZ()));
    }

    /**
     * 获取包围球半径
     */
    public float getBoundingSphereRadius() {
        float dx = getSizeX() * 0.5f;
        float dy = getSizeY() * 0.5f;
        float dz = getSizeZ() * 0.5f;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 判断包围盒是否有效
     */
    public boolean isValid() {
        return minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    // ==================== 碰撞检测 ====================

    /**
     * 判断点是否在包围盒内
     */
    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * 判断点是否在包围盒内（向量版本）
     */
    public boolean contains(Vector3f point) {
        return contains(point.x, point.y, point.z);
    }

    /**
     * 判断是否与另一个包围盒相交
     */
    public boolean intersects(AABB other) {
        return minX <= other.maxX && maxX >= other.minX
            && minY <= other.maxY
            && maxY >= other.minY
            && minZ <= other.maxZ
            && maxZ >= other.minZ;
    }

    /**
     * 获取 P 顶点（相对于平面法向量最远的顶点）
     * 用于视锥体剔除
     */
    public void getPositiveVertex(float nx, float ny, float nz, Vector3f dest) {
        dest.x = nx >= 0 ? maxX : minX;
        dest.y = ny >= 0 ? maxY : minY;
        dest.z = nz >= 0 ? maxZ : minZ;
    }

    /**
     * 获取 N 顶点（相对于平面法向量最近的顶点）
     * 用于视锥体剔除
     */
    public void getNegativeVertex(float nx, float ny, float nz, Vector3f dest) {
        dest.x = nx >= 0 ? minX : maxX;
        dest.y = ny >= 0 ? minY : maxY;
        dest.z = nz >= 0 ? minZ : maxZ;
    }

    @Override
    public String toString() {
        return String.format("AABB[(%.2f, %.2f, %.2f) - (%.2f, %.2f, %.2f)]", minX, minY, minZ, maxX, maxY, maxZ);
    }
}
