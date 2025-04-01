package moe.takochan.takotech.client.renderer.shader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.common.Reference;
import org.lwjgl.opengl.GL20;

@SideOnly(Side.CLIENT)
public enum ShaderType {

    BLUR("shaders/blur.vert", "shaders/blur.frag"),
    AERO("shaders/aero.vert", "shaders/aero.frag");

    private final String vertFilepath;
    private final String fragFilepath;

    ShaderType(String vertFilepath, String fragFilepath) {
        this.vertFilepath = vertFilepath;
        this.fragFilepath = fragFilepath;
    }

    private static final Map<ShaderType, ShaderProgram> SHADER_CACHE = new ConcurrentHashMap<>();

    private static final String DOMAIN = Reference.RESOURCE_ROOT_ID;

    /**
     * 初始化所有着色器程序
     */
    public static void register() {
        for (ShaderType type : values()) {
            create(type);
        }
    }

    // Getter方法
    public String getVertShaderFilename() {
        return vertFilepath;
    }

    public String getFragShaderFilename() {
        return fragFilepath;
    }

    /**
     * 创建单个着色器程序
     */
    private static void create(ShaderType type) {
        if (!SHADER_CACHE.containsKey(type)) {
            ShaderProgram shader = new ShaderProgram(DOMAIN, type.vertFilepath, type.fragFilepath);
            if (shader.getProgram() != 0) {
                SHADER_CACHE.put(type, shader);
            }
        }
    }

    /**
     * 获取已初始化的着色器程序
     *
     * @throws IllegalStateException 如果着色器尚未初始化
     */
    public ShaderProgram get() {
        if (!SHADER_CACHE.containsKey(this)) {
            throw new IllegalStateException("Shader " + name() + " not initialized. Call initializeAll() first.");
        }
        return SHADER_CACHE.get(this);
    }

    /**
     * 清理所有着色器程序
     */
    public static void cleanupAll() {
        SHADER_CACHE.forEach((type, shader) -> {
            if (shader.getProgram() != 0) {
                GL20.glDeleteProgram(shader.getProgram());
            }
        });
        SHADER_CACHE.clear();
    }
}
