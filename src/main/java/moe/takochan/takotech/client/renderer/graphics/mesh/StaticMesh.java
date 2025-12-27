package moe.takochan.takotech.client.renderer.graphics.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 静态 Mesh 实现，用于不需要频繁更新的几何体。
 * 使用 GL_STATIC_DRAW 提示 GPU 优化存储。
 */
@SideOnly(Side.CLIENT)
public class StaticMesh extends Mesh {

    /**
     * 创建静态 Mesh
     *
     * @param vertexData  顶点数据
     * @param indexData   索引数据
     * @param strideBytes 每个顶点的字节数
     * @param attributes  顶点属性定义
     */
    public StaticMesh(float[] vertexData, int[] indexData, int strideBytes, VertexAttribute... attributes) {
        super(strideBytes, attributes);
        this.elementCount = indexData.length;

        initBuffers();

        GL30.glBindVertexArray(vao);

        // 上传顶点数据
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer vb = BufferUtils.createFloatBuffer(vertexData.length);
        vb.put(vertexData)
            .flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vb, GL15.GL_STATIC_DRAW);

        setupVertexAttributes();

        // 上传索引数据
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = BufferUtils.createIntBuffer(indexData.length);
        ib.put(indexData)
            .flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);

        // 解绑 VAO（切换到默认 VAO 0）
        GL30.glBindVertexArray(0);

        // 在 VAO 0 状态下禁用顶点属性，确保不影响固定管线渲染
        disableAttributes();

        // 解绑 VBO/EBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        valid = true;
    }
}
