package moe.takochan.takotech.client.renderer.graphics.shader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.Reference;

@SideOnly(Side.CLIENT)
public enum ShaderType {

    /**
     * 用于常规 2D 纹理绘制的基础着色器。
     */
    SIMPLE("shaders/simple.vert", "shaders/simple.frag"),

    /**
     * 用于绘制 2D 纹理的高斯模糊效果的着色器。
     */
    BLUR("shaders/blur.vert", "shaders/blur.frag"),

    /**
     * 用于 GUI 纯色渲染的着色器（支持顶点颜色）。
     */
    GUI_COLOR("shaders/gui_color.vert", "shaders/gui_color.frag"),

    /**
     * 统一着色器（Uber Shader），支持多种渲染模式。
     * 通过 uniform 切换模式，减少 shader 切换开销。
     * 支持：纯色、纹理、纹理+颜色调制、高斯模糊等。
     */
    UBER("shaders/uber.vert", "shaders/uber.frag"),

    /**
     * PBR 着色器（Physically Based Rendering）。
     * 基于 Cook-Torrance BRDF，支持金属度/粗糙度工作流。
     * 支持：IBL 环境光照、法线贴图、自发光等。
     */
    PBR("shaders/pbr.vert", "shaders/pbr.frag"),

    /**
     * 3D 世界渲染着色器（GLSL 1.20 兼容）。
     * 用于在 MC 世界中渲染 3D 图元（线框、方块、粒子等）。
     * 支持顶点颜色和 ViewProjection 矩阵变换。
     */
    WORLD_3D("shaders/world3d.vert", "shaders/world3d.frag"),

    /**
     * 线条渲染着色器。
     * 用于 LineRendererComponent 的 billboard 线条渲染。
     * 支持顶点颜色、全局透明度和发光强度（用于 Bloom）。
     */
    LINE("shaders/line.vert", "shaders/line.frag");

    private final String vertFilepath;
    private final String fragFilepath;

    ShaderType(String vertFilepath, String fragFilepath) {
        this.vertFilepath = vertFilepath;
        this.fragFilepath = fragFilepath;
    }

    private static final Map<ShaderType, ShaderProgram> SHADER_CACHE = new ConcurrentHashMap<>();

    private static final String DOMAIN = Reference.RESOURCE_ROOT_ID;

    /**
     * 初始化所有 ShaderType 枚举项对应的着色器程序。 应在 mod 加载阶段或渲染前调用一次。
     */
    public static void register() {
        for (ShaderType type : values()) {
            create(type);
        }
    }

    /**
     * 获取当前 ShaderType 对应的顶点着色器路径。
     *
     * @return 顶点着色器路径
     */
    public String getVertShaderFilename() {
        return vertFilepath;
    }

    /**
     * 获取当前 ShaderType 对应的片元着色器路径。
     *
     * @return 片元着色器路径
     */
    public String getFragShaderFilename() {
        return fragFilepath;
    }

    /**
     * 获取当前 ShaderType 对应的着色器程序。 如果尚未调用 {@link #register()} 进行初始化，将抛出异常。
     *
     * @return 对应的 ShaderProgram 实例
     * @throws IllegalStateException 若该着色器尚未初始化
     */
    public ShaderProgram get() {
        if (!SHADER_CACHE.containsKey(this)) {
            throw new IllegalStateException("Shader " + name() + " not initialized. Call initializeAll() first.");
        }
        return SHADER_CACHE.get(this);
    }

    /**
     * 安全获取着色器程序，不抛出异常。
     *
     * @return 对应的 ShaderProgram 实例，如果未初始化或加载失败则返回 null
     */
    public ShaderProgram getOrNull() {
        return SHADER_CACHE.get(this);
    }

    /**
     * 检查该着色器是否已成功加载。
     *
     * @return true 如果着色器已加载且有效
     */
    public boolean isLoaded() {
        ShaderProgram shader = SHADER_CACHE.get(this);
        return shader != null && shader.isValid();
    }

    /**
     * 清理并删除所有着色器程序，释放 GPU 资源。 通常在游戏退出或资源重载时调用。
     * 注意：必须在具有 OpenGL 上下文的线程中调用
     */
    public static void cleanupAll() {
        SHADER_CACHE.forEach((type, shader) -> {
            try {
                shader.close();
            } catch (Exception e) {
                // 忽略清理时的错误（可能没有 GL 上下文）
            }
        });
        SHADER_CACHE.clear();
    }

    /**
     * 内部方法：尝试创建指定类型的着色器，并加入缓存。 加载失败时将记录错误日志。
     *
     * @param type ShaderType 类型
     */
    private static void create(ShaderType type) {
        if (!SHADER_CACHE.containsKey(type)) {
            ShaderProgram shader = new ShaderProgram(DOMAIN, type.vertFilepath, type.fragFilepath);
            if (shader.getProgram() != 0) {
                SHADER_CACHE.put(type, shader);
                TakoTechMod.LOG
                    .info("Shader '{}' loaded successfully. (Program ID = {})", type.name(), shader.getProgram());
            } else {
                TakoTechMod.LOG.error(
                    "Failed to load shader '{}'. vert='{}', frag='{}'.",
                    type.name(),
                    type.vertFilepath,
                    type.fragFilepath);
            }
        }
    }
}
