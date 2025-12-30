package moe.takochan.takotech.client.renderer.graphics.postprocess;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.framebuffer.Framebuffer;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.common.Reference;

/**
 * 后处理器。
 * 管理 Bloom 等后处理效果。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     PostProcessor postProcessor = new PostProcessor();
 *     postProcessor.initialize();
 *     postProcessor.setBloomEnabled(true);
 *     postProcessor.setBloomThreshold(0.8f);
 *     postProcessor.setBloomIntensity(1.5f);
 *
 *     // 每帧渲染
 *     postProcessor.beginCapture();
 *     // ... 渲染场景 ...
 *     postProcessor.endCapture();
 *     postProcessor.process();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class PostProcessor {

    // ==================== FBO ====================

    /** 场景渲染目标 */
    private Framebuffer sceneFbo;

    /** 亮部提取 FBO */
    private Framebuffer brightFbo;

    /** 模糊 ping-pong FBO 1 */
    private Framebuffer blurFbo1;

    /** 模糊 ping-pong FBO 2 */
    private Framebuffer blurFbo2;

    // ==================== 着色器 ====================

    /** 亮度提取着色器 */
    private ShaderProgram brightExtractShader;

    /** 模糊着色器 */
    private ShaderProgram blurShader;

    /** 合成着色器 */
    private ShaderProgram compositeShader;

    // ==================== 全屏四边形 ====================

    /** VAO */
    private int quadVao;

    /** VBO */
    private int quadVbo;

    /** EBO */
    private int quadEbo;

    // ==================== 参数 ====================

    /** 是否启用 */
    private boolean enabled = false;

    /** Bloom 阈值 */
    private float bloomThreshold = 0.8f;

    /** Bloom 软膝盖 */
    private float bloomSoftKnee = 0.5f;

    /** Bloom 强度 */
    private float bloomIntensity = 1.5f;

    /** 模糊迭代次数 */
    private int blurIterations = 4;

    /** 模糊缩放 */
    private float blurScale = 1.0f;

    /** 曝光 */
    private float exposure = 1.0f;

    /** 是否启用色调映射 */
    private boolean tonemapEnabled = false;

    /** Bloom Alpha 缩放系数（控制发光扩散到背景的强度） */
    private float bloomAlphaScale = 2.0f;

    /** 当前 FBO 尺寸 */
    private int fboWidth;
    private int fboHeight;

    /** 是否已初始化 */
    private boolean initialized = false;

    /** 保存的 viewport */
    private IntBuffer savedViewport = BufferUtils.createIntBuffer(16);

    /** 保存的帧缓冲 ID */
    private int savedFramebuffer = 0;

    // ==================== 初始化 ====================

    /**
     * 初始化后处理器
     *
     * @return true 初始化成功
     */
    public boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            // 获取屏幕尺寸
            fboWidth = Display.getWidth();
            fboHeight = Display.getHeight();

            // 创建 FBOs
            sceneFbo = new Framebuffer(fboWidth, fboHeight, true);
            brightFbo = new Framebuffer(fboWidth / 2, fboHeight / 2, false);
            blurFbo1 = new Framebuffer(fboWidth / 2, fboHeight / 2, false);
            blurFbo2 = new Framebuffer(fboWidth / 2, fboHeight / 2, false);

            // 加载着色器
            String domain = Reference.RESOURCE_ROOT_ID;

            brightExtractShader = new ShaderProgram(
                domain,
                "shaders/postprocess/brightness_extract.vert",
                "shaders/postprocess/brightness_extract.frag");

            blurShader = new ShaderProgram(
                domain,
                "shaders/postprocess/bloom_blur.vert",
                "shaders/postprocess/bloom_blur.frag");

            compositeShader = new ShaderProgram(
                domain,
                "shaders/postprocess/bloom_composite.vert",
                "shaders/postprocess/bloom_composite.frag");

            // 验证着色器
            TakoTechMod.LOG.info(
                "PostProcessor: Shader validation - brightExtract={}, blur={}, composite={}",
                brightExtractShader.isValid(),
                blurShader.isValid(),
                compositeShader.isValid());

            if (!brightExtractShader.isValid() || !blurShader.isValid() || !compositeShader.isValid()) {
                TakoTechMod.LOG.error("PostProcessor: Failed to load shaders");
                cleanup();
                return false;
            }

            // 创建全屏四边形
            createFullscreenQuad();

            initialized = true;
            TakoTechMod.LOG.info("PostProcessor: Initialized successfully ({}x{})", fboWidth, fboHeight);
            return true;

        } catch (Exception e) {
            TakoTechMod.LOG.error("PostProcessor: Initialization failed", e);
            cleanup();
            return false;
        }
    }

    /**
     * 创建全屏四边形 Mesh
     */
    private void createFullscreenQuad() {
        // 顶点数据: position (2) + texcoord (2)
        float[] vertices = {
            // pos // uv
            -1.0f, -1.0f, 0.0f, 0.0f, // 左下
            1.0f, -1.0f, 1.0f, 0.0f, // 右下
            1.0f, 1.0f, 1.0f, 1.0f, // 右上
            -1.0f, 1.0f, 0.0f, 1.0f // 左上
        };

        int[] indices = { 0, 1, 2, 0, 2, 3 };

        // 创建 VAO
        quadVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(quadVao);

        // 创建 VBO
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices)
            .flip();

        quadVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        // 创建 EBO
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices)
            .flip();

        quadEbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, quadEbo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        // 顶点属性
        int stride = 4 * 4; // 4 floats * 4 bytes
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2 * 4);

        // 解绑
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * 检查并调整 FBO 尺寸
     */
    private void checkAndResizeFbos() {
        int width = Display.getWidth();
        int height = Display.getHeight();

        if (width != fboWidth || height != fboHeight) {
            fboWidth = width;
            fboHeight = height;

            // 重新创建 FBOs
            if (sceneFbo != null) sceneFbo.delete();
            if (brightFbo != null) brightFbo.delete();
            if (blurFbo1 != null) blurFbo1.delete();
            if (blurFbo2 != null) blurFbo2.delete();

            sceneFbo = new Framebuffer(fboWidth, fboHeight, true);
            brightFbo = new Framebuffer(fboWidth / 2, fboHeight / 2, false);
            blurFbo1 = new Framebuffer(fboWidth / 2, fboHeight / 2, false);
            blurFbo2 = new Framebuffer(fboWidth / 2, fboHeight / 2, false);

            TakoTechMod.LOG.info("PostProcessor: Resized FBOs to {}x{}", fboWidth, fboHeight);
        }
    }

    // ==================== 渲染流程 ====================

    /**
     * 开始捕获场景
     * Overlay 模式：仅捕获框架渲染的内容，不复制 MC 世界
     */
    public void beginCapture() {
        if (!initialized || !enabled) {
            return;
        }

        checkAndResizeFbos();

        // 保存当前 viewport
        savedViewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, savedViewport);

        // 保存当前帧缓冲（MC 或优化 mod 可能使用非 0 的帧缓冲）
        savedFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        // 绑定 sceneFbo 用于渲染框架内容
        sceneFbo.bind();

        // 清空为透明背景（Overlay 模式关键）
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 结束捕获场景
     */
    public void endCapture() {
        if (!initialized || !enabled) {
            return;
        }

        // 恢复到保存的帧缓冲（而不是假设是 0）
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFramebuffer);

        // 恢复 viewport
        GL11.glViewport(savedViewport.get(0), savedViewport.get(1), savedViewport.get(2), savedViewport.get(3));
    }

    /**
     * 处理并渲染到屏幕
     */
    public void process() {
        if (!initialized || !enabled) {
            return;
        }

        // 保存 GL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // 禁用深度测试和混合
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // 1. 亮度提取: sceneFbo -> brightFbo
        extractBrightness();

        // 2. 高斯模糊 (ping-pong)
        gaussianBlur();

        // 3. 合成: sceneFbo + blurFbo -> screen
        composite();

        // 恢复 GL 状态
        GL11.glPopAttrib();
    }

    /**
     * 亮度提取
     */
    private void extractBrightness() {
        brightFbo.bind();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        brightExtractShader.use();
        brightExtractShader.setUniformInt("uSceneTexture", 0);
        brightExtractShader.setUniformFloat("uThreshold", bloomThreshold);
        brightExtractShader.setUniformFloat("uSoftKnee", bloomSoftKnee);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        sceneFbo.bindTexture();

        drawFullscreenQuad();

        ShaderProgram.unbind();
        brightFbo.unbind();
    }

    /**
     * 高斯模糊 (ping-pong)
     */
    private void gaussianBlur() {
        boolean horizontal = true;
        Framebuffer source = brightFbo;

        blurShader.use();
        blurShader.setUniformInt("uSourceTexture", 0);
        blurShader.setUniformFloat("uBlurScale", blurScale);

        float texelWidth = 1.0f / blurFbo1.getWidth();
        float texelHeight = 1.0f / blurFbo1.getHeight();
        blurShader.setUniformVec2("uTexelSize", texelWidth, texelHeight);

        for (int i = 0; i < blurIterations * 2; i++) {
            Framebuffer target = horizontal ? blurFbo1 : blurFbo2;
            target.bind();
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            blurShader.setUniformInt("uHorizontal", horizontal ? 1 : 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            source.bindTexture();

            drawFullscreenQuad();

            target.unbind();
            source = target;
            horizontal = !horizontal;
        }

        ShaderProgram.unbind();
    }

    /**
     * 合成
     * Overlay 模式：将后处理结果叠加到 MC 世界上，不清除屏幕
     */
    private void composite() {
        // 检查 shader 是否有效
        if (compositeShader == null || !compositeShader.isValid()) {
            TakoTechMod.LOG.error("[PostProcessor] compositeShader is invalid!");
            return;
        }

        // 保存 GL 状态
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

        // 渲染到保存的帧缓冲（MC/优化mod可能使用非0的帧缓冲）
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFramebuffer);
        GL11.glViewport(savedViewport.get(0), savedViewport.get(1), savedViewport.get(2), savedViewport.get(3));

        // 不清屏！保留 MC 世界内容（Overlay 模式关键）

        // 禁用深度测试和深度写入（全屏 quad 不需要深度测试）
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        // 启用 Alpha 混合（将后处理结果叠加到 MC 世界上）
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        compositeShader.use();
        compositeShader.setUniformInt("uSceneTexture", 0);
        compositeShader.setUniformInt("uBloomTexture", 1);
        compositeShader.setUniformFloat("uBloomIntensity", bloomIntensity);
        compositeShader.setUniformFloat("uExposure", exposure);
        compositeShader.setUniformInt("uEnableTonemap", tonemapEnabled ? 1 : 0);
        compositeShader.setUniformFloat("uBloomAlphaScale", bloomAlphaScale);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        sceneFbo.bindTexture();

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        // 使用最后一次模糊的结果
        Framebuffer finalBlur = (blurIterations * 2) % 2 == 0 ? blurFbo2 : blurFbo1;
        finalBlur.bindTexture();

        drawFullscreenQuad();

        ShaderProgram.unbind();

        // 解绑纹理
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // 恢复 GL 状态
        GL11.glPopAttrib();
    }

    /**
     * 绘制全屏四边形
     */
    private void drawFullscreenQuad() {
        GL30.glBindVertexArray(quadVao);
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    // ==================== 参数设置 ====================

    /**
     * 是否启用
     *
     * @return true 启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用
     *
     * @param enabled true 启用
     * @return this
     */
    public PostProcessor setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * 获取 Bloom 阈值
     *
     * @return 阈值
     */
    public float getBloomThreshold() {
        return bloomThreshold;
    }

    /**
     * 设置 Bloom 阈值
     *
     * @param threshold 阈值 (0-1)
     * @return this
     */
    public PostProcessor setBloomThreshold(float threshold) {
        this.bloomThreshold = Math.max(0, Math.min(1, threshold));
        return this;
    }

    /**
     * 获取 Bloom 软膝盖系数
     *
     * @return 系数
     */
    public float getBloomSoftKnee() {
        return bloomSoftKnee;
    }

    /**
     * 设置 Bloom 软膝盖系数
     *
     * @param softKnee 系数 (0-1)
     * @return this
     */
    public PostProcessor setBloomSoftKnee(float softKnee) {
        this.bloomSoftKnee = Math.max(0, Math.min(1, softKnee));
        return this;
    }

    /**
     * 获取 Bloom 强度
     *
     * @return 强度
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * 设置 Bloom 强度
     *
     * @param intensity 强度
     * @return this
     */
    public PostProcessor setBloomIntensity(float intensity) {
        this.bloomIntensity = Math.max(0, intensity);
        return this;
    }

    /**
     * 获取模糊迭代次数
     *
     * @return 迭代次数
     */
    public int getBlurIterations() {
        return blurIterations;
    }

    /**
     * 设置模糊迭代次数
     *
     * @param iterations 迭代次数
     * @return this
     */
    public PostProcessor setBlurIterations(int iterations) {
        this.blurIterations = Math.max(1, Math.min(10, iterations));
        return this;
    }

    /**
     * 获取模糊缩放
     *
     * @return 缩放
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * 设置模糊缩放
     *
     * @param scale 缩放
     * @return this
     */
    public PostProcessor setBlurScale(float scale) {
        this.blurScale = Math.max(0.1f, scale);
        return this;
    }

    /**
     * 获取曝光
     *
     * @return 曝光
     */
    public float getExposure() {
        return exposure;
    }

    /**
     * 设置曝光
     *
     * @param exposure 曝光
     * @return this
     */
    public PostProcessor setExposure(float exposure) {
        this.exposure = Math.max(0, exposure);
        return this;
    }

    /**
     * 是否启用色调映射
     *
     * @return true 启用
     */
    public boolean isTonemapEnabled() {
        return tonemapEnabled;
    }

    /**
     * 设置是否启用色调映射
     *
     * @param enabled true 启用
     * @return this
     */
    public PostProcessor setTonemapEnabled(boolean enabled) {
        this.tonemapEnabled = enabled;
        return this;
    }

    /**
     * 获取 Bloom Alpha 缩放系数
     *
     * @return 缩放系数
     */
    public float getBloomAlphaScale() {
        return bloomAlphaScale;
    }

    /**
     * 设置 Bloom Alpha 缩放系数
     * 控制发光扩散到背景的强度，值越大扩散越明显
     *
     * @param scale 缩放系数 (默认 2.0)
     * @return this
     */
    public PostProcessor setBloomAlphaScale(float scale) {
        this.bloomAlphaScale = Math.max(0, scale);
        return this;
    }

    // ==================== 清理 ====================

    /**
     * 清理资源
     */
    public void cleanup() {
        if (sceneFbo != null) {
            sceneFbo.delete();
            sceneFbo = null;
        }
        if (brightFbo != null) {
            brightFbo.delete();
            brightFbo = null;
        }
        if (blurFbo1 != null) {
            blurFbo1.delete();
            blurFbo1 = null;
        }
        if (blurFbo2 != null) {
            blurFbo2.delete();
            blurFbo2 = null;
        }

        if (brightExtractShader != null) {
            brightExtractShader.close();
            brightExtractShader = null;
        }
        if (blurShader != null) {
            blurShader.close();
            blurShader = null;
        }
        if (compositeShader != null) {
            compositeShader.close();
            compositeShader = null;
        }

        if (quadVao != 0) {
            GL30.glDeleteVertexArrays(quadVao);
            quadVao = 0;
        }
        if (quadVbo != 0) {
            GL15.glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (quadEbo != 0) {
            GL15.glDeleteBuffers(quadEbo);
            quadEbo = 0;
        }

        initialized = false;
    }

    /**
     * 是否已初始化
     *
     * @return true 已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
