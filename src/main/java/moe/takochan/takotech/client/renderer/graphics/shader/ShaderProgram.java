package moe.takochan.takotech.client.renderer.graphics.shader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;

/**
 * Shader Program 封装类。
 * 负责加载、编译、链接着色器，并提供 uniform 设置接口。
 * 实现 AutoCloseable 以支持 try-with-resources。
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Shader">OpenGL Shader Wiki</a>
 */
@SideOnly(Side.CLIENT)
public class ShaderProgram implements AutoCloseable {

    private int programId = 0;
    private int vertexShaderId = 0;
    private int fragmentShaderId = 0;

    /** Uniform location 缓存 */
    private final Map<String, Integer> uniformCache = new HashMap<>();

    /**
     * 检查当前系统是否支持 Shader
     *
     * @return true 如果支持 Shader
     */
    public static boolean isSupported() {
        return OpenGlHelper.shadersSupported;
    }

    /**
     * 从资源文件创建 Shader Program
     *
     * @param domain       资源域 (mod id)
     * @param vertexPath   顶点着色器路径
     * @param fragmentPath 片元着色器路径
     */
    public ShaderProgram(String domain, String vertexPath, String fragmentPath) {
        if (!isSupported()) {
            TakoTechMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        try {
            String vertexSource = loadShaderSource(domain, vertexPath);
            String fragmentSource = loadShaderSource(domain, fragmentPath);

            if (vertexSource == null || fragmentSource == null) {
                TakoTechMod.LOG.error("Failed to load shader sources: {} / {}", vertexPath, fragmentPath);
                return;
            }

            createProgram(vertexSource, fragmentSource);

        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create shader program", e);
            cleanup();
        }
    }

    /**
     * 从源码直接创建 Shader Program
     *
     * @param vertexSource   顶点着色器源码
     * @param fragmentSource 片元着色器源码
     */
    public ShaderProgram(String vertexSource, String fragmentSource) {
        if (!isSupported()) {
            TakoTechMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        try {
            createProgram(vertexSource, fragmentSource);
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create shader program", e);
            cleanup();
        }
    }

    /**
     * 创建着色器程序的核心逻辑
     */
    private void createProgram(String vertexSource, String fragmentSource) {
        vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            cleanup();
            return;
        }

        programId = linkProgram(vertexShaderId, fragmentShaderId);

        if (programId != 0) {
            validateProgram();
        }
    }

    /**
     * 加载着色器源码
     */
    private String loadShaderSource(String domain, String path) {
        try {
            ResourceLocation location = new ResourceLocation(domain, path);
            InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(location)
                .getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines()
                    .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to load shader: {}:{}", domain, path, e);
            return null;
        }
    }

    /**
     * 编译着色器
     */
    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 1024);
            String typeName = (type == GL20.GL_VERTEX_SHADER) ? "vertex" : "fragment";
            TakoTechMod.LOG.error("Failed to compile {} shader:\n{}", typeName, log);
            GL20.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    /**
     * 链接着色器程序
     */
    private int linkProgram(int vertexId, int fragmentId) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexId);
        GL20.glAttachShader(program, fragmentId);

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            TakoTechMod.LOG.error("Failed to link shader program:\n{}", log);
            GL20.glDeleteProgram(program);
            return 0;
        }

        // 链接成功后可以删除着色器对象
        GL20.glDetachShader(program, vertexId);
        GL20.glDetachShader(program, fragmentId);
        GL20.glDeleteShader(vertexId);
        GL20.glDeleteShader(fragmentId);
        vertexShaderId = 0;
        fragmentShaderId = 0;

        return program;
    }

    /**
     * 验证着色器程序
     * 在当前 OpenGL 状态下检查程序是否可执行
     */
    private void validateProgram() {
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId, 1024);
            TakoTechMod.LOG.warn("Shader program validation warning (ID={}):\n{}", programId, log);
            // 注意：验证失败不一定意味着程序不可用，只是警告
        }
    }

    /**
     * 使用此着色器程序
     */
    public void use() {
        if (programId != 0) {
            GL20.glUseProgram(programId);
        }
    }

    /**
     * 解绑着色器程序（使用固定管线）
     */
    public static void unbind() {
        GL20.glUseProgram(0);
    }

    /**
     * 获取程序 ID
     */
    public int getProgram() {
        return programId;
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return programId != 0;
    }

    /**
     * 释放资源（遗留方法，建议使用 close()）
     */
    public void delete() {
        cleanup();
    }

    /**
     * 实现 AutoCloseable，支持 try-with-resources
     */
    @Override
    public void close() {
        cleanup();
    }

    private void cleanup() {
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId);
            vertexShaderId = 0;
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        uniformCache.clear();
    }

    // ==================== Uniform Location ====================

    /**
     * 获取 uniform location（带缓存）
     */
    public int getUniformLocation(String name) {
        if (!isValid()) {
            return -1;
        }

        return uniformCache.computeIfAbsent(name, n -> {
            int loc = GL20.glGetUniformLocation(programId, n);
            if (loc == -1) {
                TakoTechMod.LOG.warn("Uniform '{}' not found in shader program (ID = {})", n, programId);
            }
            return loc;
        });
    }

    // ==================== Int Uniforms ====================

    public boolean setUniformInt(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform1i(loc, value);
        return true;
    }

    public boolean setUniformInt(String name, int x, int y) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform2i(loc, x, y);
        return true;
    }

    public boolean setUniformInt(String name, int x, int y, int z) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform3i(loc, x, y, z);
        return true;
    }

    public boolean setUniformInt(String name, int x, int y, int z, int w) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform4i(loc, x, y, z, w);
        return true;
    }

    // ==================== Boolean Uniforms ====================

    public boolean setUniformBool(String name, boolean value) {
        return setUniformInt(name, value ? 1 : 0);
    }

    // ==================== Float Uniforms ====================

    public boolean setUniformFloat(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform1f(loc, value);
        return true;
    }

    public boolean setUniformVec2(String name, float x, float y) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform2f(loc, x, y);
        return true;
    }

    public boolean setUniformVec3(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform3f(loc, x, y, z);
        return true;
    }

    public boolean setUniformVec4(String name, float x, float y, float z, float w) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniform4f(loc, x, y, z, w);
        return true;
    }

    // ==================== Matrix Uniforms ====================

    public boolean setUniformMatrix3(String name, boolean transpose, FloatBuffer matrix) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniformMatrix3(loc, transpose, matrix);
        return true;
    }

    public boolean setUniformMatrix4(String name, boolean transpose, FloatBuffer matrix) {
        int loc = getUniformLocation(name);
        if (loc == -1) return false;
        GL20.glUniformMatrix4(loc, transpose, matrix);
        return true;
    }
}
