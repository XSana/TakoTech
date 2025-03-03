package moe.takochan.takotech.utils;

import net.minecraft.util.MathHelper;

public class MathUtils extends MathHelper {

    public static int ceil(float pValue) {
        int i = (int) pValue;
        return pValue > (float) i ? i + 1 : i;
    }

    public static int ceil(double pValue) {
        int i = (int) pValue;
        return pValue > (float) i ? i + 1 : i;
    }


    /**
     * Returns the value of the first parameter, clamped to be within the lower and upper limits given by the second and
     * third parameters.
     */
    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    /**
     * Returns the value of the first parameter, clamped to be within the lower and upper limits given by the second and
     * third parameters
     */
    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static double clamp(double num, double min, double max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }
}
