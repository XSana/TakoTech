package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 颜色生命周期曲线。
 * 定义粒子颜色随生命周期（0-1）变化的效果。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     ColorOverLifetime col = ColorOverLifetime.fire();
 *     float[] rgba = col.evaluate(0.5f); // 获取生命周期 50% 时的颜色
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ColorOverLifetime {

    /** 颜色渐变 */
    private Gradient gradient;

    /**
     * 创建空的颜色生命周期曲线
     */
    public ColorOverLifetime() {
        this.gradient = new Gradient();
    }

    /**
     * 使用指定渐变创建颜色生命周期曲线
     *
     * @param gradient 颜色渐变
     */
    public ColorOverLifetime(Gradient gradient) {
        this.gradient = gradient != null ? gradient : new Gradient();
    }

    /**
     * 获取渐变
     */
    public Gradient getGradient() {
        return gradient;
    }

    /**
     * 设置渐变
     *
     * @param gradient 颜色渐变
     * @return this
     */
    public ColorOverLifetime setGradient(Gradient gradient) {
        this.gradient = gradient != null ? gradient : new Gradient();
        return this;
    }

    /**
     * 添加颜色关键帧
     *
     * @param time 时间 (0-1)
     * @param r    红色分量 (0-1)
     * @param g    绿色分量 (0-1)
     * @param b    蓝色分量 (0-1)
     * @return this
     */
    public ColorOverLifetime addColorKey(float time, float r, float g, float b) {
        gradient.addColorKey(time, r, g, b);
        return this;
    }

    /**
     * 添加透明度关键帧
     *
     * @param time  时间 (0-1)
     * @param alpha 透明度 (0-1)
     * @return this
     */
    public ColorOverLifetime addAlphaKey(float time, float alpha) {
        gradient.addAlphaKey(time, alpha);
        return this;
    }

    /**
     * 添加完整 RGBA 关键帧
     *
     * @param time 时间 (0-1)
     * @param r    红色分量 (0-1)
     * @param g    绿色分量 (0-1)
     * @param b    蓝色分量 (0-1)
     * @param a    透明度 (0-1)
     * @return this
     */
    public ColorOverLifetime addKey(float time, float r, float g, float b, float a) {
        gradient.addKey(time, r, g, b, a);
        return this;
    }

    /**
     * 在指定生命周期采样颜色
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return [r, g, b, a] 数组
     */
    public float[] evaluate(float lifePercent) {
        return gradient.evaluate(lifePercent);
    }

    /**
     * 转换为 LUT（用于上传到 GPU）
     *
     * @param samples 采样数量
     * @return 浮点数组 [r0, g0, b0, a0, r1, g1, b1, a1, ...]
     */
    public float[] toLUT(int samples) {
        return gradient.toLUT(samples);
    }

    /**
     * 清除所有关键帧
     *
     * @return this
     */
    public ColorOverLifetime clear() {
        gradient.clear();
        return this;
    }

    // ==================== 预设曲线 ====================

    /** 恒定白色 */
    public static ColorOverLifetime constantWhite() {
        return new ColorOverLifetime(Gradient.white());
    }

    /** 淡出（白色渐隐） */
    public static ColorOverLifetime fadeOut() {
        return new ColorOverLifetime(Gradient.whiteFadeOut());
    }

    /** 火焰效果 - 黄→橙→红→黑 */
    public static ColorOverLifetime fire() {
        return new ColorOverLifetime(Gradient.fire());
    }

    /** 烟雾效果 - 灰色渐隐 */
    public static ColorOverLifetime smoke() {
        return new ColorOverLifetime(Gradient.smoke());
    }

    /** 爆炸效果 - 白→黄→橙→红→灰 */
    public static ColorOverLifetime explosion() {
        return new ColorOverLifetime(Gradient.explosion());
    }

    /** 传送门效果 - 紫色脉冲 */
    public static ColorOverLifetime portal() {
        return new ColorOverLifetime(Gradient.portal());
    }

    /** 治愈效果 - 绿色发光 */
    public static ColorOverLifetime healing() {
        return new ColorOverLifetime(Gradient.healing());
    }

    /** 电弧效果 - 蓝白色 */
    public static ColorOverLifetime lightning() {
        return new ColorOverLifetime(Gradient.lightning());
    }

    /** 彩虹效果 */
    public static ColorOverLifetime rainbow() {
        return new ColorOverLifetime(Gradient.rainbow());
    }

    /** 能量效果 - 蓝色发光渐隐 */
    public static ColorOverLifetime energy() {
        return new ColorOverLifetime().addColorKey(0, 0.5f, 0.8f, 1.0f)
            .addColorKey(0.3f, 0.3f, 0.6f, 1.0f)
            .addColorKey(0.7f, 0.2f, 0.4f, 0.9f)
            .addColorKey(1, 0.1f, 0.2f, 0.5f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.6f, 0.8f)
            .addAlphaKey(1, 0);
    }

    /** 毒性效果 - 绿色 */
    public static ColorOverLifetime toxic() {
        return new ColorOverLifetime().addColorKey(0, 0.4f, 1.0f, 0.2f)
            .addColorKey(0.5f, 0.2f, 0.8f, 0.1f)
            .addColorKey(1, 0.1f, 0.4f, 0.05f)
            .addAlphaKey(0, 0.9f)
            .addAlphaKey(0.7f, 0.6f)
            .addAlphaKey(1, 0);
    }

    /** 冰霜效果 - 冰蓝色 */
    public static ColorOverLifetime frost() {
        return new ColorOverLifetime().addColorKey(0, 0.9f, 0.95f, 1.0f)
            .addColorKey(0.4f, 0.7f, 0.9f, 1.0f)
            .addColorKey(1, 0.5f, 0.7f, 0.9f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.5f, 0.8f)
            .addAlphaKey(1, 0);
    }

    /** 血液效果 - 深红色 */
    public static ColorOverLifetime blood() {
        return new ColorOverLifetime().addColorKey(0, 0.8f, 0.1f, 0.1f)
            .addColorKey(0.5f, 0.6f, 0.05f, 0.05f)
            .addColorKey(1, 0.3f, 0.02f, 0.02f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.8f, 0.9f)
            .addAlphaKey(1, 0.7f);
    }

    /** 金色闪光 */
    public static ColorOverLifetime gold() {
        return new ColorOverLifetime().addColorKey(0, 1.0f, 0.95f, 0.6f)
            .addColorKey(0.3f, 1.0f, 0.85f, 0.3f)
            .addColorKey(0.7f, 0.9f, 0.7f, 0.2f)
            .addColorKey(1, 0.6f, 0.4f, 0.1f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.5f, 0.9f)
            .addAlphaKey(1, 0);
    }

    /** 暗影效果 - 深紫黑色 */
    public static ColorOverLifetime shadow() {
        return new ColorOverLifetime().addColorKey(0, 0.2f, 0.1f, 0.3f)
            .addColorKey(0.5f, 0.1f, 0.05f, 0.15f)
            .addColorKey(1, 0.05f, 0.02f, 0.08f)
            .addAlphaKey(0, 0.8f)
            .addAlphaKey(0.6f, 0.6f)
            .addAlphaKey(1, 0);
    }

    /** 神圣效果 - 金白色 */
    public static ColorOverLifetime holy() {
        return new ColorOverLifetime().addColorKey(0, 1.0f, 1.0f, 0.9f)
            .addColorKey(0.4f, 1.0f, 0.95f, 0.7f)
            .addColorKey(1, 1.0f, 0.9f, 0.5f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.7f, 0.8f)
            .addAlphaKey(1, 0);
    }

    /** 水花效果 - 蓝白色 */
    public static ColorOverLifetime water() {
        return new ColorOverLifetime().addColorKey(0, 0.8f, 0.9f, 1.0f)
            .addColorKey(0.5f, 0.5f, 0.7f, 0.9f)
            .addColorKey(1, 0.3f, 0.5f, 0.7f)
            .addAlphaKey(0, 0.8f)
            .addAlphaKey(0.5f, 0.5f)
            .addAlphaKey(1, 0);
    }

    /** 岩浆效果 - 红橙黄 */
    public static ColorOverLifetime lava() {
        return new ColorOverLifetime().addColorKey(0, 1.0f, 0.9f, 0.3f)
            .addColorKey(0.3f, 1.0f, 0.5f, 0.1f)
            .addColorKey(0.6f, 0.8f, 0.2f, 0.05f)
            .addColorKey(1, 0.3f, 0.1f, 0.05f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.8f, 0.9f)
            .addAlphaKey(1, 0.6f);
    }
}
