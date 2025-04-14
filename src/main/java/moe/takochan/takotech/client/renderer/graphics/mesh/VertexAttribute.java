package moe.takochan.takotech.client.renderer.graphics.mesh;

public class VertexAttribute {

    public final int dataType; // GL_FLOAT / GL_INT ...
    public final int offset;   // 属性在单个顶点内的字节偏移
    public final int componentCount; // vec2 = 2, vec3 = 3, ...

    public VertexAttribute(int dataType, int offset, int componentCount) {
        this.dataType = dataType;
        this.offset = offset;
        this.componentCount = componentCount;
    }
}
