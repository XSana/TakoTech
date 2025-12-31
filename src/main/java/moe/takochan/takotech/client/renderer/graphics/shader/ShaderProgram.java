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
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLContext;

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
    private int geometryShaderId = 0;
    private int computeShaderId = 0;

    /** 是否为 Compute Shader 程序 */
    private boolean isComputeProgram = false;

    /** Uniform location 缓存 */
    private final Map<String, Integer> uniformCache = new HashMap<>();

    /** Geometry Shader 支持状态缓存 */
    private static Boolean geometryShaderSupported = null;

    /** Compute Shader 支持状态缓存 */
    private static Boolean computeShaderSupported = null;

    /** SSBO 支持状态缓存 */
    private static Boolean ssboSupported = null;

    /** Compute Shader 工作组大小限制缓存 */
    private static int[] maxWorkGroupSize = null;
    private static int maxWorkGroupInvocations = -1;

    /**
     * 检查当前系统是否支持 Shader
     *
     * @return true 如果支持 Shader
     */
    public static boolean isSupported() {
        return OpenGlHelper.shadersSupported;
    }

    /**
     * 检查是否支持 Geometry Shader (GL32)
     *
     * @return true 如果支持 Geometry Shader
     */
    public static boolean isGeometryShaderSupported() {
        if (geometryShaderSupported == null) {
            try {
                geometryShaderSupported = GLContext.getCapabilities().OpenGL32
                    || GLContext.getCapabilities().GL_ARB_geometry_shader4;
            } catch (Exception e) {
                geometryShaderSupported = false;
            }
        }
        return geometryShaderSupported;
    }

    /**
     * 检查是否支持 Compute Shader (GL43)
     *
     * @return true 如果支持 Compute Shader
     */
    public static boolean isComputeShaderSupported() {
        if (computeShaderSupported == null) {
            try {
                var caps = GLContext.getCapabilities();
                boolean gl43 = caps.OpenGL43;
                boolean arbCompute = caps.GL_ARB_compute_shader;
                computeShaderSupported = gl43 || arbCompute;
                TakoTechMod.LOG.info(
                    "Compute Shader support check: OpenGL43={}, ARB_compute_shader={}, result={}",
                    gl43,
                    arbCompute,
                    computeShaderSupported);
            } catch (Exception e) {
                TakoTechMod.LOG.error("Failed to check compute shader support", e);
                computeShaderSupported = false;
            }
        }
        return computeShaderSupported;
    }

    /**
     * 检查是否支持 SSBO (Shader Storage Buffer Object, GL43)
     *
     * @return true 如果支持 SSBO
     */
    public static boolean isSSBOSupported() {
        if (ssboSupported == null) {
            try {
                var caps = GLContext.getCapabilities();
                boolean gl43 = caps.OpenGL43;
                boolean arbSsbo = caps.GL_ARB_shader_storage_buffer_object;
                ssboSupported = gl43 || arbSsbo;
                TakoTechMod.LOG.info(
                    "SSBO support check: OpenGL43={}, ARB_shader_storage_buffer_object={}, result={}",
                    gl43,
                    arbSsbo,
                    ssboSupported);
            } catch (Exception e) {
                TakoTechMod.LOG.error("Failed to check SSBO support", e);
                ssboSupported = false;
            }
        }
        return ssboSupported;
    }

    /**
     * 获取 Compute Shader 最大工作组大小
     *
     * @return [maxX, maxY, maxZ] 数组
     */
    public static int[] getMaxWorkGroupSize() {
        if (maxWorkGroupSize == null && isComputeShaderSupported()) {
            maxWorkGroupSize = new int[3];
            maxWorkGroupSize[0] = GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE);
            // 需要用 indexed 查询获取 Y 和 Z
            // 这里使用通用限制
            maxWorkGroupSize[1] = maxWorkGroupSize[0];
            maxWorkGroupSize[2] = maxWorkGroupSize[0];
        }
        return maxWorkGroupSize != null ? maxWorkGroupSize : new int[] { 0, 0, 0 };
    }

    /**
     * 获取单个工作组最大调用数
     *
     * @return 最大调用数
     */
    public static int getMaxWorkGroupInvocations() {
        if (maxWorkGroupInvocations < 0 && isComputeShaderSupported()) {
            maxWorkGroupInvocations = GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
        }
        return maxWorkGroupInvocations > 0 ? maxWorkGroupInvocations : 0;
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

            createProgram(vertexSource, null, fragmentSource);

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
            createProgram(vertexSource, null, fragmentSource);
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create shader program", e);
            cleanup();
        }
    }

    /**
     * 从资源文件创建带 Geometry Shader 的程序
     *
     * @param domain       资源域 (mod id)
     * @param vertexPath   顶点着色器路径
     * @param geometryPath 几何着色器路径 (可为 null)
     * @param fragmentPath 片元着色器路径
     */
    public ShaderProgram(String domain, String vertexPath, String geometryPath, String fragmentPath) {
        if (!isSupported()) {
            TakoTechMod.LOG.warn("Shaders not supported on this system");
            return;
        }

        if (geometryPath != null && !isGeometryShaderSupported()) {
            TakoTechMod.LOG.warn("Geometry shaders not supported, falling back to vertex/fragment only");
            geometryPath = null;
        }

        try {
            String vertexSource = loadShaderSource(domain, vertexPath);
            String geometrySource = geometryPath != null ? loadShaderSource(domain, geometryPath) : null;
            String fragmentSource = loadShaderSource(domain, fragmentPath);

            if (vertexSource == null || fragmentSource == null) {
                TakoTechMod.LOG.error("Failed to load shader sources: {} / {}", vertexPath, fragmentPath);
                return;
            }

            createProgram(vertexSource, geometrySource, fragmentSource);

        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create shader program with geometry shader", e);
            cleanup();
        }
    }

    /**
     * 从源码创建带 Geometry Shader 的程序（静态工厂方法）
     *
     * @param vertexSource   顶点着色器源码
     * @param geometrySource 几何着色器源码 (可为 null)
     * @param fragmentSource 片元着色器源码
     * @return 创建的 ShaderProgram，如果失败则返回无效的程序
     */
    public static ShaderProgram createFromSource(String vertexSource, String geometrySource, String fragmentSource) {
        ShaderProgram program = new ShaderProgram();

        if (!isSupported()) {
            TakoTechMod.LOG.warn("Shaders not supported on this system");
            return program;
        }

        if (geometrySource != null && !isGeometryShaderSupported()) {
            TakoTechMod.LOG.warn("Geometry shaders not supported, ignoring geometry shader");
            geometrySource = null;
        }

        try {
            program.createProgram(vertexSource, geometrySource, fragmentSource);
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create shader program", e);
            program.cleanup();
        }

        return program;
    }

    /**
     * 从资源文件创建 Compute Shader 程序
     *
     * @param domain      资源域 (mod id)
     * @param computePath 计算着色器路径
     * @return 创建的 ShaderProgram，如果失败则返回无效的程序
     */
    public static ShaderProgram createCompute(String domain, String computePath) {
        ShaderProgram program = new ShaderProgram();

        if (!isComputeShaderSupported()) {
            TakoTechMod.LOG.warn("Compute shaders not supported on this system");
            return program;
        }

        try {
            String computeSource = program.loadShaderSource(domain, computePath);
            if (computeSource == null) {
                TakoTechMod.LOG.error("Failed to load compute shader source: {}", computePath);
                return program;
            }
            program.createComputeProgram(computeSource);
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create compute shader program", e);
            program.cleanup();
        }

        return program;
    }

    /**
     * 从源码创建 Compute Shader 程序
     *
     * @param computeSource 计算着色器源码
     * @return 创建的 ShaderProgram，如果失败则返回无效的程序
     */
    public static ShaderProgram createComputeFromSource(String computeSource) {
        ShaderProgram program = new ShaderProgram();

        if (!isComputeShaderSupported()) {
            TakoTechMod.LOG.warn("Compute shaders not supported on this system");
            return program;
        }

        try {
            program.createComputeProgram(computeSource);
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to create compute shader program", e);
            program.cleanup();
        }

        return program;
    }

    /**
     * 私有默认构造函数（用于静态工厂方法）
     */
    private ShaderProgram() {
        // 空构造函数
    }

    /**
     * 创建 Compute Shader 程序的核心逻辑
     */
    private void createComputeProgram(String computeSource) {
        computeShaderId = compileShader(GL43.GL_COMPUTE_SHADER, computeSource);

        if (computeShaderId == 0) {
            cleanup();
            return;
        }

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, computeShaderId);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(programId, 1024);
            TakoTechMod.LOG.error("Failed to link compute shader program:\n{}", log);
            GL20.glDeleteProgram(programId);
            programId = 0;
            return;
        }

        // 链接成功后删除着色器对象
        GL20.glDetachShader(programId, computeShaderId);
        GL20.glDeleteShader(computeShaderId);
        computeShaderId = 0;

        isComputeProgram = true;
        validateProgram();
    }

    /**
     * 创建着色器程序的核心逻辑
     */
    private void createProgram(String vertexSource, String geometrySource, String fragmentSource) {
        vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            cleanup();
            return;
        }

        // 编译几何着色器（可选）
        if (geometrySource != null && isGeometryShaderSupported()) {
            geometryShaderId = compileShader(GL32.GL_GEOMETRY_SHADER, geometrySource);
            if (geometryShaderId == 0) {
                TakoTechMod.LOG.warn("Geometry shader compilation failed, continuing without it");
            }
        }

        programId = linkProgram(vertexShaderId, geometryShaderId, fragmentShaderId);

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
            TakoTechMod.LOG.info("Loading shader from: {}", location);

            InputStream stream = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(location)
                .getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String source = reader.lines()
                    .collect(Collectors.joining("\n"));
                TakoTechMod.LOG.info("Loaded shader {} ({} chars)", path, source.length());
                if (source.isEmpty()) {
                    TakoTechMod.LOG.error("Shader source is empty: {}", path);
                }
                return source;
            }
        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to load shader: {}:{} - {}", domain, path, e.getMessage());
            e.printStackTrace();
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
            String typeName = getShaderTypeName(type);
            TakoTechMod.LOG.error("Failed to compile {} shader:\n{}", typeName, log);
            GL20.glDeleteShader(shaderId);
            return 0;
        }

        return shaderId;
    }

    /**
     * 获取着色器类型名称
     */
    private static String getShaderTypeName(int type) {
        if (type == GL20.GL_VERTEX_SHADER) return "vertex";
        if (type == GL20.GL_FRAGMENT_SHADER) return "fragment";
        if (type == GL32.GL_GEOMETRY_SHADER) return "geometry";
        if (type == GL43.GL_COMPUTE_SHADER) return "compute";
        return "unknown";
    }

    /**
     * 链接着色器程序
     */
    private int linkProgram(int vertexId, int geometryId, int fragmentId) {
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexId);
        if (geometryId != 0) {
            GL20.glAttachShader(program, geometryId);
        }
        GL20.glAttachShader(program, fragmentId);

        // Bind vertex attribute locations before linking
        // Must match the order used by Mesh/DynamicMesh
        GL20.glBindAttribLocation(program, 0, "aPos");
        GL20.glBindAttribLocation(program, 1, "aColor");
        GL20.glBindAttribLocation(program, 2, "aTexCoord");
        GL20.glBindAttribLocation(program, 2, "aLightCoord");
        GL20.glBindAttribLocation(program, 3, "aNormal");

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            TakoTechMod.LOG.error("Failed to link shader program:\n{}", log);
            GL20.glDeleteProgram(program);
            return 0;
        }

        // 链接成功后可以删除着色器对象
        GL20.glDetachShader(program, vertexId);
        GL20.glDeleteShader(vertexId);
        vertexShaderId = 0;

        if (geometryId != 0) {
            GL20.glDetachShader(program, geometryId);
            GL20.glDeleteShader(geometryId);
            geometryShaderId = 0;
        }

        GL20.glDetachShader(program, fragmentId);
        GL20.glDeleteShader(fragmentId);
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

    // ==================== Compute Shader 调度 ====================

    /**
     * 调度 Compute Shader
     * 必须先调用 use() 绑定程序
     *
     * @param numGroupsX X 方向工作组数量
     * @param numGroupsY Y 方向工作组数量
     * @param numGroupsZ Z 方向工作组数量
     */
    public void dispatch(int numGroupsX, int numGroupsY, int numGroupsZ) {
        if (!isComputeProgram || programId == 0) {
            TakoTechMod.LOG.warn("Cannot dispatch non-compute shader program");
            return;
        }
        GL43.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
    }

    /**
     * 调度 Compute Shader（1D）
     *
     * @param numGroupsX X 方向工作组数量
     */
    public void dispatch(int numGroupsX) {
        dispatch(numGroupsX, 1, 1);
    }

    /**
     * 设置内存屏障，确保 Compute Shader 写入完成后再读取
     *
     * @param barriers 屏障类型（GL_SHADER_STORAGE_BARRIER_BIT 等）
     */
    public static void memoryBarrier(int barriers) {
        if (isComputeShaderSupported()) {
            GL42.glMemoryBarrier(barriers);
        }
    }

    /**
     * SSBO 内存屏障
     */
    public static void memoryBarrierSSBO() {
        memoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
    }

    /**
     * 顶点属性内存屏障（用于 Compute → 渲染）
     */
    public static void memoryBarrierVertexAttrib() {
        // GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001
        memoryBarrier(0x00000001);
    }

    /**
     * 缓冲区更新内存屏障
     */
    public static void memoryBarrierBufferUpdate() {
        // GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200
        memoryBarrier(0x00000200);
    }

    /**
     * 所有内存屏障
     */
    public static void memoryBarrierAll() {
        memoryBarrier(GL42.GL_ALL_BARRIER_BITS);
    }

    /**
     * 是否为 Compute Shader 程序
     */
    public boolean isComputeProgram() {
        return isComputeProgram;
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
        if (geometryShaderId != 0) {
            GL20.glDeleteShader(geometryShaderId);
            geometryShaderId = 0;
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
            fragmentShaderId = 0;
        }
        if (computeShaderId != 0) {
            GL20.glDeleteShader(computeShaderId);
            computeShaderId = 0;
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
        uniformCache.clear();
        blockIndexCache.clear();
        ssboIndexCache.clear();
        isComputeProgram = false;
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

    // ==================== Uniform Block (UBO) ====================

    /** Uniform Block index 缓存 */
    private final Map<String, Integer> blockIndexCache = new HashMap<>();

    /**
     * 获取 Uniform Block 的索引
     *
     * @param blockName uniform block 名称
     * @return block index，如果不存在返回 -1
     */
    public int getUniformBlockIndex(String blockName) {
        if (!isValid()) {
            return -1;
        }

        return blockIndexCache.computeIfAbsent(blockName, name -> {
            int index = GL31.glGetUniformBlockIndex(programId, name);
            if (index == GL31.GL_INVALID_INDEX) {
                TakoTechMod.LOG.warn("Uniform block '{}' not found in shader program (ID = {})", name, programId);
                return -1;
            }
            return index;
        });
    }

    /**
     * 将 Uniform Block 绑定到指定的 binding point
     *
     * @param blockName    uniform block 名称
     * @param bindingPoint 绑定点（与 UBO 绑定的 binding point 对应）
     * @return true 如果绑定成功
     */
    public boolean bindUniformBlock(String blockName, int bindingPoint) {
        int blockIndex = getUniformBlockIndex(blockName);
        if (blockIndex == -1) {
            return false;
        }

        GL31.glUniformBlockBinding(programId, blockIndex, bindingPoint);
        return true;
    }

    /**
     * 获取 Uniform Block 的大小（字节）
     *
     * @param blockName uniform block 名称
     * @return block 大小，如果不存在返回 -1
     */
    public int getUniformBlockSize(String blockName) {
        int blockIndex = getUniformBlockIndex(blockName);
        if (blockIndex == -1) {
            return -1;
        }

        return GL31.glGetActiveUniformBlocki(programId, blockIndex, GL31.GL_UNIFORM_BLOCK_DATA_SIZE);
    }

    // ==================== Shader Storage Buffer (SSBO) ====================

    /** SSBO index 缓存 */
    private final Map<String, Integer> ssboIndexCache = new HashMap<>();

    /**
     * 获取 Shader Storage Block 的资源索引
     *
     * @param blockName storage block 名称
     * @return resource index，如果不存在返回 -1
     */
    public int getShaderStorageBlockIndex(String blockName) {
        if (!isValid() || !isSSBOSupported()) {
            return -1;
        }

        return ssboIndexCache.computeIfAbsent(blockName, name -> {
            int index = GL43.glGetProgramResourceIndex(programId, GL43.GL_SHADER_STORAGE_BLOCK, name);
            if (index == GL31.GL_INVALID_INDEX) {
                TakoTechMod.LOG
                    .warn("Shader storage block '{}' not found in shader program (ID = {})", name, programId);
                return -1;
            }
            return index;
        });
    }

    /**
     * 将 Shader Storage Block 绑定到指定的 binding point
     *
     * @param blockName    storage block 名称
     * @param bindingPoint 绑定点
     * @return true 如果绑定成功
     */
    public boolean bindShaderStorageBlock(String blockName, int bindingPoint) {
        int blockIndex = getShaderStorageBlockIndex(blockName);
        if (blockIndex == -1) {
            return false;
        }

        GL43.glShaderStorageBlockBinding(programId, blockIndex, bindingPoint);
        return true;
    }

    // GL43 常量 (LWJGL 2 可能不完整支持 GL43)
    /** GL_SHADER_STORAGE_BUFFER = 0x90D2 */
    private static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;

    /**
     * 将 SSBO 绑定到指定的 binding point（静态方法）
     *
     * @param ssboId       SSBO ID
     * @param bindingPoint 绑定点
     */
    public static void bindSSBO(int ssboId, int bindingPoint) {
        if (isSSBOSupported()) {
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId);
        }
    }

    /**
     * 将 SSBO 的一部分绑定到指定的 binding point
     *
     * @param ssboId       SSBO ID
     * @param bindingPoint 绑定点
     * @param offset       偏移量（字节）
     * @param size         大小（字节）
     */
    public static void bindSSBORange(int ssboId, int bindingPoint, long offset, long size) {
        if (isSSBOSupported()) {
            GL30.glBindBufferRange(GL_SHADER_STORAGE_BUFFER, bindingPoint, ssboId, offset, size);
        }
    }

    /**
     * 解绑指定 binding point 上的 SSBO
     *
     * @param bindingPoint 绑定点
     */
    public static void unbindSSBO(int bindingPoint) {
        if (isSSBOSupported()) {
            GL30.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingPoint, 0);
        }
    }
}
