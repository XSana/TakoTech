package moe.takochan.takotech.client.renderer.graphics.scene;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 变换组件，管理位置、旋转、缩放及层级关系。
 * 支持 dirty 标记优化，避免不必要的矩阵重算。
 */
@SideOnly(Side.CLIENT)
public class Transform {

    /** 本地位置 */
    private final Vector3f position = new Vector3f(0, 0, 0);

    /** 本地旋转（四元数） */
    private final Quaternion rotation = new Quaternion(0, 0, 0, 1);

    /** 本地缩放 */
    private final Vector3f scale = new Vector3f(1, 1, 1);

    /** 本地矩阵缓存 */
    private final Matrix4f localMatrix = new Matrix4f();

    /** 世界矩阵缓存 */
    private final Matrix4f worldMatrix = new Matrix4f();

    /** 父变换 */
    private Transform parent;

    /** 本地矩阵是否需要更新 */
    private boolean localDirty = true;

    /** 世界矩阵是否需要更新 */
    private boolean worldDirty = true;

    /** 临时矩阵，用于中间计算 */
    private static final Matrix4f TEMP_MATRIX = new Matrix4f();

    // ==================== 位置 ====================

    public Vector3f getPosition() {
        return position;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    public float getZ() {
        return position.z;
    }

    public Transform setPosition(float x, float y, float z) {
        position.set(x, y, z);
        markLocalDirty();
        return this;
    }

    public Transform setPosition(Vector3f pos) {
        position.set(pos);
        markLocalDirty();
        return this;
    }

    public Transform translate(float dx, float dy, float dz) {
        position.x += dx;
        position.y += dy;
        position.z += dz;
        markLocalDirty();
        return this;
    }

    public Transform translate(Vector3f delta) {
        Vector3f.add(position, delta, position);
        markLocalDirty();
        return this;
    }

    // ==================== 旋转 ====================

    public Quaternion getRotation() {
        return rotation;
    }

    public Transform setRotation(Quaternion q) {
        rotation.set(q.x, q.y, q.z, q.w);
        markLocalDirty();
        return this;
    }

    public Transform setRotation(float x, float y, float z, float w) {
        rotation.set(x, y, z, w);
        markLocalDirty();
        return this;
    }

    /**
     * 设置欧拉角旋转（弧度）
     *
     * @param pitch 绕 X 轴旋转
     * @param yaw   绕 Y 轴旋转
     * @param roll  绕 Z 轴旋转
     */
    public Transform setRotationEuler(float pitch, float yaw, float roll) {
        // 从欧拉角构造四元数 (ZYX 顺序)
        float cy = (float) Math.cos(yaw * 0.5f);
        float sy = (float) Math.sin(yaw * 0.5f);
        float cp = (float) Math.cos(pitch * 0.5f);
        float sp = (float) Math.sin(pitch * 0.5f);
        float cr = (float) Math.cos(roll * 0.5f);
        float sr = (float) Math.sin(roll * 0.5f);

        rotation.w = cr * cp * cy + sr * sp * sy;
        rotation.x = sr * cp * cy - cr * sp * sy;
        rotation.y = cr * sp * cy + sr * cp * sy;
        rotation.z = cr * cp * sy - sr * sp * cy;

        markLocalDirty();
        return this;
    }

    /**
     * 绕轴旋转（弧度）
     */
    public Transform rotate(float axisX, float axisY, float axisZ, float angle) {
        Quaternion deltaRot = new Quaternion();
        deltaRot.setFromAxisAngle(new org.lwjgl.util.vector.Vector4f(axisX, axisY, axisZ, angle));
        Quaternion.mul(rotation, deltaRot, rotation);
        rotation.normalise();
        markLocalDirty();
        return this;
    }

    // ==================== 缩放 ====================

    public Vector3f getScale() {
        return scale;
    }

    public Transform setScale(float x, float y, float z) {
        scale.set(x, y, z);
        markLocalDirty();
        return this;
    }

    public Transform setScale(float uniform) {
        scale.set(uniform, uniform, uniform);
        markLocalDirty();
        return this;
    }

    public Transform setScale(Vector3f s) {
        scale.set(s);
        markLocalDirty();
        return this;
    }

    // ==================== 层级 ====================

    public Transform getParent() {
        return parent;
    }

    public Transform setParent(Transform newParent) {
        if (this.parent != newParent) {
            this.parent = newParent;
            markWorldDirty();
        }
        return this;
    }

    /**
     * 判断是否是根节点
     */
    public boolean isRoot() {
        return parent == null;
    }

    // ==================== 矩阵获取 ====================

    /**
     * 获取本地变换矩阵
     */
    public Matrix4f getLocalMatrix() {
        if (localDirty) {
            updateLocalMatrix();
        }
        return localMatrix;
    }

    /**
     * 获取世界变换矩阵
     */
    public Matrix4f getWorldMatrix() {
        if (worldDirty || localDirty) {
            updateWorldMatrix();
        }
        return worldMatrix;
    }

    /**
     * 获取世界位置
     */
    public Vector3f getWorldPosition(Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        Matrix4f world = getWorldMatrix();
        dest.set(world.m30, world.m31, world.m32);
        return dest;
    }

    // ==================== Dirty 标记 ====================

    /**
     * 标记本地矩阵需要更新
     */
    private void markLocalDirty() {
        localDirty = true;
        markWorldDirty();
    }

    /**
     * 标记世界矩阵需要更新
     */
    private void markWorldDirty() {
        worldDirty = true;
        // 注意：如果有子节点，也需要标记它们的 worldDirty
        // 这个由 SceneNode 管理
    }

    /**
     * 强制标记世界矩阵需要更新（供外部调用，如父节点变化时）
     */
    public void invalidateWorldMatrix() {
        worldDirty = true;
    }

    // ==================== 矩阵更新 ====================

    /**
     * 更新本地变换矩阵
     */
    private void updateLocalMatrix() {
        localMatrix.setIdentity();

        // 应用平移
        localMatrix.m30 = position.x;
        localMatrix.m31 = position.y;
        localMatrix.m32 = position.z;

        // 应用旋转（四元数转矩阵）
        float qx = rotation.x;
        float qy = rotation.y;
        float qz = rotation.z;
        float qw = rotation.w;

        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float wx = qw * qx;
        float wy = qw * qy;
        float wz = qw * qz;

        // 旋转矩阵部分
        TEMP_MATRIX.setIdentity();
        TEMP_MATRIX.m00 = 1.0f - 2.0f * (yy + zz);
        TEMP_MATRIX.m01 = 2.0f * (xy + wz);
        TEMP_MATRIX.m02 = 2.0f * (xz - wy);

        TEMP_MATRIX.m10 = 2.0f * (xy - wz);
        TEMP_MATRIX.m11 = 1.0f - 2.0f * (xx + zz);
        TEMP_MATRIX.m12 = 2.0f * (yz + wx);

        TEMP_MATRIX.m20 = 2.0f * (xz + wy);
        TEMP_MATRIX.m21 = 2.0f * (yz - wx);
        TEMP_MATRIX.m22 = 1.0f - 2.0f * (xx + yy);

        // localMatrix = T * R
        Matrix4f.mul(localMatrix, TEMP_MATRIX, localMatrix);

        // 应用缩放
        localMatrix.m00 *= scale.x;
        localMatrix.m01 *= scale.x;
        localMatrix.m02 *= scale.x;
        localMatrix.m10 *= scale.y;
        localMatrix.m11 *= scale.y;
        localMatrix.m12 *= scale.y;
        localMatrix.m20 *= scale.z;
        localMatrix.m21 *= scale.z;
        localMatrix.m22 *= scale.z;

        localDirty = false;
    }

    /**
     * 更新世界变换矩阵
     */
    private void updateWorldMatrix() {
        if (localDirty) {
            updateLocalMatrix();
        }

        if (parent != null) {
            Matrix4f parentWorld = parent.getWorldMatrix();
            Matrix4f.mul(parentWorld, localMatrix, worldMatrix);
        } else {
            worldMatrix.load(localMatrix);
        }

        worldDirty = false;
    }

    // ==================== 工具方法 ====================

    /**
     * 重置为默认值
     */
    public Transform reset() {
        position.set(0, 0, 0);
        rotation.set(0, 0, 0, 1);
        scale.set(1, 1, 1);
        markLocalDirty();
        return this;
    }

    /**
     * 复制另一个变换的值
     */
    public Transform copyFrom(Transform other) {
        position.set(other.position);
        rotation.set(other.rotation.x, other.rotation.y, other.rotation.z, other.rotation.w);
        scale.set(other.scale);
        markLocalDirty();
        return this;
    }

    @Override
    public String toString() {
        return String.format(
            "Transform[pos=(%.2f, %.2f, %.2f), scale=(%.2f, %.2f, %.2f)]",
            position.x,
            position.y,
            position.z,
            scale.x,
            scale.y,
            scale.z);
    }
}
