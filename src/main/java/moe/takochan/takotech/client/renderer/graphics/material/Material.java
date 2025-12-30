package moe.takochan.takotech.client.renderer.graphics.material;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 统一材质类，用于 Uber Shader 系统。
 * 支持多种渲染模式，减少 shader 切换开销。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Material mat = new Material();
 *     mat.setRenderMode(RenderMode.TEXTURE_COLOR);
 *     mat.setBaseColor(1.0f, 1.0f, 1.0f, 1.0f);
 *     mat.apply(shader);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class Material {

    // 渲染模式
    private RenderMode renderMode = RenderMode.COLOR;

    // 基础颜色 (RGBA)
    private float baseR = 1.0f;
    private float baseG = 1.0f;
    private float baseB = 1.0f;
    private float baseA = 1.0f;

    // 全局透明度
    private float alpha = 1.0f;

    // 纹理 ID (0 表示无纹理)
    private int textureId = 0;

    // 模糊参数
    private float blurScale = 1.0f;

    // PBR 参数 (预留)
    private float metallic = 0.0f;
    private float roughness = 0.5f;
    private float ao = 1.0f;
    private float emissiveR = 0.0f;
    private float emissiveG = 0.0f;
    private float emissiveB = 0.0f;
    private float emissiveA = 0.0f;

    // 变换矩阵参数
    private boolean useProjection = true;
    private boolean useView = false;

    /**
     * 创建默认材质（纯白色）
     */
    public Material() {}

    /**
     * 创建指定颜色的材质
     */
    public Material(float r, float g, float b, float a) {
        setBaseColor(r, g, b, a);
    }

    /**
     * 创建指定模式的材质
     */
    public Material(RenderMode mode) {
        this.renderMode = mode;
    }

    // ==================== Setters ====================

    public Material setRenderMode(RenderMode mode) {
        this.renderMode = mode;
        return this;
    }

    public Material setBaseColor(float r, float g, float b, float a) {
        this.baseR = r;
        this.baseG = g;
        this.baseB = b;
        this.baseA = a;
        return this;
    }

    public Material setBaseColor(int rgba) {
        this.baseR = ((rgba >> 24) & 0xFF) / 255f;
        this.baseG = ((rgba >> 16) & 0xFF) / 255f;
        this.baseB = ((rgba >> 8) & 0xFF) / 255f;
        this.baseA = (rgba & 0xFF) / 255f;
        return this;
    }

    public Material setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public Material setTexture(int textureId) {
        this.textureId = textureId;
        return this;
    }

    public Material setBlurScale(float scale) {
        this.blurScale = scale;
        return this;
    }

    public Material setUseProjection(boolean use) {
        this.useProjection = use;
        return this;
    }

    public Material setUseView(boolean use) {
        this.useView = use;
        return this;
    }

    // PBR setters (预留)
    public Material setMetallic(float metallic) {
        this.metallic = metallic;
        return this;
    }

    public Material setRoughness(float roughness) {
        this.roughness = roughness;
        return this;
    }

    public Material setAO(float ao) {
        this.ao = ao;
        return this;
    }

    public Material setEmissive(float r, float g, float b, float a) {
        this.emissiveR = r;
        this.emissiveG = g;
        this.emissiveB = b;
        this.emissiveA = a;
        return this;
    }

    // ==================== Getters ====================

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public int getTextureId() {
        return textureId;
    }

    public boolean hasTexture() {
        return textureId != 0;
    }

    public float getEmissiveR() {
        return emissiveR;
    }

    public float getEmissiveG() {
        return emissiveG;
    }

    public float getEmissiveB() {
        return emissiveB;
    }

    public float getEmissiveA() {
        return emissiveA;
    }

    // ==================== Apply ====================

    /**
     * 将材质参数应用到指定的 shader program
     *
     * @param shader Uber shader program
     */
    public void apply(ShaderProgram shader) {
        if (shader == null || !shader.isValid()) return;

        // 渲染模式
        shader.setUniformInt("uRenderMode", renderMode.getId());

        // 基础颜色
        shader.setUniformVec4("uBaseColor", baseR, baseG, baseB, baseA);

        // 全局透明度
        shader.setUniformFloat("uAlpha", alpha);

        // 纹理相关
        shader.setUniformBool("uUseTexture", textureId != 0);
        if (textureId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            shader.setUniformInt("uMainTexture", 0);
        }

        // 投影/视图矩阵开关
        shader.setUniformBool("uUseProjection", useProjection);
        shader.setUniformBool("uUseView", useView);

        // 模糊参数
        if (renderMode == RenderMode.BLUR_HORIZONTAL || renderMode == RenderMode.BLUR_VERTICAL) {
            shader.setUniformFloat("uBlurScale", blurScale);
        }

        // PBR 参数 (预留)
        shader.setUniformFloat("uMetallic", metallic);
        shader.setUniformFloat("uRoughness", roughness);
        shader.setUniformFloat("uAO", ao);
        shader.setUniformVec4("uEmissive", emissiveR, emissiveG, emissiveB, emissiveA);
    }

    // ==================== 预定义材质 ====================

    /** 创建纯色材质 */
    public static Material color(float r, float g, float b, float a) {
        return new Material(r, g, b, a).setRenderMode(RenderMode.COLOR);
    }

    /** 创建纯色材质 (RGBA 整数) */
    public static Material color(int rgba) {
        return new Material().setBaseColor(rgba)
            .setRenderMode(RenderMode.COLOR);
    }

    /** 创建纹理材质 */
    public static Material texture(int textureId) {
        return new Material().setTexture(textureId)
            .setRenderMode(RenderMode.TEXTURE);
    }

    /** 创建纹理+颜色调制材质 */
    public static Material textureColor(int textureId, float r, float g, float b, float a) {
        return new Material(r, g, b, a).setTexture(textureId)
            .setRenderMode(RenderMode.TEXTURE_COLOR);
    }

    /** 创建水平模糊材质 */
    public static Material blurHorizontal(int textureId, float scale) {
        return new Material().setTexture(textureId)
            .setBlurScale(scale)
            .setRenderMode(RenderMode.BLUR_HORIZONTAL);
    }

    /** 创建垂直模糊材质 */
    public static Material blurVertical(int textureId, float scale) {
        return new Material().setTexture(textureId)
            .setBlurScale(scale)
            .setRenderMode(RenderMode.BLUR_VERTICAL);
    }
}
