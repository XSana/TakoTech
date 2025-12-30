package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子碰撞响应类型枚举。
 * 定义粒子碰撞后的行为。
 */
@SideOnly(Side.CLIENT)
public enum CollisionResponse {

    /** 销毁粒子 */
    KILL(0),

    /** 反弹（保持能量） */
    BOUNCE(1),

    /** 反弹（损失能量） */
    BOUNCE_DAMPED(2),

    /** 粘附到碰撞表面 */
    STICK(3),

    /** 沿表面滑动 */
    SLIDE(4),

    /** 触发子发射器 */
    SUB_EMIT(5),

    /** 继续穿透（不响应） */
    PASS_THROUGH(6);

    private final int id;

    CollisionResponse(int id) {
        this.id = id;
    }

    /**
     * 获取响应 ID（用于着色器）
     */
    public int getId() {
        return id;
    }

    /**
     * 根据 ID 获取响应类型
     */
    public static CollisionResponse fromId(int id) {
        for (CollisionResponse response : values()) {
            if (response.id == id) {
                return response;
            }
        }
        return KILL;
    }
}
