package moe.takochan.takotech.client.renderer.graphics.camera;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;

/**
 * 相机类，管理视图和投影矩阵。
 * 支持透视和正交投影模式。
 */
@SideOnly(Side.CLIENT)
public class Camera {

    /** 投影类型 */
    public enum ProjectionType {
        PERSPECTIVE,
        ORTHOGRAPHIC
    }

    // ==================== 变换 ====================

    /** 相机位置 */
    private final Vector3f position = new Vector3f(0, 0, 0);

    /** 相机目标点 */
    private final Vector3f target = new Vector3f(0, 0, -1);

    /** 上方向 */
    private final Vector3f up = new Vector3f(0, 1, 0);

    // ==================== 投影参数 ====================

    private ProjectionType projectionType = ProjectionType.PERSPECTIVE;

    /** 透视投影：视野角度（弧度） */
    private float fov = MathUtils.toRadians(60.0f);

    /** 宽高比 */
    private float aspectRatio = 16.0f / 9.0f;

    /** 近裁剪面 */
    private float nearPlane = 0.1f;

    /** 远裁剪面 */
    private float farPlane = 1000.0f;

    /** 正交投影：视口尺寸 */
    private float orthoWidth = 10.0f;
    private float orthoHeight = 10.0f;

    // ==================== 矩阵缓存 ====================

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();

    private boolean viewDirty = true;
    private boolean projectionDirty = true;

    /** 视锥体（懒加载） */
    private Frustum frustum;
    private boolean frustumDirty = true;

    /** 可复用的 FloatBuffer */
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    // ==================== 构造函数 ====================

    public Camera() {}

    /**
     * 创建透视相机
     */
    public static Camera perspective(float fovDegrees, float aspect, float near, float far) {
        Camera camera = new Camera();
        camera.setPerspective(fovDegrees, aspect, near, far);
        return camera;
    }

    /**
     * 创建正交相机
     */
    public static Camera orthographic(float width, float height, float near, float far) {
        Camera camera = new Camera();
        camera.setOrthographic(width, height, near, far);
        return camera;
    }

    // ==================== 位置和朝向 ====================

    public Vector3f getPosition() {
        return position;
    }

    public Camera setPosition(float x, float y, float z) {
        position.set(x, y, z);
        markViewDirty();
        return this;
    }

    public Camera setPosition(Vector3f pos) {
        position.set(pos);
        markViewDirty();
        return this;
    }

    public Vector3f getTarget() {
        return target;
    }

    public Camera setTarget(float x, float y, float z) {
        target.set(x, y, z);
        markViewDirty();
        return this;
    }

    public Camera setTarget(Vector3f t) {
        target.set(t);
        markViewDirty();
        return this;
    }

    public Vector3f getUp() {
        return up;
    }

    public Camera setUp(float x, float y, float z) {
        up.set(x, y, z);
        markViewDirty();
        return this;
    }

    /**
     * 设置相机位置和目标点
     */
    public Camera lookAt(float eyeX, float eyeY, float eyeZ, float targetX, float targetY, float targetZ) {
        position.set(eyeX, eyeY, eyeZ);
        target.set(targetX, targetY, targetZ);
        markViewDirty();
        return this;
    }

    /**
     * 设置相机位置和目标点（向量版本）
     */
    public Camera lookAt(Vector3f eye, Vector3f center) {
        position.set(eye);
        target.set(center);
        markViewDirty();
        return this;
    }

    /**
     * 移动相机
     */
    public Camera translate(float dx, float dy, float dz) {
        position.x += dx;
        position.y += dy;
        position.z += dz;
        target.x += dx;
        target.y += dy;
        target.z += dz;
        markViewDirty();
        return this;
    }

    // ==================== 投影设置 ====================

    public ProjectionType getProjectionType() {
        return projectionType;
    }

    /**
     * 设置透视投影
     *
     * @param fovDegrees 垂直视野角度（度）
     * @param aspect     宽高比
     * @param near       近裁剪面
     * @param far        远裁剪面
     */
    public Camera setPerspective(float fovDegrees, float aspect, float near, float far) {
        this.projectionType = ProjectionType.PERSPECTIVE;
        this.fov = MathUtils.toRadians(fovDegrees);
        this.aspectRatio = aspect;
        this.nearPlane = near;
        this.farPlane = far;
        markProjectionDirty();
        return this;
    }

    /**
     * 设置正交投影
     */
    public Camera setOrthographic(float width, float height, float near, float far) {
        this.projectionType = ProjectionType.ORTHOGRAPHIC;
        this.orthoWidth = width;
        this.orthoHeight = height;
        this.nearPlane = near;
        this.farPlane = far;
        markProjectionDirty();
        return this;
    }

    /**
     * 更新宽高比（窗口大小变化时调用）
     */
    public Camera setAspectRatio(float aspect) {
        if (this.aspectRatio != aspect) {
            this.aspectRatio = aspect;
            if (projectionType == ProjectionType.PERSPECTIVE) {
                markProjectionDirty();
            }
        }
        return this;
    }

    /**
     * 设置视野角度（透视投影）
     */
    public Camera setFov(float fovDegrees) {
        this.fov = MathUtils.toRadians(fovDegrees);
        if (projectionType == ProjectionType.PERSPECTIVE) {
            markProjectionDirty();
        }
        return this;
    }

    public float getFov() {
        return MathUtils.toDegrees(fov);
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public float getFarPlane() {
        return farPlane;
    }

    // ==================== 矩阵获取 ====================

    /**
     * 获取视图矩阵
     */
    public Matrix4f getViewMatrix() {
        if (viewDirty) {
            updateViewMatrix();
        }
        return viewMatrix;
    }

    /**
     * 获取投影矩阵
     */
    public Matrix4f getProjectionMatrix() {
        if (projectionDirty) {
            updateProjectionMatrix();
        }
        return projectionMatrix;
    }

    /**
     * 获取视图投影矩阵 (P * V)
     */
    public Matrix4f getViewProjectionMatrix() {
        if (viewDirty || projectionDirty) {
            if (viewDirty) updateViewMatrix();
            if (projectionDirty) updateProjectionMatrix();
            Matrix4f.mul(projectionMatrix, viewMatrix, viewProjectionMatrix);
            frustumDirty = true;
        }
        return viewProjectionMatrix;
    }

    /**
     * 获取视图矩阵的 FloatBuffer（用于传递给 shader）
     */
    public FloatBuffer getViewMatrixBuffer() {
        matrixBuffer.clear();
        getViewMatrix().store(matrixBuffer);
        matrixBuffer.flip();
        return matrixBuffer;
    }

    /**
     * 获取投影矩阵的 FloatBuffer
     */
    public FloatBuffer getProjectionMatrixBuffer() {
        matrixBuffer.clear();
        getProjectionMatrix().store(matrixBuffer);
        matrixBuffer.flip();
        return matrixBuffer;
    }

    // ==================== 视锥体 ====================

    /**
     * 获取视锥体（用于剔除）
     */
    public Frustum getFrustum() {
        if (frustum == null) {
            frustum = new Frustum();
        }
        if (frustumDirty || viewDirty || projectionDirty) {
            frustum.extractFromMatrix(getViewProjectionMatrix());
            frustumDirty = false;
        }
        return frustum;
    }

    // ==================== 与 MC 同步 ====================

    /**
     * 从 Minecraft 的相机数据同步（每帧调用）
     *
     * @param posX         相机 X 位置
     * @param posY         相机 Y 位置
     * @param posZ         相机 Z 位置
     * @param pitch        俯仰角（度）
     * @param yaw          偏航角（度）
     * @param partialTicks 插值因子
     */
    public void syncFromMinecraft(double posX, double posY, double posZ, float pitch, float yaw, float partialTicks) {
        position.set((float) posX, (float) posY, (float) posZ);

        // 从 pitch/yaw 计算目标点
        float pitchRad = MathUtils.toRadians(pitch);
        float yawRad = MathUtils.toRadians(yaw);

        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);

        // MC 的 yaw: 0 = south (+Z), 90 = west (-X)
        float dirX = -sinYaw * cosPitch;
        float dirY = -sinPitch;
        float dirZ = cosYaw * cosPitch;

        target.set(position.x + dirX, position.y + dirY, position.z + dirZ);

        markViewDirty();
    }

    /**
     * 自动从 Minecraft 当前玩家同步相机状态
     * 包括位置、朝向、FOV 和宽高比
     *
     * @param partialTicks 插值因子（用于平滑）
     */
    public void syncFromMinecraft(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        EntityLivingBase entity = mc.renderViewEntity != null ? mc.renderViewEntity : mc.thePlayer;

        // 插值位置
        double posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + entity.getEyeHeight();
        double posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        // 插值旋转
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;

        syncFromMinecraft(posX, posY, posZ, pitch, yaw, partialTicks);

        // 同步 FOV 和宽高比
        float mcFov = mc.gameSettings.fovSetting;
        float aspect = (float) mc.displayWidth / (float) mc.displayHeight;

        if (projectionType == ProjectionType.PERSPECTIVE) {
            if (Math.abs(getFov() - mcFov) > 0.01f || Math.abs(aspectRatio - aspect) > 0.001f) {
                setPerspective(mcFov, aspect, nearPlane, farPlane);
            }
        }
    }

    /**
     * 设置为 2D GUI 正交相机
     *
     * @param screenWidth  屏幕宽度
     * @param screenHeight 屏幕高度
     */
    public Camera setupForGUI(int screenWidth, int screenHeight) {
        setPosition(0, 0, 0);
        setTarget(0, 0, -1);
        setUp(0, 1, 0);
        setOrthographic(screenWidth, screenHeight, -1000, 1000);
        return this;
    }

    // ==================== 内部方法 ====================

    private void markViewDirty() {
        viewDirty = true;
        frustumDirty = true;
    }

    private void markProjectionDirty() {
        projectionDirty = true;
        frustumDirty = true;
    }

    private void updateViewMatrix() {
        MathUtils.lookAt(position, target, up, viewMatrix);
        viewDirty = false;
    }

    private void updateProjectionMatrix() {
        if (projectionType == ProjectionType.PERSPECTIVE) {
            // fov is already stored in radians (see setPerspective), pass directly
            MathUtils.perspective(fov, aspectRatio, nearPlane, farPlane, projectionMatrix);
        } else {
            float halfW = orthoWidth / 2.0f;
            float halfH = orthoHeight / 2.0f;
            MathUtils.ortho(-halfW, halfW, -halfH, halfH, nearPlane, farPlane, projectionMatrix);
        }
        projectionDirty = false;
    }

    @Override
    public String toString() {
        return String.format(
            "Camera[%s, pos=(%.1f, %.1f, %.1f), target=(%.1f, %.1f, %.1f)]",
            projectionType,
            position.x,
            position.y,
            position.z,
            target.x,
            target.y,
            target.z);
    }
}
