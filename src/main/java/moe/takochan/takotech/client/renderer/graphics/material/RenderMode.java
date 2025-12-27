package moe.takochan.takotech.client.renderer.graphics.material;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 渲染模式枚举，对应 uber.frag 中的模式常量
 */
@SideOnly(Side.CLIENT)
public enum RenderMode {

    /** 纯色渲染（使用顶点颜色或 baseColor） */
    COLOR(0),

    /** 纯纹理渲染 */
    TEXTURE(1),

    /** 纹理与颜色混合 */
    TEXTURE_COLOR(2),

    /** 水平高斯模糊 */
    BLUR_HORIZONTAL(3),

    /** 垂直高斯模糊 */
    BLUR_VERTICAL(4);

    private final int id;

    RenderMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
