package moe.takochan.takotech.client.renderer.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.renderer.graphics.ecs.Entity;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleSystem;
import moe.takochan.takotech.client.renderer.graphics.postprocess.PostProcessor;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;

/**
 * 渲染框架测试日志系统。
 * 输出详细的测试信息到独立日志文件，便于问题排查。
 *
 * <p>
 * 日志文件位置: minecraft/logs/takotech_render_test.log
 * </p>
 *
 * <p>
 * 日志内容包括:
 * </p>
 * <ul>
 * <li>GL/Shader 环境信息</li>
 * <li>玩家信息（位置、朝向、维度）</li>
 * <li>世界信息（时间、天气、生物群系）</li>
 * <li>ECS 实体状态</li>
 * <li>渲染组件状态（Mesh、Material、Particle 等）</li>
 * <li>性能数据（FPS、帧时间）</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class TestLogger {

    private static final String LOG_FILE_NAME = "takotech_render_test.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private PrintWriter writer;
    private File logFile;
    private boolean isOpen = false;

    private int frameCounter = 0;
    private int logFrameInterval = 60; // 每 60 帧记录一次帧日志

    // ==================== Lifecycle ====================

    /**
     * 开始新的测试会话，创建日志文件
     */
    public void startSession(String sessionName) {
        try {
            // 创建日志目录
            File logDir = new File(Minecraft.getMinecraft().mcDataDir, "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 创建带时间戳的日志文件
            String timestamp = FILE_DATE_FORMAT.format(new Date());
            String fileName = "takotech_render_test_" + timestamp + ".log";
            logFile = new File(logDir, fileName);

            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)));
            isOpen = true;

            // 写入会话头
            writeSeparator('=');
            log("RENDER FRAMEWORK TEST SESSION: " + sessionName);
            log("Log File: " + logFile.getAbsolutePath());
            log("Started: " + DATE_FORMAT.format(new Date()));
            writeSeparator('=');
            writeNewLine();

            TakoTechMod.LOG.info("[TestLogger] Log file created: {}", logFile.getAbsolutePath());

        } catch (IOException e) {
            TakoTechMod.LOG.error("[TestLogger] Failed to create log file", e);
            isOpen = false;
        }
    }

    /**
     * 结束测试会话，关闭日志文件
     */
    public void endSession() {
        if (!isOpen) return;

        writeNewLine();
        writeSeparator('=');
        log("SESSION ENDED: " + DATE_FORMAT.format(new Date()));
        writeSeparator('=');

        writer.flush();
        writer.close();
        isOpen = false;

        TakoTechMod.LOG.info("[TestLogger] Log file closed: {}", logFile.getAbsolutePath());
    }

    /**
     * 获取日志文件路径
     */
    public String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "N/A";
    }

    // ==================== Environment Logging ====================

    /**
     * 记录 OpenGL 环境信息
     */
    public void logGLEnvironment() {
        if (!isOpen) return;

        writeSection("OPENGL ENVIRONMENT");

        log("GL_VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
        log("GL_RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
        log("GL_VERSION: " + GL11.glGetString(GL11.GL_VERSION));

        try {
            log("GL_SHADING_LANGUAGE_VERSION: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        } catch (Exception e) {
            log("GL_SHADING_LANGUAGE_VERSION: Not available");
        }

        log("GL_MAX_TEXTURE_SIZE: " + GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        log("GL_MAX_VIEWPORT_DIMS: " + GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS));

        try {
            log("GL_MAX_VERTEX_ATTRIBS: " + GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS));
            log("GL_MAX_VERTEX_UNIFORM_COMPONENTS: " + GL11.glGetInteger(GL20.GL_MAX_VERTEX_UNIFORM_COMPONENTS));
            log("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS: " + GL11.glGetInteger(GL20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS));
        } catch (Exception e) {
            log("Shader limits: Not available");
        }

        writeNewLine();
    }

    /**
     * 记录渲染系统状态
     */
    public void logRenderSystemState() {
        if (!isOpen) return;

        writeSection("RENDER SYSTEM STATE");

        log("RenderSystem.isInitialized: " + RenderSystem.isInitialized());
        log("RenderSystem.isShaderSupported: " + RenderSystem.isShaderSupported());
        log("RenderSystem.isParticleSystemSupported: " + RenderSystem.isParticleSystemSupported());

        // ECS World
        moe.takochan.takotech.client.renderer.graphics.ecs.World world = RenderSystem.getWorld();
        if (world != null) {
            log("ECS World: entity count = " + world.getEntityCount());
        } else {
            log("ECS World: null");
        }

        // Post Processor
        PostProcessor pp = RenderSystem.getPostProcessor();
        if (pp != null) {
            log("PostProcessor: enabled = " + pp.isEnabled());
        } else {
            log("PostProcessor: null");
        }

        writeNewLine();
    }

    /**
     * 记录 Shader 状态
     */
    public void logShaderState() {
        if (!isOpen) return;

        writeSection("SHADER STATE");

        ShaderType[] types = ShaderType.values();
        for (ShaderType type : types) {
            try {
                ShaderProgram shader = type.get();
                if (shader != null) {
                    log(
                        String.format(
                            "Shader %s: valid=%b, programId=%d",
                            type.name(),
                            shader.isValid(),
                            shader.getProgram()));
                } else {
                    log(String.format("Shader %s: null", type.name()));
                }
            } catch (Exception e) {
                log(String.format("Shader %s: error - %s", type.name(), e.getMessage()));
            }
        }

        writeNewLine();
    }

    // ==================== Player & World Logging ====================

    /**
     * 记录玩家信息
     */
    public void logPlayerInfo(EntityPlayer player) {
        if (!isOpen || player == null) return;

        writeSection("PLAYER INFO");

        log("Name: " + player.getCommandSenderName());
        log(
            "Position: x=" + String.format("%.3f", player.posX)
                + ", y="
                + String.format("%.3f", player.posY)
                + ", z="
                + String.format("%.3f", player.posZ));
        log(
            "Motion: vx=" + String.format("%.3f", player.motionX)
                + ", vy="
                + String.format("%.3f", player.motionY)
                + ", vz="
                + String.format("%.3f", player.motionZ));
        log(
            "Rotation: yaw=" + String.format("%.1f", player.rotationYaw)
                + ", pitch="
                + String.format("%.1f", player.rotationPitch));
        log("Eye Height: " + String.format("%.3f", player.getEyeHeight()));
        log("On Ground: " + player.onGround);
        log("In Water: " + player.isInWater());
        log("Sneaking: " + player.isSneaking());
        log("Sprinting: " + player.isSprinting());

        // 手持物品
        if (player.getHeldItem() != null) {
            log(
                "Held Item: " + player.getHeldItem()
                    .getDisplayName()
                    + " (meta="
                    + player.getHeldItem()
                        .getItemDamage()
                    + ")");
        } else {
            log("Held Item: none");
        }

        writeNewLine();
    }

    /**
     * 记录世界信息
     */
    public void logWorldInfo(World world) {
        if (!isOpen || world == null) return;

        writeSection("WORLD INFO");

        log("Dimension: " + world.provider.dimensionId + " (" + world.provider.getDimensionName() + ")");
        log("World Time: " + world.getWorldTime() + " (day " + (world.getWorldTime() / 24000) + ")");
        log("Total Time: " + world.getTotalWorldTime());
        log("Is Daytime: " + world.isDaytime());
        log("Is Raining: " + world.isRaining());
        log("Is Thundering: " + world.isThundering());
        log("Moon Phase: " + world.getMoonPhase());
        log("Difficulty: " + world.difficultySetting.name());

        // 玩家所在区块和生物群系
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            int px = (int) Math.floor(mc.thePlayer.posX);
            int py = (int) Math.floor(mc.thePlayer.posY);
            int pz = (int) Math.floor(mc.thePlayer.posZ);
            log("Player Chunk: " + (px >> 4) + ", " + (pz >> 4));
            log("Player Biome: " + world.getBiomeGenForCoords(px, pz).biomeName);
            log("Light Level (block): " + world.getBlockLightValue(px, py, pz));
            log(
                "Light Level (sky): "
                    + world.getSkyBlockTypeBrightness(net.minecraft.world.EnumSkyBlock.Sky, px, py, pz));
        }

        writeNewLine();
    }

    // ==================== Entity & Component Logging ====================

    /**
     * 记录实体创建
     */
    public void logEntityCreated(Entity entity, String description) {
        if (!isOpen || entity == null) return;

        log(
            String.format(
                "[ENTITY CREATED] id=%d, name='%s', description='%s'",
                entity.getId(),
                entity.getName(),
                description));
    }

    /**
     * 记录 Mesh 创建
     */
    public void logMeshCreated(String name, Mesh mesh) {
        if (!isOpen) return;

        if (mesh != null) {
            log(
                String.format(
                    "[MESH CREATED] name='%s', valid=%b, vao=%d, vbo=%d, ebo=%d, elements=%d",
                    name,
                    mesh.isValid(),
                    mesh.getVao(),
                    mesh.getVbo(),
                    mesh.getEbo(),
                    mesh.getElementCount()));
        } else {
            log(String.format("[MESH CREATED] name='%s', mesh=null (FAILED)", name));
        }
    }

    /**
     * 记录粒子系统创建
     */
    public void logParticleSystemCreated(String name, ParticleSystem system) {
        if (!isOpen) return;

        if (system != null) {
            log(String.format("[PARTICLE CREATED] name='%s', maxParticles=%d", name, system.getMaxParticles()));
        } else {
            log(String.format("[PARTICLE CREATED] name='%s', system=null (FAILED)", name));
        }
    }

    // ==================== Frame Logging ====================

    /**
     * 记录帧信息（按间隔记录）
     */
    public void logFrameInfo(float time, float deltaTime, int fps, int entityCount, int visibleMeshes,
        int activeParticles) {
        if (!isOpen) return;

        frameCounter++;
        if (frameCounter < logFrameInterval) return;
        frameCounter = 0;

        log(
            String.format(
                "[FRAME] time=%.2fs, dt=%.4fs, fps=%d, entities=%d, meshes=%d, particles=%d",
                time,
                deltaTime,
                fps,
                entityCount,
                visibleMeshes,
                activeParticles));
    }

    /**
     * 强制记录当前帧信息
     */
    public void logFrameInfoForced(float time, float deltaTime, int fps, int entityCount, int visibleMeshes,
        int activeParticles) {
        if (!isOpen) return;

        log(
            String.format(
                "[FRAME SNAPSHOT] time=%.2fs, dt=%.4fs, fps=%d, entities=%d, meshes=%d, particles=%d",
                time,
                deltaTime,
                fps,
                entityCount,
                visibleMeshes,
                activeParticles));
    }

    /**
     * 记录相机信息
     */
    public void logCameraInfo(double camX, double camY, double camZ, float partialTicks) {
        if (!isOpen) return;

        log(String.format("[CAMERA] pos=(%.3f, %.3f, %.3f), partialTicks=%.3f", camX, camY, camZ, partialTicks));
    }

    // ==================== Error & Warning Logging ====================

    /**
     * 记录错误
     */
    public void logError(String message, Throwable t) {
        if (!isOpen) return;

        log("[ERROR] " + message);
        if (t != null) {
            log(
                "  Exception: " + t.getClass()
                    .getName() + ": " + t.getMessage());
            for (StackTraceElement elem : t.getStackTrace()) {
                log("    at " + elem.toString());
                if (elem.getClassName()
                    .startsWith("moe.takochan")
                    && !elem.getClassName()
                        .contains("TestLogger")) {
                    break; // 只显示到我们的代码
                }
            }
        }
    }

    /**
     * 记录警告
     */
    public void logWarning(String message) {
        if (!isOpen) return;
        log("[WARNING] " + message);
    }

    /**
     * 记录信息
     */
    public void logInfo(String message) {
        if (!isOpen) return;
        log("[INFO] " + message);
    }

    /**
     * 记录调试信息
     */
    public void logDebug(String message) {
        if (!isOpen) return;
        log("[DEBUG] " + message);
    }

    // ==================== Test Result Logging ====================

    /**
     * 记录测试结果
     */
    public void logTestResult(String testName, boolean passed, String details) {
        if (!isOpen) return;

        String status = passed ? "PASS" : "FAIL";
        log(String.format("[TEST %s] %s: %s", status, testName, details));
    }

    /**
     * 记录测试摘要
     */
    public void logTestSummary(int totalTests, int passedTests, int failedTests) {
        if (!isOpen) return;

        writeNewLine();
        writeSeparator('-');
        log("TEST SUMMARY");
        log("Total: " + totalTests);
        log("Passed: " + passedTests);
        log("Failed: " + failedTests);
        log("Pass Rate: " + String.format("%.1f%%", (passedTests * 100.0 / totalTests)));
        writeSeparator('-');
    }

    // ==================== Helper Methods ====================

    private void log(String message) {
        if (writer != null) {
            String timestamp = DATE_FORMAT.format(new Date());
            writer.println("[" + timestamp + "] " + message);
            writer.flush();
        }
    }

    private void writeNewLine() {
        if (writer != null) {
            writer.println();
            writer.flush();
        }
    }

    private void writeSeparator(char c) {
        if (writer != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 80; i++) {
                sb.append(c);
            }
            writer.println(sb.toString());
            writer.flush();
        }
    }

    private void writeSection(String title) {
        if (writer != null) {
            writeSeparator('-');
            log(title);
            writeSeparator('-');
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    /**
     * 设置帧日志间隔
     */
    public void setLogFrameInterval(int interval) {
        this.logFrameInterval = Math.max(1, interval);
    }
}
