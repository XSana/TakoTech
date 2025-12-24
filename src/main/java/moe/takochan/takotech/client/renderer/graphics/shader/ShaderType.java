package moe.takochan.takotech.client.renderer.graphics.shader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.opengl.GL20;

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
    GUI_COLOR("shaders/gui_color.vert", "shaders/gui_color.frag");

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
     * 清理并删除所有着色器程序，释放 GPU 资源。 通常在游戏退出或资源重载时调用。
     * 注意：必须在具有 OpenGL 上下文的线程中调用
     */
    public static void cleanupAll() {
        SHADER_CACHE.forEach((type, shader) -> {
            try {
                if (shader.getProgram() != 0) {
                    GL20.glDeleteProgram(shader.getProgram());
                }
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
