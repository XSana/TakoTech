package moe.takochan.takotech.client.renderer.graphics.material;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * PBR (Physically Based Rendering) 材质类。
 * 基于 Cook-Torrance BRDF 模型，支持金属度/粗糙度工作流。
 *
 * <p>
 * 纹理槽位分配:
 * </p>
 * <ul>
 * <li>GL_TEXTURE0: Albedo (基础颜色)</li>
 * <li>GL_TEXTURE1: Normal (法线贴图)</li>
 * <li>GL_TEXTURE2: Metallic (金属度)</li>
 * <li>GL_TEXTURE3: Roughness (粗糙度)</li>
 * <li>GL_TEXTURE4: AO (环境光遮蔽)</li>
 * <li>GL_TEXTURE5: Emissive (自发光)</li>
 * <li>GL_TEXTURE6: IBL Irradiance (环境漫反射)</li>
 * <li>GL_TEXTURE7: IBL Prefilter (环境高光)</li>
 * <li>GL_TEXTURE8: BRDF LUT (BRDF 查找表)</li>
 * </ul>
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     PBRMaterial mat = new PBRMaterial();
 *     mat.setAlbedo(1.0f, 0.8f, 0.6f);
 *     mat.setMetallic(0.0f);
 *     mat.setRoughness(0.5f);
 *     mat.setAlbedoMap(albedoTextureId);
 *     mat.setNormalMap(normalTextureId);
 *     mat.apply(pbrShader);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class PBRMaterial {

    // ==================== 纹理槽位常量 ====================

    public static final int TEXTURE_SLOT_ALBEDO = 0;
    public static final int TEXTURE_SLOT_NORMAL = 1;
    public static final int TEXTURE_SLOT_METALLIC = 2;
    public static final int TEXTURE_SLOT_ROUGHNESS = 3;
    public static final int TEXTURE_SLOT_AO = 4;
    public static final int TEXTURE_SLOT_EMISSIVE = 5;
    public static final int TEXTURE_SLOT_IBL_IRRADIANCE = 6;
    public static final int TEXTURE_SLOT_IBL_PREFILTER = 7;
    public static final int TEXTURE_SLOT_BRDF_LUT = 8;

    // ==================== 基础颜色 ====================

    /** Albedo 颜色 (RGB) */
    private float albedoR = 1.0f;
    private float albedoG = 1.0f;
    private float albedoB = 1.0f;

    /** 透明度 */
    private float alpha = 1.0f;

    // ==================== PBR 参数 ====================

    /** 金属度 (0 = 电介质, 1 = 金属) */
    private float metallic = 0.0f;

    /** 粗糙度 (0 = 光滑/镜面, 1 = 粗糙/漫反射) */
    private float roughness = 0.5f;

    /** 环境光遮蔽强度 */
    private float ao = 1.0f;

    /** 自发光颜色 (RGB) */
    private float emissiveR = 0.0f;
    private float emissiveG = 0.0f;
    private float emissiveB = 0.0f;

    /** 自发光强度 */
    private float emissiveIntensity = 1.0f;

    /** 法线贴图强度 */
    private float normalStrength = 1.0f;

    // ==================== 纹理 ID ====================

    private int albedoMap = 0;
    private int normalMap = 0;
    private int metallicMap = 0;
    private int roughnessMap = 0;
    private int aoMap = 0;
    private int emissiveMap = 0;

    // ==================== IBL 纹理 ====================

    /** 环境漫反射 Cubemap (Irradiance) */
    private int iblIrradianceMap = 0;

    /** 环境高光 Cubemap (Prefiltered) */
    private int iblPrefilterMap = 0;

    /** BRDF 查找表纹理 */
    private int brdfLutMap = 0;

    // ==================== 附加选项 ====================

    /** 是否使用 IBL */
    private boolean useIBL = false;

    /** 是否双面渲染 */
    private boolean doubleSided = false;

    /** 是否使用顶点颜色作为 Albedo */
    private boolean useVertexColor = false;

    /** 折射率 (IOR) - 用于菲涅尔计算, 默认 1.5 (玻璃) */
    private float ior = 1.5f;

    // ==================== 构造函数 ====================

    public PBRMaterial() {}

    /**
     * 创建指定 Albedo 颜色的 PBR 材质
     */
    public PBRMaterial(float r, float g, float b) {
        setAlbedo(r, g, b);
    }

    /**
     * 创建指定金属度和粗糙度的 PBR 材质
     */
    public PBRMaterial(float metallic, float roughness) {
        this.metallic = metallic;
        this.roughness = roughness;
    }

    // ==================== Albedo ====================

    public PBRMaterial setAlbedo(float r, float g, float b) {
        this.albedoR = r;
        this.albedoG = g;
        this.albedoB = b;
        return this;
    }

    public PBRMaterial setAlbedo(int rgb) {
        this.albedoR = ((rgb >> 16) & 0xFF) / 255f;
        this.albedoG = ((rgb >> 8) & 0xFF) / 255f;
        this.albedoB = (rgb & 0xFF) / 255f;
        return this;
    }

    public PBRMaterial setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public PBRMaterial setAlbedoMap(int textureId) {
        this.albedoMap = textureId;
        return this;
    }

    // ==================== PBR 参数 ====================

    public PBRMaterial setMetallic(float metallic) {
        this.metallic = Math.max(0, Math.min(1, metallic));
        return this;
    }

    public PBRMaterial setMetallicMap(int textureId) {
        this.metallicMap = textureId;
        return this;
    }

    public PBRMaterial setRoughness(float roughness) {
        this.roughness = Math.max(0.04f, Math.min(1, roughness)); // 最小 0.04 避免除零
        return this;
    }

    public PBRMaterial setRoughnessMap(int textureId) {
        this.roughnessMap = textureId;
        return this;
    }

    public PBRMaterial setAO(float ao) {
        this.ao = Math.max(0, Math.min(1, ao));
        return this;
    }

    public PBRMaterial setAOMap(int textureId) {
        this.aoMap = textureId;
        return this;
    }

    // ==================== 法线 ====================

    public PBRMaterial setNormalMap(int textureId) {
        this.normalMap = textureId;
        return this;
    }

    public PBRMaterial setNormalStrength(float strength) {
        this.normalStrength = strength;
        return this;
    }

    // ==================== 自发光 ====================

    public PBRMaterial setEmissive(float r, float g, float b) {
        this.emissiveR = r;
        this.emissiveG = g;
        this.emissiveB = b;
        return this;
    }

    public PBRMaterial setEmissiveMap(int textureId) {
        this.emissiveMap = textureId;
        return this;
    }

    public PBRMaterial setEmissiveIntensity(float intensity) {
        this.emissiveIntensity = intensity;
        return this;
    }

    // ==================== IBL ====================

    public PBRMaterial setIBLMaps(int irradianceMap, int prefilterMap, int brdfLut) {
        this.iblIrradianceMap = irradianceMap;
        this.iblPrefilterMap = prefilterMap;
        this.brdfLutMap = brdfLut;
        this.useIBL = irradianceMap != 0 && prefilterMap != 0 && brdfLut != 0;
        return this;
    }

    public PBRMaterial setUseIBL(boolean use) {
        this.useIBL = use;
        return this;
    }

    // ==================== 其他选项 ====================

    public PBRMaterial setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
        return this;
    }

    public PBRMaterial setUseVertexColor(boolean use) {
        this.useVertexColor = use;
        return this;
    }

    public PBRMaterial setIOR(float ior) {
        this.ior = ior;
        return this;
    }

    // ==================== Getters ====================

    public float getMetallic() {
        return metallic;
    }

    public float getRoughness() {
        return roughness;
    }

    public float getAO() {
        return ao;
    }

    public boolean hasAlbedoMap() {
        return albedoMap != 0;
    }

    public boolean hasNormalMap() {
        return normalMap != 0;
    }

    public boolean isDoubleSided() {
        return doubleSided;
    }

    public boolean usesIBL() {
        return useIBL;
    }

    // ==================== Apply ====================

    /**
     * 将 PBR 材质参数应用到着色器
     *
     * @param shader PBR 着色器程序
     */
    public void apply(ShaderProgram shader) {
        if (shader == null || !shader.isValid()) {
            return;
        }

        // Albedo
        shader.setUniformVec4("uAlbedo", albedoR, albedoG, albedoB, alpha);
        shader.setUniformBool("uHasAlbedoMap", albedoMap != 0);
        shader.setUniformBool("uUseVertexColor", useVertexColor);

        // PBR 参数
        shader.setUniformFloat("uMetallic", metallic);
        shader.setUniformFloat("uRoughness", roughness);
        shader.setUniformFloat("uAO", ao);
        shader.setUniformBool("uHasMetallicMap", metallicMap != 0);
        shader.setUniformBool("uHasRoughnessMap", roughnessMap != 0);
        shader.setUniformBool("uHasAOMap", aoMap != 0);

        // 法线
        shader.setUniformBool("uHasNormalMap", normalMap != 0);
        shader.setUniformFloat("uNormalStrength", normalStrength);

        // 自发光
        shader.setUniformVec3("uEmissive", emissiveR, emissiveG, emissiveB);
        shader.setUniformFloat("uEmissiveIntensity", emissiveIntensity);
        shader.setUniformBool("uHasEmissiveMap", emissiveMap != 0);

        // IBL
        shader.setUniformBool("uUseIBL", useIBL);

        // IOR / F0
        float f0 = (ior - 1) / (ior + 1);
        f0 = f0 * f0;
        shader.setUniformFloat("uF0", f0);

        // 绑定纹理
        bindTextures(shader);
    }

    /**
     * 绑定所有纹理到对应槽位
     */
    private void bindTextures(ShaderProgram shader) {
        // Albedo
        if (albedoMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_ALBEDO);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, albedoMap);
            shader.setUniformInt("uAlbedoMap", TEXTURE_SLOT_ALBEDO);
        }

        // Normal
        if (normalMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_NORMAL);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap);
            shader.setUniformInt("uNormalMap", TEXTURE_SLOT_NORMAL);
        }

        // Metallic
        if (metallicMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_METALLIC);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, metallicMap);
            shader.setUniformInt("uMetallicMap", TEXTURE_SLOT_METALLIC);
        }

        // Roughness
        if (roughnessMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_ROUGHNESS);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, roughnessMap);
            shader.setUniformInt("uRoughnessMap", TEXTURE_SLOT_ROUGHNESS);
        }

        // AO
        if (aoMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_AO);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, aoMap);
            shader.setUniformInt("uAOMap", TEXTURE_SLOT_AO);
        }

        // Emissive
        if (emissiveMap != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_EMISSIVE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, emissiveMap);
            shader.setUniformInt("uEmissiveMap", TEXTURE_SLOT_EMISSIVE);
        }

        // IBL textures
        if (useIBL) {
            if (iblIrradianceMap != 0) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_IBL_IRRADIANCE);
                GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, iblIrradianceMap);
                shader.setUniformInt("uIrradianceMap", TEXTURE_SLOT_IBL_IRRADIANCE);
            }

            if (iblPrefilterMap != 0) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_IBL_PREFILTER);
                GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, iblPrefilterMap);
                shader.setUniformInt("uPrefilterMap", TEXTURE_SLOT_IBL_PREFILTER);
            }

            if (brdfLutMap != 0) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_SLOT_BRDF_LUT);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, brdfLutMap);
                shader.setUniformInt("uBrdfLUT", TEXTURE_SLOT_BRDF_LUT);
            }
        }

        // 恢复到默认纹理单元
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    /**
     * 解绑所有纹理
     */
    public void unbindTextures() {
        for (int i = 0; i <= TEXTURE_SLOT_BRDF_LUT; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    // ==================== 预设材质 ====================

    /** 创建金属材质 */
    public static PBRMaterial metal(float r, float g, float b, float roughness) {
        return new PBRMaterial(r, g, b).setMetallic(1.0f)
            .setRoughness(roughness);
    }

    /** 创建塑料材质 */
    public static PBRMaterial plastic(float r, float g, float b, float roughness) {
        return new PBRMaterial(r, g, b).setMetallic(0.0f)
            .setRoughness(roughness);
    }

    /** 创建发光材质 */
    public static PBRMaterial emissive(float r, float g, float b, float intensity) {
        return new PBRMaterial(r, g, b).setEmissive(r, g, b)
            .setEmissiveIntensity(intensity);
    }

    /** 金材质预设 */
    public static PBRMaterial gold() {
        return new PBRMaterial(1.0f, 0.766f, 0.336f).setMetallic(1.0f)
            .setRoughness(0.3f);
    }

    /** 银材质预设 */
    public static PBRMaterial silver() {
        return new PBRMaterial(0.972f, 0.960f, 0.915f).setMetallic(1.0f)
            .setRoughness(0.2f);
    }

    /** 铜材质预设 */
    public static PBRMaterial copper() {
        return new PBRMaterial(0.955f, 0.638f, 0.538f).setMetallic(1.0f)
            .setRoughness(0.4f);
    }

    /** 铁材质预设 */
    public static PBRMaterial iron() {
        return new PBRMaterial(0.56f, 0.57f, 0.58f).setMetallic(1.0f)
            .setRoughness(0.5f);
    }

    /** 橡胶材质预设 */
    public static PBRMaterial rubber() {
        return new PBRMaterial(0.02f, 0.02f, 0.02f).setMetallic(0.0f)
            .setRoughness(0.9f);
    }

    @Override
    public String toString() {
        return String.format(
            "PBRMaterial[albedo=(%.2f,%.2f,%.2f), metallic=%.2f, roughness=%.2f, ao=%.2f]",
            albedoR,
            albedoG,
            albedoB,
            metallic,
            roughness,
            ao);
    }
}
