package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子碰撞检测模式枚举。
 * 定义粒子如何与环境进行碰撞检测。
 */
@SideOnly(Side.CLIENT)
public enum CollisionMode {

    /** 无碰撞检测 */
    NONE(0),

    /** 与世界方块碰撞（使用距离场或射线检测） */
    WORLD(1),

    /** 与平面碰撞 */
    PLANE(2),

    /** 与球体碰撞 */
    SPHERE(3),

    /** 与盒子碰撞 */
    BOX(4),

    /** 与圆柱碰撞 */
    CYLINDER(5),

    /** 与地形高度图碰撞 */
    HEIGHTMAP(6),

    /** 与自定义距离场碰撞 */
    SDF(7);

    private final int id;

    CollisionMode(int id) {
        this.id = id;
    }

    /**
     * 获取碰撞模式 ID（用于着色器）
     */
    public int getId() {
        return id;
    }

    /**
     * 根据 ID 获取碰撞模式
     */
    public static CollisionMode fromId(int id) {
        for (CollisionMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return NONE;
    }
}
