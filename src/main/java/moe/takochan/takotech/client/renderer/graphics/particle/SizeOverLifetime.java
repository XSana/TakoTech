package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 大小生命周期曲线。
 * 定义粒子大小随生命周期（0-1）变化的效果。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     SizeOverLifetime size = SizeOverLifetime.expand();
 *     float scale = size.evaluate(0.5f); // 获取生命周期 50% 时的大小倍率
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class SizeOverLifetime {

    /** 动画曲线 */
    private AnimationCurve curve;

    /** 基础大小乘数 */
    private float baseMultiplier = 1.0f;

    /**
     * 创建空的大小生命周期曲线
     */
    public SizeOverLifetime() {
        this.curve = new AnimationCurve();
    }

    /**
     * 使用指定曲线创建大小生命周期曲线
     *
     * @param curve 动画曲线
     */
    public SizeOverLifetime(AnimationCurve curve) {
        this.curve = curve != null ? curve : new AnimationCurve();
    }

    /**
     * 获取曲线
     */
    public AnimationCurve getCurve() {
        return curve;
    }

    /**
     * 设置曲线
     *
     * @param curve 动画曲线
     * @return this
     */
    public SizeOverLifetime setCurve(AnimationCurve curve) {
        this.curve = curve != null ? curve : new AnimationCurve();
        return this;
    }

    /**
     * 获取基础大小乘数
     */
    public float getBaseMultiplier() {
        return baseMultiplier;
    }

    /**
     * 设置基础大小乘数
     *
     * @param multiplier 乘数
     * @return this
     */
    public SizeOverLifetime setBaseMultiplier(float multiplier) {
        this.baseMultiplier = multiplier;
        return this;
    }

    /**
     * 添加关键帧
     *
     * @param time  时间 (0-1)
     * @param value 大小倍率
     * @return this
     */
    public SizeOverLifetime addKey(float time, float value) {
        curve.addKey(time, value);
        return this;
    }

    /**
     * 设置是否使用平滑插值
     *
     * @param smooth true 使用平滑插值，false 使用线性插值
     * @return this
     */
    public SizeOverLifetime setSmooth(boolean smooth) {
        curve.setSmooth(smooth);
        return this;
    }

    /**
     * 在指定生命周期采样大小
     *
     * @param lifePercent 生命周期百分比 (0-1)
     * @return 大小倍率（已乘以 baseMultiplier）
     */
    public float evaluate(float lifePercent) {
        return curve.evaluate(lifePercent) * baseMultiplier;
    }

    /**
     * 转换为数组（用于上传到 GPU）
     *
     * @return [time0, value0, time1, value1, ...]
     */
    public float[] toArray() {
        return curve.toArray();
    }

    /**
     * 清除所有关键帧
     *
     * @return this
     */
    public SizeOverLifetime clear() {
        curve.clear();
        return this;
    }

    // ==================== 预设曲线 ====================

    /** 恒定大小 */
    public static SizeOverLifetime constant() {
        return new SizeOverLifetime(AnimationCurve.constant(1.0f));
    }

    /** 恒定大小（指定值） */
    public static SizeOverLifetime constant(float value) {
        return new SizeOverLifetime(AnimationCurve.constant(value));
    }

    /** 线性增长 - 从 0 到 1 */
    public static SizeOverLifetime grow() {
        return new SizeOverLifetime(AnimationCurve.linear());
    }

    /** 线性收缩 - 从 1 到 0 */
    public static SizeOverLifetime shrink() {
        return new SizeOverLifetime(AnimationCurve.linearDecay());
    }

    /** 快速膨胀后缓慢收缩 */
    public static SizeOverLifetime expand() {
        return new SizeOverLifetime().addKey(0, 0)
            .addKey(0.2f, 1.0f)
            .addKey(1, 0.5f);
    }

    /** 快速膨胀后快速收缩 */
    public static SizeOverLifetime pulse() {
        return new SizeOverLifetime(AnimationCurve.pulse());
    }

    /** 弹跳效果 */
    public static SizeOverLifetime bounce() {
        return new SizeOverLifetime(AnimationCurve.bounce());
    }

    /** 爆炸效果 - 快速膨胀 */
    public static SizeOverLifetime explosion() {
        return new SizeOverLifetime().addKey(0, 0.2f)
            .addKey(0.1f, 1.0f)
            .addKey(0.3f, 0.8f)
            .addKey(1, 0.3f);
    }

    /** 烟雾效果 - 缓慢膨胀 */
    public static SizeOverLifetime smoke() {
        return new SizeOverLifetime().addKey(0, 0.3f)
            .addKey(0.5f, 0.8f)
            .addKey(1, 1.0f);
    }

    /** 火花效果 - 快速收缩 */
    public static SizeOverLifetime spark() {
        return new SizeOverLifetime().addKey(0, 1.0f)
            .addKey(0.3f, 0.5f)
            .addKey(1, 0);
    }

    /** 气泡效果 - 上浮时膨胀然后破裂 */
    public static SizeOverLifetime bubble() {
        return new SizeOverLifetime().addKey(0, 0.5f)
            .addKey(0.7f, 1.0f)
            .addKey(0.9f, 1.2f)
            .addKey(1, 0);
    }

    /** 心跳效果 - 脉动 */
    public static SizeOverLifetime heartbeat() {
        return new SizeOverLifetime().addKey(0, 0.8f)
            .addKey(0.15f, 1.2f)
            .addKey(0.3f, 0.9f)
            .addKey(0.45f, 1.1f)
            .addKey(0.6f, 0.85f)
            .addKey(1, 0.8f);
    }

    /** 呼吸效果 - 缓慢脉动 */
    public static SizeOverLifetime breathe() {
        return new SizeOverLifetime().addKey(0, 0.8f)
            .addKey(0.25f, 1.0f)
            .addKey(0.5f, 0.8f)
            .addKey(0.75f, 1.0f)
            .addKey(1, 0.8f);
    }

    /** 闪烁效果 - 快速切换 */
    public static SizeOverLifetime flicker() {
        return new SizeOverLifetime().addKey(0, 1.0f)
            .addKey(0.1f, 0.7f)
            .addKey(0.2f, 1.0f)
            .addKey(0.3f, 0.8f)
            .addKey(0.4f, 1.0f)
            .addKey(0.5f, 0.6f)
            .addKey(0.6f, 1.0f)
            .addKey(0.7f, 0.9f)
            .addKey(0.8f, 1.0f)
            .addKey(0.9f, 0.5f)
            .addKey(1, 0)
            .setSmooth(false);
    }

    /** 涟漪效果 - 环形扩散 */
    public static SizeOverLifetime ripple() {
        return new SizeOverLifetime().addKey(0, 0)
            .addKey(1, 1.0f)
            .setSmooth(false);
    }

    /** 冲击波效果 - 快速扩张然后消散 */
    public static SizeOverLifetime shockwave() {
        return new SizeOverLifetime().addKey(0, 0)
            .addKey(0.2f, 0.8f)
            .addKey(0.5f, 1.0f)
            .addKey(1, 1.2f);
    }

    /** 凝聚效果 - 从大到小聚集 */
    public static SizeOverLifetime converge() {
        return new SizeOverLifetime().addKey(0, 1.5f)
            .addKey(0.5f, 0.8f)
            .addKey(1, 0.2f);
    }

    /** 稳定膨胀后保持 */
    public static SizeOverLifetime expandAndHold() {
        return new SizeOverLifetime().addKey(0, 0)
            .addKey(0.3f, 1.0f)
            .addKey(1, 1.0f);
    }

    /** 延迟收缩 */
    public static SizeOverLifetime delayedShrink() {
        return new SizeOverLifetime().addKey(0, 1.0f)
            .addKey(0.7f, 1.0f)
            .addKey(1, 0);
    }
}
