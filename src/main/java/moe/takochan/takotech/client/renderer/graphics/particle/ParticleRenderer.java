package moe.takochan.takotech.client.renderer.graphics.particle;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.renderer.entity.RenderManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 粒子渲染器。
 * 使用 GPU 实例化渲染绘制粒子。
 *
 * <p>
 * 支持的渲染模式:
 * </p>
 * <ul>
 * <li>点精灵 (Point Sprite)</li>
 * <li>四边形 (Billboard Quad)</li>
 * <li>软粒子 (带深度衰减)</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class ParticleRenderer {

    /** 渲染模式 */
    public enum RenderMode {
        /** 点精灵 */
        POINT_SPRITE,
        /** 广告牌四边形 */
        BILLBOARD_QUAD,
        /** 拉伸广告牌（沿速度方向） */
        STRETCHED_BILLBOARD,
        /** 3D 网格模型 */
        MESH
    }

    /** 内置几何体类型 */
    public enum BuiltinMesh {
        /** 立方体 */
        CUBE,
        /** 四面体 */
        TETRAHEDRON,
        /** 八面体 */
        OCTAHEDRON,
        /** 二十面体 */
        ICOSAHEDRON
    }

    /** 混合模式 */
    public enum BlendMode {
        /** Alpha 混合 */
        ALPHA,
        /** 加法混合 */
        ADDITIVE,
        /** 乘法混合 */
        MULTIPLY,
        /** 预乘 Alpha */
        PREMULTIPLIED
    }

    /** 渲染着色器 */
    private ShaderProgram shader;

    /** VAO */
    private int vao;

    /** 四边形顶点 VBO */
    private int quadVbo;

    /** 渲染模式 */
    private RenderMode renderMode = RenderMode.BILLBOARD_QUAD;

    /** 混合模式 */
    private BlendMode blendMode = BlendMode.ALPHA;

    /** 是否启用深度写入 */
    private boolean depthWrite = false;

    /** 是否启用软粒子 */
    private boolean softParticles = false;

    /** 软粒子衰减距离 */
    private float softParticleDistance = 0.5f;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** Mesh VAO */
    private int meshVao = 0;

    /** Mesh VBO (顶点数据) */
    private int meshVbo = 0;

    /** Mesh EBO (索引数据) */
    private int meshEbo = 0;

    /** Mesh 顶点数量 */
    private int meshVertexCount = 0;

    /** Mesh 索引数量 */
    private int meshIndexCount = 0;

    /** Mesh 着色器 */
    private ShaderProgram meshShader;

    /** 当前内置 Mesh 类型 */
    private BuiltinMesh currentBuiltinMesh = null;

    /** 四边形顶点数据 (2D position + UV) */
    private static final float[] QUAD_VERTICES = { // @formatter:off
        // pos.x, pos.y, uv.x, uv.y
        -0.5f, -0.5f, 0.0f, 0.0f,
         0.5f, -0.5f, 1.0f, 0.0f,
         0.5f,  0.5f, 1.0f, 1.0f,
        -0.5f, -0.5f, 0.0f, 0.0f,
         0.5f,  0.5f, 1.0f, 1.0f,
        -0.5f,  0.5f, 0.0f, 1.0f
    }; // @formatter:on

    /**
     * 创建粒子渲染器
     */
    public ParticleRenderer() {}

    /**
     * 初始化渲染器
     *
     * @return true 初始化成功
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            // 创建 VAO
            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            // 创建四边形 VBO
            quadVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);

            FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(QUAD_VERTICES.length);
            vertexBuffer.put(QUAD_VERTICES)
                .flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

            // 设置顶点属性 (位置 + UV)
            // location 0: vec2 position
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0);
            GL20.glEnableVertexAttribArray(0);

            // location 1: vec2 uv
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4);
            GL20.glEnableVertexAttribArray(1);

            GL30.glBindVertexArray(0);

            // 加载着色器
            shader = new ShaderProgram("takotech", "shaders/particle/particle.vert", "shaders/particle/particle.frag");

            if (!shader.isValid()) {
                TakoTechMod.LOG.warn("ParticleRenderer: Failed to load shader, using fallback");
                shader = createFallbackShader();
            }

            if (shader == null || !shader.isValid()) {
                TakoTechMod.LOG.error("ParticleRenderer: Failed to initialize shader");
                return false;
            }

            initialized = true;
            TakoTechMod.LOG.info("ParticleRenderer: Initialized successfully");
            return true;

        } catch (Exception e) {
            TakoTechMod.LOG.error("ParticleRenderer: Initialization failed", e);
            cleanup();
            return false;
        }
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 渲染粒子
     *
     * @param buffer     粒子缓冲区
     * @param viewMatrix 视图矩阵 (16 floats, column-major)
     * @param projMatrix 投影矩阵 (16 floats, column-major)
     * @param cameraPos  相机位置 [x, y, z]
     * @param textureId  粒子纹理 ID (0 表示无纹理)
     */
    public void render(ParticleBuffer buffer, float[] viewMatrix, float[] projMatrix, float[] cameraPos,
        int textureId) {
        render(buffer, viewMatrix, projMatrix, cameraPos, textureId, buffer.getMaxParticles());
    }

    /**
     * 渲染粒子
     *
     * @param buffer        粒子缓冲区
     * @param viewMatrix    视图矩阵
     * @param projMatrix    投影矩阵
     * @param cameraPos     相机位置
     * @param textureId     纹理 ID
     * @param particleCount 要渲染的粒子数量
     */
    public void render(ParticleBuffer buffer, float[] viewMatrix, float[] projMatrix, float[] cameraPos, int textureId,
        int particleCount) {

        if (!initialized || particleCount <= 0) {
            return;
        }

        // MESH 模式使用专用渲染路径
        if (renderMode == RenderMode.MESH) {
            renderMesh(buffer, viewMatrix, projMatrix, cameraPos, particleCount);
            return;
        }

        if (shader == null) {
            return;
        }

        // 保存 GL 状态
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // 设置渲染状态
        setupRenderState();

        shader.use();

        // 设置矩阵 uniform
        setUniformMatrix4("uViewMatrix", viewMatrix);
        setUniformMatrix4("uProjMatrix", projMatrix);
        setUniform("uCameraPos", cameraPos[0], cameraPos[1], cameraPos[2]);
        setUniform("uRenderMode", renderMode.ordinal());
        setUniform("uSoftParticles", softParticles ? 1 : 0);
        setUniform("uSoftDistance", softParticleDistance);

        // 绑定纹理
        if (textureId > 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            setUniform("uTexture", 0);
            setUniform("uHasTexture", 1);
        } else {
            setUniform("uHasTexture", 0);
        }

        // 绑定 VAO
        GL30.glBindVertexArray(vao);

        // 绑定粒子数据作为实例属性
        // SSBO 数据通过 TBO (Texture Buffer Object) 或直接作为顶点属性绑定
        buffer.bindAsVertexAttribute();

        // 实例化渲染
        if (renderMode == RenderMode.POINT_SPRITE) {
            // 启用点精灵
            GL11.glEnable(GL20.GL_POINT_SPRITE);
            GL11.glEnable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);
            GL31.glDrawArraysInstanced(GL11.GL_POINTS, 0, 1, particleCount);
            // 显式禁用点精灵状态
            GL11.glDisable(GL20.GL_VERTEX_PROGRAM_POINT_SIZE);
            GL11.glDisable(GL20.GL_POINT_SPRITE);
        } else {
            // 渲染四边形
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, 6, particleCount);
        }

        // 解绑
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // 关键：解绑 VBO，否则会影响 MC 的 Tessellator
        GL30.glBindVertexArray(0);
        ShaderProgram.unbind();

        if (textureId > 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        // 恢复 GL 状态
        GL11.glPopAttrib();
    }

    /**
     * 渲染 Mesh 粒子
     */
    private void renderMesh(ParticleBuffer buffer, float[] viewMatrix, float[] projMatrix, float[] cameraPos,
        int particleCount) {

        if (meshVao == 0 || meshIndexCount == 0) {
            TakoTechMod.LOG.warn("ParticleRenderer: Mesh not set, cannot render in MESH mode");
            return;
        }

        // 懒加载 Mesh 着色器
        if (meshShader == null) {
            meshShader = new ShaderProgram(
                "takotech",
                "shaders/particle/particle_mesh.vert",
                "shaders/particle/particle_mesh.frag");
            if (!meshShader.isValid()) {
                TakoTechMod.LOG.error("ParticleRenderer: Failed to load mesh shader");
                return;
            }
            // 配置实例属性
            setupMeshInstancedAttributes(buffer.getSsboId());
        }

        // 保存 GL 状态
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // 设置渲染状态
        setupRenderState();

        // 启用面剔除 (Mesh 需要)
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        meshShader.use();

        // 设置 uniform
        setMeshUniformMatrix4("uViewMatrix", viewMatrix);
        setMeshUniformMatrix4("uProjMatrix", projMatrix);
        setMeshUniform("uCameraPos", cameraPos[0], cameraPos[1], cameraPos[2]);

        // 绑定 Mesh VAO
        GL30.glBindVertexArray(meshVao);

        // 绑定粒子数据
        buffer.bindAsVertexAttribute();

        // 实例化渲染 Mesh
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, meshIndexCount, GL11.GL_UNSIGNED_INT, 0, particleCount);

        // 解绑
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        ShaderProgram.unbind();

        // 恢复 GL 状态
        GL11.glPopAttrib();
    }

    /** 设置 Mesh shader 的 mat4 uniform */
    private void setMeshUniformMatrix4(String name, float[] matrix) {
        if (meshShader == null) return;
        int location = GL20.glGetUniformLocation(meshShader.getProgram(), name);
        if (location >= 0) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
            buffer.put(matrix)
                .flip();
            GL20.glUniformMatrix4(location, false, buffer);
        }
    }

    /** 设置 Mesh shader 的 vec3 uniform */
    private void setMeshUniform(String name, float x, float y, float z) {
        if (meshShader == null) return;
        int location = GL20.glGetUniformLocation(meshShader.getProgram(), name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * 渲染粒子（简化版，使用 Minecraft 相机矩阵）
     *
     * @param buffer       粒子缓冲区
     * @param partialTicks 渲染 tick 插值
     */
    public void renderWithMinecraftCamera(ParticleBuffer buffer, float partialTicks) {
        if (!initialized) {
            return;
        }

        // 从 OpenGL 获取当前矩阵
        float[] modelView = new float[16];
        float[] projection = new float[16];
        FloatBuffer mvBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mvBuffer);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuffer);

        mvBuffer.get(modelView);
        projBuffer.get(projection);

        // 使用 MC 的 RenderManager 获取相机世界位置
        // 注意：MC 的 ModelView 矩阵在 RenderWorldLast 事件中已经是相机相对坐标系
        // renderPosX/Y/Z 是相机在世界坐标中的位置
        float[] cameraPos = new float[] { (float) RenderManager.renderPosX, (float) RenderManager.renderPosY,
            (float) RenderManager.renderPosZ };

        render(buffer, modelView, projection, cameraPos, 0);
    }

    /**
     * 设置渲染模式
     *
     * @param mode 渲染模式
     * @return this
     */
    public ParticleRenderer setRenderMode(RenderMode mode) {
        this.renderMode = mode;
        return this;
    }

    /**
     * 设置混合模式
     *
     * @param mode 混合模式
     * @return this
     */
    public ParticleRenderer setBlendMode(BlendMode mode) {
        this.blendMode = mode;
        return this;
    }

    /**
     * 设置是否启用深度写入
     *
     * @param enable true 启用
     * @return this
     */
    public ParticleRenderer setDepthWrite(boolean enable) {
        this.depthWrite = enable;
        return this;
    }

    /**
     * 设置软粒子
     *
     * @param enable   是否启用
     * @param distance 衰减距离
     * @return this
     */
    public ParticleRenderer setSoftParticles(boolean enable, float distance) {
        this.softParticles = enable;
        this.softParticleDistance = distance;
        return this;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    // ==================== Mesh 粒子方法 ====================

    /**
     * 设置内置几何体
     *
     * @param type 内置几何体类型
     * @return this
     */
    public ParticleRenderer setBuiltinMesh(BuiltinMesh type) {
        if (type == currentBuiltinMesh) {
            return this;
        }

        float[] vertices;
        int[] indices;

        switch (type) {
            case CUBE:
                vertices = generateCubeVertices();
                indices = generateCubeIndices();
                break;
            case TETRAHEDRON:
                vertices = generateTetrahedronVertices();
                indices = generateTetrahedronIndices();
                break;
            case OCTAHEDRON:
                vertices = generateOctahedronVertices();
                indices = generateOctahedronIndices();
                break;
            case ICOSAHEDRON:
                vertices = generateIcosahedronVertices();
                indices = generateIcosahedronIndices();
                break;
            default:
                return this;
        }

        setMesh(vertices, indices);
        currentBuiltinMesh = type;
        return this;
    }

    /**
     * 设置自定义 Mesh
     *
     * @param vertices 顶点数据 (每顶点 6 floats: pos.xyz + normal.xyz)
     * @param indices  索引数据
     * @return this
     */
    public ParticleRenderer setMesh(float[] vertices, int[] indices) {
        // 清理旧的 Mesh
        cleanupMesh();

        // 创建 VAO
        meshVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(meshVao);

        // 创建 VBO
        meshVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, meshVbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices)
            .flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        // 顶点属性: location 0 = position (vec3)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0);
        GL20.glEnableVertexAttribArray(0);

        // 顶点属性: location 1 = normal (vec3)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(1);

        // 创建 EBO
        meshEbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, meshEbo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices)
            .flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        meshVertexCount = vertices.length / 6;
        meshIndexCount = indices.length;

        GL30.glBindVertexArray(0);

        TakoTechMod.LOG
            .info("ParticleRenderer: Mesh set with {} vertices, {} indices", meshVertexCount, meshIndexCount);
        return this;
    }

    /**
     * 清理 Mesh 资源
     */
    private void cleanupMesh() {
        if (meshVao != 0) {
            GL30.glDeleteVertexArrays(meshVao);
            meshVao = 0;
        }
        if (meshVbo != 0) {
            GL15.glDeleteBuffers(meshVbo);
            meshVbo = 0;
        }
        if (meshEbo != 0) {
            GL15.glDeleteBuffers(meshEbo);
            meshEbo = 0;
        }
        if (meshShader != null) {
            meshShader.close();
            meshShader = null;
        }
        meshVertexCount = 0;
        meshIndexCount = 0;
        currentBuiltinMesh = null;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (quadVbo != 0) {
            GL15.glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (shader != null) {
            shader.close();
            shader = null;
        }
        cleanupMesh();
        initialized = false;
    }

    // ==================== 内部方法 ====================

    /**
     * 设置渲染状态
     */
    private void setupRenderState() {
        // 深度测试
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(depthWrite);

        // 混合
        GL11.glEnable(GL11.GL_BLEND);
        switch (blendMode) {
            case ALPHA:
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                break;
            case ADDITIVE:
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                break;
            case MULTIPLY:
                GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ZERO);
                break;
            case PREMULTIPLIED:
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                break;
        }

        // 禁用面剔除（双面渲染）
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    /**
     * 设置 float uniform
     */
    private void setUniform(String name, float value) {
        int location = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    /**
     * 设置 int uniform
     */
    private void setUniform(String name, int value) {
        int location = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    /**
     * 设置 vec3 uniform
     */
    private void setUniform(String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * 设置 mat4 uniform
     */
    private void setUniformMatrix4(String name, float[] matrix) {
        int location = GL20.glGetUniformLocation(shader.getProgram(), name);
        if (location >= 0) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
            buffer.put(matrix)
                .flip();
            GL20.glUniformMatrix4(location, false, buffer);
        }
    }

    /**
     * 从 ModelView 矩阵提取相机位置
     */
    private float[] extractCameraPosition(float[] modelView) {
        // ModelView = View * Model
        // 对于只有 View 变换的情况，相机位置在变换矩阵的第4列的负值
        // 这里简化处理，假设没有额外的模型变换
        float[] invRot = new float[9];
        invRot[0] = modelView[0];
        invRot[1] = modelView[4];
        invRot[2] = modelView[8];
        invRot[3] = modelView[1];
        invRot[4] = modelView[5];
        invRot[5] = modelView[9];
        invRot[6] = modelView[2];
        invRot[7] = modelView[6];
        invRot[8] = modelView[10];

        float tx = modelView[12];
        float ty = modelView[13];
        float tz = modelView[14];

        float cx = -(invRot[0] * tx + invRot[3] * ty + invRot[6] * tz);
        float cy = -(invRot[1] * tx + invRot[4] * ty + invRot[7] * tz);
        float cz = -(invRot[2] * tx + invRot[5] * ty + invRot[8] * tz);

        return new float[] { cx, cy, cz };
    }

    /**
     * 创建回退着色器
     */
    private ShaderProgram createFallbackShader() {
        String vertSource = "#version 330 core\n" + "\n"
            + "layout(location = 0) in vec2 aQuadPos;\n"
            + "layout(location = 1) in vec2 aQuadUV;\n"
            + "\n"
            + "// 实例属性 (从 SSBO 读取)\n"
            + "layout(location = 2) in vec4 aPosition;  // xyz: pos, w: life\n"
            + "layout(location = 3) in vec4 aVelocity;  // xyz: vel, w: maxLife\n"
            + "layout(location = 4) in vec4 aColor;\n"
            + "layout(location = 5) in vec4 aParams;    // x: size, y: rot, z: type, w: reserved\n"
            + "\n"
            + "uniform mat4 uViewMatrix;\n"
            + "uniform mat4 uProjMatrix;\n"
            + "uniform vec3 uCameraPos;\n"
            + "uniform int uRenderMode;\n"
            + "\n"
            + "out vec2 vUV;\n"
            + "out vec4 vColor;\n"
            + "out float vLifePercent;\n"
            + "\n"
            + "void main() {\n"
            + "    // Skip dead particles\n"
            + "    if (aPosition.w <= 0.0) {\n"
            + "        gl_Position = vec4(0.0, 0.0, -1000.0, 1.0);\n"
            + "        return;\n"
            + "    }\n"
            + "    \n"
            + "    float size = aParams.x;\n"
            + "    float rotation = aParams.y;\n"
            + "    float lifePercent = 1.0 - (aPosition.w / aVelocity.w);\n"
            + "    \n"
            + "    // Rotate quad vertex\n"
            + "    float c = cos(rotation);\n"
            + "    float s = sin(rotation);\n"
            + "    vec2 rotatedPos = vec2(\n"
            + "        aQuadPos.x * c - aQuadPos.y * s,\n"
            + "        aQuadPos.x * s + aQuadPos.y * c\n"
            + "    );\n"
            + "    \n"
            + "    // Convert to camera-relative position\n"
            + "    vec3 relativePos = aPosition.xyz - uCameraPos;\n"
            + "    \n"
            + "    // Transform to view space\n"
            + "    vec4 viewCenter = uViewMatrix * vec4(relativePos, 1.0);\n"
            + "    \n"
            + "    // Billboard in view space (right=X, up=Y)\n"
            + "    vec3 viewPos = viewCenter.xyz;\n"
            + "    viewPos.x += rotatedPos.x * size;\n"
            + "    viewPos.y += rotatedPos.y * size;\n"
            + "    \n"
            + "    // Apply projection only\n"
            + "    gl_Position = uProjMatrix * vec4(viewPos, 1.0);\n"
            + "    \n"
            + "    vUV = aQuadUV;\n"
            + "    vColor = aColor;\n"
            + "    vLifePercent = lifePercent;\n"
            + "}\n";

        String fragSource = "#version 330 core\n" + "\n"
            + "in vec2 vUV;\n"
            + "in vec4 vColor;\n"
            + "in float vLifePercent;\n"
            + "\n"
            + "uniform sampler2D uTexture;\n"
            + "uniform int uHasTexture;\n"
            + "\n"
            + "out vec4 fragColor;\n"
            + "\n"
            + "void main() {\n"
            + "    vec4 texColor = vec4(1.0);\n"
            + "    if (uHasTexture == 1) {\n"
            + "        texColor = texture(uTexture, vUV);\n"
            + "    } else {\n"
            + "        // 无纹理时绘制圆形\n"
            + "        float dist = length(vUV - vec2(0.5));\n"
            + "        if (dist > 0.5) discard;\n"
            + "        texColor.a = smoothstep(0.5, 0.3, dist);\n"
            + "    }\n"
            + "    \n"
            + "    fragColor = texColor * vColor;\n"
            + "    \n"
            + "    // 丢弃透明像素\n"
            + "    if (fragColor.a < 0.01) discard;\n"
            + "}\n";

        return ShaderProgram.createFromSource(vertSource, null, fragSource);
    }

    /**
     * 使用实例属性除数配置顶点属性
     */
    public void setupInstancedAttributes(int ssboId) {
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssboId);

        int stride = ParticleBuffer.PARTICLE_SIZE_BYTES;

        // location 2: position (vec4)
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_POSITION);
        GL20.glEnableVertexAttribArray(2);
        GL33.glVertexAttribDivisor(2, 1); // 每实例一份

        // location 3: velocity (vec4)
        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_VELOCITY);
        GL20.glEnableVertexAttribArray(3);
        GL33.glVertexAttribDivisor(3, 1);

        // location 4: color (vec4)
        GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_COLOR);
        GL20.glEnableVertexAttribArray(4);
        GL33.glVertexAttribDivisor(4, 1);

        // location 5: params (vec4)
        GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_PARAMS);
        GL20.glEnableVertexAttribArray(5);
        GL33.glVertexAttribDivisor(5, 1);

        // 解绑 VBO 和 VAO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * 配置 Mesh 的实例属性
     */
    public void setupMeshInstancedAttributes(int ssboId) {
        if (meshVao == 0) return;

        GL30.glBindVertexArray(meshVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssboId);

        int stride = ParticleBuffer.PARTICLE_SIZE_BYTES;

        // location 2: position (vec4) - 从 ParticleBuffer
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_POSITION);
        GL20.glEnableVertexAttribArray(2);
        GL33.glVertexAttribDivisor(2, 1);

        // location 3: velocity (vec4)
        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_VELOCITY);
        GL20.glEnableVertexAttribArray(3);
        GL33.glVertexAttribDivisor(3, 1);

        // location 4: color (vec4)
        GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_COLOR);
        GL20.glEnableVertexAttribArray(4);
        GL33.glVertexAttribDivisor(4, 1);

        // location 5: params (vec4)
        GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, stride, ParticleBuffer.OFFSET_PARAMS);
        GL20.glEnableVertexAttribArray(5);
        GL33.glVertexAttribDivisor(5, 1);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    // ==================== 内置几何体生成 ====================

    /** 生成立方体顶点 (每顶点 6 floats: pos + normal) */
    private float[] generateCubeVertices() {
        float s = 0.5f;
        return new float[] { // @formatter:off
            // 前面 (Z+)
            -s, -s,  s,  0,  0,  1,
             s, -s,  s,  0,  0,  1,
             s,  s,  s,  0,  0,  1,
            -s,  s,  s,  0,  0,  1,
            // 后面 (Z-)
             s, -s, -s,  0,  0, -1,
            -s, -s, -s,  0,  0, -1,
            -s,  s, -s,  0,  0, -1,
             s,  s, -s,  0,  0, -1,
            // 上面 (Y+)
            -s,  s,  s,  0,  1,  0,
             s,  s,  s,  0,  1,  0,
             s,  s, -s,  0,  1,  0,
            -s,  s, -s,  0,  1,  0,
            // 下面 (Y-)
            -s, -s, -s,  0, -1,  0,
             s, -s, -s,  0, -1,  0,
             s, -s,  s,  0, -1,  0,
            -s, -s,  s,  0, -1,  0,
            // 右面 (X+)
             s, -s,  s,  1,  0,  0,
             s, -s, -s,  1,  0,  0,
             s,  s, -s,  1,  0,  0,
             s,  s,  s,  1,  0,  0,
            // 左面 (X-)
            -s, -s, -s, -1,  0,  0,
            -s, -s,  s, -1,  0,  0,
            -s,  s,  s, -1,  0,  0,
            -s,  s, -s, -1,  0,  0,
        }; // @formatter:on
    }

    /** 生成立方体索引 */
    private int[] generateCubeIndices() {
        return new int[] { // @formatter:off
            0, 1, 2, 2, 3, 0,       // 前
            4, 5, 6, 6, 7, 4,       // 后
            8, 9, 10, 10, 11, 8,    // 上
            12, 13, 14, 14, 15, 12, // 下
            16, 17, 18, 18, 19, 16, // 右
            20, 21, 22, 22, 23, 20  // 左
        }; // @formatter:on
    }

    /** 生成四面体顶点 */
    private float[] generateTetrahedronVertices() {
        float a = 0.5f;
        float h = (float) Math.sqrt(2.0 / 3.0) * a;
        float r = (float) Math.sqrt(1.0 / 3.0) * a;

        // 四个顶点
        float[] v0 = { 0, h * 0.75f, 0 };
        float[] v1 = { -a / 2, -h * 0.25f, r };
        float[] v2 = { a / 2, -h * 0.25f, r };
        float[] v3 = { 0, -h * 0.25f, -r * 2 };

        return new float[] { // @formatter:off
            // 面 0-1-2
            v0[0], v0[1], v0[2], 0, 0.5f, 0.866f,
            v1[0], v1[1], v1[2], 0, 0.5f, 0.866f,
            v2[0], v2[1], v2[2], 0, 0.5f, 0.866f,
            // 面 0-2-3
            v0[0], v0[1], v0[2], 0.816f, 0.5f, -0.289f,
            v2[0], v2[1], v2[2], 0.816f, 0.5f, -0.289f,
            v3[0], v3[1], v3[2], 0.816f, 0.5f, -0.289f,
            // 面 0-3-1
            v0[0], v0[1], v0[2], -0.816f, 0.5f, -0.289f,
            v3[0], v3[1], v3[2], -0.816f, 0.5f, -0.289f,
            v1[0], v1[1], v1[2], -0.816f, 0.5f, -0.289f,
            // 面 1-3-2 (底面)
            v1[0], v1[1], v1[2], 0, -1, 0,
            v3[0], v3[1], v3[2], 0, -1, 0,
            v2[0], v2[1], v2[2], 0, -1, 0,
        }; // @formatter:on
    }

    /** 生成四面体索引 */
    private int[] generateTetrahedronIndices() {
        return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
    }

    /** 生成八面体顶点 */
    private float[] generateOctahedronVertices() {
        float s = 0.5f;
        float n = 0.577f; // 1/sqrt(3)

        return new float[] { // @formatter:off
            // 上半部分 4 个三角形
            0, s, 0,  n, n, n,  s, 0, 0,  n, n, n,  0, 0, s,  n, n, n,
            0, s, 0,  -n, n, n,  0, 0, s,  -n, n, n,  -s, 0, 0,  -n, n, n,
            0, s, 0,  -n, n, -n,  -s, 0, 0,  -n, n, -n,  0, 0, -s,  -n, n, -n,
            0, s, 0,  n, n, -n,  0, 0, -s,  n, n, -n,  s, 0, 0,  n, n, -n,
            // 下半部分 4 个三角形
            0, -s, 0,  n, -n, n,  0, 0, s,  n, -n, n,  s, 0, 0,  n, -n, n,
            0, -s, 0,  -n, -n, n,  -s, 0, 0,  -n, -n, n,  0, 0, s,  -n, -n, n,
            0, -s, 0,  -n, -n, -n,  0, 0, -s,  -n, -n, -n,  -s, 0, 0,  -n, -n, -n,
            0, -s, 0,  n, -n, -n,  s, 0, 0,  n, -n, -n,  0, 0, -s,  n, -n, -n,
        }; // @formatter:on
    }

    /** 生成八面体索引 */
    private int[] generateOctahedronIndices() {
        int[] indices = new int[24];
        for (int i = 0; i < 24; i++) {
            indices[i] = i;
        }
        return indices;
    }

    /** 生成二十面体顶点 */
    private float[] generateIcosahedronVertices() {
        float phi = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
        float s = 0.3f; // 缩放因子

        // 12 个顶点
        float[][] v = { { -1, phi, 0 }, { 1, phi, 0 }, { -1, -phi, 0 }, { 1, -phi, 0 }, { 0, -1, phi }, { 0, 1, phi },
            { 0, -1, -phi }, { 0, 1, -phi }, { phi, 0, -1 }, { phi, 0, 1 }, { -phi, 0, -1 }, { -phi, 0, 1 } };

        // 归一化并缩放
        for (float[] vertex : v) {
            float len = (float) Math.sqrt(vertex[0] * vertex[0] + vertex[1] * vertex[1] + vertex[2] * vertex[2]);
            vertex[0] = vertex[0] / len * s;
            vertex[1] = vertex[1] / len * s;
            vertex[2] = vertex[2] / len * s;
        }

        // 20 个三角形面
        int[][] faces = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 },
            { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 }, { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 }, { 3, 6, 8 },
            { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };

        float[] vertices = new float[20 * 3 * 6]; // 20 faces * 3 vertices * 6 floats
        int idx = 0;

        for (int[] face : faces) {
            float[] p0 = v[face[0]];
            float[] p1 = v[face[1]];
            float[] p2 = v[face[2]];

            // 计算法线
            float[] e1 = { p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2] };
            float[] e2 = { p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2] };
            float[] n = { e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0] };
            float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            n[0] /= len;
            n[1] /= len;
            n[2] /= len;

            // 三个顶点
            vertices[idx++] = p0[0];
            vertices[idx++] = p0[1];
            vertices[idx++] = p0[2];
            vertices[idx++] = n[0];
            vertices[idx++] = n[1];
            vertices[idx++] = n[2];

            vertices[idx++] = p1[0];
            vertices[idx++] = p1[1];
            vertices[idx++] = p1[2];
            vertices[idx++] = n[0];
            vertices[idx++] = n[1];
            vertices[idx++] = n[2];

            vertices[idx++] = p2[0];
            vertices[idx++] = p2[1];
            vertices[idx++] = p2[2];
            vertices[idx++] = n[0];
            vertices[idx++] = n[1];
            vertices[idx++] = n[2];
        }

        return vertices;
    }

    /** 生成二十面体索引 */
    private int[] generateIcosahedronIndices() {
        int[] indices = new int[60];
        for (int i = 0; i < 60; i++) {
            indices[i] = i;
        }
        return indices;
    }
}
