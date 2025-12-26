package moe.takochan.takotech.client.renderer.graphics.mesh;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Mesh 基类，封装 VAO/VBO/EBO 的通用操作。
 * 遵循 OpenGL 标准命名和资源管理模式。
 * 实现 AutoCloseable 以支持 try-with-resources。
 */
@SideOnly(Side.CLIENT)
public abstract class Mesh implements AutoCloseable {

    protected int vao = 0;
    protected int vbo = 0;
    protected int ebo = 0;
    protected int elementCount = 0;
    protected int drawMode = GL11.GL_TRIANGLES;
    protected boolean valid = false;

    protected final VertexFormat format;
    protected final int strideBytes;
    protected final VertexAttribute[] attributes;

    /**
     * 使用 VertexFormat 创建 Mesh
     *
     * @param format 顶点格式
     */
    protected Mesh(VertexFormat format) {
        this.format = format;
        this.strideBytes = format.getStride();
        this.attributes = format.getAttributes();
    }

    /**
     * 使用自定义属性创建 Mesh（向后兼容）
     *
     * @param strideBytes 每个顶点的字节数
     * @param attributes  顶点属性定义
     */
    protected Mesh(int strideBytes, VertexAttribute[] attributes) {
        this.format = null;
        this.strideBytes = strideBytes;
        this.attributes = attributes;
    }

    /**
     * 初始化 VAO/VBO/EBO
     */
    protected void initBuffers() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ebo = GL15.glGenBuffers();
    }

    /**
     * 配置顶点属性指针
     * 必须在 VAO 绑定状态下调用
     */
    protected void setupVertexAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            VertexAttribute attr = attributes[i];
            GL20.glEnableVertexAttribArray(i);
            GL20.glVertexAttribPointer(i, attr.size, attr.type, attr.normalized, strideBytes, attr.offset);
        }
    }

    /**
     * 启用顶点属性
     */
    protected void enableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            GL20.glEnableVertexAttribArray(i);
        }
    }

    /**
     * 禁用顶点属性（恢复固定管线兼容状态）
     */
    protected void disableAttributes() {
        for (int i = 0; i < attributes.length; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
    }

    /**
     * 绑定 VAO、VBO 和 EBO，并重新设置顶点属性
     * 注意：由于 Angelica/Embeddium GLStateManager 可能不完全兼容 VAO 状态，
     * 需要显式绑定所有缓冲区并重新设置属性以确保兼容性。
     */
    public void bind() {
        if (valid) {
            GL30.glBindVertexArray(vao);
            // 显式绑定 VBO 和 EBO 以兼容 Angelica GLStateManager
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            // 重新启用并设置顶点属性
            setupVertexAttributes();
        }
    }

    /**
     * 解绑 VAO、VBO、EBO 并禁用顶点属性
     */
    public void unbind() {
        // 禁用顶点属性以恢复固定管线兼容状态
        disableAttributes();
        // 解绑所有缓冲区
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * 绘制 Mesh
     */
    public void draw() {
        if (valid && elementCount > 0) {
            bind();
            GL11.glDrawElements(drawMode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbind();
        }
    }

    /**
     * 使用指定的绘制模式绘制
     *
     * @param mode 绘制模式 (GL_TRIANGLES, GL_LINES, etc.)
     */
    public void draw(int mode) {
        if (valid && elementCount > 0) {
            bind();
            GL11.glDrawElements(mode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbind();
        }
    }

    /**
     * 释放 GPU 资源（遗留方法，建议使用 close()）
     */
    public void delete() {
        cleanup();
    }

    /**
     * 实现 AutoCloseable，支持 try-with-resources
     */
    @Override
    public void close() {
        cleanup();
    }

    /**
     * 清理资源
     * 注意：必须在具有 OpenGL 上下文的线程中调用
     */
    protected void cleanup() {
        if (valid) {
            try {
                GL30.glDeleteVertexArrays(vao);
                GL15.glDeleteBuffers(vbo);
                GL15.glDeleteBuffers(ebo);
            } catch (Exception e) {
                // 忽略清理时的错误（可能没有 GL 上下文）
            }
            vao = vbo = ebo = 0;
            valid = false;
        }
    }

    /**
     * 设置绘制模式
     *
     * @param mode GL_TRIANGLES, GL_LINES, GL_POINTS, etc.
     */
    public void setDrawMode(int mode) {
        this.drawMode = mode;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public int getElementCount() {
        return elementCount;
    }

    public boolean isValid() {
        return valid;
    }

    public int getVao() {
        return vao;
    }

    public int getVbo() {
        return vbo;
    }

    public int getEbo() {
        return ebo;
    }

    public VertexFormat getFormat() {
        return format;
    }

    public int getStrideBytes() {
        return strideBytes;
    }
}
