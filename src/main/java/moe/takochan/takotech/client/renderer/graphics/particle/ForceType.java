package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子力场类型枚举。
 * 定义作用于粒子的各种力场效果。
 */
@SideOnly(Side.CLIENT)
public enum ForceType {

    /** 重力 - 恒定方向的加速度 */
    GRAVITY(0),

    /** 风力 - 带湍流的方向性力 */
    WIND(1),

    /** 漩涡力 - 绕轴旋转的切向力 */
    VORTEX(2),

    /** 湍流 - 基于噪声的随机扰动 */
    TURBULENCE(3),

    /** 吸引力 - 向中心点吸引 */
    ATTRACTOR(4),

    /** 排斥力 - 从中心点排斥 */
    REPULSOR(5),

    /** 阻力 - 与速度相反的力 */
    DRAG(6),

    /** 卷曲噪声 - 无散度噪声场，产生流体效果 */
    CURL_NOISE(7),

    /** 向心力 - 绕点的圆周运动 */
    ORBITAL(8),

    /** 速度限制 - 限制最大速度 */
    VELOCITY_LIMIT(9),

    /** 矢量场 - 自定义 3D 矢量场 */
    VECTOR_FIELD(10),

    /** 弹簧力 - 回复到原点的弹性力 */
    SPRING(11);

    private final int id;

    ForceType(int id) {
        this.id = id;
    }

    /**
     * 获取力场 ID（用于着色器）
     */
    public int getId() {
        return id;
    }

    /**
     * 根据 ID 获取力场类型
     */
    public static ForceType fromId(int id) {
        for (ForceType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return GRAVITY;
    }
}
