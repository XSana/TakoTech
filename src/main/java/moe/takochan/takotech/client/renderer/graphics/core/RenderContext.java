package moe.takochan.takotech.client.renderer.graphics.core;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 渲染上下文。
 * 包含渲染所需的相机信息、矩阵和着色器。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     RenderContext ctx = new RenderContext();
 *     ctx.syncFromMinecraft(partialTicks);
 *
 *     // 在 Component 中使用
 *     ctx.setModelMatrix(transform.getWorldMatrix());
 *     material.apply(ctx.getShader());
 *     mesh.draw();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class RenderContext {

    /** 视图矩阵 (从 MC 获取) */
    private final float[] viewMatrix = new float[16];

    /** 投影矩阵 (从 MC 获取) */
    private final float[] projMatrix = new float[16];

    /** 模型矩阵 (每个对象单独设置) */
    private final float[] modelMatrix = new float[16];

    /** 相机位置 (世界坐标) */
    private final float[] cameraPos = new float[3];

    /** 相机方向 */
    private float cameraPitch;
    private float cameraYaw;

    /** 当前着色器 */
    private ShaderProgram shader;

    /** partialTicks */
    private float partialTicks;

    /** 临时缓冲区 */
    private final FloatBuffer mvBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * 创建渲染上下文
     */
    public RenderContext() {
        // 初始化模型矩阵为单位矩阵
        setIdentityMatrix(modelMatrix);
    }

    /**
     * 从 Minecraft 同步相机信息
     *
     * @param partialTicks 渲染插值
     */
    public void syncFromMinecraft(float partialTicks) {
        this.partialTicks = partialTicks;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        // 获取当前 GL 矩阵
        mvBuffer.clear();
        projBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mvBuffer);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuffer);

        mvBuffer.get(viewMatrix);
        projBuffer.get(projMatrix);

        mvBuffer.rewind();
        projBuffer.rewind();

        // RenderManager.renderPosX/Y/Z matches the active render camera.
        double camX = RenderManager.renderPosX;
        double camY = RenderManager.renderPosY;
        double camZ = RenderManager.renderPosZ;

        cameraPos[0] = (float) camX;
        cameraPos[1] = (float) camY;
        cameraPos[2] = (float) camZ;

        // 相机方向
        cameraPitch = player.rotationPitch;
        cameraYaw = player.rotationYaw;
    }

    // ==================== 矩阵访问 ====================

    /**
     * 获取视图矩阵
     *
     * @return 16 个浮点数的数组 (column-major)
     */
    public float[] getViewMatrix() {
        return viewMatrix;
    }

    /**
     * 获取投影矩阵
     *
     * @return 16 个浮点数的数组 (column-major)
     */
    public float[] getProjMatrix() {
        return projMatrix;
    }

    /**
     * 获取模型矩阵
     *
     * @return 16 个浮点数的数组 (column-major)
     */
    public float[] getModelMatrix() {
        return modelMatrix;
    }

    /**
     * 设置模型矩阵
     *
     * @param matrix LWJGL Matrix4f
     */
    public void setModelMatrix(Matrix4f matrix) {
        if (matrix != null) {
            modelBuffer.clear();
            matrix.store(modelBuffer);
            modelBuffer.flip();
            modelBuffer.get(modelMatrix);
            modelBuffer.rewind();
        } else {
            setIdentityMatrix(modelMatrix);
        }
    }

    /**
     * 设置模型矩阵
     *
     * @param matrix 16 个浮点数的数组
     */
    public void setModelMatrix(float[] matrix) {
        if (matrix != null && matrix.length == 16) {
            System.arraycopy(matrix, 0, modelMatrix, 0, 16);
        } else {
            setIdentityMatrix(modelMatrix);
        }
    }

    /**
     * 重置模型矩阵为单位矩阵
     */
    public void resetModelMatrix() {
        setIdentityMatrix(modelMatrix);
    }

    /**
     * 设置视图矩阵
     *
     * @param matrix LWJGL Matrix4f
     */
    public void setViewMatrix(Matrix4f matrix) {
        if (matrix != null) {
            mvBuffer.clear();
            matrix.store(mvBuffer);
            mvBuffer.flip();
            mvBuffer.get(viewMatrix);
            mvBuffer.rewind();
        } else {
            setIdentityMatrix(viewMatrix);
        }
    }

    /**
     * 设置投影矩阵
     *
     * @param matrix LWJGL Matrix4f
     */
    public void setProjMatrix(Matrix4f matrix) {
        if (matrix != null) {
            projBuffer.clear();
            matrix.store(projBuffer);
            projBuffer.flip();
            projBuffer.get(projMatrix);
            projBuffer.rewind();
        } else {
            setIdentityMatrix(projMatrix);
        }
    }

    /**
     * 从 Camera 同步视图和投影矩阵
     *
     * @param camera 相机对象
     */
    public void syncFromCamera(Camera camera) {
        if (camera == null) return;
        setViewMatrix(camera.getViewMatrix());
        setProjMatrix(camera.getProjectionMatrix());
        Vector3f pos = camera.getPosition();
        setCameraPos(pos.x, pos.y, pos.z);
    }

    // ==================== 相机访问 ====================

    /**
     * 获取相机位置
     *
     * @return [x, y, z]
     */
    public float[] getCameraPos() {
        return cameraPos;
    }

    /**
     * 获取相机 X 坐标
     *
     * @return X 坐标
     */
    public float getCameraX() {
        return cameraPos[0];
    }

    /**
     * 获取相机 Y 坐标
     *
     * @return Y 坐标
     */
    public float getCameraY() {
        return cameraPos[1];
    }

    /**
     * 获取相机 Z 坐标
     *
     * @return Z 坐标
     */
    public float getCameraZ() {
        return cameraPos[2];
    }

    /**
     * 设置相机位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    public void setCameraPos(float x, float y, float z) {
        cameraPos[0] = x;
        cameraPos[1] = y;
        cameraPos[2] = z;
    }

    /**
     * 获取相机俯仰角
     *
     * @return 俯仰角（度）
     */
    public float getCameraPitch() {
        return cameraPitch;
    }

    /**
     * 获取相机偏航角
     *
     * @return 偏航角（度）
     */
    public float getCameraYaw() {
        return cameraYaw;
    }

    /**
     * 获取 partialTicks
     *
     * @return partialTicks
     */
    public float getPartialTicks() {
        return partialTicks;
    }

    // ==================== 着色器 ====================

    /**
     * 获取当前着色器
     *
     * @return 着色器
     */
    public ShaderProgram getShader() {
        return shader;
    }

    /**
     * 设置着色器
     *
     * @param shader 着色器
     */
    public void setShader(ShaderProgram shader) {
        this.shader = shader;
    }

    /**
     * 应用矩阵 uniform 到当前着色器
     */
    public void applyMatricesToShader() {
        if (shader == null || !shader.isValid()) return;

        shader.setUniformMatrix4("uViewMatrix", false, MathUtils.toFloatBuffer(viewMatrix));
        shader.setUniformMatrix4("uProjMatrix", false, MathUtils.toFloatBuffer(projMatrix));
        shader.setUniformMatrix4("uModelMatrix", false, MathUtils.toFloatBuffer(modelMatrix));
        shader.setUniformMatrix4("uView", false, MathUtils.toFloatBuffer(viewMatrix));
        shader.setUniformMatrix4("uProjection", false, MathUtils.toFloatBuffer(projMatrix));
        shader.setUniformMatrix4("uModel", false, MathUtils.toFloatBuffer(modelMatrix));
        shader.setUniformVec3("uCameraPos", cameraPos[0], cameraPos[1], cameraPos[2]);
    }

    /**
     * 仅应用视图和投影矩阵到着色器
     */
    public void applyViewProjToShader() {
        if (shader == null || !shader.isValid()) return;

        shader.setUniformMatrix4("uViewMatrix", false, MathUtils.toFloatBuffer(viewMatrix));
        shader.setUniformMatrix4("uProjMatrix", false, MathUtils.toFloatBuffer(projMatrix));
        shader.setUniformMatrix4("uView", false, MathUtils.toFloatBuffer(viewMatrix));
        shader.setUniformMatrix4("uProjection", false, MathUtils.toFloatBuffer(projMatrix));
        shader.setUniformVec3("uCameraPos", cameraPos[0], cameraPos[1], cameraPos[2]);
        // 启用变换矩阵（某些 shader 使用这些 uniform 控制是否应用变换）
        shader.setUniformBool("uUseProjection", true);
        shader.setUniformBool("uUseView", true);
    }

    /**
     * 仅应用模型矩阵到着色器
     */
    public void applyModelToShader() {
        if (shader == null || !shader.isValid()) return;

        shader.setUniformMatrix4("uModelMatrix", false, MathUtils.toFloatBuffer(modelMatrix));
        shader.setUniformMatrix4("uModel", false, MathUtils.toFloatBuffer(modelMatrix));
    }

    // ==================== 工具方法 ====================

    /**
     * 设置单位矩阵
     */
    private void setIdentityMatrix(float[] matrix) {
        for (int i = 0; i < 16; i++) {
            matrix[i] = (i % 5 == 0) ? 1.0f : 0.0f;
        }
    }

    /**
     * 计算从相机到指定点的距离平方
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 距离平方
     */
    public float distanceSquaredToCamera(float x, float y, float z) {
        float dx = x - cameraPos[0];
        float dy = y - cameraPos[1];
        float dz = z - cameraPos[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 计算从相机到指定点的距离
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 距离
     */
    public float distanceToCamera(float x, float y, float z) {
        return (float) Math.sqrt(distanceSquaredToCamera(x, y, z));
    }
}
