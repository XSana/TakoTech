package moe.takochan.takotech.client.renderer.graphics.math;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 数学工具类，封装常用的矩阵和向量操作。
 * 基于 LWJGL 2.9 的 {@code org.lwjgl.util.vector} 包。
 */
@SideOnly(Side.CLIENT)
public final class MathUtils {

    /** 可复用的 FloatBuffer，用于传递矩阵到 OpenGL */
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    /** 临时向量，避免频繁分配 */
    private static final Vector3f TEMP_VEC_X = new Vector3f();
    private static final Vector3f TEMP_VEC_Y = new Vector3f();
    private static final Vector3f TEMP_VEC_Z = new Vector3f();

    private MathUtils() {}

    // ==================== 矩阵创建 ====================

    /**
     * 创建透视投影矩阵
     *
     * @param fovY   垂直视野角度（弧度）
     * @param aspect 宽高比
     * @param zNear  近裁剪面距离
     * @param zFar   远裁剪面距离
     * @param dest   目标矩阵（如果为 null 则创建新矩阵）
     * @return 投影矩阵
     */
    public static Matrix4f perspective(float fovY, float aspect, float zNear, float zFar, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setZero();

        float tanHalfFovy = (float) Math.tan(fovY / 2.0f);

        dest.m00 = 1.0f / (aspect * tanHalfFovy);
        dest.m11 = 1.0f / tanHalfFovy;
        dest.m22 = -(zFar + zNear) / (zFar - zNear);
        dest.m23 = -1.0f;
        dest.m32 = -(2.0f * zFar * zNear) / (zFar - zNear);

        return dest;
    }

    /**
     * 创建正交投影矩阵
     *
     * @param left   左边界
     * @param right  右边界
     * @param bottom 下边界
     * @param top    上边界
     * @param zNear  近裁剪面
     * @param zFar   远裁剪面
     * @param dest   目标矩阵
     * @return 正交投影矩阵
     */
    public static Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar,
        Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();

        dest.m00 = 2.0f / (right - left);
        dest.m11 = 2.0f / (top - bottom);
        dest.m22 = -2.0f / (zFar - zNear);
        dest.m30 = -(right + left) / (right - left);
        dest.m31 = -(top + bottom) / (top - bottom);
        dest.m32 = -(zFar + zNear) / (zFar - zNear);

        return dest;
    }

    /**
     * 创建 2D 正交投影矩阵（用于 GUI）
     *
     * @param width  屏幕宽度
     * @param height 屏幕高度
     * @param dest   目标矩阵
     * @return 正交投影矩阵（原点在左上角，Y 向下）
     */
    public static Matrix4f ortho2D(float width, float height, Matrix4f dest) {
        return ortho(0, width, height, 0, -1, 1, dest);
    }

    /**
     * 创建 lookAt 视图矩阵
     *
     * @param eyeX    相机位置 X
     * @param eyeY    相机位置 Y
     * @param eyeZ    相机位置 Z
     * @param centerX 目标点 X
     * @param centerY 目标点 Y
     * @param centerZ 目标点 Z
     * @param upX     上方向 X
     * @param upY     上方向 Y
     * @param upZ     上方向 Z
     * @param dest    目标矩阵
     * @return 视图矩阵
     */
    public static Matrix4f lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ,
        float upX, float upY, float upZ, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }

        // 计算 forward 向量 (z轴，指向相机后方)
        TEMP_VEC_Z.set(eyeX - centerX, eyeY - centerY, eyeZ - centerZ);
        TEMP_VEC_Z.normalise();

        // 计算 right 向量 (x轴)
        Vector3f up = TEMP_VEC_Y;
        up.set(upX, upY, upZ);
        Vector3f.cross(up, TEMP_VEC_Z, TEMP_VEC_X);
        TEMP_VEC_X.normalise();

        // 重新计算 up 向量 (y轴)
        Vector3f.cross(TEMP_VEC_Z, TEMP_VEC_X, TEMP_VEC_Y);

        dest.setIdentity();

        // 设置旋转部分
        dest.m00 = TEMP_VEC_X.x;
        dest.m10 = TEMP_VEC_X.y;
        dest.m20 = TEMP_VEC_X.z;

        dest.m01 = TEMP_VEC_Y.x;
        dest.m11 = TEMP_VEC_Y.y;
        dest.m21 = TEMP_VEC_Y.z;

        dest.m02 = TEMP_VEC_Z.x;
        dest.m12 = TEMP_VEC_Z.y;
        dest.m22 = TEMP_VEC_Z.z;

        // 设置平移部分
        dest.m30 = -Vector3f.dot(TEMP_VEC_X, new Vector3f(eyeX, eyeY, eyeZ));
        dest.m31 = -Vector3f.dot(TEMP_VEC_Y, new Vector3f(eyeX, eyeY, eyeZ));
        dest.m32 = -Vector3f.dot(TEMP_VEC_Z, new Vector3f(eyeX, eyeY, eyeZ));

        return dest;
    }

    /**
     * 创建 lookAt 视图矩阵（向量版本）
     */
    public static Matrix4f lookAt(Vector3f eye, Vector3f center, Vector3f up, Matrix4f dest) {
        return lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest);
    }

    /**
     * 创建平移矩阵
     */
    public static Matrix4f translation(float x, float y, float z, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();
        dest.m30 = x;
        dest.m31 = y;
        dest.m32 = z;
        return dest;
    }

    /**
     * 创建缩放矩阵
     */
    public static Matrix4f scale(float x, float y, float z, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();
        dest.m00 = x;
        dest.m11 = y;
        dest.m22 = z;
        return dest;
    }

    /**
     * 创建绕 X 轴旋转矩阵
     *
     * @param angle 旋转角度（弧度）
     */
    public static Matrix4f rotationX(float angle, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        dest.m11 = cos;
        dest.m12 = sin;
        dest.m21 = -sin;
        dest.m22 = cos;
        return dest;
    }

    /**
     * 创建绕 Y 轴旋转矩阵
     *
     * @param angle 旋转角度（弧度）
     */
    public static Matrix4f rotationY(float angle, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        dest.m00 = cos;
        dest.m02 = -sin;
        dest.m20 = sin;
        dest.m22 = cos;
        return dest;
    }

    /**
     * 创建绕 Z 轴旋转矩阵
     *
     * @param angle 旋转角度（弧度）
     */
    public static Matrix4f rotationZ(float angle, Matrix4f dest) {
        if (dest == null) {
            dest = new Matrix4f();
        }
        dest.setIdentity();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        dest.m00 = cos;
        dest.m01 = sin;
        dest.m10 = -sin;
        dest.m11 = cos;
        return dest;
    }

    // ==================== 矩阵操作 ====================

    /**
     * 矩阵乘法：left * right -> dest
     * <p>
     * 注意：LWJGL 的 Matrix4f.mul() 是 this * right，结果存入 dest
     * </p>
     */
    public static Matrix4f multiply(Matrix4f left, Matrix4f right, Matrix4f dest) {
        return Matrix4f.mul(left, right, dest);
    }

    /**
     * 将矩阵存入 FloatBuffer（列优先）
     *
     * @param matrix 源矩阵
     * @param buffer 目标缓冲区
     */
    public static void toBuffer(Matrix4f matrix, FloatBuffer buffer) {
        buffer.clear();
        matrix.store(buffer);
        buffer.flip();
    }

    /**
     * 获取可复用的矩阵缓冲区（线程不安全，仅用于单帧内临时传递）
     */
    public static FloatBuffer getMatrixBuffer(Matrix4f matrix) {
        toBuffer(matrix, MATRIX_BUFFER);
        return MATRIX_BUFFER;
    }

    /**
     * 将 float 数组转换为 FloatBuffer
     *
     * @param array 源数组
     * @return FloatBuffer
     */
    public static FloatBuffer toFloatBuffer(float[] array) {
        MATRIX_BUFFER.clear();
        MATRIX_BUFFER.put(array, 0, Math.min(array.length, 16));
        MATRIX_BUFFER.flip();
        return MATRIX_BUFFER;
    }

    // ==================== 向量操作 ====================

    /**
     * 向量线性插值
     */
    public static Vector3f lerp(Vector3f a, Vector3f b, float t, Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        dest.x = a.x + (b.x - a.x) * t;
        dest.y = a.y + (b.y - a.y) * t;
        dest.z = a.z + (b.z - a.z) * t;
        return dest;
    }

    /**
     * 用矩阵变换点（应用平移）
     */
    public static Vector3f transformPoint(Matrix4f matrix, Vector3f point, Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        float x = point.x;
        float y = point.y;
        float z = point.z;

        dest.x = matrix.m00 * x + matrix.m10 * y + matrix.m20 * z + matrix.m30;
        dest.y = matrix.m01 * x + matrix.m11 * y + matrix.m21 * z + matrix.m31;
        dest.z = matrix.m02 * x + matrix.m12 * y + matrix.m22 * z + matrix.m32;

        return dest;
    }

    /**
     * 用矩阵变换方向（不应用平移）
     */
    public static Vector3f transformDirection(Matrix4f matrix, Vector3f direction, Vector3f dest) {
        if (dest == null) {
            dest = new Vector3f();
        }
        float x = direction.x;
        float y = direction.y;
        float z = direction.z;

        dest.x = matrix.m00 * x + matrix.m10 * y + matrix.m20 * z;
        dest.y = matrix.m01 * x + matrix.m11 * y + matrix.m21 * z;
        dest.z = matrix.m02 * x + matrix.m12 * y + matrix.m22 * z;

        return dest;
    }

    // ==================== 角度转换 ====================

    /**
     * 角度转弧度
     */
    public static float toRadians(float degrees) {
        return degrees * (float) (Math.PI / 180.0);
    }

    /**
     * 弧度转角度
     */
    public static float toDegrees(float radians) {
        return radians * (float) (180.0 / Math.PI);
    }

    // ==================== 数值工具 ====================

    /**
     * 限制值在范围内
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制值在范围内（整数版本）
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 浮点数近似相等比较
     */
    public static boolean approximately(float a, float b) {
        return Math.abs(a - b) < 1e-6f;
    }

    /**
     * 浮点数近似相等比较（自定义精度）
     */
    public static boolean approximately(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }
}
