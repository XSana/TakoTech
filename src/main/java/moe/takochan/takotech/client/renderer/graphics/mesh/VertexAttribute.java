package moe.takochan.takotech.client.renderer.graphics.mesh;

import org.lwjgl.opengl.GL11;

/**
 * 顶点属性描述，遵循 OpenGL 标准命名。
 * 用于配置 glVertexAttribPointer。
 */
public class VertexAttribute {

    /** 数据类型 (GL_FLOAT, GL_INT, GL_UNSIGNED_BYTE, etc.) */
    public final int type;
    /** 属性在单个顶点内的字节偏移 */
    public final int offset;
    /** 分量数量 (vec2=2, vec3=3, vec4=4) */
    public final int size;
    /** 是否归一化到 [0,1] 或 [-1,1] */
    public final boolean normalized;

    /**
     * 创建顶点属性
     *
     * @param type       数据类型
     * @param offset     字节偏移
     * @param size       分量数量
     * @param normalized 是否归一化
     */
    public VertexAttribute(int type, int offset, int size, boolean normalized) {
        this.type = type;
        this.offset = offset;
        this.size = size;
        this.normalized = normalized;
    }

    /**
     * 创建非归一化顶点属性
     */
    public VertexAttribute(int type, int offset, int size) {
        this(type, offset, size, false);
    }

    // ==================== 常用属性工厂方法 ====================

    /**
     * 2D 位置属性 (vec2)
     */
    public static VertexAttribute position2D(int offset) {
        return new VertexAttribute(GL11.GL_FLOAT, offset, 2);
    }

    /**
     * 3D 位置属性 (vec3)
     */
    public static VertexAttribute position3D(int offset) {
        return new VertexAttribute(GL11.GL_FLOAT, offset, 3);
    }

    /**
     * 纹理坐标属性 (vec2)
     */
    public static VertexAttribute texCoord(int offset) {
        return new VertexAttribute(GL11.GL_FLOAT, offset, 2);
    }

    /**
     * 法线属性 (vec3)
     */
    public static VertexAttribute normal(int offset) {
        return new VertexAttribute(GL11.GL_FLOAT, offset, 3);
    }

    /**
     * 颜色属性 (vec4, float)
     */
    public static VertexAttribute colorFloat(int offset) {
        return new VertexAttribute(GL11.GL_FLOAT, offset, 4);
    }

    /**
     * 颜色属性 (vec4, unsigned byte, normalized)
     */
    public static VertexAttribute colorByte(int offset) {
        return new VertexAttribute(GL11.GL_UNSIGNED_BYTE, offset, 4, true);
    }
}
