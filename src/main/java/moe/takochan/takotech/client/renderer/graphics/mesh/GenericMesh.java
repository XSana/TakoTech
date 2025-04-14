package moe.takochan.takotech.client.renderer.graphics.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * 通用 Mesh 实现，支持多属性顶点（如位置、法线、UV 等），并封装 VAO/VBO/EBO。
 */
@SideOnly(Side.CLIENT)
public class GenericMesh {

    private int VAO = 0;
    private int VBO = 0;
    private int IBO = 0;
    private int elementCount = 0;

    private boolean initialized = false;

    public GenericMesh(float[] vertexData, int[] indexData, int strideBytes, VertexAttribute... attributes) {
        this.elementCount = indexData.length;

        VAO = GL30.glGenVertexArrays();
        VBO = GL15.glGenBuffers();
        IBO = GL15.glGenBuffers();

        // Upload vertex buffer
        GL30.glBindVertexArray(VAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO);
        FloatBuffer vb = BufferUtils.createFloatBuffer(vertexData.length)
            .put(vertexData);
        vb.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vb, GL15.GL_STATIC_DRAW);

        for (int i = 0; i < attributes.length; i++) {
            VertexAttribute attr = attributes[i];
            GL20.glEnableVertexAttribArray(i);
            GL20.glVertexAttribPointer(i, attr.componentCount, attr.dataType, false, strideBytes, attr.offset);
        }

        // Upload index buffer
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, IBO);
        IntBuffer ib = BufferUtils.createIntBuffer(indexData.length)
            .put(indexData);
        ib.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);

        // Unbind
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        initialized = true;
    }

    public void bind() {
        if (initialized) {
            GL30.glBindVertexArray(VAO);
        }
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    public void draw() {
        if (initialized) {
            bind();
            GL11.glDrawElements(GL11.GL_TRIANGLES, elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbind();
        }
    }

    public void delete() {
        if (initialized) {
            GL30.glDeleteVertexArrays(VAO);
            GL15.glDeleteBuffers(VBO);
            GL15.glDeleteBuffers(IBO);
            initialized = false;
        }
    }

    public boolean isValid() {
        return initialized;
    }
}
