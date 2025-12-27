package moe.takochan.takotech.client.renderer.graphics.camera;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;

/**
 * 视锥体，用于可见性剔除。
 * 由 6 个平面组成：近、远、左、右、上、下。
 */
@SideOnly(Side.CLIENT)
public class Frustum {

    /** 平面索引 */
    public static final int NEAR = 0;
    public static final int FAR = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int TOP = 4;
    public static final int BOTTOM = 5;

    /** 6 个裁剪平面 */
    private final Plane[] planes = new Plane[6];

    /** 临时向量，用于剔除计算 */
    private final Vector3f tempVec = new Vector3f();

    public Frustum() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Plane();
        }
    }

    /**
     * 从视图投影矩阵 (VP = Projection * View) 提取视锥体平面
     * <p>
     * 使用 Gribb/Hartmann 方法从组合矩阵中提取平面
     * </p>
     *
     * @param viewProjection VP 矩阵
     */
    public void extractFromMatrix(Matrix4f viewProjection) {
        Matrix4f m = viewProjection;

        // 左平面: row3 + row0
        planes[LEFT].set(m.m03 + m.m00, m.m13 + m.m10, m.m23 + m.m20, m.m33 + m.m30);

        // 右平面: row3 - row0
        planes[RIGHT].set(m.m03 - m.m00, m.m13 - m.m10, m.m23 - m.m20, m.m33 - m.m30);

        // 下平面: row3 + row1
        planes[BOTTOM].set(m.m03 + m.m01, m.m13 + m.m11, m.m23 + m.m21, m.m33 + m.m31);

        // 上平面: row3 - row1
        planes[TOP].set(m.m03 - m.m01, m.m13 - m.m11, m.m23 - m.m21, m.m33 - m.m31);

        // 近平面: row3 + row2
        planes[NEAR].set(m.m03 + m.m02, m.m13 + m.m12, m.m23 + m.m22, m.m33 + m.m32);

        // 远平面: row3 - row2
        planes[FAR].set(m.m03 - m.m02, m.m13 - m.m12, m.m23 - m.m22, m.m33 - m.m32);

        // 归一化所有平面
        for (Plane plane : planes) {
            plane.normalize();
        }
    }

    /**
     * 获取指定索引的平面
     */
    public Plane getPlane(int index) {
        return planes[index];
    }

    // ==================== 点测试 ====================

    /**
     * 判断点是否在视锥体内
     */
    public boolean containsPoint(float x, float y, float z) {
        for (Plane plane : planes) {
            if (plane.distanceToPoint(x, y, z) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断点是否在视锥体内（向量版本）
     */
    public boolean containsPoint(Vector3f point) {
        return containsPoint(point.x, point.y, point.z);
    }

    // ==================== 球体测试 ====================

    /**
     * 判断球体是否与视锥体相交
     *
     * @return true 如果球体完全或部分在视锥体内
     */
    public boolean intersectsSphere(float cx, float cy, float cz, float radius) {
        for (Plane plane : planes) {
            if (plane.distanceToPoint(cx, cy, cz) < -radius) {
                return false; // 球体完全在平面负半空间
            }
        }
        return true;
    }

    /**
     * 判断球体是否与视锥体相交（向量版本）
     */
    public boolean intersectsSphere(Vector3f center, float radius) {
        return intersectsSphere(center.x, center.y, center.z, radius);
    }

    // ==================== AABB 测试 ====================

    /**
     * 测试结果枚举
     */
    public enum TestResult {
        /** 完全在视锥体外 */
        OUTSIDE,
        /** 与视锥体相交 */
        INTERSECT,
        /** 完全在视锥体内 */
        INSIDE
    }

    /**
     * 判断 AABB 是否与视锥体相交（快速版本）
     *
     * @return true 如果 AABB 完全或部分在视锥体内
     */
    public boolean intersectsAABB(AABB aabb) {
        for (Plane plane : planes) {
            // 获取 P 顶点（最正方向的顶点）
            aabb.getPositiveVertex(plane.a, plane.b, plane.c, tempVec);

            // 如果 P 顶点在平面负半空间，则整个 AABB 都在外面
            if (plane.distanceToPoint(tempVec) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 精确测试 AABB 与视锥体的关系
     *
     * @return OUTSIDE, INTERSECT, 或 INSIDE
     */
    public TestResult testAABB(AABB aabb) {
        boolean allInside = true;

        for (Plane plane : planes) {
            // 获取 P 顶点
            aabb.getPositiveVertex(plane.a, plane.b, plane.c, tempVec);
            float pDist = plane.distanceToPoint(tempVec);

            if (pDist < 0) {
                // P 顶点在外面，整个 AABB 都在外面
                return TestResult.OUTSIDE;
            }

            // 获取 N 顶点
            aabb.getNegativeVertex(plane.a, plane.b, plane.c, tempVec);
            float nDist = plane.distanceToPoint(tempVec);

            if (nDist < 0) {
                // N 顶点在外面，P 顶点在里面，相交
                allInside = false;
            }
        }

        return allInside ? TestResult.INSIDE : TestResult.INTERSECT;
    }

    /**
     * 判断 AABB 是否与视锥体相交（直接坐标版本）
     */
    public boolean intersectsAABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (Plane plane : planes) {
            // 内联计算 P 顶点
            float px = plane.a >= 0 ? maxX : minX;
            float py = plane.b >= 0 ? maxY : minY;
            float pz = plane.c >= 0 ? maxZ : minZ;

            if (plane.distanceToPoint(px, py, pz) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(
            "Frustum[near=%s, far=%s, left=%s, right=%s, top=%s, bottom=%s]",
            planes[NEAR],
            planes[FAR],
            planes[LEFT],
            planes[RIGHT],
            planes[TOP],
            planes[BOTTOM]);
    }
}
