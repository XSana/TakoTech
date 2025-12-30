package moe.takochan.takotech.client.renderer.graphics.component;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.ecs.Component;
import moe.takochan.takotech.client.renderer.graphics.ecs.Entity;

/**
 * 变换组件。
 * 管理实体的位置、旋转、缩放，支持层级变换。
 *
 * <p>
 * 特性:
 * </p>
 * <ul>
 * <li>Dirty Flag 优化 - 避免不必要的矩阵重算</li>
 * <li>层级支持 - 自动从父实体继承变换</li>
 * <li>四元数旋转 - 避免万向节锁</li>
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
 *     TransformComponent transform = entity.addComponent(new TransformComponent());
 *     transform.setPosition(10, 64, 20);
 *     transform.setRotationEuler(0, Math.PI / 4, 0);
 *     transform.setScale(2.0f);
 *
 *     Matrix4f worldMatrix = transform.getWorldMatrix();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class TransformComponent extends Component {

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

    /** 本地矩阵是否需要更新 */
    private boolean localDirty = true;

    /** 世界矩阵是否需要更新 */
    private boolean worldDirty = true;

    /** 临时矩阵，用于中间计算 */
    private static final Matrix4f TEMP_MATRIX = new Matrix4f();

    /**
     * 创建变换组件
     */
    public TransformComponent() {}

    // ==================== 位置 ====================

    /**
     * 获取本地位置
     *
     * @return 位置向量
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * 获取 X 坐标
     *
     * @return X 坐标
     */
    public float getX() {
        return position.x;
    }

    /**
     * 获取 Y 坐标
     *
     * @return Y 坐标
     */
    public float getY() {
        return position.y;
    }

    /**
     * 获取 Z 坐标
     *
     * @return Z 坐标
     */
    public float getZ() {
        return position.z;
    }

    /**
     * 设置本地位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this
     */
    public TransformComponent setPosition(float x, float y, float z) {
        position.set(x, y, z);
        markLocalDirty();
        return this;
    }

    /**
     * 设置本地位置
     *
     * @param pos 位置向量
     * @return this
     */
    public TransformComponent setPosition(Vector3f pos) {
        position.set(pos);
        markLocalDirty();
        return this;
    }

    /**
     * 平移
     *
     * @param dx X 增量
     * @param dy Y 增量
     * @param dz Z 增量
     * @return this
     */
    public TransformComponent translate(float dx, float dy, float dz) {
        position.x += dx;
        position.y += dy;
        position.z += dz;
        markLocalDirty();
        return this;
    }

    /**
     * 平移
     *
     * @param delta 增量向量
     * @return this
     */
    public TransformComponent translate(Vector3f delta) {
        Vector3f.add(position, delta, position);
        markLocalDirty();
        return this;
    }

    // ==================== 旋转 ====================

    /**
     * 获取本地旋转
     *
     * @return 四元数
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * 设置本地旋转
     *
     * @param q 四元数
     * @return this
     */
    public TransformComponent setRotation(Quaternion q) {
        rotation.set(q.x, q.y, q.z, q.w);
        markLocalDirty();
        return this;
    }

    /**
     * 设置本地旋转
     *
     * @param x X 分量
     * @param y Y 分量
     * @param z Z 分量
     * @param w W 分量
     * @return this
     */
    public TransformComponent setRotation(float x, float y, float z, float w) {
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
     * @return this
     */
    public TransformComponent setRotationEuler(float pitch, float yaw, float roll) {
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
     * 设置欧拉角旋转（度）
     *
     * @param pitchDeg 绕 X 轴旋转（度）
     * @param yawDeg   绕 Y 轴旋转（度）
     * @param rollDeg  绕 Z 轴旋转（度）
     * @return this
     */
    public TransformComponent setRotationEulerDegrees(float pitchDeg, float yawDeg, float rollDeg) {
        return setRotationEuler(
            (float) Math.toRadians(pitchDeg),
            (float) Math.toRadians(yawDeg),
            (float) Math.toRadians(rollDeg));
    }

    /**
     * 绕轴旋转（弧度）
     *
     * @param axisX X 轴分量
     * @param axisY Y 轴分量
     * @param axisZ Z 轴分量
     * @param angle 角度（弧度）
     * @return this
     */
    public TransformComponent rotate(float axisX, float axisY, float axisZ, float angle) {
        Quaternion deltaRot = new Quaternion();
        deltaRot.setFromAxisAngle(new Vector4f(axisX, axisY, axisZ, angle));
        Quaternion.mul(rotation, deltaRot, rotation);
        rotation.normalise();
        markLocalDirty();
        return this;
    }

    /**
     * 绕 Y 轴旋转（弧度）
     *
     * @param angle 角度（弧度）
     * @return this
     */
    public TransformComponent rotateY(float angle) {
        return rotate(0, 1, 0, angle);
    }

    /**
     * 绕 X 轴旋转（弧度）
     *
     * @param angle 角度（弧度）
     * @return this
     */
    public TransformComponent rotateX(float angle) {
        return rotate(1, 0, 0, angle);
    }

    /**
     * 绕 Z 轴旋转（弧度）
     *
     * @param angle 角度（弧度）
     * @return this
     */
    public TransformComponent rotateZ(float angle) {
        return rotate(0, 0, 1, angle);
    }

    // ==================== 缩放 ====================

    /**
     * 获取本地缩放
     *
     * @return 缩放向量
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * 设置本地缩放
     *
     * @param x X 缩放
     * @param y Y 缩放
     * @param z Z 缩放
     * @return this
     */
    public TransformComponent setScale(float x, float y, float z) {
        scale.set(x, y, z);
        markLocalDirty();
        return this;
    }

    /**
     * 设置统一缩放
     *
     * @param uniform 统一缩放值
     * @return this
     */
    public TransformComponent setScale(float uniform) {
        scale.set(uniform, uniform, uniform);
        markLocalDirty();
        return this;
    }

    /**
     * 设置本地缩放
     *
     * @param s 缩放向量
     * @return this
     */
    public TransformComponent setScale(Vector3f s) {
        scale.set(s);
        markLocalDirty();
        return this;
    }

    // ==================== 矩阵获取 ====================

    /**
     * 获取本地变换矩阵
     *
     * @return 本地矩阵
     */
    public Matrix4f getLocalMatrix() {
        if (localDirty) {
            updateLocalMatrix();
        }
        return localMatrix;
    }

    /**
     * 获取世界变换矩阵
     *
     * @return 世界矩阵
     */
    public Matrix4f getWorldMatrix() {
        if (worldDirty || localDirty) {
            updateWorldMatrix();
        }
        return worldMatrix;
    }

    /**
     * 获取世界位置
     *
     * @param dest 目标向量（可为 null）
     * @return 世界位置
     */
    public Vector3f getWorldPosition(Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        Matrix4f world = getWorldMatrix();
        dest.set(world.m30, world.m31, world.m32);
        return dest;
    }

    /**
     * 获取世界位置
     *
     * @return 世界位置
     */
    public Vector3f getWorldPosition() {
        return getWorldPosition(null);
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
        // 通知子实体更新世界矩阵
        if (entity != null) {
            for (Entity child : entity.getChildren()) {
                TransformComponent childTransform = child.getComponent(TransformComponent.class);
                if (childTransform != null) {
                    childTransform.invalidateWorldMatrix();
                }
            }
        }
    }

    /**
     * 强制标记世界矩阵需要更新
     */
    public void invalidateWorldMatrix() {
        worldDirty = true;
        // 递归标记子实体
        if (entity != null) {
            for (Entity child : entity.getChildren()) {
                TransformComponent childTransform = child.getComponent(TransformComponent.class);
                if (childTransform != null) {
                    childTransform.invalidateWorldMatrix();
                }
            }
        }
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

        // 从父实体获取 TransformComponent
        TransformComponent parentTransform = getParentTransform();

        if (parentTransform != null) {
            Matrix4f parentWorld = parentTransform.getWorldMatrix();
            Matrix4f.mul(parentWorld, localMatrix, worldMatrix);
        } else {
            worldMatrix.load(localMatrix);
        }

        worldDirty = false;
    }

    /**
     * 获取父实体的 TransformComponent
     *
     * @return 父变换组件，如果不存在则返回 null
     */
    private TransformComponent getParentTransform() {
        if (entity == null) return null;
        Entity parent = entity.getParent();
        if (parent == null) return null;
        return parent.getComponent(TransformComponent.class);
    }

    // ==================== 工具方法 ====================

    /**
     * 重置为默认值
     *
     * @return this
     */
    public TransformComponent reset() {
        position.set(0, 0, 0);
        rotation.set(0, 0, 0, 1);
        scale.set(1, 1, 1);
        markLocalDirty();
        return this;
    }

    /**
     * 复制另一个变换组件的值
     *
     * @param other 源变换组件
     * @return this
     */
    public TransformComponent copyFrom(TransformComponent other) {
        position.set(other.position);
        rotation.set(other.rotation.x, other.rotation.y, other.rotation.z, other.rotation.w);
        scale.set(other.scale);
        markLocalDirty();
        return this;
    }

    /**
     * 朝向指定点
     *
     * @param targetX 目标 X 坐标
     * @param targetY 目标 Y 坐标
     * @param targetZ 目标 Z 坐标
     * @return this
     */
    public TransformComponent lookAt(float targetX, float targetY, float targetZ) {
        Vector3f worldPos = getWorldPosition();
        float dx = targetX - worldPos.x;
        float dy = targetY - worldPos.y;
        float dz = targetZ - worldPos.z;

        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) -Math.atan2(dy, dist);

        return setRotationEuler(pitch, yaw, 0);
    }

    @Override
    public String toString() {
        return String.format(
            "TransformComponent[pos=(%.2f, %.2f, %.2f), scale=(%.2f, %.2f, %.2f)]",
            position.x,
            position.y,
            position.z,
            scale.x,
            scale.y,
            scale.z);
    }
}
