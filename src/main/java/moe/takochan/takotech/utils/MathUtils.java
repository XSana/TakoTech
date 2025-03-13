package moe.takochan.takotech.utils;

import net.minecraft.util.MathHelper;

public class MathUtils extends MathHelper {

    /**
     * 对浮点数进行向上取整
     *
     * @param pValue 浮点数
     * @return 向上取整后的整数值
     */
    public static int ceil(float pValue) {
        int i = (int) pValue;
        return pValue > (float) i ? i + 1 : i;
    }

    /**
     * 对双精度浮点数进行向上取整
     *
     * @param pValue 双精度浮点数
     * @return 向上取整后的整数值
     */
    public static int ceil(double pValue) {
        int i = (int) pValue;
        return pValue > (float) i ? i + 1 : i;
    }

    /**
     * 将整数限制在指定范围内
     *
     * @param num 要限制的整数
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的整数
     */
    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    /**
     * 将浮点数限制在指定范围内
     *
     * @param num 要限制的浮点数
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的浮点数
     */
    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    /**
     * 将双精度浮点数限制在指定范围内
     *
     * @param num 要限制的双精度浮点数
     * @param min 最小值
     * @param max 最大值
     * @return 限制后的双精度浮点数
     */
    public static double clamp(double num, double min, double max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }
}
