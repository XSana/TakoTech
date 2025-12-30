package moe.takochan.takotech.client.renderer.util;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * OpenGL 状态管理器。
 * 用于在自定义渲染前后保存和恢复 GL 状态，确保与 MC 渲染系统兼容。
 *
 * <p>
 * MC 1.7.10 使用混合的 GL 状态：
 * </p>
 * <ul>
 * <li>传统固定管线状态（glPushAttrib 可保存）</li>
 * <li>现代可编程管线状态（需要手动保存）</li>
 * </ul>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * GLStateManager state = new GLStateManager();
 * state.save();
 * try {
 *     // 自定义渲染代码
 * } finally {
 *     state.restore();
 * }
 * </pre>
 *
 * <p>
 * 或者使用 try-with-resources:
 * </p>
 *
 * <pre>
 * try (GLStateManager state = GLStateManager.save()) {
 *     // 自定义渲染代码
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class GLStateManager implements AutoCloseable {

    // 传统 GL 状态标志
    private boolean legacySaved = false;

    // 现代 GL 状态（glPushAttrib 不保存这些）
    private int savedVao;
    private int savedVbo;
    private int savedEbo;
    private int savedProgram;
    private int savedActiveTexture;
    private int savedTexture2D;

    /**
     * 创建状态管理器并立即保存状态
     */
    public static GLStateManager save() {
        GLStateManager manager = new GLStateManager();
        manager.saveState();
        return manager;
    }

    /**
     * 保存当前 GL 状态
     */
    public void saveState() {
        // 传统 GL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glPushMatrix();
        legacySaved = true;

        // 现代 GL 状态（glPushAttrib 不保存这些）
        savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        savedVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        savedEbo = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        savedActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        savedTexture2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    /**
     * 恢复之前保存的 GL 状态
     */
    public void restoreState() {
        if (!legacySaved) return;

        // 恢复现代 GL 状态（必须在 glPopAttrib 之前）
        GL20.glUseProgram(savedProgram);

        // 恢复 VAO（会自动恢复其记录的 EBO 绑定）
        GL30.glBindVertexArray(savedVao);

        // 恢复 VBO（全局状态，不是 VAO 的一部分）
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);

        // 只有在 VAO 0 时才手动恢复 EBO（因为 VAO 0 的 EBO 是全局状态）
        // 注意：如果 savedVao != 0，绑定 EBO 会污染那个 VAO 的 EBO 状态！
        if (savedVao == 0) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedEbo);
        }

        GL13.glActiveTexture(savedActiveTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTexture2D);

        // 恢复传统 GL 状态
        GL11.glPopMatrix();
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();

        legacySaved = false;
    }

    /**
     * 解绑所有现代 GL 对象，确保后续 MC 渲染正常
     */
    public static void unbindAll() {
        GL20.glUseProgram(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * 设置适合 2D HUD 渲染的 GL 状态
     */
    public static void setupForHUD() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    /**
     * 设置适合 3D 世界渲染的 GL 状态
     */
    public static void setupForWorld3D() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false); // 半透明物体不写入深度
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    /**
     * 设置 MC 字体渲染需要的 GL 状态
     */
    public static void setupForText() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    public void close() {
        restoreState();
        // 确保 MC 固定管线渲染不受影响 - VBO/VAO 必须解绑
        // MC 1.7.10 的 Tessellator 使用 glTexCoordPointer 等函数，
        // 这些函数在 VBO 绑定时会抛出 "Cannot use Buffers when Array Buffer Object is enabled"
        unbindAll();
    }
}
