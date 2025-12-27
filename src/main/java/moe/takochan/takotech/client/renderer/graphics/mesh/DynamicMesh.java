package moe.takochan.takotech.client.renderer.graphics.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 动态 Mesh 实现，支持每帧更新顶点数据。
 * 使用 GL_DYNAMIC_DRAW 提示 GPU 优化频繁更新场景。
 * 预分配缓冲区以避免每帧分配内存。
 */
@SideOnly(Side.CLIENT)
public class DynamicMesh extends Mesh {

    private final int maxVertices;
    private final int maxIndices;

    /** 预分配的顶点缓冲区 */
    private final FloatBuffer vertexBuffer;
    /** 预分配的索引缓冲区 */
    private final IntBuffer indexBuffer;

    /**
     * 使用 VertexFormat 创建动态 Mesh
     *
     * @param maxVertices 最大顶点数
     * @param maxIndices  最大索引数
     * @param format      顶点格式
     */
    public DynamicMesh(int maxVertices, int maxIndices, VertexFormat format) {
        super(format);
        this.maxVertices = maxVertices;
        this.maxIndices = maxIndices;

        // 预分配 CPU 端缓冲区
        int floatsPerVertex = format.getFloatsPerVertex();
        this.vertexBuffer = BufferUtils.createFloatBuffer(maxVertices * floatsPerVertex);
        this.indexBuffer = BufferUtils.createIntBuffer(maxIndices);

        initializeBuffers();
    }

    /**
     * 创建动态 Mesh（向后兼容）
     *
     * @param maxVertices 最大顶点数
     * @param maxIndices  最大索引数
     * @param strideBytes 每个顶点的字节数
     * @param attributes  顶点属性定义
     */
    public DynamicMesh(int maxVertices, int maxIndices, int strideBytes, VertexAttribute... attributes) {
        super(strideBytes, attributes);
        this.maxVertices = maxVertices;
        this.maxIndices = maxIndices;

        // 预分配 CPU 端缓冲区
        int floatsPerVertex = strideBytes / 4;
        this.vertexBuffer = BufferUtils.createFloatBuffer(maxVertices * floatsPerVertex);
        this.indexBuffer = BufferUtils.createIntBuffer(maxIndices);

        initializeBuffers();
    }

    /**
     * 初始化 GPU 缓冲区
     */
    private void initializeBuffers() {
        initBuffers();

        GL30.glBindVertexArray(vao);

        // 预分配 GPU 端 VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) maxVertices * strideBytes, GL15.GL_DYNAMIC_DRAW);

        setupVertexAttributes();

        // 预分配 GPU 端 EBO
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) maxIndices * 4, GL15.GL_DYNAMIC_DRAW);

        // 解绑 VAO（切换到默认 VAO 0）
        GL30.glBindVertexArray(0);

        // 在 VAO 0 状态下禁用顶点属性，确保不影响固定管线渲染
        disableAttributes();

        // 解绑 VBO/EBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        valid = true;
    }

    /**
     * 更新顶点和索引数据
     *
     * @param vertexData  顶点数据数组
     * @param vertexCount 要使用的顶点浮点数数量
     * @param indexData   索引数据数组
     * @param indexCount  要使用的索引数量
     */
    public void updateData(float[] vertexData, int vertexCount, int[] indexData, int indexCount) {
        if (!valid) return;

        elementCount = indexCount;

        // 保存当前状态
        int savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int savedVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int savedEbo = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        // 绑定自己的 VAO（重要：EBO 绑定是 VAO 状态的一部分！）
        GL30.glBindVertexArray(vao);

        // 更新顶点数据（复用预分配缓冲区）
        vertexBuffer.clear();
        vertexBuffer.put(vertexData, 0, vertexCount);
        vertexBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertexBuffer);

        // 更新索引数据
        indexBuffer.clear();
        indexBuffer.put(indexData, 0, indexCount);
        indexBuffer.flip();

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, indexBuffer);

        // 恢复状态
        // 注意：EBO 绑定是 VAO 状态的一部分，恢复 VAO 会自动恢复其 EBO
        GL30.glBindVertexArray(savedVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        // 只有在 VAO 0 时才手动恢复 EBO
        if (savedVao == 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedEbo);
        }
    }

    /**
     * 更新顶点和索引数据（使用整个数组）
     *
     * @param vertexData 顶点数据
     * @param indexData  索引数据
     */
    public void updateData(float[] vertexData, int[] indexData) {
        updateData(vertexData, vertexData.length, indexData, indexData.length);
    }

    public int getMaxVertices() {
        return maxVertices;
    }

    public int getMaxIndices() {
        return maxIndices;
    }
}
