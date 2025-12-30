package moe.takochan.takotech.client.renderer.graphics.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 动画曲线类。
 * 用于定义粒子属性随时间变化的曲线，支持线性和平滑插值。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     AnimationCurve curve = new AnimationCurve().addKey(0.0f, 0.0f) // 开始时为 0
 *         .addKey(0.5f, 1.0f) // 中间达到最大
 *         .addKey(1.0f, 0.0f); // 结束时为 0
 *
 *     float value = curve.evaluate(0.25f); // 在 25% 时采样
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class AnimationCurve {

    /** 关键帧列表 */
    private final List<Keyframe> keyframes = new ArrayList<>();

    /** 是否使用平滑插值 */
    private boolean smoothInterpolation = true;

    /**
     * 关键帧数据
     */
    public static class Keyframe implements Comparable<Keyframe> {

        public final float time;
        public final float value;

        public Keyframe(float time, float value) {
            this.time = Math.max(0, Math.min(1, time));
            this.value = value;
        }

        @Override
        public int compareTo(Keyframe other) {
            return Float.compare(this.time, other.time);
        }
    }

    /**
     * 添加关键帧
     *
     * @param time  时间 (0-1)
     * @param value 值
     * @return this
     */
    public AnimationCurve addKey(float time, float value) {
        keyframes.add(new Keyframe(time, value));
        Collections.sort(keyframes);
        return this;
    }

    /**
     * 清除所有关键帧
     */
    public AnimationCurve clear() {
        keyframes.clear();
        return this;
    }

    /**
     * 设置是否使用平滑插值
     *
     * @param smooth true 使用平滑插值，false 使用线性插值
     */
    public AnimationCurve setSmooth(boolean smooth) {
        this.smoothInterpolation = smooth;
        return this;
    }

    /**
     * 在指定时间采样曲线值
     *
     * @param t 时间 (0-1)
     * @return 插值后的值
     */
    public float evaluate(float t) {
        if (keyframes.isEmpty()) {
            return 0;
        }

        if (keyframes.size() == 1) {
            return keyframes.get(0).value;
        }

        t = Math.max(0, Math.min(1, t));

        // 找到相邻的两个关键帧
        Keyframe prev = keyframes.get(0);
        Keyframe next = keyframes.get(keyframes.size() - 1);

        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (keyframes.get(i).time <= t && keyframes.get(i + 1).time >= t) {
                prev = keyframes.get(i);
                next = keyframes.get(i + 1);
                break;
            }
        }

        // 边界情况
        if (t <= prev.time) {
            return prev.value;
        }
        if (t >= next.time) {
            return next.value;
        }

        // 计算插值因子
        float factor = (t - prev.time) / (next.time - prev.time);

        if (smoothInterpolation) {
            // 平滑步进插值 (smoothstep)
            factor = factor * factor * (3 - 2 * factor);
        }

        return prev.value + (next.value - prev.value) * factor;
    }

    /**
     * 获取关键帧数量
     */
    public int getKeyframeCount() {
        return keyframes.size();
    }

    /**
     * 获取指定索引的关键帧
     */
    public Keyframe getKeyframe(int index) {
        if (index >= 0 && index < keyframes.size()) {
            return keyframes.get(index);
        }
        return null;
    }

    /**
     * 转换为浮点数组（用于上传到 GPU）
     * 格式: [time0, value0, time1, value1, ...]
     *
     * @return 浮点数组
     */
    public float[] toArray() {
        float[] data = new float[keyframes.size() * 2];
        for (int i = 0; i < keyframes.size(); i++) {
            data[i * 2] = keyframes.get(i).time;
            data[i * 2 + 1] = keyframes.get(i).value;
        }
        return data;
    }

    // ==================== 预设曲线 ====================

    /** 常量曲线 - 恒定值 1 */
    public static AnimationCurve constant(float value) {
        return new AnimationCurve().addKey(0, value)
            .addKey(1, value);
    }

    /** 线性增长 - 从 0 到 1 */
    public static AnimationCurve linear() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(1, 1)
            .setSmooth(false);
    }

    /** 线性衰减 - 从 1 到 0 */
    public static AnimationCurve linearDecay() {
        return new AnimationCurve().addKey(0, 1)
            .addKey(1, 0)
            .setSmooth(false);
    }

    /** 缓入 - 慢开始快结束 */
    public static AnimationCurve easeIn() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(0.5f, 0.25f)
            .addKey(1, 1);
    }

    /** 缓出 - 快开始慢结束 */
    public static AnimationCurve easeOut() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(0.5f, 0.75f)
            .addKey(1, 1);
    }

    /** 缓入缓出 - 慢开始慢结束 */
    public static AnimationCurve easeInOut() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(0.5f, 0.5f)
            .addKey(1, 1);
    }

    /** 脉冲 - 快速上升后下降 */
    public static AnimationCurve pulse() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(0.2f, 1)
            .addKey(1, 0);
    }

    /** 弹跳 - 多次反弹衰减 */
    public static AnimationCurve bounce() {
        return new AnimationCurve().addKey(0, 1)
            .addKey(0.2f, 0)
            .addKey(0.4f, 0.5f)
            .addKey(0.6f, 0)
            .addKey(0.8f, 0.2f)
            .addKey(1, 0);
    }

    /** 阶梯 - 两级阶梯 */
    public static AnimationCurve step() {
        return new AnimationCurve().addKey(0, 0)
            .addKey(0.49f, 0)
            .addKey(0.5f, 1)
            .addKey(1, 1)
            .setSmooth(false);
    }
}
