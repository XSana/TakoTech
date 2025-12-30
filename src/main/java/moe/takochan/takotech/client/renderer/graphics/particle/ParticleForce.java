package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子力场类。
 * 定义作用于粒子的各种力场效果。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * {@code
 * // 添加重力
 * emitter.addForce(ParticleForce.gravity(0, -9.8f, 0));
 *
 * // 添加漩涡力
 * emitter.addForce(ParticleForce.vortex(0, 64, 0, 0, 1, 0, 5.0f));
 *
 * // 添加吸引力
 * emitter.addForce(ParticleForce.attractor(10, 64, 10, 50.0f, 10.0f));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleForce {

    /** 力场类型 */
    private final ForceType type;

    /** 方向/位置 X */
    private float x;

    /** 方向/位置 Y */
    private float y;

    /** 方向/位置 Z */
    private float z;

    /** 强度 */
    private float strength;

    /** 附加参数1（频率、衰减等） */
    private float param1;

    /** 附加参数2 */
    private float param2;

    /** 附加参数3（轴向量等） */
    private float axisX;
    private float axisY;
    private float axisZ;

    /** 是否启用 */
    private boolean enabled = true;

    /**
     * 创建力场
     *
     * @param type     力场类型
     * @param x        X 分量
     * @param y        Y 分量
     * @param z        Z 分量
     * @param strength 强度
     */
    public ParticleForce(ForceType type, float x, float y, float z, float strength) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.strength = strength;
    }

    // ==================== Getters ====================

    public ForceType getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getStrength() {
        return strength;
    }

    public float getParam1() {
        return param1;
    }

    public float getParam2() {
        return param2;
    }

    public float getAxisX() {
        return axisX;
    }

    public float getAxisY() {
        return axisY;
    }

    public float getAxisZ() {
        return axisZ;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ==================== Setters ====================

    public ParticleForce setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ParticleForce setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    public ParticleForce setParam1(float param1) {
        this.param1 = param1;
        return this;
    }

    public ParticleForce setParam2(float param2) {
        this.param2 = param2;
        return this;
    }

    public ParticleForce setAxis(float x, float y, float z) {
        this.axisX = x;
        this.axisY = y;
        this.axisZ = z;
        return this;
    }

    public ParticleForce setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // ==================== 转换为着色器数据 ====================

    /**
     * 转换为浮点数组（用于上传到 GPU）
     * 格式：[type, x, y, z, strength, param1, param2, reserved,
     * axisX, axisY, axisZ, enabled]
     *
     * @return 12 个 float 的数组
     */
    public float[] toFloatArray() {
        return new float[] { type.getId(), x, y, z, strength, param1, param2, 0, axisX, axisY, axisZ, enabled ? 1 : 0 };
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建重力力场
     *
     * @param x 重力方向 X
     * @param y 重力方向 Y（通常为负值）
     * @param z 重力方向 Z
     * @return 力场对象
     */
    public static ParticleForce gravity(float x, float y, float z) {
        return new ParticleForce(ForceType.GRAVITY, x, y, z, 1.0f);
    }

    /**
     * 创建默认重力（向下 9.8）
     *
     * @return 力场对象
     */
    public static ParticleForce gravity() {
        return gravity(0, -9.8f, 0);
    }

    /**
     * 创建风力力场
     *
     * @param x          风向 X
     * @param y          风向 Y
     * @param z          风向 Z
     * @param strength   风力强度
     * @param turbulence 湍流强度
     * @return 力场对象
     */
    public static ParticleForce wind(float x, float y, float z, float strength, float turbulence) {
        ParticleForce force = new ParticleForce(ForceType.WIND, x, y, z, strength);
        force.param1 = turbulence;
        return force;
    }

    /**
     * 创建漩涡力场
     *
     * @param centerX  漩涡中心 X
     * @param centerY  漩涡中心 Y
     * @param centerZ  漩涡中心 Z
     * @param axisX    旋转轴 X
     * @param axisY    旋转轴 Y
     * @param axisZ    旋转轴 Z
     * @param strength 漩涡强度
     * @return 力场对象
     */
    public static ParticleForce vortex(float centerX, float centerY, float centerZ, float axisX, float axisY,
        float axisZ, float strength) {
        ParticleForce force = new ParticleForce(ForceType.VORTEX, centerX, centerY, centerZ, strength);
        force.setAxis(axisX, axisY, axisZ);
        return force;
    }

    /**
     * 创建垂直漩涡（绕 Y 轴）
     *
     * @param centerX  中心 X
     * @param centerY  中心 Y
     * @param centerZ  中心 Z
     * @param strength 强度
     * @return 力场对象
     */
    public static ParticleForce vortexY(float centerX, float centerY, float centerZ, float strength) {
        return vortex(centerX, centerY, centerZ, 0, 1, 0, strength);
    }

    /**
     * 创建湍流力场
     *
     * @param frequency 噪声频率
     * @param strength  湍流强度
     * @return 力场对象
     */
    public static ParticleForce turbulence(float frequency, float strength) {
        ParticleForce force = new ParticleForce(ForceType.TURBULENCE, 0, 0, 0, strength);
        force.param1 = frequency;
        return force;
    }

    /**
     * 创建吸引力场
     *
     * @param centerX  吸引点 X
     * @param centerY  吸引点 Y
     * @param centerZ  吸引点 Z
     * @param strength 吸引强度
     * @param radius   影响半径（0 表示无限）
     * @return 力场对象
     */
    public static ParticleForce attractor(float centerX, float centerY, float centerZ, float strength, float radius) {
        ParticleForce force = new ParticleForce(ForceType.ATTRACTOR, centerX, centerY, centerZ, strength);
        force.param1 = radius;
        return force;
    }

    /**
     * 创建排斥力场
     *
     * @param centerX  排斥点 X
     * @param centerY  排斥点 Y
     * @param centerZ  排斥点 Z
     * @param strength 排斥强度
     * @param radius   影响半径
     * @return 力场对象
     */
    public static ParticleForce repulsor(float centerX, float centerY, float centerZ, float strength, float radius) {
        ParticleForce force = new ParticleForce(ForceType.REPULSOR, centerX, centerY, centerZ, strength);
        force.param1 = radius;
        return force;
    }

    /**
     * 创建阻力场
     *
     * @param coefficient 阻力系数
     * @return 力场对象
     */
    public static ParticleForce drag(float coefficient) {
        return new ParticleForce(ForceType.DRAG, 0, 0, 0, coefficient);
    }

    /**
     * 创建卷曲噪声力场
     *
     * @param frequency 噪声频率
     * @param strength  强度
     * @return 力场对象
     */
    public static ParticleForce curlNoise(float frequency, float strength) {
        ParticleForce force = new ParticleForce(ForceType.CURL_NOISE, 0, 0, 0, strength);
        force.param1 = frequency;
        return force;
    }

    /**
     * 创建轨道力场（使粒子绕点旋转）
     *
     * @param centerX      轨道中心 X
     * @param centerY      轨道中心 Y
     * @param centerZ      轨道中心 Z
     * @param orbitalSpeed 轨道速度
     * @param radialSpeed  径向速度（正值外扩，负值内缩）
     * @return 力场对象
     */
    public static ParticleForce orbital(float centerX, float centerY, float centerZ, float orbitalSpeed,
        float radialSpeed) {
        ParticleForce force = new ParticleForce(ForceType.ORBITAL, centerX, centerY, centerZ, orbitalSpeed);
        force.param1 = radialSpeed;
        return force;
    }

    /**
     * 创建速度限制力场
     *
     * @param maxSpeed 最大速度
     * @return 力场对象
     */
    public static ParticleForce velocityLimit(float maxSpeed) {
        return new ParticleForce(ForceType.VELOCITY_LIMIT, 0, 0, 0, maxSpeed);
    }

    /**
     * 创建弹簧力场（回复到原点）
     *
     * @param originX   原点 X
     * @param originY   原点 Y
     * @param originZ   原点 Z
     * @param stiffness 弹簧刚度
     * @param damping   阻尼系数
     * @return 力场对象
     */
    public static ParticleForce spring(float originX, float originY, float originZ, float stiffness, float damping) {
        ParticleForce force = new ParticleForce(ForceType.SPRING, originX, originY, originZ, stiffness);
        force.param1 = damping;
        return force;
    }
}
