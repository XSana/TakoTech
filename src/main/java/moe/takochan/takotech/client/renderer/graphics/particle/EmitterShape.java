package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子发射器形状枚举。
 * 定义粒子的初始生成位置和方向分布。
 */
@SideOnly(Side.CLIENT)
public enum EmitterShape {

    /** 点发射 - 所有粒子从同一点发射 */
    POINT(0),

    /** 球体发射 - 粒子在球体内部随机生成 */
    SPHERE(1),

    /** 球面发射 - 粒子在球体表面生成，向外发射 */
    SPHERE_SURFACE(2),

    /** 半球发射 - 粒子在半球内部随机生成 */
    HEMISPHERE(3),

    /** 圆形发射 - 粒子在 XZ 平面圆形内生成 */
    CIRCLE(4),

    /** 圆环发射 - 粒子在 XZ 平面圆环上生成 */
    RING(5),

    /** 锥形发射 - 粒子沿锥形方向发射 */
    CONE(6),

    /** 盒体发射 - 粒子在立方体内部随机生成 */
    BOX(7),

    /** 盒体边缘发射 - 粒子在立方体边缘生成 */
    BOX_EDGE(8),

    /** 圆柱发射 - 粒子在圆柱体内随机生成 */
    CYLINDER(9),

    /** 线段发射 - 粒子沿线段均匀分布 */
    LINE(10),

    /** 矩形平面发射 - 粒子在 XZ 平面矩形内生成 */
    RECTANGLE(11),

    /** 网格发射 - 粒子在网格节点上生成 */
    GRID(12),

    /** 自定义网格发射 - 粒子从网格顶点发射 */
    MESH(13);

    private final int id;

    EmitterShape(int id) {
        this.id = id;
    }

    /**
     * 获取形状 ID（用于着色器）
     */
    public int getId() {
        return id;
    }

    /**
     * 根据 ID 获取形状
     */
    public static EmitterShape fromId(int id) {
        for (EmitterShape shape : values()) {
            if (shape.id == id) {
                return shape;
            }
        }
        return POINT;
    }
}
