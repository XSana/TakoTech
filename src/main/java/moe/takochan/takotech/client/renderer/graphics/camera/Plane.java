package moe.takochan.takotech.client.renderer.graphics.camera;

import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 平面方程：ax + by + cz + d = 0
 * 法向量 (a, b, c) 指向平面的正半空间。
 */
@SideOnly(Side.CLIENT)
public class Plane {

    /** 平面法向量 */
    public float a, b, c;

    /** 平面常数项 */
    public float d;

    public Plane() {}

    public Plane(float a, float b, float c, float d) {
        set(a, b, c, d);
    }

    /**
     * 设置平面参数
     */
    public void set(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /**
     * 归一化平面方程（使法向量长度为 1）
     */
    public void normalize() {
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        if (length > 0) {
            float invLength = 1.0f / length;
            a *= invLength;
            b *= invLength;
            c *= invLength;
            d *= invLength;
        }
    }

    /**
     * 计算点到平面的有符号距离
     * 正值：点在平面正半空间（法向量方向）
     * 负值：点在平面负半空间
     * 零：点在平面上
     */
    public float distanceToPoint(float x, float y, float z) {
        return a * x + b * y + c * z + d;
    }

    /**
     * 计算点到平面的有符号距离（向量版本）
     */
    public float distanceToPoint(Vector3f point) {
        return distanceToPoint(point.x, point.y, point.z);
    }

    /**
     * 判断点是否在平面正半空间
     */
    public boolean isInFront(float x, float y, float z) {
        return distanceToPoint(x, y, z) >= 0;
    }

    /**
     * 判断点是否在平面正半空间（向量版本）
     */
    public boolean isInFront(Vector3f point) {
        return distanceToPoint(point) >= 0;
    }

    @Override
    public String toString() {
        return String.format("Plane[%.3fx + %.3fy + %.3fz + %.3f = 0]", a, b, c, d);
    }
}
