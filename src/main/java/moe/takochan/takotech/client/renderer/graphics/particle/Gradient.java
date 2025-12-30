package moe.takochan.takotech.client.renderer.graphics.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 颜色渐变类。
 * 用于定义粒子颜色随生命周期变化的渐变效果。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Gradient gradient = new Gradient().addColorKey(0.0f, 1.0f, 1.0f, 0.0f) // 黄色
 *         .addColorKey(0.5f, 1.0f, 0.5f, 0.0f) // 橙色
 *         .addColorKey(1.0f, 0.3f, 0.0f, 0.0f) // 深红
 *         .addAlphaKey(0.0f, 1.0f) // 完全不透明
 *         .addAlphaKey(0.8f, 1.0f) // 保持不透明
 *         .addAlphaKey(1.0f, 0.0f); // 淡出
 *
 *     float[] color = gradient.evaluate(0.25f); // [r, g, b, a]
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class Gradient {

    /** 颜色关键帧列表 */
    private final List<ColorKey> colorKeys = new ArrayList<>();

    /** 透明度关键帧列表 */
    private final List<AlphaKey> alphaKeys = new ArrayList<>();

    /**
     * 颜色关键帧
     */
    public static class ColorKey implements Comparable<ColorKey> {

        public final float time;
        public final float r, g, b;

        public ColorKey(float time, float r, float g, float b) {
            this.time = Math.max(0, Math.min(1, time));
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public int compareTo(ColorKey other) {
            return Float.compare(this.time, other.time);
        }
    }

    /**
     * 透明度关键帧
     */
    public static class AlphaKey implements Comparable<AlphaKey> {

        public final float time;
        public final float alpha;

        public AlphaKey(float time, float alpha) {
            this.time = Math.max(0, Math.min(1, time));
            this.alpha = alpha;
        }

        @Override
        public int compareTo(AlphaKey other) {
            return Float.compare(this.time, other.time);
        }
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
    public Gradient addColorKey(float time, float r, float g, float b) {
        colorKeys.add(new ColorKey(time, r, g, b));
        Collections.sort(colorKeys);
        return this;
    }

    /**
     * 添加透明度关键帧
     *
     * @param time  时间 (0-1)
     * @param alpha 透明度 (0-1)
     * @return this
     */
    public Gradient addAlphaKey(float time, float alpha) {
        alphaKeys.add(new AlphaKey(time, alpha));
        Collections.sort(alphaKeys);
        return this;
    }

    /**
     * 添加完整的 RGBA 关键帧
     *
     * @param time 时间 (0-1)
     * @param r    红色分量 (0-1)
     * @param g    绿色分量 (0-1)
     * @param b    蓝色分量 (0-1)
     * @param a    透明度 (0-1)
     * @return this
     */
    public Gradient addKey(float time, float r, float g, float b, float a) {
        addColorKey(time, r, g, b);
        addAlphaKey(time, a);
        return this;
    }

    /**
     * 清除所有关键帧
     */
    public Gradient clear() {
        colorKeys.clear();
        alphaKeys.clear();
        return this;
    }

    /**
     * 在指定时间采样颜色
     *
     * @param t 时间 (0-1)
     * @return [r, g, b, a] 数组
     */
    public float[] evaluate(float t) {
        float[] result = new float[4];
        t = Math.max(0, Math.min(1, t));

        // 采样颜色
        if (colorKeys.isEmpty()) {
            result[0] = result[1] = result[2] = 1;
        } else if (colorKeys.size() == 1) {
            ColorKey key = colorKeys.get(0);
            result[0] = key.r;
            result[1] = key.g;
            result[2] = key.b;
        } else {
            ColorKey prev = colorKeys.get(0);
            ColorKey next = colorKeys.get(colorKeys.size() - 1);

            for (int i = 0; i < colorKeys.size() - 1; i++) {
                if (colorKeys.get(i).time <= t && colorKeys.get(i + 1).time >= t) {
                    prev = colorKeys.get(i);
                    next = colorKeys.get(i + 1);
                    break;
                }
            }

            float factor = prev.time == next.time ? 0 : (t - prev.time) / (next.time - prev.time);
            result[0] = prev.r + (next.r - prev.r) * factor;
            result[1] = prev.g + (next.g - prev.g) * factor;
            result[2] = prev.b + (next.b - prev.b) * factor;
        }

        // 采样透明度
        if (alphaKeys.isEmpty()) {
            result[3] = 1;
        } else if (alphaKeys.size() == 1) {
            result[3] = alphaKeys.get(0).alpha;
        } else {
            AlphaKey prev = alphaKeys.get(0);
            AlphaKey next = alphaKeys.get(alphaKeys.size() - 1);

            for (int i = 0; i < alphaKeys.size() - 1; i++) {
                if (alphaKeys.get(i).time <= t && alphaKeys.get(i + 1).time >= t) {
                    prev = alphaKeys.get(i);
                    next = alphaKeys.get(i + 1);
                    break;
                }
            }

            float factor = prev.time == next.time ? 0 : (t - prev.time) / (next.time - prev.time);
            result[3] = prev.alpha + (next.alpha - prev.alpha) * factor;
        }

        return result;
    }

    /**
     * 获取颜色关键帧数量
     */
    public int getColorKeyCount() {
        return colorKeys.size();
    }

    /**
     * 获取透明度关键帧数量
     */
    public int getAlphaKeyCount() {
        return alphaKeys.size();
    }

    /**
     * 转换为浮点数组（用于上传到 GPU）
     * 采样 N 个点生成查找表
     *
     * @param samples 采样数量
     * @return 浮点数组 [r0, g0, b0, a0, r1, g1, b1, a1, ...]
     */
    public float[] toLUT(int samples) {
        float[] lut = new float[samples * 4];
        for (int i = 0; i < samples; i++) {
            float t = (float) i / (samples - 1);
            float[] color = evaluate(t);
            lut[i * 4] = color[0];
            lut[i * 4 + 1] = color[1];
            lut[i * 4 + 2] = color[2];
            lut[i * 4 + 3] = color[3];
        }
        return lut;
    }

    // ==================== 工厂方法 ====================

    /**
     * Create a two-color gradient
     *
     * @param sr start red
     * @param sg start green
     * @param sb start blue
     * @param sa start alpha
     * @param er end red
     * @param eg end green
     * @param eb end blue
     * @param ea end alpha
     * @return gradient
     */
    public static Gradient twoColor(float sr, float sg, float sb, float sa, float er, float eg, float eb, float ea) {
        return new Gradient().addKey(0, sr, sg, sb, sa)
            .addKey(1, er, eg, eb, ea);
    }

    /**
     * Create a solid color gradient
     *
     * @param r red
     * @param g green
     * @param b blue
     * @param a alpha
     * @return gradient
     */
    public static Gradient solid(float r, float g, float b, float a) {
        return new Gradient().addKey(0, r, g, b, a)
            .addKey(1, r, g, b, a);
    }

    // ==================== 预设渐变 ====================

    /** Pure white */
    public static Gradient white() {
        return new Gradient().addKey(0, 1, 1, 1, 1)
            .addKey(1, 1, 1, 1, 1);
    }

    /** 淡出白色 */
    public static Gradient whiteFadeOut() {
        return new Gradient().addKey(0, 1, 1, 1, 1)
            .addKey(1, 1, 1, 1, 0);
    }

    /** 火焰渐变 */
    public static Gradient fire() {
        return new Gradient().addColorKey(0, 1.0f, 1.0f, 0.3f)
            .addColorKey(0.3f, 1.0f, 0.5f, 0.0f)
            .addColorKey(0.7f, 0.8f, 0.2f, 0.0f)
            .addColorKey(1.0f, 0.2f, 0.1f, 0.1f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.7f, 0.8f)
            .addAlphaKey(1, 0);
    }

    /** 烟雾渐变 */
    public static Gradient smoke() {
        return new Gradient().addColorKey(0, 0.5f, 0.5f, 0.5f)
            .addColorKey(1, 0.2f, 0.2f, 0.2f)
            .addAlphaKey(0, 0.6f)
            .addAlphaKey(0.5f, 0.4f)
            .addAlphaKey(1, 0);
    }

    /** 爆炸渐变 */
    public static Gradient explosion() {
        return new Gradient().addColorKey(0, 1.0f, 1.0f, 0.8f)
            .addColorKey(0.1f, 1.0f, 0.7f, 0.2f)
            .addColorKey(0.3f, 1.0f, 0.3f, 0.0f)
            .addColorKey(0.6f, 0.3f, 0.1f, 0.0f)
            .addColorKey(1.0f, 0.1f, 0.1f, 0.1f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.3f, 0.9f)
            .addAlphaKey(1, 0);
    }

    /** 传送门渐变 */
    public static Gradient portal() {
        return new Gradient().addColorKey(0, 0.4f, 0.2f, 0.8f)
            .addColorKey(0.5f, 0.6f, 0.3f, 1.0f)
            .addColorKey(1, 0.3f, 0.1f, 0.6f)
            .addAlphaKey(0, 0.8f)
            .addAlphaKey(0.5f, 1.0f)
            .addAlphaKey(1, 0);
    }

    /** 治愈渐变 */
    public static Gradient healing() {
        return new Gradient().addColorKey(0, 0.3f, 1.0f, 0.5f)
            .addColorKey(0.5f, 0.5f, 1.0f, 0.7f)
            .addColorKey(1, 0.2f, 0.8f, 0.4f)
            .addAlphaKey(0, 0.8f)
            .addAlphaKey(0.7f, 0.6f)
            .addAlphaKey(1, 0);
    }

    /** 电弧渐变 */
    public static Gradient lightning() {
        return new Gradient().addColorKey(0, 0.8f, 0.9f, 1.0f)
            .addColorKey(0.3f, 0.6f, 0.8f, 1.0f)
            .addColorKey(1, 0.4f, 0.5f, 0.9f)
            .addAlphaKey(0, 1)
            .addAlphaKey(0.5f, 0.8f)
            .addAlphaKey(1, 0);
    }

    /** 彩虹渐变 */
    public static Gradient rainbow() {
        return new Gradient().addColorKey(0, 1, 0, 0)
            .addColorKey(0.17f, 1, 0.5f, 0)
            .addColorKey(0.33f, 1, 1, 0)
            .addColorKey(0.5f, 0, 1, 0)
            .addColorKey(0.67f, 0, 0.5f, 1)
            .addColorKey(0.83f, 0.5f, 0, 1)
            .addColorKey(1, 1, 0, 0.5f)
            .addAlphaKey(0, 1)
            .addAlphaKey(1, 1);
    }
}
