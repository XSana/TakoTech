package moe.takochan.takotech.client.renderer.graphics.state;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * OpenGL 状态管理器。
 * 提供状态保存/恢复功能，确保与 Minecraft 固定管线兼容。
 */
@SideOnly(Side.CLIENT)
public final class RenderState {

    private RenderState() {}

    /**
     * 保存当前 OpenGL 状态快照
     *
     * @return 状态快照对象
     */
    public static StateSnapshot save() {
        return new StateSnapshot();
    }

    /**
     * 恢复之前保存的 OpenGL 状态
     *
     * @param snapshot 要恢复的状态快照
     */
    public static void restore(StateSnapshot snapshot) {
        if (snapshot == null) return;
        snapshot.restore();
    }

    // ==================== 预定义状态设置 ====================

    /**
     * 设置标准 Alpha 混合模式
     */
    public static void setBlendAlpha() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * 设置加法混合模式
     */
    public static void setBlendAdditive() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    /**
     * 设置乘法混合模式
     */
    public static void setBlendMultiply() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
    }

    /**
     * 禁用混合
     */
    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * 启用深度测试
     */
    public static void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /**
     * 禁用深度测试
     */
    public static void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    /**
     * 启用深度写入
     */
    public static void enableDepthMask() {
        GL11.glDepthMask(true);
    }

    /**
     * 禁用深度写入
     */
    public static void disableDepthMask() {
        GL11.glDepthMask(false);
    }

    /**
     * 启用背面剔除
     */
    public static void enableCullFace() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    /**
     * 禁用背面剔除
     */
    public static void disableCullFace() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    /**
     * 启用纹理
     */
    public static void enableTexture2D() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * 禁用纹理
     */
    public static void disableTexture2D() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /**
     * OpenGL 状态快照，用于保存和恢复状态。
     */
    public static class StateSnapshot {

        // 缓冲区绑定状态
        private final int vao;
        private final int vbo;
        private final int ebo;
        private final int program;
        private final int texture2D;
        private final int activeTexture;

        // 启用/禁用状态
        private final boolean blend;
        private final boolean depthTest;
        private final boolean cullFace;
        private final boolean texture2DEnabled;
        private final boolean alphaTest;
        private final boolean lighting;

        // 混合函数
        private final int blendSrc;
        private final int blendDst;

        // 深度函数
        private final int depthFunc;
        private final boolean depthMask;

        // 颜色（使用静态缓冲区避免每次分配）
        private static final FloatBuffer COLOR_BUFFER = BufferUtils.createFloatBuffer(16);
        private final float colorR, colorG, colorB, colorA;

        /**
         * 创建快照时自动捕获当前状态
         */
        StateSnapshot() {
            // 缓冲区状态
            this.vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            this.vbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            this.ebo = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            this.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            this.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            this.texture2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

            // 启用状态
            this.blend = GL11.glIsEnabled(GL11.GL_BLEND);
            this.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            this.cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            this.texture2DEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            this.alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            this.lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);

            // 混合函数
            this.blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            this.blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);

            // 深度状态
            this.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            this.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

            // 当前颜色
            COLOR_BUFFER.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR_BUFFER);
            this.colorR = COLOR_BUFFER.get(0);
            this.colorG = COLOR_BUFFER.get(1);
            this.colorB = COLOR_BUFFER.get(2);
            this.colorA = COLOR_BUFFER.get(3);
        }

        /**
         * 恢复所有保存的状态
         */
        void restore() {
            // 恢复 shader 程序
            GL20.glUseProgram(program);

            // 恢复缓冲区绑定
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);

            // 恢复纹理
            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture2D);

            // 恢复启用状态
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_DEPTH_TEST, depthTest);
            setEnabled(GL11.GL_CULL_FACE, cullFace);
            setEnabled(GL11.GL_TEXTURE_2D, texture2DEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, alphaTest);
            setEnabled(GL11.GL_LIGHTING, lighting);

            // 恢复混合函数
            GL11.glBlendFunc(blendSrc, blendDst);

            // 恢复深度状态
            GL11.glDepthFunc(depthFunc);
            GL11.glDepthMask(depthMask);

            // 恢复颜色
            GL11.glColor4f(colorR, colorG, colorB, colorA);
        }

        private static void setEnabled(int cap, boolean enabled) {
            if (enabled) {
                GL11.glEnable(cap);
            } else {
                GL11.glDisable(cap);
            }
        }

        // ==================== Getters ====================

        public int getVao() {
            return vao;
        }

        public int getVbo() {
            return vbo;
        }

        public int getEbo() {
            return ebo;
        }

        public int getProgram() {
            return program;
        }

        public int getTexture2D() {
            return texture2D;
        }

        public boolean isBlendEnabled() {
            return blend;
        }

        public boolean isDepthTestEnabled() {
            return depthTest;
        }
    }
}
