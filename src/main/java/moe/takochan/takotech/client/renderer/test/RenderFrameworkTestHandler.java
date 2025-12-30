package moe.takochan.takotech.client.renderer.test;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.renderer.graphics.batch.InstancedBatch;
import moe.takochan.takotech.client.renderer.graphics.batch.SpriteBatch;
import moe.takochan.takotech.client.renderer.graphics.batch.World3DBatch;
import moe.takochan.takotech.client.renderer.graphics.buffer.GlobalUniforms;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.component.LineRendererComponent;
import moe.takochan.takotech.client.renderer.graphics.component.MeshRendererComponent;
import moe.takochan.takotech.client.renderer.graphics.component.ParticleSystemComponent;
import moe.takochan.takotech.client.renderer.graphics.component.TransformComponent;
import moe.takochan.takotech.client.renderer.graphics.core.RenderContext;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;
import moe.takochan.takotech.client.renderer.graphics.ecs.Entity;
import moe.takochan.takotech.client.renderer.graphics.ecs.World;
import moe.takochan.takotech.client.renderer.graphics.framebuffer.Framebuffer;
import moe.takochan.takotech.client.renderer.graphics.material.Material;
import moe.takochan.takotech.client.renderer.graphics.material.PBRMaterial;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;
import moe.takochan.takotech.client.renderer.graphics.mesh.DynamicMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.LODGroup;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.StaticMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexFormat;
import moe.takochan.takotech.client.renderer.graphics.model.MCModelLoader;
import moe.takochan.takotech.client.renderer.graphics.particle.CollisionMode;
import moe.takochan.takotech.client.renderer.graphics.particle.CollisionResponse;
import moe.takochan.takotech.client.renderer.graphics.particle.ColorOverLifetime;
import moe.takochan.takotech.client.renderer.graphics.particle.EmitterShape;
import moe.takochan.takotech.client.renderer.graphics.particle.Gradient;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleEmitter;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleForce;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticlePresets;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleRenderer;
import moe.takochan.takotech.client.renderer.graphics.particle.ParticleSystem;
import moe.takochan.takotech.client.renderer.graphics.particle.SizeOverLifetime;
import moe.takochan.takotech.client.renderer.graphics.postprocess.PostProcessor;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.client.renderer.graphics.texture.Texture2D;
import moe.takochan.takotech.client.renderer.util.GLStateManager;
import moe.takochan.takotech.client.renderer.util.MCRenderHelper;

/**
 * Comprehensive Render Framework Test Handler.
 *
 * <p>
 * Auto-starts when entering a world. Tests ALL rendering features:
 * </p>
 * <ul>
 * <li>ECS Core - Entity, Component, World</li>
 * <li>TransformComponent - position, rotation, scale, hierarchy</li>
 * <li>MeshRendererComponent - cube, sphere, plane, custom mesh</li>
 * <li>LineRendererComponent - spiral, circle, wave, lightning</li>
 * <li>ParticleSystemComponent - fire, smoke, electric, portal, healing</li>
 * <li>Material System - color, texture modes</li>
 * <li>PostProcessor - Bloom effect</li>
 * <li>SpriteBatch - 2D rendering</li>
 * <li>World3DBatch - 3D line/triangle batch</li>
 * <li>Custom Mesh Generation</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class RenderFrameworkTestHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 单例实例 */
    private static RenderFrameworkTestHandler instance;

    // ==================== State ====================

    /** 测试是否激活（按键 U 触发） */
    private boolean testActive = false;
    /** 资源是否已初始化 */
    private boolean initialized = false;
    /** 当前测试阶段 */
    private TestStage currentStage = TestStage.ENVIRONMENT;
    /** 是否启用自动接续 */
    private boolean autoAdvanceEnabled = false;
    /** 当前阶段计时 */
    private float stageTimer = 0.0f;
    /** Entity 控制阶段计时 */
    private float entityControlTimer = 0.0f;
    /** Entity 控制阶段显示切换 */
    private boolean entityControlVisible = true;
    /** Stage feedback map */
    private final EnumMap<TestStage, StageFeedback> stageFeedback = new EnumMap<>(TestStage.class);
    /** Last feedback result */
    private StageFeedback lastFeedback = null;
    /** Last feedback stage */
    private TestStage lastFeedbackStage = null;

    // ==================== Key State Tracking ====================

    /** 按键状态跟踪 (用于检测按键按下事件) */
    private boolean keyU_wasDown = false;
    private boolean keyI_wasDown = false;
    private boolean keyO_wasDown = false;
    private boolean keyP_wasDown = false;
    private boolean keyB_wasDown = false;

    // ==================== Logger ====================

    /** 测试日志系统 */
    private TestLogger logger = new TestLogger();
    /** 是否已记录初始环境信息 */
    private boolean loggedEnvironment = false;

    // ==================== Resources ====================

    /** Standard meshes */
    private Mesh cubeMesh;
    private Mesh sphereMesh;
    private Mesh planeMesh;
    private Mesh customPyramidMesh;
    private Mesh customTorusMesh;
    private Mesh mcBlockMesh;

    /** Dynamic mesh for testing updates */
    private DynamicMesh dynamicMesh;

    /** World3DBatch for line rendering */
    private World3DBatch worldBatch;
    /** Instanced batch for GPU instancing */
    private InstancedBatch instancedBatch;
    /** Instanced test base mesh */
    private Mesh instancedBaseMesh;
    /** Test shaders */
    private ShaderProgram meshNormalShader;
    private ShaderProgram meshColorShader;
    private ShaderProgram instancedShader;
    /** Test texture and framebuffer */
    private Texture2D testTexture;
    private Framebuffer testFramebuffer;
    /** PiP framebuffer for Camera test */
    private Framebuffer pipFramebuffer;
    private static final int PIP_SIZE = 256;
    /** PiP dedicated mesh and material for Camera test */
    private Mesh pipCubeMesh;
    private Material pipMaterial;
    private RenderContext pipRenderContext;
    /** Stage-specific helpers */
    private LODGroup lodGroup;
    private Entity lodEntity;
    private Camera debugCamera;
    private final FloatBuffer normalMatrixBuffer = BufferUtils.createFloatBuffer(9);

    // ==================== ECS Entities ====================

    /** Mesh test entities (ring around player) */
    private List<Entity> meshEntities = new ArrayList<>();

    /** Line test entity */
    private Entity lineEntity;
    /** MC block model test entity */
    private Entity mcBlockEntity;

    /** Particle test entities - basic presets */
    private List<Entity> particleEntities = new ArrayList<>();

    /** Advanced particle test entities - forces, collision, mesh particles */
    private List<Entity> advancedParticleEntities = new ArrayList<>();
    private Entity vortexEntity;
    private Entity attractorEntity;
    private List<Entity> meshParticleEntities = new ArrayList<>();

    /** Particle showcase entities and labels */
    private List<Entity> showcaseParticles = new ArrayList<>();
    private List<String> showcaseLabels = new ArrayList<>();
    private float[][] showcasePositions; // [index][x,y,z] for each station

    /** Bounce physics test entities and labels */
    private List<Entity> bounceTestEntities = new ArrayList<>();
    private List<String> bounceTestLabels = new ArrayList<>();
    private float[][] bounceTestPositions; // [index][x,y,z] for each station

    /** PBR test entities */
    private List<Entity> pbrEntities = new ArrayList<>();
    private List<PBRMaterial> pbrMaterials = new ArrayList<>();

    /** Parent-child hierarchy test */
    private Entity parentEntity;
    private List<Entity> childEntities = new ArrayList<>();

    /** Dynamic mesh test entity */
    private Entity dynamicMeshEntity;

    /** Gradient test entities for LineRenderer */
    private List<Entity> gradientLineEntities = new ArrayList<>();

    /** Magic circle entities */
    private Entity magicCircleOuter;
    private Entity magicCircleInner;
    private Entity magicTriangle1;
    private Entity magicTriangle2;
    private Entity magicCircleParticles;
    private float magicCircleRotation = 0;

    /** Magic circle build animation */
    private static final float MAGIC_CIRCLE_BUILD_TIME = 8.0f; // 8 seconds to fully build
    private float magicCircleBuildProgress = 0; // 0 to 1
    private boolean magicCircleBuilding = false;
    private float magicCircleCenterX, magicCircleCenterY, magicCircleCenterZ; // World position

    /** Magic circle target points (stored for gradual build) */
    private List<float[]> outerCirclePoints = new ArrayList<>();
    private List<float[]> innerCirclePoints = new ArrayList<>();
    private List<float[]> triangle1Points = new ArrayList<>();
    private List<float[]> triangle2Points = new ArrayList<>();

    /** Bloom test entities - large bright objects */
    private List<Entity> bloomTestEntities = new ArrayList<>();
    private boolean bloomTestEnabled = true; // Toggle with B key

    // ==================== Test Results ====================

    /** 测试结果统计 */
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;

    /** Stage stats */
    private int frustumVisibleCount = 0;
    private int frustumCulledCount = 0;
    private int currentLodLevel = -1;

    // ==================== Timing ====================

    private long lastTime = 0;
    private float time = 0;
    private float deltaTime = 0;

    /** 测试激活时玩家的初始朝向（弧度），用于固定实体位置 */
    private double baseYawRad = 0;
    /** 测试激活时玩家的初始位置 */
    private double basePlayerX = 0;
    private double basePlayerY = 0;
    private double basePlayerZ = 0;

    // ==================== Statistics ====================

    private int frameCount = 0;
    private float fpsTimer = 0;
    private int currentFPS = 0;

    // ==================== Constructor ====================

    public RenderFrameworkTestHandler() {
        instance = this;
        logger.logInfo("[RenderTest] Comprehensive test handler created (trigger: U key to toggle)");
    }

    // ==================== Key Input Handling ====================

    /**
     * 按键输入事件处理
     * U = 开关测试
     * I = 下一阶段
     * O = 上一阶段
     * P = 自动推进
     * B = 切换 Bloom
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // 检查是否在游戏中 (不在 GUI 界面)
        if (mc.currentScreen != null) {
            // 在 GUI 中，重置状态
            keyU_wasDown = keyI_wasDown = keyO_wasDown = keyP_wasDown = keyB_wasDown = false;
            return;
        }

        // U 键 - 开关测试
        boolean keyU_isDown = Keyboard.isKeyDown(Keyboard.KEY_U);
        if (keyU_isDown && !keyU_wasDown) {
            toggleTest();
        }
        keyU_wasDown = keyU_isDown;

        // I 键 - 下一阶段
        boolean keyI_isDown = Keyboard.isKeyDown(Keyboard.KEY_I);
        if (keyI_isDown && !keyI_wasDown) {
            nextStage();
        }
        keyI_wasDown = keyI_isDown;

        // O 键 - 上一阶段
        boolean keyO_isDown = Keyboard.isKeyDown(Keyboard.KEY_O);
        if (keyO_isDown && !keyO_wasDown) {
            previousStage();
        }
        keyO_wasDown = keyO_isDown;

        // P 键 - 自动推进
        boolean keyP_isDown = Keyboard.isKeyDown(Keyboard.KEY_P);
        if (keyP_isDown && !keyP_wasDown) {
            toggleAutoAdvance();
        }
        keyP_wasDown = keyP_isDown;

        // B 键 - 切换 Bloom
        boolean keyB_isDown = Keyboard.isKeyDown(Keyboard.KEY_B);
        if (keyB_isDown && !keyB_wasDown) {
            toggleBloom();
        }
        keyB_wasDown = keyB_isDown;
    }

    /**
     * 获取单例实例
     */
    public static RenderFrameworkTestHandler getInstance() {
        return instance;
    }

    /**
     * 静态方法：切换测试状态（由按键处理器调用）
     */
    public static void toggleTest() {
        if (instance != null) {
            instance.doToggleTest();
        }
    }

    /**
     * 静态方法：进入下一阶段
     */
    public static void nextStage() {
        if (instance != null) {
            instance.advanceStage(true);
        }
    }

    /**
     * 静态方法：进入上一阶段
     */
    public static void previousStage() {
        if (instance != null) {
            instance.advanceStage(false);
        }
    }

    /**
     * 静态方法：切换自动接续
     */
    public static void toggleAutoAdvance() {
        if (instance != null) {
            instance.doToggleAutoAdvance();
        }
    }

    /**
     * 静态方法：切换 Bloom 效果 (用于 BLOOM_TEST 阶段)
     */
    public static void toggleBloom() {
        if (instance != null) {
            instance.doToggleBloom();
        }
    }

    // ==================== Initialization ====================

    private void initialize() {
        if (initialized) return;
        if (!RenderSystem.isInitialized() || !RenderSystem.isShaderSupported()) return;

        logger.logInfo("[RenderTest] Initializing comprehensive test...");
        logger.logInfo("Initializing render framework test resources...");

        // Reset test counters
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;

        // 清空实体列表，防止重复初始化导致实体重复
        meshEntities.clear();
        particleEntities.clear();
        advancedParticleEntities.clear();
        childEntities.clear();
        gradientLineEntities.clear();
        pbrEntities.clear();
        pbrMaterials.clear();
        meshParticleEntities.clear();
        showcaseParticles.clear();
        showcaseLabels.clear();
        bounceTestEntities.clear();
        bounceTestLabels.clear();
        bloomTestEntities.clear();

        try {
            // ==================== 1. Shader Tests ====================
            logger.logInfo("--- Testing Shader System ---");
            testAllShaders();
            createTestShaders();

            // ==================== 2. Mesh Tests ====================
            logger.logInfo("--- Testing Mesh System ---");

            // Standard meshes
            cubeMesh = MCModelLoader.createCube(1.0f);
            logTestResult("StaticMesh Cube", cubeMesh != null && cubeMesh.isValid());
            logger.logMeshCreated("Cube (standard)", cubeMesh);

            sphereMesh = MCModelLoader.createSphere(0.5f, 16, 16);
            logTestResult("StaticMesh Sphere", sphereMesh != null && sphereMesh.isValid());
            logger.logMeshCreated("Sphere (standard)", sphereMesh);

            planeMesh = MCModelLoader.createPlane(2.0f, 2.0f);
            logTestResult("StaticMesh Plane", planeMesh != null && planeMesh.isValid());
            logger.logMeshCreated("Plane (standard)", planeMesh);

            // Custom meshes
            customPyramidMesh = createPyramidMesh();
            logTestResult("Custom Pyramid Mesh", customPyramidMesh != null && customPyramidMesh.isValid());
            logger.logMeshCreated("Pyramid (custom)", customPyramidMesh);

            customTorusMesh = createTorusMesh(0.5f, 0.2f, 24, 12);
            logTestResult("Custom Torus Mesh", customTorusMesh != null && customTorusMesh.isValid());
            logger.logMeshCreated("Torus (custom)", customTorusMesh);

            mcBlockMesh = MCModelLoader
                .loadBlockModel(new net.minecraft.util.ResourceLocation("takotech", "models/block/test_cube.json"));
            logTestResult("MCModelLoader.loadBlockModel()", mcBlockMesh != null && mcBlockMesh.isValid());
            logger.logMeshCreated("MC Block Model (test_cube)", mcBlockMesh);

            // Dynamic mesh
            dynamicMesh = new DynamicMesh(1000, 1500, VertexFormat.POSITION_3D_COLOR);
            logTestResult("DynamicMesh Creation", dynamicMesh != null && dynamicMesh.isValid());
            logger.logInfo("DynamicMesh created: maxVerts=1000, maxIndices=1500");

            // ==================== 3. Batch Rendering ====================
            logger.logInfo("--- Testing Batch Rendering ---");

            worldBatch = new World3DBatch(16384);
            logTestResult("World3DBatch Creation", worldBatch != null);
            logger.logInfo("World3DBatch created with capacity 16384");

            SpriteBatch spriteBatch = RenderSystem.getSpriteBatch();
            logTestResult("SpriteBatch Available", spriteBatch != null);

            if (InstancedBatch.isSupported()) {
                instancedBaseMesh = MCModelLoader.createCube(0.4f);
                instancedBatch = new InstancedBatch(instancedBaseMesh, 256);
                instancedBatch.init(); // Initialize GPU resources
                logTestResult("InstancedBatch Creation", instancedBatch != null);
                logger.logInfo("InstancedBatch created with capacity 256");
            } else {
                logTestResult("InstancedBatch Creation", false);
                logger.logWarning("InstancedBatch not supported by GPU");
            }

            // ==================== 4. Material Tests ====================
            logger.logInfo("--- Testing Material System ---");
            testMaterialSystem();

            // ==================== 4.1 Resource Tests ====================
            logger.logInfo("--- Testing Texture/Framebuffer ---");
            testGraphicsResources();

            // ==================== 5. AABB Tests ====================
            logger.logInfo("--- Testing AABB System ---");
            testAABBSystem();

            // ==================== 6. ECS Entity Tests ====================
            logger.logInfo("--- Testing ECS Entities ---");
            createMeshTestEntities();
            createModelTestEntity();
            createLineTestEntity();
            createGradientLineEntities();
            createParticleTestEntities();
            createAdvancedParticleTestEntities();
            createParticleShowcase();
            createBounceDemo();
            createBloomTestEntities();
            createMagicCircle();
            createHierarchyTestEntities();
            createDynamicMeshEntity();
            createLodTestEntity();
            createPbrTestEntities();

            // ==================== 7. PostProcessor Tests ====================
            logger.logInfo("--- Testing PostProcessor ---");
            PostProcessor pp = RenderSystem.getPostProcessor();
            if (pp != null) {
                pp.setEnabled(false);
                pp.setBloomThreshold(0.3f); // 更低的阈值，捕获更多亮部
                pp.setBloomIntensity(2.5f); // 更高的强度
                pp.setBlurIterations(6); // 更多迭代，更大扩散
                pp.setBlurScale(8.0f); // 更大的模糊范围
                logTestResult("PostProcessor Available", true);
                logger.logInfo("PostProcessor configured: threshold=0.3, intensity=2.5, blur=6, scale=2.0");
            } else {
                logTestResult("PostProcessor Available", false);
                logger.logWarning("PostProcessor is null, Bloom not available");
            }

            initialized = true;

            // Log summary
            logger.logInfo("--- Initialization Complete ---");
            logger.logTestSummary(totalTests, passedTests, failedTests);

        } catch (Exception e) {
            logger.logError("Failed to initialize render test", e);
        }
    }

    // ==================== Test Methods ====================

    /**
     * Test all shader types
     */
    private void testAllShaders() {
        for (ShaderType type : ShaderType.values()) {
            try {
                ShaderProgram shader = type.get();
                boolean valid = shader != null && shader.isValid();
                logTestResult("Shader " + type.name(), valid);
                if (valid) {
                    logger.logInfo(String.format("Shader %s: programId=%d", type.name(), shader.getProgram()));
                } else {
                    logger.logWarning("Shader " + type.name() + " is invalid or null");
                }
            } catch (Exception e) {
                logTestResult("Shader " + type.name(), false);
                logger.logError("Shader " + type.name() + " failed", e);
            }
        }
    }

    private void createTestShaders() {
        if (!ShaderProgram.isSupported()) {
            logger.logWarning("ShaderProgram not supported, skipping test shaders");
            return;
        }

        String meshNormalVert = String.join(
            "\n",
            "#version 330 core",
            "layout (location = 0) in vec3 aPos;",
            "layout (location = 1) in vec3 aNormal;",
            "layout (location = 2) in vec2 aTexCoord;",
            "",
            "out vec3 vNormal;",
            "out vec2 vTexCoord;",
            "",
            "uniform mat4 uModel;",
            "uniform mat4 uView;",
            "uniform mat4 uProjection;",
            "uniform bool uUseProjection;",
            "uniform bool uUseView;",
            "",
            "void main()",
            "{",
            "    vec4 worldPos = uModel * vec4(aPos, 1.0);",
            "    mat4 vp = uProjection;",
            "    if (uUseView) {",
            "        vp = vp * uView;",
            "    }",
            "    if (uUseProjection) {",
            "        gl_Position = vp * worldPos;",
            "    } else {",
            "        gl_Position = worldPos;",
            "    }",
            "    vNormal = mat3(uModel) * aNormal;",
            "    vTexCoord = aTexCoord;",
            "}");

        String meshNormalFrag = String.join(
            "\n",
            "#version 330 core",
            "in vec3 vNormal;",
            "in vec2 vTexCoord;",
            "out vec4 FragColor;",
            "",
            "uniform int uRenderMode;",
            "uniform vec4 uBaseColor;",
            "uniform float uAlpha;",
            "uniform bool uUseTexture;",
            "uniform sampler2D uMainTexture;",
            "uniform float uBlurScale;",
            "uniform float uMetallic;",
            "uniform float uRoughness;",
            "uniform float uAO;",
            "uniform vec4 uEmissive;",
            "",
            "void main()",
            "{",
            "    vec3 N = normalize(vNormal);",
            "    vec3 L = normalize(vec3(0.3, 0.8, 0.5));",
            "    float ndotl = max(dot(N, L), 0.0);",
            "    vec3 base = uBaseColor.rgb;",
            "    if (uUseTexture) {",
            "        base *= texture(uMainTexture, vTexCoord).rgb;",
            "    }",
            "    float modeScale = (uRenderMode == 0) ? 1.0 : 0.9;",
            "    float ao = clamp(uAO, 0.0, 1.0);",
            "    vec3 color = base * (0.2 + 0.8 * ndotl) * ao * modeScale;",
            "    color += uEmissive.rgb * 0.15;",
            "    color += vec3(uMetallic) * 0.05;",
            "    color *= 1.0 - clamp(uBlurScale, 0.0, 1.0) * 0.05;",
            "    color *= 1.0 - clamp(uRoughness, 0.0, 1.0) * 0.2;",
            "    FragColor = vec4(color, uAlpha * uBaseColor.a);",
            "}");

        meshNormalShader = ShaderProgram.createFromSource(meshNormalVert, null, meshNormalFrag);
        logTestResult("TestShader MeshNormal", meshNormalShader != null && meshNormalShader.isValid());

        String meshColorVert = String.join(
            "\n",
            "#version 330 core",
            "layout (location = 0) in vec3 aPos;",
            "layout (location = 1) in vec4 aColor;",
            "",
            "out vec4 vColor;",
            "",
            "uniform mat4 uModel;",
            "uniform mat4 uView;",
            "uniform mat4 uProjection;",
            "uniform bool uUseProjection;",
            "uniform bool uUseView;",
            "",
            "void main()",
            "{",
            "    vec4 worldPos = uModel * vec4(aPos, 1.0);",
            "    mat4 vp = uProjection;",
            "    if (uUseView) {",
            "        vp = vp * uView;",
            "    }",
            "    if (uUseProjection) {",
            "        gl_Position = vp * worldPos;",
            "    } else {",
            "        gl_Position = worldPos;",
            "    }",
            "    vColor = aColor;",
            "}");

        String meshColorFrag = String.join(
            "\n",
            "#version 330 core",
            "in vec4 vColor;",
            "out vec4 FragColor;",
            "",
            "uniform int uRenderMode;",
            "uniform vec4 uBaseColor;",
            "uniform float uAlpha;",
            "uniform bool uUseTexture;",
            "uniform float uBlurScale;",
            "uniform float uMetallic;",
            "uniform float uRoughness;",
            "uniform float uAO;",
            "uniform vec4 uEmissive;",
            "",
            "void main()",
            "{",
            "    vec3 base = vColor.rgb * uBaseColor.rgb;",
            "    if (uUseTexture) {",
            "        base *= 0.9;",
            "    }",
            "    float ao = clamp(uAO, 0.0, 1.0);",
            "    float modeScale = (uRenderMode == 0) ? 1.0 : 0.92;",
            "    vec3 color = base * ao * modeScale;",
            "    color += uEmissive.rgb * 0.1;",
            "    color += vec3(uMetallic) * 0.05;",
            "    color *= 1.0 - clamp(uBlurScale, 0.0, 1.0) * 0.05;",
            "    color *= 1.0 - clamp(uRoughness, 0.0, 1.0) * 0.2;",
            "    FragColor = vec4(color, uAlpha * vColor.a * uBaseColor.a);",
            "}");

        meshColorShader = ShaderProgram.createFromSource(meshColorVert, null, meshColorFrag);
        logTestResult("TestShader MeshColor", meshColorShader != null && meshColorShader.isValid());

        String instancedVert = String.join(
            "\n",
            "#version 330 core",
            "layout (location = 0) in vec3 aPos;",
            "layout (location = 3) in mat4 iModel;",
            "layout (location = 7) in vec4 iColor;",
            "",
            "out vec4 vColor;",
            "",
            "uniform mat4 uView;",
            "uniform mat4 uProjection;",
            "",
            "void main()",
            "{",
            "    gl_Position = uProjection * uView * iModel * vec4(aPos, 1.0);",
            "    vColor = iColor;",
            "}");

        String instancedFrag = String.join(
            "\n",
            "#version 330 core",
            "in vec4 vColor;",
            "out vec4 FragColor;",
            "",
            "void main()",
            "{",
            "    FragColor = vColor;",
            "}");

        instancedShader = ShaderProgram.createFromSource(instancedVert, null, instancedFrag);
        logTestResult("TestShader Instanced", instancedShader != null && instancedShader.isValid());
    }

    /**
     * Test material system
     */
    private void testMaterialSystem() {
        // Color material
        Material colorMat = Material.color(1.0f, 0.5f, 0.0f, 1.0f);
        logTestResult("Material.color()", colorMat != null);

        // Texture material (without actual texture)
        Material texMat = Material.texture(0);
        logTestResult("Material.texture()", texMat != null);

        // Blur materials
        Material blurH = Material.blurHorizontal(0, 1.0f);
        Material blurV = Material.blurVertical(0, 1.0f);
        logTestResult("Material.blur()", blurH != null && blurV != null);

        logger.logInfo("Material system tests complete");
    }

    private void testGraphicsResources() {
        try {
            testTexture = Texture2D.fromResource("minecraft", "textures/gui/widgets.png", 0);
            logTestResult("Texture2D.fromResource()", testTexture != null && testTexture.isValid());
        } catch (Exception e) {
            logTestResult("Texture2D.fromResource()", false);
            logger.logError("Texture2D resource test failed", e);
        }

        try {
            testFramebuffer = new Framebuffer(64, 64, true);
            logTestResult(
                "Framebuffer Creation",
                testFramebuffer != null && testFramebuffer.getFramebufferId() != 0
                    && testFramebuffer.getTextureId() != 0);
        } catch (Exception e) {
            logTestResult("Framebuffer Creation", false);
            logger.logError("Framebuffer creation failed", e);
        }

        // Create PiP framebuffer for Camera test
        try {
            pipFramebuffer = new Framebuffer(PIP_SIZE, PIP_SIZE, true);
            logTestResult("PiP Framebuffer", pipFramebuffer != null && pipFramebuffer.getFramebufferId() != 0);
        } catch (Exception e) {
            logTestResult("PiP Framebuffer", false);
            logger.logError("PiP Framebuffer creation failed", e);
        }

        // Create PiP mesh and material for Camera test (uses framework, not fixed pipeline)
        try {
            pipCubeMesh = MCModelLoader.createCube(1.0f);
            pipMaterial = Material.color(0.8f, 0.4f, 0.2f, 1.0f); // Orange color
            pipRenderContext = new RenderContext();
            logTestResult("PiP Mesh/Material", pipCubeMesh != null && pipCubeMesh.isValid());
        } catch (Exception e) {
            logTestResult("PiP Mesh/Material", false);
            logger.logError("PiP Mesh/Material creation failed", e);
        }
    }

    /**
     * Test AABB system
     */
    private void testAABBSystem() {
        AABB box1 = new AABB(-1, -1, -1, 1, 1, 1);
        logTestResult("AABB Creation", box1.isValid());

        AABB box2 = AABB.fromCenterExtent(0, 0, 0, 0.5f);
        logTestResult("AABB.fromCenterExtent()", box2.isValid());

        boolean contains = box1.contains(0, 0, 0);
        logTestResult("AABB.contains()", contains);

        boolean intersects = box1.intersects(box2);
        logTestResult("AABB.intersects()", intersects);

        float radius = box1.getBoundingSphereRadius();
        logTestResult("AABB.getBoundingSphereRadius()", radius > 0);

        logger.logInfo(
            String.format(
                "AABB test: center=(%.1f,%.1f,%.1f), size=(%.1f,%.1f,%.1f), radius=%.2f",
                box1.getCenterX(),
                box1.getCenterY(),
                box1.getCenterZ(),
                box1.getSizeX(),
                box1.getSizeY(),
                box1.getSizeZ(),
                radius));
    }

    /**
     * Log test result and update counters
     */
    private void logTestResult(String testName, boolean passed) {
        totalTests++;
        if (passed) {
            passedTests++;
        } else {
            failedTests++;
        }
        logger.logTestResult(testName, passed, passed ? "OK" : "FAILED");
    }

    // ==================== Custom Mesh Generation ====================

    /**
     * Create a custom pyramid mesh (4-sided)
     */
    private Mesh createPyramidMesh() {
        float h = 1.0f; // height
        float b = 0.7f; // base half-size

        // Vertices: position (3) + normal (3) + uv (2)
        float[] vertices = {
            // Base (Y-)
            -b, 0, -b, 0, -1, 0, 0, 0, b, 0, -b, 0, -1, 0, 1, 0, b, 0, b, 0, -1, 0, 1, 1, -b, 0, b, 0, -1, 0, 0, 1,
            // Front face (Z+)
            -b, 0, b, 0, 0.7f, 0.7f, 0, 0, b, 0, b, 0, 0.7f, 0.7f, 1, 0, 0, h, 0, 0, 0.7f, 0.7f, 0.5f, 1,
            // Right face (X+)
            b, 0, b, 0.7f, 0.7f, 0, 0, 0, b, 0, -b, 0.7f, 0.7f, 0, 1, 0, 0, h, 0, 0.7f, 0.7f, 0, 0.5f, 1,
            // Back face (Z-)
            b, 0, -b, 0, 0.7f, -0.7f, 0, 0, -b, 0, -b, 0, 0.7f, -0.7f, 1, 0, 0, h, 0, 0, 0.7f, -0.7f, 0.5f, 1,
            // Left face (X-)
            -b, 0, -b, -0.7f, 0.7f, 0, 0, 0, -b, 0, b, -0.7f, 0.7f, 0, 1, 0, 0, h, 0, -0.7f, 0.7f, 0, 0.5f, 1, };

        int[] indices = {
            // Base
            0, 1, 2, 0, 2, 3,
            // Sides
            4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, };

        return new StaticMesh(
            vertices,
            indices,
            VertexFormat.POSITION_3D_NORMAL_TEX.getStride(),
            VertexFormat.POSITION_3D_NORMAL_TEX.getAttributes());
    }

    /**
     * Create a custom torus (donut) mesh
     */
    private Mesh createTorusMesh(float majorRadius, float minorRadius, int majorSegments, int minorSegments) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i <= majorSegments; i++) {
            float u = (float) i / majorSegments;
            float theta = u * (float) Math.PI * 2;
            float cosTheta = (float) Math.cos(theta);
            float sinTheta = (float) Math.sin(theta);

            for (int j = 0; j <= minorSegments; j++) {
                float v = (float) j / minorSegments;
                float phi = v * (float) Math.PI * 2;
                float cosPhi = (float) Math.cos(phi);
                float sinPhi = (float) Math.sin(phi);

                // Position
                float x = (majorRadius + minorRadius * cosPhi) * cosTheta;
                float y = minorRadius * sinPhi;
                float z = (majorRadius + minorRadius * cosPhi) * sinTheta;

                // Normal
                float nx = cosPhi * cosTheta;
                float ny = sinPhi;
                float nz = cosPhi * sinTheta;

                vertices.add(x);
                vertices.add(y);
                vertices.add(z);
                vertices.add(nx);
                vertices.add(ny);
                vertices.add(nz);
                vertices.add(u);
                vertices.add(v);
            }
        }

        // Generate indices
        for (int i = 0; i < majorSegments; i++) {
            for (int j = 0; j < minorSegments; j++) {
                int curr = i * (minorSegments + 1) + j;
                int next = curr + minorSegments + 1;

                indices.add(curr);
                indices.add(next);
                indices.add(curr + 1);

                indices.add(curr + 1);
                indices.add(next);
                indices.add(next + 1);
            }
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        return new StaticMesh(
            vertexArray,
            indexArray,
            VertexFormat.POSITION_3D_NORMAL_TEX.getStride(),
            VertexFormat.POSITION_3D_NORMAL_TEX.getAttributes());
    }

    // ==================== Entity Creation ====================

    /**
     * Create mesh test entities in a ring around spawn point
     */
    private void createMeshTestEntities() {
        World world = RenderSystem.getWorld();
        if (world == null) {
            logger.logWarning("ECS World is null, cannot create mesh entities");
            return;
        }

        Mesh[] meshes = { cubeMesh, sphereMesh, planeMesh, customPyramidMesh, customTorusMesh };
        String[] names = { "Cube", "Sphere", "Plane", "Pyramid", "Torus" };
        float[][] colors = { { 1.0f, 0.3f, 0.3f, 1.0f }, // Red
            { 0.3f, 1.0f, 0.3f, 1.0f }, // Green
            { 0.3f, 0.3f, 1.0f, 0.8f }, // Blue (semi-transparent)
            { 1.0f, 0.8f, 0.0f, 1.0f }, // Gold
            { 0.8f, 0.3f, 1.0f, 1.0f }, // Purple
        };

        // Bounds for each mesh type (cube/sphere = 1 unit, plane = 2 unit, pyramid/torus = 1 unit)
        float[][] bounds = { { -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f }, // Cube
            { -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f }, // Sphere
            { -1.0f, -0.01f, -1.0f, 1.0f, 0.01f, 1.0f }, // Plane
            { -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f }, // Pyramid
            { -0.6f, -0.2f, -0.6f, 0.6f, 0.2f, 0.6f }, // Torus
        };

        for (int i = 0; i < meshes.length; i++) {
            Entity entity = world.createEntity("MeshTest_" + names[i]);
            entity.addComponent(new TransformComponent());

            MeshRendererComponent renderer = entity.addComponent(new MeshRendererComponent());
            renderer.setMesh(meshes[i]);
            renderer.setLocalBounds(bounds[i][0], bounds[i][1], bounds[i][2], bounds[i][3], bounds[i][4], bounds[i][5]);
            Material material = Material.color(colors[i][0], colors[i][1], colors[i][2], colors[i][3]);
            renderer.setMaterial(material.setUseView(true));

            meshEntities.add(entity);
            logger.logEntityCreated(
                entity,
                String.format(
                    "MeshRenderer with %s mesh, color=(%.1f,%.1f,%.1f,%.1f)",
                    names[i],
                    colors[i][0],
                    colors[i][1],
                    colors[i][2],
                    colors[i][3]));
        }

        logger.logInfo("Created " + meshEntities.size() + " mesh test entities");
    }

    private void createModelTestEntity() {
        World world = RenderSystem.getWorld();
        if (world == null || mcBlockMesh == null || !mcBlockMesh.isValid()) {
            logger.logWarning("MC block model mesh invalid, skipping model entity");
            return;
        }

        mcBlockEntity = world.createEntity("MCBlockModelTest");
        mcBlockEntity.addComponent(new TransformComponent());

        MeshRendererComponent renderer = mcBlockEntity.addComponent(new MeshRendererComponent());
        renderer.setMesh(mcBlockMesh);
        renderer.setLocalBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f); // 1 unit cube
        renderer.setMaterial(
            Material.color(0.9f, 0.9f, 0.9f, 1.0f)
                .setUseView(true));

        logger.logEntityCreated(mcBlockEntity, "MC block model test mesh");
    }

    /**
     * Create line renderer test entity
     */
    private void createLineTestEntity() {
        World world = RenderSystem.getWorld();
        if (world == null) {
            logger.logWarning("ECS World is null, cannot create line entity");
            return;
        }

        lineEntity = world.createEntity("LineTest");
        lineEntity.addComponent(new TransformComponent());

        LineRendererComponent line = lineEntity.addComponent(new LineRendererComponent());
        line.setUseWorldSpace(true);
        line.setWidth(0.08f, 0.02f);
        line.setColorGradient(Gradient.rainbow());
        line.setAlpha(1.0f);
        line.setEmissive(1.0f);

        logger.logEntityCreated(lineEntity, "LineRenderer with rainbow gradient, width 0.08->0.02");
        logger.logInfo("[RenderTest] Created line test entity");
    }

    /**
     * Create particle system test entities
     */
    private void createParticleTestEntities() {
        if (!RenderSystem.isParticleSystemSupported()) {
            logger.logWarning("Particle system not supported, skipping particle entities");
            logger.logWarning("[RenderTest] Particle system not supported, skipping");
            return;
        }

        World world = RenderSystem.getWorld();
        if (world == null) {
            logger.logWarning("ECS World is null, cannot create particle entities");
            return;
        }

        // Create various particle effects
        ParticleSystem[] systems = { ParticlePresets.createFire(0.6f), ParticlePresets.createSmoke(0.5f),
            ParticlePresets.createElectric(), ParticlePresets.createPortal(), ParticlePresets.createHealing(), };
        String[] names = { "Fire", "Smoke", "Electric", "Portal", "Healing" };

        for (int i = 0; i < systems.length; i++) {
            Entity entity = world.createEntity("ParticleTest_" + names[i]);
            entity.addComponent(new TransformComponent());
            entity.addComponent(new ParticleSystemComponent(systems[i], true));
            particleEntities.add(entity);

            logger.logEntityCreated(entity, "ParticleSystem " + names[i]);
            logger.logParticleSystemCreated(names[i], systems[i]);
        }

        logger.logInfo("Created " + particleEntities.size() + " particle test entities");
    }

    /**
     * Create parent-child hierarchy test entities
     */
    private void createHierarchyTestEntities() {
        World world = RenderSystem.getWorld();
        if (world == null) {
            logger.logWarning("ECS World is null, cannot create hierarchy entities");
            return;
        }

        // Parent entity with cube
        parentEntity = world.createEntity("HierarchyParent");
        TransformComponent parentTransform = parentEntity.addComponent(new TransformComponent());
        parentTransform.setScale(0.5f, 0.5f, 0.5f);

        MeshRendererComponent parentRenderer = parentEntity.addComponent(new MeshRendererComponent());
        parentRenderer.setMesh(cubeMesh);
        parentRenderer.setLocalBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        parentRenderer.setMaterial(
            Material.color(1.0f, 1.0f, 1.0f, 1.0f)
                .setUseView(true));

        logger.logEntityCreated(parentEntity, "Hierarchy parent with cube mesh, white color");

        // Child entities orbiting the parent
        for (int i = 0; i < 4; i++) {
            Entity child = world.createEntity("HierarchyChild_" + i);
            TransformComponent childTransform = child.addComponent(new TransformComponent());
            childTransform.setScale(0.3f, 0.3f, 0.3f);

            MeshRendererComponent childRenderer = child.addComponent(new MeshRendererComponent());
            childRenderer.setMesh(sphereMesh);
            childRenderer.setLocalBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);

            float hue = (float) i / 4;
            float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
            childRenderer.setMaterial(
                Material.color(rgb[0], rgb[1], rgb[2], 1.0f)
                    .setUseView(true));

            childEntities.add(child);
            logger.logEntityCreated(child, String.format("Hierarchy child #%d with sphere mesh, hue=%.2f", i, hue));
        }

        logger.logInfo("Created hierarchy test: 1 parent + " + childEntities.size() + " children");
        TakoTechMod.LOG
            .info("[RenderTest] Created hierarchy test entities (1 parent + {} children)", childEntities.size());
    }

    /**
     * Create gradient line test entities to test different Gradient presets
     */
    private void createGradientLineEntities() {
        World world = RenderSystem.getWorld();
        if (world == null) return;

        Gradient[] gradients = { Gradient.fire(), Gradient.smoke(), Gradient.lightning(), Gradient.portal(),
            Gradient.healing() };
        String[] names = { "Fire", "Smoke", "Lightning", "Portal", "Healing" };

        for (int i = 0; i < gradients.length; i++) {
            Entity entity = world.createEntity("GradientLine_" + names[i]);
            entity.addComponent(new TransformComponent());

            LineRendererComponent line = entity.addComponent(new LineRendererComponent());
            line.setUseWorldSpace(true);
            line.setWidth(0.05f, 0.02f);
            line.setColorGradient(gradients[i]);

            gradientLineEntities.add(entity);
            logger.logEntityCreated(entity, "LineRenderer with " + names[i] + " gradient");
        }

        logTestResult("Gradient Lines", gradientLineEntities.size() == gradients.length);
        logger.logInfo("Created " + gradientLineEntities.size() + " gradient line entities");
    }

    /**
     * Create advanced particle test entities with forces, collision, mesh particles
     */
    private void createAdvancedParticleTestEntities() {
        if (!RenderSystem.isParticleSystemSupported()) {
            logger.logWarning("Particle system not supported, skipping advanced particle tests");
            return;
        }

        World world = RenderSystem.getWorld();
        if (world == null) return;

        // 1. Force test: Vortex + Turbulence
        ParticleSystem vortexSystem = new ParticleSystem(500);
        ParticleEmitter vortexEmitter = new ParticleEmitter().setRate(50)
            .setLifetime(2.0f, 3.0f)
            .setSize(0.08f, 0.12f)
            .setColor(0.3f, 0.8f, 1.0f, 1.0f)
            .setColorOverLifetime(ColorOverLifetime.fadeOut())
            .setSizeOverLifetime(SizeOverLifetime.shrink());
        vortexSystem.addEmitter(vortexEmitter);
        vortexSystem.addForce(ParticleForce.vortexY(0, 0, 0, 3.0f));
        vortexSystem.addForce(ParticleForce.turbulence(2.0f, 0.5f));
        vortexSystem.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        vortexEntity = world.createEntity("AdvParticle_Vortex");
        vortexEntity.addComponent(new TransformComponent());
        vortexEntity.addComponent(new ParticleSystemComponent(vortexSystem, true));
        advancedParticleEntities.add(vortexEntity);
        logger.logEntityCreated(vortexEntity, "Vortex + Turbulence force particle");

        // 2. Attractor test
        ParticleSystem attractorSystem = new ParticleSystem(300);
        ParticleEmitter attractorEmitter = new ParticleEmitter()
            .setShape(moe.takochan.takotech.client.renderer.graphics.particle.EmitterShape.SPHERE, 1.5f)
            .setRate(30)
            .setLifetime(3.0f, 5.0f)
            .setSpeed(0.5f)
            .setSize(0.1f, 0.15f)
            .setColor(1.0f, 0.5f, 0.0f, 1.0f);
        attractorSystem.addEmitter(attractorEmitter);
        attractorSystem.addForce(ParticleForce.attractor(0, 0, 0, 5.0f, 1.5f));
        attractorSystem.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        attractorEntity = world.createEntity("AdvParticle_Attractor");
        attractorEntity.addComponent(new TransformComponent());
        attractorEntity.addComponent(new ParticleSystemComponent(attractorSystem, true));
        advancedParticleEntities.add(attractorEntity);
        logger.logEntityCreated(attractorEntity, "Attractor force particle");

        // 3. Mesh particle test: Cube particles
        ParticleSystem cubeSystem = ParticlePresets.createCubeParticles(0x00AAFF);
        Entity cubeEntity = world.createEntity("AdvParticle_CubeMesh");
        cubeEntity.addComponent(new TransformComponent());
        cubeEntity.addComponent(new ParticleSystemComponent(cubeSystem, true));
        advancedParticleEntities.add(cubeEntity);
        meshParticleEntities.add(cubeEntity);
        logger.logEntityCreated(cubeEntity, "Mesh particle (Cube)");

        // 4. Mesh particle test: Gem (Octahedron)
        ParticleSystem gemSystem = ParticlePresets.createGemParticles();
        Entity gemEntity = world.createEntity("AdvParticle_GemMesh");
        gemEntity.addComponent(new TransformComponent());
        gemEntity.addComponent(new ParticleSystemComponent(gemSystem, true));
        advancedParticleEntities.add(gemEntity);
        meshParticleEntities.add(gemEntity);
        logger.logEntityCreated(gemEntity, "Mesh particle (Octahedron/Gem)");

        // 5. Debris explosion (Tetrahedron mesh)
        ParticleSystem debrisSystem = ParticlePresets.createDebrisExplosion(0.5f);
        debrisSystem.setLooping(true); // Loop for testing
        Entity debrisEntity = world.createEntity("AdvParticle_Debris");
        debrisEntity.addComponent(new TransformComponent());
        debrisEntity.addComponent(new ParticleSystemComponent(debrisSystem, true));
        advancedParticleEntities.add(debrisEntity);
        meshParticleEntities.add(debrisEntity);
        logger.logEntityCreated(debrisEntity, "Mesh particle (Tetrahedron/Debris)");

        logTestResult("Advanced Particles", advancedParticleEntities.size() >= 5);
        logger.logInfo("Created " + advancedParticleEntities.size() + " advanced particle entities");
    }

    /**
     * Create particle showcase with S-curve layout.
     * 10 particle effects along an S-shaped path with floating labels.
     */
    private void createParticleShowcase() {
        World world = RenderSystem.getWorld();
        if (world == null || !RenderSystem.isParticleSystemSupported()) {
            logger.logWarning("Particle showcase skipped - system not supported");
            return;
        }

        // S-curve layout: 10 stations arranged in S pattern
        // Starting 3 blocks in front of player, 4 blocks horizontal spacing, 3 blocks depth per row
        showcasePositions = new float[10][3];

        // Row 1: stations 0, 1 (left to right)
        // Row 2: stations 3, 2 (right to left)
        // Row 3: stations 4, 5 (left to right)
        // Row 4: stations 7, 6 (right to left)
        // Row 5: stations 8, 9 (left to right)
        float startZ = 3.0f; // Distance in front
        float spacingX = 4.0f; // Horizontal spacing
        float spacingZ = 3.0f; // Depth per row
        float baseY = 1.0f; // Height above ground

        // Calculate positions (relative to activation point, will be offset later)
        int[] rowOrder = { 0, 1, 3, 2, 4, 5, 7, 6, 8, 9 };
        for (int i = 0; i < 10; i++) {
            int station = rowOrder[i];
            int row = station / 2;
            int col = station % 2;
            // Alternate direction per row
            float xOffset = (row % 2 == 0) ? (col * spacingX - spacingX / 2) : ((1 - col) * spacingX - spacingX / 2);
            showcasePositions[station][0] = xOffset;
            showcasePositions[station][1] = baseY;
            showcasePositions[station][2] = startZ + row * spacingZ;
        }

        // Create 10 particle systems with different effects
        String[] names = { "Fire", "Smoke", "Sphere", "Cone", "Box", "Vortex", "Attractor", "Cube", "Gem", "Debris" };
        ParticleSystem[] systems = new ParticleSystem[10];

        // 1. Fire preset (large intensity)
        systems[0] = ParticlePresets.createFire(2.5f);
        systems[0].setLooping(true);

        // 2. Smoke preset (large density)
        systems[1] = ParticlePresets.createSmoke(2.5f);
        systems[1].setLooping(true);

        // 3. Sphere shape emitter (large radius)
        systems[2] = createShapeDemo(EmitterShape.SPHERE_SURFACE, 1.5f, 0, 0);

        // 4. Cone shape emitter (wide angle)
        systems[3] = createShapeDemo(EmitterShape.CONE, 0.5f, 1.2f, 1.5f);

        // 5. Box shape emitter (large box)
        systems[4] = createShapeDemo(EmitterShape.BOX, 1.5f, 1.5f, 1.5f);

        // 6. Vortex force
        systems[5] = createVortexDemo();

        // 7. Attractor force
        systems[6] = createAttractorDemo();

        // 8. Cube mesh particles (cyan color)
        systems[7] = ParticlePresets.createCubeParticles(0xFF00FFFF);
        systems[7].setLooping(true);

        // 9. Gem mesh particles
        systems[8] = ParticlePresets.createGemParticles();
        systems[8].setLooping(true);

        // 10. Debris mesh particles (large scale)
        systems[9] = ParticlePresets.createDebrisExplosion(1.5f);
        systems[9].setLooping(true);

        // Create entities for each system
        for (int i = 0; i < 10; i++) {
            Entity entity = world.createEntity("Showcase_" + names[i]);
            entity.addComponent(new TransformComponent());
            entity.addComponent(new ParticleSystemComponent(systems[i], true));
            showcaseParticles.add(entity);
            showcaseLabels.add((i + 1) + ". " + names[i]);
            logger.logEntityCreated(entity, "Showcase particle: " + names[i]);
        }

        logTestResult("Particle Showcase", showcaseParticles.size() == 10);
        logger.logInfo("Created " + showcaseParticles.size() + " particle showcase entities");
    }

    /**
     * Create a shape demo particle system (high volume)
     */
    private ParticleSystem createShapeDemo(EmitterShape shape, float p1, float p2, float p3) {
        ParticleSystem system = new ParticleSystem(2000);
        ParticleEmitter emitter = new ParticleEmitter().setRate(200)
            .setLifetime(2.0f, 3.5f)
            .setSize(0.2f, 0.4f)
            .setShape(shape, p1, p2, p3)
            .setSpeed(1.5f)
            .setEmitAlongNormal(true)
            .setColorOverLifetime(ColorOverLifetime.energy())
            .setSizeOverLifetime(SizeOverLifetime.shrink());
        system.addEmitter(emitter);
        system.addForce(ParticleForce.gravity(0, -1.5f, 0));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        system.setLooping(true);
        return system;
    }

    /**
     * Create vortex force demo (high volume)
     */
    private ParticleSystem createVortexDemo() {
        ParticleSystem system = new ParticleSystem(1500);
        ParticleEmitter emitter = new ParticleEmitter().setRate(150)
            .setLifetime(2.5f, 4.0f)
            .setSize(0.15f, 0.3f)
            .setShape(EmitterShape.RING, 1.5f, 1.2f, 0)
            .setSpeed(0.8f)
            .setColorOverLifetime(ColorOverLifetime.portal())
            .setSizeOverLifetime(SizeOverLifetime.pulse());
        system.addEmitter(emitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 4.0f));
        system.addForce(ParticleForce.gravity(0, 2.0f, 0)); // Upward
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        system.setLooping(true);
        return system;
    }

    /**
     * Create attractor force demo (high volume)
     */
    private ParticleSystem createAttractorDemo() {
        ParticleSystem system = new ParticleSystem(1500);
        ParticleEmitter emitter = new ParticleEmitter().setRate(150)
            .setLifetime(2.5f, 4.0f)
            .setSize(0.15f, 0.25f)
            .setShape(EmitterShape.SPHERE, 2.5f, 0, 0)
            .setSpeed(0.5f)
            .setColorOverLifetime(ColorOverLifetime.healing())
            .setSizeOverLifetime(SizeOverLifetime.converge());
        system.addEmitter(emitter);
        system.addForce(ParticleForce.attractor(0, 0.5f, 0, 6.0f, 3.0f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        system.setLooping(true);
        return system;
    }

    /**
     * Create bounce physics demo with 3 particle systems.
     * Demonstrates bounceChance and bounceSpread parameters.
     */
    private void createBounceDemo() {
        World world = RenderSystem.getWorld();
        if (world == null || !RenderSystem.isParticleSystemSupported()) {
            logger.logWarning("Bounce demo skipped - system not supported");
            return;
        }

        // Layout: 3 systems in a row, 4 blocks apart
        bounceTestPositions = new float[3][3];
        float startZ = 5.0f; // Distance in front
        float spacingX = 4.0f; // Horizontal spacing
        float baseY = 3.0f; // Height for emission (particles fall down)

        for (int i = 0; i < 3; i++) {
            bounceTestPositions[i][0] = (i - 1) * spacingX; // -4, 0, 4
            bounceTestPositions[i][1] = baseY;
            bounceTestPositions[i][2] = startZ;
        }

        // 1. Perfect bounce: 100% chance, 0° spread
        ParticleSystem system1 = createBouncingSystem(1.0f, 0.0f, 0xFF00FF00); // Green
        String label1 = "100% Bounce\n0° Spread";

        // 2. Moderate bounce: 50% chance, 15° spread
        ParticleSystem system2 = createBouncingSystem(0.5f, 15.0f, 0xFFFFFF00); // Yellow
        String label2 = "50% Bounce\n15° Spread";

        // 3. Chaotic bounce: 20% chance, 45° spread
        ParticleSystem system3 = createBouncingSystem(0.2f, 45.0f, 0xFFFF4400); // Orange
        String label3 = "20% Bounce\n45° Spread";

        ParticleSystem[] systems = { system1, system2, system3 };
        String[] labels = { label1, label2, label3 };
        String[] names = { "PerfectBounce", "ModerateBounce", "ChaoticBounce" };

        for (int i = 0; i < 3; i++) {
            Entity entity = world.createEntity("BounceTest_" + names[i]);
            entity.addComponent(new TransformComponent());
            entity.addComponent(new ParticleSystemComponent(systems[i], true));
            bounceTestEntities.add(entity);
            bounceTestLabels.add(labels[i]);
            logger.logEntityCreated(entity, "Bounce test: " + names[i]);
        }

        logTestResult("Bounce Demo", bounceTestEntities.size() == 3);
        logger.logInfo("Created " + bounceTestEntities.size() + " bounce demo entities");
    }

    /**
     * Create a bouncing particle system with specified bounce parameters.
     */
    private ParticleSystem createBouncingSystem(float bounceChance, float bounceSpread, int color) {
        ParticleSystem system = new ParticleSystem(300);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 粒子从小区域以小初速度发射，主要靠重力下落
        ParticleEmitter emitter = new ParticleEmitter().setRate(15) // 降低发射率，减少视觉混乱
            .setLifetime(5.0f, 8.0f) // 更长的生命周期，看多次弹跳
            .setSize(0.2f, 0.35f) // 稍大的粒子，更容易看清
            .setShape(EmitterShape.BOX, 0.5f, 0.1f, 0.5f) // 扁平盒子，粒子从上方落下
            .setSpeed(0.2f) // 很小的初速度
            .setVelocity(0, 0, 0) // 无方向速度，纯靠重力
            .setColor(r, g, b, 1.0f)
            .setCollision(
                CollisionMode.PLANE,
                CollisionResponse.BOUNCE_DAMPED,
                0.8f, // 较高弹性，弹跳更明显
                0.05f, // 低摩擦
                bounceChance,
                bounceSpread)
            .setCollisionPlane(0, 1, 0, 0); // 地面碰撞（会被动态更新）

        system.addEmitter(emitter);
        system.addForce(ParticleForce.gravity(0, -9.8f, 0)); // 真实重力
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);
        system.setLooping(true);
        return system;
    }

    /**
     * Create bloom test entities - large, bright objects for testing bloom effect.
     * Creates 3 objects:
     * - White cube (center) - pure white for maximum bloom
     * - Yellow sphere (left) - golden/warm glow
     * - Cyan box (right) - cool colored glow
     */
    private void createBloomTestEntities() {
        World world = RenderSystem.getWorld();
        if (world == null) {
            logger.logWarning("Bloom test skipped - ECS World is null");
            return;
        }

        logger.logInfo("Creating bloom test entities...");

        // 1. White cube (center) - max brightness with white glow
        Entity whiteCube = world.createEntity("BloomTest_WhiteCube");
        whiteCube.addComponent(new TransformComponent());
        Mesh cubeMesh = MCModelLoader.createCube(1.5f);
        MeshRendererComponent cubeRenderer = new MeshRendererComponent(cubeMesh);
        Material whiteMat = Material.color(1.0f, 1.0f, 1.0f, 1.0f);
        whiteMat.setUseView(true);
        cubeRenderer.setMaterial(whiteMat);
        whiteCube.addComponent(cubeRenderer);
        bloomTestEntities.add(whiteCube);

        // 2. Yellow/golden sphere (left) with golden glow
        Entity yellowSphere = world.createEntity("BloomTest_YellowSphere");
        yellowSphere.addComponent(new TransformComponent());
        Mesh sphereMesh = MCModelLoader.createSphere(1.0f, 24, 24);
        MeshRendererComponent sphereRenderer = new MeshRendererComponent(sphereMesh);
        Material yellowMat = Material.color(1.0f, 0.9f, 0.3f, 1.0f);
        yellowMat.setUseView(true);
        sphereRenderer.setMaterial(yellowMat);
        yellowSphere.addComponent(sphereRenderer);
        bloomTestEntities.add(yellowSphere);

        // 3. Cyan elongated box (right) with cyan glow
        Entity cyanBox = world.createEntity("BloomTest_CyanBox");
        cyanBox.addComponent(new TransformComponent());
        Mesh boxMesh = MCModelLoader.createBox(1.2f, 0.8f, 0.6f);
        MeshRendererComponent boxRenderer = new MeshRendererComponent(boxMesh);
        Material cyanMat = Material.color(0.3f, 1.0f, 1.0f, 1.0f);
        cyanMat.setUseView(true);
        boxRenderer.setMaterial(cyanMat);
        cyanBox.addComponent(boxRenderer);
        bloomTestEntities.add(cyanBox);

        logger.logInfo("Created " + bloomTestEntities.size() + " bloom test entities");
    }

    /**
     * Create magic circle with hexagram pattern using ONLY particles.
     * - Two concentric circles (RING emitters)
     * - Two triangles forming hexagram (LINE emitters for each edge)
     * - All visible from any angle
     */
    private void createMagicCircle() {
        World world = RenderSystem.getWorld();
        if (world == null || !RenderSystem.isParticleSystemSupported()) return;

        float outerRadius = 3.0f;
        float innerRadius = 2.0f;
        float triangleRadius = 2.5f;

        // Calculate triangle vertices
        float[][] tri1Verts = new float[3][2]; // XZ coordinates
        float[][] tri2Verts = new float[3][2];
        for (int i = 0; i < 3; i++) {
            float angle1 = (float) (Math.PI / 2 + i * Math.PI * 2 / 3);
            tri1Verts[i][0] = (float) Math.cos(angle1) * triangleRadius;
            tri1Verts[i][1] = (float) Math.sin(angle1) * triangleRadius;
            float angle2 = (float) (-Math.PI / 2 + i * Math.PI * 2 / 3);
            tri2Verts[i][0] = (float) Math.cos(angle2) * triangleRadius;
            tri2Verts[i][1] = (float) Math.sin(angle2) * triangleRadius;
        }

        // === Outer circle (rainbow ring) ===
        ParticleSystem outerSystem = new ParticleSystem(3000);
        ParticleEmitter outerEmitter = new ParticleEmitter().setRate(500)
            .setLifetime(0.5f, 0.8f)
            .setSize(0.15f, 0.25f)
            .setShape(EmitterShape.RING, outerRadius, outerRadius - 0.05f, 0)
            .setSpeed(0.02f)
            .setColorOverLifetime(ColorOverLifetime.rainbow())
            .setSizeOverLifetime(SizeOverLifetime.shrink());
        outerSystem.addEmitter(outerEmitter);
        outerSystem.addForce(ParticleForce.gravity(0, 0.2f, 0));
        outerSystem.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        outerSystem.setLooping(true);

        magicCircleOuter = world.createEntity("MagicCircle_Outer");
        magicCircleOuter.addComponent(new TransformComponent());
        magicCircleOuter.addComponent(new ParticleSystemComponent(outerSystem, true));

        // === Inner circle (purple/cyan ring) ===
        ParticleSystem innerSystem = new ParticleSystem(2000);
        ParticleEmitter innerEmitter = new ParticleEmitter().setRate(400)
            .setLifetime(0.4f, 0.7f)
            .setSize(0.12f, 0.2f)
            .setShape(EmitterShape.RING, innerRadius, innerRadius - 0.05f, 0)
            .setSpeed(0.02f)
            .setColorOverLifetime(ColorOverLifetime.portal())
            .setSizeOverLifetime(SizeOverLifetime.shrink());
        innerSystem.addEmitter(innerEmitter);
        innerSystem.addForce(ParticleForce.gravity(0, 0.15f, 0));
        innerSystem.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        innerSystem.setLooping(true);

        magicCircleInner = world.createEntity("MagicCircle_Inner");
        magicCircleInner.addComponent(new TransformComponent());
        magicCircleInner.addComponent(new ParticleSystemComponent(innerSystem, true));

        // === Triangle 1 (cyan lines) - 3 LINE emitters ===
        ParticleSystem tri1System = new ParticleSystem(1500);
        for (int i = 0; i < 3; i++) {
            int next = (i + 1) % 3;
            ParticleEmitter lineEmitter = new ParticleEmitter().setRate(100)
                .setLifetime(0.4f, 0.6f)
                .setSize(0.1f, 0.18f)
                .setShape(EmitterShape.LINE, tri1Verts[i][0], tri1Verts[i][1], tri1Verts[next][0])
                .setSpeed(0.01f)
                .setColorOverLifetime(ColorOverLifetime.lightning())
                .setSizeOverLifetime(SizeOverLifetime.shrink());
            tri1System.addEmitter(lineEmitter);
        }
        tri1System.addForce(ParticleForce.gravity(0, 0.1f, 0));
        tri1System.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        tri1System.setLooping(true);

        magicTriangle1 = world.createEntity("MagicCircle_Triangle1");
        magicTriangle1.addComponent(new TransformComponent());
        magicTriangle1.addComponent(new ParticleSystemComponent(tri1System, true));

        // === Triangle 2 (yellow/orange lines) - 3 LINE emitters ===
        ParticleSystem tri2System = new ParticleSystem(1500);
        for (int i = 0; i < 3; i++) {
            int next = (i + 1) % 3;
            ParticleEmitter lineEmitter = new ParticleEmitter().setRate(100)
                .setLifetime(0.4f, 0.6f)
                .setSize(0.1f, 0.18f)
                .setShape(EmitterShape.LINE, tri2Verts[i][0], tri2Verts[i][1], tri2Verts[next][0])
                .setSpeed(0.01f)
                .setColorOverLifetime(ColorOverLifetime.gold())
                .setSizeOverLifetime(SizeOverLifetime.shrink());
            tri2System.addEmitter(lineEmitter);
        }
        tri2System.addForce(ParticleForce.gravity(0, 0.1f, 0));
        tri2System.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        tri2System.setLooping(true);

        magicTriangle2 = world.createEntity("MagicCircle_Triangle2");
        magicTriangle2.addComponent(new TransformComponent());
        magicTriangle2.addComponent(new ParticleSystemComponent(tri2System, true));

        // === Center vortex (spiraling upward particles) ===
        ParticleSystem centerSystem = new ParticleSystem(2000);
        ParticleEmitter centerEmitter = new ParticleEmitter().setRate(300)
            .setLifetime(1.5f, 2.5f)
            .setSize(0.08f, 0.2f)
            .setShape(EmitterShape.RING, 0.5f, 0.1f, 0)
            .setSpeed(0.5f)
            .setColorOverLifetime(ColorOverLifetime.rainbow())
            .setSizeOverLifetime(SizeOverLifetime.pulse());
        centerSystem.addEmitter(centerEmitter);
        centerSystem.addForce(ParticleForce.vortexY(0, 0, 0, 3.0f));
        centerSystem.addForce(ParticleForce.gravity(0, 1.5f, 0));
        centerSystem.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        centerSystem.setLooping(true);

        magicCircleParticles = world.createEntity("MagicCircle_Center");
        magicCircleParticles.addComponent(new TransformComponent());
        magicCircleParticles.addComponent(new ParticleSystemComponent(centerSystem, true));

        // Reset build animation (not used for particle version, but keep for compatibility)
        magicCircleBuildProgress = 1.0f;
        magicCircleBuilding = false;

        logger.logInfo("Created particle-only magic circle with hexagram pattern");
    }

    /**
     * Update magic circle particle entities with position and rotation.
     * All entities are positioned at the magic circle center.
     * Rotation is applied via TransformComponent.
     */
    private void updateMagicCircleLines() {
        float cx = magicCircleCenterX;
        float cy = magicCircleCenterY;
        float cz = magicCircleCenterZ;

        // Update all magic circle entities at the same position
        Entity[] entities = { magicCircleOuter, magicCircleInner, magicTriangle1, magicTriangle2,
            magicCircleParticles };
        float[] rotations = { magicCircleRotation, -magicCircleRotation * 1.5f, magicCircleRotation * 0.5f,
            -magicCircleRotation * 0.5f, 0 };

        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            if (entity != null) {
                TransformComponent t = entity.getComponent(TransformComponent.class);
                if (t != null) {
                    t.setPosition(cx, cy, cz);
                    t.setRotationEulerDegrees(0, rotations[i], 0); // Rotate around Y axis
                }
            }
        }
    }

    /**
     * Create dynamic mesh test entity
     */
    private void createDynamicMeshEntity() {
        World world = RenderSystem.getWorld();
        if (world == null || dynamicMesh == null) return;

        dynamicMeshEntity = world.createEntity("DynamicMeshTest");
        dynamicMeshEntity.addComponent(new TransformComponent());

        MeshRendererComponent renderer = dynamicMeshEntity.addComponent(new MeshRendererComponent());
        renderer.setMesh(dynamicMesh);
        renderer.setLocalBounds(-5.0f, -1.0f, -5.0f, 5.0f, 1.0f, 5.0f); // 10x10 grid with wave
        renderer.setMaterial(
            Material.color(0.0f, 1.0f, 0.5f, 0.8f)
                .setUseView(true));

        logTestResult("DynamicMesh Entity", dynamicMeshEntity != null);
        logger.logEntityCreated(dynamicMeshEntity, "DynamicMesh with animated vertices");
    }

    private void createLodTestEntity() {
        World world = RenderSystem.getWorld();
        if (world == null || cubeMesh == null || sphereMesh == null || planeMesh == null) return;

        lodGroup = LODGroup.createSimple(cubeMesh, sphereMesh, planeMesh, 3.5f, 6.5f, 12.0f);
        lodEntity = world.createEntity("LODTest");
        lodEntity.addComponent(new TransformComponent());
        MeshRendererComponent renderer = lodEntity.addComponent(new MeshRendererComponent());
        renderer.setMesh(cubeMesh);
        renderer.setLocalBounds(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        renderer.setMaterial(
            Material.color(0.9f, 0.9f, 0.9f, 1.0f)
                .setUseView(true));

        logTestResult("LODGroup", lodGroup != null);
        logger.logEntityCreated(lodEntity, "LODGroup test entity");
    }

    private void createPbrTestEntities() {
        World world = RenderSystem.getWorld();
        if (world == null || sphereMesh == null) return;

        PBRMaterial[] materials = { PBRMaterial.gold(), PBRMaterial.silver(), PBRMaterial.copper(),
            PBRMaterial.rubber() };
        String[] names = { "Gold", "Silver", "Copper", "Rubber" };

        for (int i = 0; i < materials.length; i++) {
            Entity entity = world.createEntity("PBR_" + names[i]);
            entity.addComponent(new TransformComponent());
            pbrEntities.add(entity);
            pbrMaterials.add(materials[i]);
        }

        logTestResult("PBRMaterial Presets", pbrEntities.size() == materials.length);
        logger.logInfo("Created " + pbrEntities.size() + " PBR test entities");
    }

    // ==================== Event Handlers ====================

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!RenderSystem.isInitialized() || !RenderSystem.isShaderSupported()) return;
        if (!MCRenderHelper.isClientReady()) return;

        EntityPlayer player = MCRenderHelper.getPlayer();
        if (player == null) return;

        // If test is not active, skip rendering
        if (!testActive) return;

        // Initialize resources if needed
        if (!initialized) {
            initialize();
            if (!initialized) return;
        }

        // Log environment info once per session
        if (!loggedEnvironment) {
            logEnvironmentInfo(player);
            loggedEnvironment = true;
        }

        float partialTicks = event.partialTicks;

        // Calculate delta time
        long currentTime = System.nanoTime();
        deltaTime = lastTime == 0 ? 0.016f : (currentTime - lastTime) / 1_000_000_000f;
        lastTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);
        time += deltaTime;
        stageTimer += deltaTime;

        if (autoAdvanceEnabled && currentStage != TestStage.COMPLETED) {
            float duration = getStageDuration(currentStage);
            if (duration > 0.0f && stageTimer >= duration) {
                advanceStage(true);
            }
        }

        // FPS counter
        frameCount++;
        fpsTimer += deltaTime;
        if (fpsTimer >= 1.0f) {
            currentFPS = frameCount;
            frameCount = 0;
            fpsTimer = 0;

            // Log periodic frame stats
            World ecsWorld = RenderSystem.getWorld();
            int entityCount = ecsWorld != null ? ecsWorld.getEntityCount() : 0;
            int activeParticles = countActiveParticles();
            logger.logFrameInfo(time, deltaTime, currentFPS, entityCount, meshEntities.size(), activeParticles);
        }

        // Update render context
        RenderSystem.updateRenderContext(partialTicks);

        // Update world camera (for HUD display)
        RenderSystem.updateWorldCamera(partialTicks);

        // Update ECS world
        RenderSystem.updateWorld(deltaTime);

        // Render with GL state management
        try (GLStateManager glState = GLStateManager.save()) {
            RenderContext ctx = RenderSystem.getRenderContext();
            if (ctx == null) return;

            // Log camera info periodically
            double camX = MCRenderHelper.getCameraX();
            double camY = MCRenderHelper.getCameraY();
            double camZ = MCRenderHelper.getCameraZ();

            // Begin post-processing capture
            PostProcessor pp = RenderSystem.getPostProcessor();
            if (pp != null && pp.isEnabled()) {
                pp.beginCapture();
            }

            // Update entity positions
            updateEntities(player, partialTicks);

            // Render 3D content
            render3DContent(ctx, player, partialTicks);

            // End post-processing
            if (pp != null && pp.isEnabled()) {
                pp.endCapture();
                pp.process();
            }
        } catch (Exception e) {
            logger.logError("Exception during render", e);
        } finally {
            // 确保解绑 GlobalUniforms，避免影响 MC 渲染
            GlobalUniforms.INSTANCE.unbind();
        }
    }

    /**
     * 切换测试状态（实例方法）
     */
    private void doToggleTest() {
        testActive = !testActive;

        if (testActive) {
            // Start test session
            logger.startSession("RenderFrameworkTest");
            logger.logInfo("[RenderTest] Test ACTIVATED (press U to deactivate)");
            logger.logInfo("=== TEST SESSION STARTED ===");
            logger.logInfo("Trigger: U key pressed");
            loggedEnvironment = false; // Reset to log environment on activation
            stageTimer = 0.0f;
            entityControlTimer = 0.0f;
            entityControlVisible = true;
            stageFeedback.clear();
            lastFeedback = null;
            lastFeedbackStage = null;

            // 保存激活时玩家的位置和朝向，用于固定实体位置
            EntityPlayer player = mc.thePlayer;
            if (player != null) {
                basePlayerX = player.posX;
                basePlayerY = player.posY;
                basePlayerZ = player.posZ;
                baseYawRad = Math.toRadians(player.rotationYaw);
                logger.logInfo(
                    String.format(
                        "Base position: (%.1f, %.1f, %.1f), yaw: %.1f°",
                        basePlayerX,
                        basePlayerY,
                        basePlayerZ,
                        player.rotationYaw));
            }

            setStage(TestStage.ENVIRONMENT, true);
        } else {
            // End test session
            logger.logInfo("=== TEST SESSION ENDED ===");

            // Log initialization test summary
            logger.logInfo("--- Initialization Tests ---");
            logger.logTestSummary(totalTests, passedTests, failedTests);

            // Log stage feedback summary
            logger.logInfo("--- Stage Feedback Summary ---");
            int stagePass = 0, stageFail = 0, stageNote = 0, stageUnset = 0;
            for (TestStage stage : TestStage.values()) {
                if (stage == TestStage.COMPLETED) continue;
                StageFeedback fb = stageFeedback.get(stage);
                if (fb == null) {
                    stageUnset++;
                } else {
                    switch (fb) {
                        case PASS:
                            stagePass++;
                            break;
                        case FAIL:
                            stageFail++;
                            break;
                        case NOTE:
                            stageNote++;
                            break;
                    }
                }
                logger.logInfo(
                    String.format(
                        "  Stage %d %s: %s",
                        stage.getStageNumber(),
                        stage.getNameEn(),
                        fb != null ? fb.name() : "UNSET"));
            }
            int totalStages = TestStage.getTotalStages() - 1; // Exclude COMPLETED
            logger.logInfo(
                String.format(
                    "Stage Results: PASS=%d, FAIL=%d, NOTE=%d, UNSET=%d (Total=%d)",
                    stagePass,
                    stageFail,
                    stageNote,
                    stageUnset,
                    totalStages));

            logger.endSession();
            logger.logInfo("[RenderTest] Test DEACTIVATED. Log file: " + logger.getLogFilePath());

            // Reset initialized flag so tests run again next time
            initialized = false;
        }
    }

    private void doToggleAutoAdvance() {
        autoAdvanceEnabled = !autoAdvanceEnabled;
        logger.logInfo("Auto-advance " + (autoAdvanceEnabled ? "ENABLED" : "DISABLED"));
    }

    private void doToggleBloom() {
        logger.logInfo("doToggleBloom() called: currentStage=" + currentStage.name());
        if (currentStage == TestStage.BLOOM_TEST) {
            bloomTestEnabled = !bloomTestEnabled;
            logger.logInfo("Toggling bloomTestEnabled to: " + bloomTestEnabled);
            applyStageSettings(currentStage);
            logger.logInfo("Bloom " + (bloomTestEnabled ? "ON" : "OFF"));
        } else {
            logger.logInfo("Bloom toggle ignored - not in BLOOM_TEST stage");
        }
    }

    private void advanceStage(boolean forward) {
        if (!testActive) return;
        TestStage next = forward ? currentStage.next() : currentStage.previous();
        setStage(next, true);
    }

    private void setStage(TestStage stage, boolean resetTimer) {
        if (stage == null) return;
        currentStage = stage;
        if (resetTimer) {
            stageTimer = 0.0f;
        }
        applyStageSettings(stage);
        logger.logInfo(
            String.format("Stage %d/%d: %s", stage.getStageNumber(), TestStage.getTotalStages(), stage.getNameEn()));
        logger.logInfo("Stage Description: " + stage.getDescription());
        logger.logInfo("Expected Result: " + stage.getExpectedResult());
        logger.logInfo("Feedback: press [Y]=PASS [N]=FAIL [M]=NOTE");
    }

    private void applyStageSettings(TestStage stage) {
        PostProcessor pp = RenderSystem.getPostProcessor();
        if (pp != null) {
            // 启用 PostProcessor 用于 BLOOM_TEST 和 COMPREHENSIVE 阶段
            boolean bloomOn = (stage == TestStage.BLOOM_TEST && bloomTestEnabled) || stage == TestStage.COMPREHENSIVE;
            pp.setEnabled(bloomOn);

            // 记录 PostProcessor 状态变化
            logger.logInfo(
                String.format(
                    "PostProcessor: stage=%s, bloomTestEnabled=%s, bloomOn=%s",
                    stage.name(),
                    bloomTestEnabled,
                    bloomOn));

            if (bloomOn) {
                logger.logInfo("PostProcessor ENABLED for stage: " + stage.name());
            } else {
                logger.logInfo("PostProcessor DISABLED for stage: " + stage.name());
            }
        } else {
            logger.logError("PostProcessor is NULL!", null);
        }
    }

    private float getStageDuration(TestStage stage) {
        if (stage == null) return 0.0f;
        return switch (stage) {
            case ENVIRONMENT, SHADER_LOAD -> 3.0f;
            case BATCH_SPRITE, BATCH_WORLD3D, ECS_BASIC, TRANSFORM_BASIC, LINE_BASIC, LINE_LOOP -> 6.0f;
            case MESH_STATIC, MESH_CUSTOM, MESH_DYNAMIC, MODEL_MC, MATERIAL_COLOR, MATERIAL_PBR, TRANSFORM_HIERARCHY, ENTITY_CONTROL, CAMERA_BASIC, FRUSTUM_CULL, LOD_GROUP, INSTANCED_BATCH, PARTICLE_SHOWCASE, PARTICLE_BOUNCE, BLOOM_TEST -> 10.0f;
            case COMPREHENSIVE -> 14.0f;
            default -> 0.0f;
        };
    }

    private boolean isStageOrComprehensive(TestStage stage) {
        return currentStage == stage || currentStage == TestStage.COMPREHENSIVE;
    }

    private void recordStageFeedback(StageFeedback feedback) {
        if (!testActive || feedback == null || currentStage == TestStage.COMPLETED) return;
        stageFeedback.put(currentStage, feedback);
        lastFeedback = feedback;
        lastFeedbackStage = currentStage;
        logger.logInfo(String.format("Stage Feedback: %s -> %s", currentStage.getNameEn(), feedback.name()));
    }

    private enum StageFeedback {
        PASS,
        FAIL,
        NOTE
    }

    /**
     * Log comprehensive environment info
     */
    private void logEnvironmentInfo(EntityPlayer player) {
        logger.logGLEnvironment();
        logger.logRenderSystemState();
        logger.logShaderState();
        logger.logPlayerInfo(player);

        net.minecraft.world.World mcWorld = player.worldObj;
        if (mcWorld != null) {
            logger.logWorldInfo(mcWorld);
        }
    }

    /**
     * Count active particles across all particle systems
     */
    private int countActiveParticles() {
        int count = 0;
        for (Entity entity : particleEntities) {
            ParticleSystemComponent psc = entity.getComponent(ParticleSystemComponent.class);
            if (psc != null && psc.getParticleSystem() != null) {
                count += psc.getParticleSystem()
                    .getAliveParticleCount();
            }
        }
        for (Entity entity : advancedParticleEntities) {
            ParticleSystemComponent psc = entity.getComponent(ParticleSystemComponent.class);
            if (psc != null && psc.getParticleSystem() != null) {
                count += psc.getParticleSystem()
                    .getAliveParticleCount();
            }
        }
        for (Entity entity : showcaseParticles) {
            ParticleSystemComponent psc = entity.getComponent(ParticleSystemComponent.class);
            if (psc != null && psc.getParticleSystem() != null) {
                count += psc.getParticleSystem()
                    .getAliveParticleCount();
            }
        }
        for (Entity entity : bounceTestEntities) {
            ParticleSystemComponent psc = entity.getComponent(ParticleSystemComponent.class);
            if (psc != null && psc.getParticleSystem() != null) {
                count += psc.getParticleSystem()
                    .getAliveParticleCount();
            }
        }
        return count;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!RenderSystem.isInitialized() || !RenderSystem.isShaderSupported()) return;

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        // Always show activation hint
        renderActivationHint(width, height);

        // Only render test info if test is active and initialized
        if (!testActive || !initialized) return;

        try (GLStateManager glState = GLStateManager.save()) {
            render2DContent(width, height);
            renderTestInfo(width, height);
            renderPipQuad(width, height);
        }
    }

    /**
     * Render activation hint
     */
    private void renderActivationHint(int screenWidth, int screenHeight) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);

        String hint;
        int color;
        if (testActive) {
            hint = "[RenderTest] ACTIVE - Press U to deactivate";
            color = 0x00FF00; // Green
        } else {
            hint = "[RenderTest] Press U to activate";
            color = 0xAAAAAA; // Gray
        }

        mc.fontRenderer.drawStringWithShadow(hint, 5, screenHeight - 12, color);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    // ==================== Entity Update ====================

    private void updateEntities(EntityPlayer player, float partialTicks) {
        // 使用测试激活时保存的基准位置和朝向，这样物体不会跟随玩家视角移动
        double px = basePlayerX;
        double py = basePlayerY;
        double pz = basePlayerZ;
        double yawRad = baseYawRad;

        // Update mesh entities (ring in front of base position)
        float meshRadius = 4.0f;
        float meshBaseAngle = (float) (yawRad + Math.PI / 2); // In front of base position
        float eyeY = (float) py + player.getEyeHeight(); // 使用眼睛高度而不是固定偏移
        for (int i = 0; i < meshEntities.size(); i++) {
            Entity entity = meshEntities.get(i);
            TransformComponent t = entity.getComponent(TransformComponent.class);
            if (t != null) {
                float angle = meshBaseAngle + (float) i * (float) Math.PI * 2 / meshEntities.size();
                float bobY = (float) Math.sin(time * 2 + i) * 0.2f;
                t.setPosition(
                    (float) px + (float) Math.cos(angle) * meshRadius,
                    eyeY + bobY,
                    (float) pz + (float) Math.sin(angle) * meshRadius);
                t.setRotationEulerDegrees(time * 30, time * 45 + i * 30, 0);
                t.setScale(0.4f, 0.4f, 0.4f);
            }
        }

        if (currentStage == TestStage.ENTITY_CONTROL) {
            entityControlTimer += deltaTime;
            if (entityControlTimer >= 2.0f) {
                entityControlVisible = !entityControlVisible;
                entityControlTimer = 0.0f;
            }
            for (Entity entity : meshEntities) {
                entity.setEnabled(entityControlVisible);
            }
        } else {
            for (Entity entity : meshEntities) {
                if (!entity.isEnabled()) {
                    entity.setEnabled(true);
                }
            }
        }

        // Update line entity (spiral above player)
        if (lineEntity != null) {
            TransformComponent t = lineEntity.getComponent(TransformComponent.class);
            LineRendererComponent line = lineEntity.getComponent(LineRendererComponent.class);
            if (t != null && line != null) {
                t.setPosition((float) px, (float) py, (float) pz);
                if (currentStage == TestStage.LINE_LOOP) {
                    line.setLoop(true);
                    updateCircleLine(line);
                } else {
                    line.setLoop(false);
                    updateSpiralLine(line);
                }
            }
        }

        // Update particle entities (arc behind player)
        float particleRadius = 3.5f;
        float particleBaseAngle = (float) (yawRad - Math.PI / 2); // Behind player
        for (int i = 0; i < particleEntities.size(); i++) {
            Entity entity = particleEntities.get(i);
            TransformComponent t = entity.getComponent(TransformComponent.class);
            if (t != null) {
                float angle = particleBaseAngle + (float) (i - particleEntities.size() / 2.0f) * 0.5f;
                t.setPosition(
                    (float) px + (float) Math.cos(angle) * particleRadius,
                    (float) py + 0.5f,
                    (float) pz + (float) Math.sin(angle) * particleRadius);
            }
        }

        // Update hierarchy entities (to the right of player)
        if (parentEntity != null) {
            TransformComponent parentT = parentEntity.getComponent(TransformComponent.class);
            if (parentT != null) {
                float sideAngle = (float) (yawRad + Math.PI); // Right of player
                parentT.setPosition(
                    (float) px + (float) Math.cos(sideAngle) * 3,
                    (float) py + 2.0f,
                    (float) pz + (float) Math.sin(sideAngle) * 3);
                parentT.setRotationEulerDegrees(0, time * 60, 0);
            }

            // Update children to orbit around parent
            for (int i = 0; i < childEntities.size(); i++) {
                Entity child = childEntities.get(i);
                TransformComponent childT = child.getComponent(TransformComponent.class);
                if (childT != null && parentT != null) {
                    float orbitAngle = time * 2 + i * (float) Math.PI * 2 / childEntities.size();
                    float orbitRadius = 1.0f;
                    float orbitY = (float) Math.sin(time * 3 + i) * 0.3f;

                    // Position relative to parent
                    childT.setPosition(
                        parentT.getPosition().x + (float) Math.cos(orbitAngle) * orbitRadius,
                        parentT.getPosition().y + orbitY,
                        parentT.getPosition().z + (float) Math.sin(orbitAngle) * orbitRadius);
                    childT.setRotationEulerDegrees(time * 90, time * 120, 0);
                }
            }
        }

        // Update gradient line entities (left side of player)
        float gradientLineRadius = 5.0f;
        float gradientBaseAngle = (float) yawRad; // Left of player
        for (int i = 0; i < gradientLineEntities.size(); i++) {
            Entity entity = gradientLineEntities.get(i);
            TransformComponent t = entity.getComponent(TransformComponent.class);
            LineRendererComponent line = entity.getComponent(LineRendererComponent.class);
            if (t != null && line != null) {
                float angle = gradientBaseAngle + (float) (i - gradientLineEntities.size() / 2.0f) * 0.4f;
                t.setPosition(
                    (float) px + (float) Math.cos(angle) * gradientLineRadius,
                    (float) py,
                    (float) pz + (float) Math.sin(angle) * gradientLineRadius);
                updateWaveLine(line, i);
            }
        }

        // Update advanced particle entities (above mesh entities)
        float advParticleRadius = 6.0f;
        for (int i = 0; i < advancedParticleEntities.size(); i++) {
            Entity entity = advancedParticleEntities.get(i);
            TransformComponent t = entity.getComponent(TransformComponent.class);
            if (t != null) {
                float angle = meshBaseAngle
                    + (float) i * (float) Math.PI * 2 / Math.max(advancedParticleEntities.size(), 1);
                t.setPosition(
                    (float) px + (float) Math.cos(angle) * advParticleRadius,
                    (float) py + 3.0f,
                    (float) pz + (float) Math.sin(angle) * advParticleRadius);
            }
        }

        // Update particle showcase entities (fixed at activation position, S-curve layout)
        if (showcasePositions != null) {
            // Calculate forward direction from base yaw
            float cosYaw = (float) Math.cos(baseYawRad + Math.PI / 2);
            float sinYaw = (float) Math.sin(baseYawRad + Math.PI / 2);
            // Right direction
            float cosYawRight = (float) Math.cos(baseYawRad + Math.PI);
            float sinYawRight = (float) Math.sin(baseYawRad + Math.PI);

            for (int i = 0; i < showcaseParticles.size() && i < showcasePositions.length; i++) {
                Entity entity = showcaseParticles.get(i);
                TransformComponent t = entity.getComponent(TransformComponent.class);
                if (t != null) {
                    float localX = showcasePositions[i][0];
                    float localY = showcasePositions[i][1];
                    float localZ = showcasePositions[i][2];
                    // Transform from local to world coordinates based on player's facing direction
                    float worldX = (float) basePlayerX + cosYaw * localZ + cosYawRight * localX;
                    float worldY = (float) basePlayerY + localY;
                    float worldZ = (float) basePlayerZ + sinYaw * localZ + sinYawRight * localX;
                    t.setPosition(worldX, worldY, worldZ);
                }
            }

            // Update bounce test positions and collision plane
            if (bounceTestPositions != null) {
                // Ground plane at player's feet (basePlayerY)
                float groundPlaneY = (float) basePlayerY;

                for (int i = 0; i < bounceTestEntities.size() && i < bounceTestPositions.length; i++) {
                    Entity entity = bounceTestEntities.get(i);
                    TransformComponent t = entity.getComponent(TransformComponent.class);
                    ParticleSystemComponent psc = entity.getComponent(ParticleSystemComponent.class);
                    if (t != null) {
                        float localX = bounceTestPositions[i][0];
                        float localY = bounceTestPositions[i][1];
                        float localZ = bounceTestPositions[i][2];
                        float worldX = (float) basePlayerX + cosYaw * localZ + cosYawRight * localX;
                        float worldY = (float) basePlayerY + localY;
                        float worldZ = (float) basePlayerZ + sinYaw * localZ + sinYawRight * localX;
                        t.setPosition(worldX, worldY, worldZ);
                    }
                    // Update collision plane to be at ground level
                    if (psc != null && psc.getParticleSystem() != null) {
                        for (ParticleEmitter emitter : psc.getParticleSystem()
                            .getEmitters()) {
                            emitter.setCollisionPlane(0, 1, 0, groundPlaneY);
                        }
                    }
                }
            }

            // Update bloom test entities (in front of player, spread out)
            if (!bloomTestEntities.isEmpty()) {
                float bloomDistance = 5.0f;
                float bloomSpread = 2.5f;
                float bloomEyeY = (float) py + 1.6f;

                // Calculate forward and right directions for bloom entities
                float bloomCosYaw = (float) Math.cos(baseYawRad + Math.PI / 2);
                float bloomSinYaw = (float) Math.sin(baseYawRad + Math.PI / 2);
                float bloomCosRight = (float) Math.cos(baseYawRad + Math.PI);
                float bloomSinRight = (float) Math.sin(baseYawRad + Math.PI);

                for (int i = 0; i < bloomTestEntities.size(); i++) {
                    Entity entity = bloomTestEntities.get(i);
                    TransformComponent t = entity.getComponent(TransformComponent.class);
                    if (t != null) {
                        // Position: center, left, right
                        float offsetX = (i == 1) ? -bloomSpread : ((i == 2) ? bloomSpread : 0);
                        float bloomWorldX = (float) px + bloomCosYaw * bloomDistance + bloomCosRight * offsetX;
                        float bloomWorldZ = (float) pz + bloomSinYaw * bloomDistance + bloomSinRight * offsetX;
                        t.setPosition(bloomWorldX, bloomEyeY, bloomWorldZ);
                        // Slow rotation for visual interest
                        t.setRotationEulerDegrees(time * 20, time * 30 + i * 45, 0);
                    }
                }
            }

            // Update magic circle position (at player's feet)
            magicCircleCenterX = (float) basePlayerX;
            magicCircleCenterY = (float) basePlayerY + 0.05f; // Just above ground at player's feet
            magicCircleCenterZ = (float) basePlayerZ;

            // Update rotation animation
            magicCircleRotation += deltaTime * 30.0f; // 30 degrees per second
            if (magicCircleRotation > 360) magicCircleRotation -= 360;

            // Update build progress
            if (magicCircleBuilding && magicCircleBuildProgress < 1.0f) {
                magicCircleBuildProgress += deltaTime / MAGIC_CIRCLE_BUILD_TIME;
                if (magicCircleBuildProgress >= 1.0f) {
                    magicCircleBuildProgress = 1.0f;
                    magicCircleBuilding = false;
                }
            }

            // Update line points based on build progress with rotation
            updateMagicCircleLines();

            // Update particle position
            if (magicCircleParticles != null) {
                TransformComponent t = magicCircleParticles.getComponent(TransformComponent.class);
                if (t != null) {
                    t.setPosition(magicCircleCenterX, magicCircleCenterY, magicCircleCenterZ);
                }
            }
        }

        // Update dynamic mesh entity and its vertices
        if (dynamicMeshEntity != null && dynamicMesh != null) {
            TransformComponent t = dynamicMeshEntity.getComponent(TransformComponent.class);
            if (t != null) {
                float dmAngle = (float) (yawRad + Math.PI * 0.75); // Front-right of player
                t.setPosition(
                    (float) px + (float) Math.cos(dmAngle) * 4.0f,
                    eyeY + 0.5f,
                    (float) pz + (float) Math.sin(dmAngle) * 4.0f);
                t.setRotationEulerDegrees(0, time * 30, 0);
                t.setScale(1.0f, 1.0f, 1.0f);
            }
            updateDynamicMeshVertices();
        }

        if (lodEntity != null) {
            TransformComponent t = lodEntity.getComponent(TransformComponent.class);
            if (t != null) {
                float lodAngle = (float) (yawRad + Math.PI * 0.7); // Slightly front-right
                t.setPosition(
                    (float) px + (float) Math.cos(lodAngle) * 6.0f,
                    (float) py + 1.5f,
                    (float) pz + (float) Math.sin(lodAngle) * 6.0f);
                t.setRotationEulerDegrees(0, time * 20, 0);
                t.setScale(0.6f, 0.6f, 0.6f);
            }
        }

        if (mcBlockEntity != null) {
            TransformComponent t = mcBlockEntity.getComponent(TransformComponent.class);
            if (t != null) {
                float blockAngle = (float) (yawRad - Math.PI * 0.3); // Behind-left of player
                t.setPosition(
                    (float) px + (float) Math.cos(blockAngle) * 4.5f,
                    (float) py + 1.2f,
                    (float) pz + (float) Math.sin(blockAngle) * 4.5f);
                t.setRotationEulerDegrees(0, time * 15, 0);
                t.setScale(0.8f, 0.8f, 0.8f);
            }
        }

        if (!pbrEntities.isEmpty()) {
            float pbrRadius = 2.8f;
            float baseAngle = (float) (yawRad + Math.PI / 2); // In front of player
            for (int i = 0; i < pbrEntities.size(); i++) {
                Entity entity = pbrEntities.get(i);
                TransformComponent t = entity.getComponent(TransformComponent.class);
                if (t != null) {
                    float angle = baseAngle + i * 0.6f;
                    t.setPosition(
                        (float) px + (float) Math.cos(angle) * pbrRadius,
                        eyeY,
                        (float) pz + (float) Math.sin(angle) * pbrRadius);
                    t.setScale(0.5f, 0.5f, 0.5f);
                }
            }
        }

        if (currentStage == TestStage.CAMERA_BASIC || currentStage == TestStage.COMPREHENSIVE) {
            float aspect = 1.0f; // PiP is square
            // Dynamic FOV: pulsing between 50 and 90 degrees
            float dynamicFov = 70.0f + (float) Math.sin(time * 2.0f) * 20.0f;
            if (debugCamera == null) {
                debugCamera = Camera.perspective(dynamicFov, aspect, 0.1f, 200.0f);
            } else {
                debugCamera.setPerspective(dynamicFov, aspect, 0.1f, 200.0f);
            }
            // Orbit around the base position
            float orbitRadius = 6.0f;
            float orbitAngle = time * 0.5f;
            debugCamera.lookAt(
                (float) (px + Math.cos(orbitAngle) * orbitRadius),
                (float) py + 2.5f,
                (float) (pz + Math.sin(orbitAngle) * orbitRadius),
                (float) px,
                (float) py + 1.2f,
                (float) pz);
        }
    }

    private void updateSpiralLine(LineRendererComponent line) {
        line.clearPoints();

        int segments = 64;
        float height = 4.0f;
        float radius = 1.5f;

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = t * (float) Math.PI * 6 + time * 2;
            float r = radius * (1 - t * 0.7f);
            float x = (float) Math.cos(angle) * r;
            float y = t * height;
            float z = (float) Math.sin(angle) * r;
            line.addPoint(x, y, z);
        }
    }

    private void updateCircleLine(LineRendererComponent line) {
        line.clearPoints();

        int segments = 64;
        float radius = 1.8f;
        float y = 0.8f + (float) Math.sin(time * 1.5f) * 0.2f;

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = t * (float) Math.PI * 2;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            line.addPoint(x, y, z);
        }
    }

    private void updateWaveLine(LineRendererComponent line, int index) {
        line.clearPoints();

        int segments = 32;
        float height = 2.0f;
        float waveFreq = 3.0f + index * 0.5f;
        float waveAmp = 0.3f;

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float x = (float) Math.sin(t * waveFreq * Math.PI + time * 2 + index) * waveAmp;
            float y = t * height;
            float z = (float) Math.cos(t * waveFreq * Math.PI + time * 2 + index) * waveAmp;
            line.addPoint(x, y, z);
        }
    }

    private void updateDynamicMeshVertices() {
        if (dynamicMesh == null) return;

        // Create animated wave surface
        int gridSize = 10;
        int vertexCount = gridSize * gridSize;
        int indexCount = (gridSize - 1) * (gridSize - 1) * 6;

        // Format: POSITION_3D_COLOR = x, y, z, r, g, b, a
        float[] vertices = new float[vertexCount * 7];
        int[] indices = new int[indexCount];

        int vi = 0;
        for (int z = 0; z < gridSize; z++) {
            for (int x = 0; x < gridSize; x++) {
                float fx = (x - gridSize / 2.0f) * 0.15f;
                float fz = (z - gridSize / 2.0f) * 0.15f;
                float fy = (float) Math.sin(fx * 4 + time * 3) * 0.1f + (float) Math.cos(fz * 4 + time * 2) * 0.1f;

                // Position
                vertices[vi++] = fx;
                vertices[vi++] = fy;
                vertices[vi++] = fz;

                // Color based on height
                float hue = (fy + 0.2f) * 2.5f;
                float[] rgb = hsvToRgb(hue % 1.0f, 0.8f, 1.0f);
                vertices[vi++] = rgb[0];
                vertices[vi++] = rgb[1];
                vertices[vi++] = rgb[2];
                vertices[vi++] = 0.9f; // Alpha
            }
        }

        // Generate indices for triangles
        int ii = 0;
        for (int z = 0; z < gridSize - 1; z++) {
            for (int x = 0; x < gridSize - 1; x++) {
                int topLeft = z * gridSize + x;
                int topRight = topLeft + 1;
                int bottomLeft = topLeft + gridSize;
                int bottomRight = bottomLeft + 1;

                indices[ii++] = topLeft;
                indices[ii++] = bottomLeft;
                indices[ii++] = topRight;

                indices[ii++] = topRight;
                indices[ii++] = bottomLeft;
                indices[ii++] = bottomRight;
            }
        }

        // 注意：第二个参数是浮点数数量，不是顶点数量
        dynamicMesh.updateData(vertices, vi, indices, ii);
    }

    // ==================== 3D Rendering ====================

    private void render3DContent(RenderContext ctx, EntityPlayer player, float partialTicks) {
        boolean renderWorldBatch = isStageOrComprehensive(TestStage.BATCH_WORLD3D);
        boolean renderLines = isStageOrComprehensive(TestStage.LINE_BASIC)
            || isStageOrComprehensive(TestStage.LINE_LOOP);
        boolean renderDynamic = isStageOrComprehensive(TestStage.MESH_DYNAMIC);
        boolean renderHierarchy = isStageOrComprehensive(TestStage.TRANSFORM_HIERARCHY);
        boolean renderPbr = isStageOrComprehensive(TestStage.MATERIAL_PBR);
        boolean renderInstanced = isStageOrComprehensive(TestStage.INSTANCED_BATCH);
        boolean renderLod = isStageOrComprehensive(TestStage.LOD_GROUP);
        boolean renderFrustum = currentStage == TestStage.FRUSTUM_CULL;
        boolean renderMeshes = currentStage == TestStage.TRANSFORM_BASIC || currentStage == TestStage.MESH_STATIC
            || currentStage == TestStage.MESH_CUSTOM
            || currentStage == TestStage.MODEL_MC
            || currentStage == TestStage.MATERIAL_COLOR
            || currentStage == TestStage.ENTITY_CONTROL
            || currentStage == TestStage.TRANSFORM_HIERARCHY
            || currentStage == TestStage.CAMERA_BASIC
            || currentStage == TestStage.BLOOM_TEST
            || renderFrustum
            || renderLod
            || currentStage == TestStage.COMPREHENSIVE;

        if (renderMeshes && meshNormalShader != null && meshNormalShader.isValid()) {
            // 设置 Mesh 渲染所需的 GL 状态
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_CULL_FACE); // 双面可见
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D); // shader 使用自己的纹理绑定

            meshNormalShader.use();
            ctx.setShader(meshNormalShader);
            // Note: Custom camera rendering not feasible in MC's render pipeline
            // CAMERA_BASIC stage just verifies Camera API works (shown in HUD)
            ctx.applyViewProjToShader();

            if (renderFrustum) {
                renderFrustumCulledMeshes(ctx);
            } else {
                switch (currentStage) {
                    case TRANSFORM_BASIC:
                        renderMeshRange(ctx, 0, 1);
                        break;
                    case MESH_STATIC:
                        renderMeshRange(ctx, 0, 3);
                        break;
                    case MESH_CUSTOM:
                        renderMeshRange(ctx, 0, meshEntities.size()); // 显示全部5个（3个静态+2个自定义）
                        break;
                    case MODEL_MC:
                        renderMeshEntity(ctx, mcBlockEntity);
                        break;
                    case MATERIAL_COLOR:
                    case ENTITY_CONTROL:
                        renderMeshRange(ctx, 0, meshEntities.size());
                        break;
                    case CAMERA_BASIC:
                        renderMeshRange(ctx, 0, Math.min(3, meshEntities.size()));
                        break;
                    case LOD_GROUP:
                        renderLodEntity(ctx);
                        break;
                    case BLOOM_TEST:
                        renderBloomTestEntities(ctx);
                        break;
                    case COMPREHENSIVE:
                        renderMeshRange(ctx, 0, meshEntities.size());
                        renderMeshEntity(ctx, mcBlockEntity);
                        renderLodEntity(ctx);
                        break;
                    default:
                        break;
                }
            }

            if (renderHierarchy) {
                renderMeshEntity(ctx, parentEntity);
                for (Entity child : childEntities) {
                    renderMeshEntity(ctx, child);
                }
            }

            ShaderProgram.unbind();
        }

        // PiP rendering for Camera test: render meshes from debugCamera to pipFramebuffer
        if (currentStage == TestStage.CAMERA_BASIC && debugCamera != null
            && pipFramebuffer != null
            && meshNormalShader != null
            && meshNormalShader.isValid()) {
            renderPipView(ctx);
        }

        if (renderDynamic && meshColorShader != null && meshColorShader.isValid()) {
            MeshRendererComponent dmRenderer = dynamicMeshEntity != null
                ? dynamicMeshEntity.getComponent(MeshRendererComponent.class)
                : null;
            if (dmRenderer != null && dmRenderer.shouldRender()) {
                // 设置 DynamicMesh 渲染所需的 GL 状态
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_TEXTURE_2D);

                meshColorShader.use();
                ctx.setShader(meshColorShader);
                ctx.applyViewProjToShader();
                dmRenderer.render(ctx);
                ShaderProgram.unbind();
            }
        }

        if (renderPbr) {
            renderPbrEntities(ctx);
        }

        if (renderLines) {
            if (lineEntity != null) {
                LineRendererComponent line = lineEntity.getComponent(LineRendererComponent.class);
                if (line != null && line.shouldRender()) {
                    line.render(ctx);
                }
            }
            for (Entity entity : gradientLineEntities) {
                LineRendererComponent line = entity.getComponent(LineRendererComponent.class);
                if (line != null && line.shouldRender()) {
                    line.render(ctx);
                }
            }
        }

        if (currentStage == TestStage.PARTICLE_SHOWCASE) {
            renderParticleEntities(showcaseParticles, partialTicks);
            renderShowcaseLabels(ctx);
            // Render magic circle
            renderMagicCircle(ctx, partialTicks);
        } else if (currentStage == TestStage.PARTICLE_BOUNCE) {
            renderParticleEntities(bounceTestEntities, partialTicks);
            renderBounceLabels(ctx);
        } else if (currentStage == TestStage.COMPREHENSIVE) {
            renderParticleEntities(particleEntities, partialTicks);
            renderParticleEntities(advancedParticleEntities, partialTicks);
        }

        if (renderInstanced) {
            renderInstancedBatch(ctx, player, partialTicks);
        }

        if (renderWorldBatch) {
            double camX = MCRenderHelper.getCameraX();
            double camY = MCRenderHelper.getCameraY();
            double camZ = MCRenderHelper.getCameraZ();
            renderWorld3DDecorations(camX, camY, camZ, player, partialTicks);
        }
    }

    private void renderWorld3DDecorations(double camX, double camY, double camZ, EntityPlayer player,
        float partialTicks) {
        if (worldBatch == null) return;

        double[] footPos = MCRenderHelper.getPlayerFootRenderPos(player, partialTicks);
        double footX = footPos[0];
        double footY = footPos[1];
        double footZ = footPos[2];

        // Ground circles
        worldBatch.begin(GL11.GL_LINES);

        // Animated magic circle at player's feet
        for (int ring = 0; ring < 3; ring++) {
            float pulse = (float) (Math.sin(time * 3 + ring) * 0.1 + 0.9);
            float[] color = hsvToRgb((time * 0.2f + ring * 0.15f) % 1.0f, 0.9f, 1.0f);
            double radius = (1.5 + ring * 0.5) * pulse;
            worldBatch.drawCircleXZ(
                footX,
                footY + 0.02 + ring * 0.01,
                footZ,
                radius,
                48,
                color[0],
                color[1],
                color[2],
                0.7f - ring * 0.15f);
        }

        // Coordinate axes
        worldBatch.drawLine(footX, footY + 0.05, footZ, footX + 1, footY + 0.05, footZ, 1, 0, 0, 1);
        worldBatch.drawLine(footX, footY + 0.05, footZ, footX, footY + 1.05, footZ, 0, 1, 0, 1);
        worldBatch.drawLine(footX, footY + 0.05, footZ, footX, footY + 0.05, footZ + 1, 0, 0, 1, 1);

        worldBatch.end();
    }

    private void renderMeshEntity(RenderContext ctx, Entity entity) {
        if (entity == null) return;
        MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class);
        if (renderer != null && renderer.shouldRender()) {
            renderer.render(ctx);
        }
    }

    private void renderMeshRange(RenderContext ctx, int start, int end) {
        int safeEnd = Math.min(end, meshEntities.size());
        for (int i = Math.max(start, 0); i < safeEnd; i++) {
            renderMeshEntity(ctx, meshEntities.get(i));
        }
    }

    private void renderBloomTestEntities(RenderContext ctx) {
        // 渲染主体（PostProcessor 会自动捕获并添加 Bloom）
        for (Entity entity : bloomTestEntities) {
            renderMeshEntity(ctx, entity);
        }
        // 物体级发光已禁用，改用屏幕级 PostProcessor Bloom
    }

    private void renderFrustumCulledMeshes(RenderContext ctx) {
        Camera camera = RenderSystem.getWorldCamera();
        frustumVisibleCount = 0;
        frustumCulledCount = 0;

        if (camera == null) {
            return;
        }

        moe.takochan.takotech.client.renderer.graphics.camera.Frustum frustum = camera.getFrustum();
        for (Entity entity : meshEntities) {
            MeshRendererComponent renderer = entity.getComponent(MeshRendererComponent.class);
            if (renderer == null || !renderer.shouldRender()) {
                continue;
            }
            AABB bounds = renderer.getWorldBounds();
            if (bounds != null && frustum.intersectsAABB(bounds)) {
                frustumVisibleCount++;
                renderer.render(ctx);
            } else {
                frustumCulledCount++;
            }
        }
    }

    private void renderLodEntity(RenderContext ctx) {
        if (lodEntity == null || lodGroup == null) return;

        MeshRendererComponent renderer = lodEntity.getComponent(MeshRendererComponent.class);
        TransformComponent transform = lodEntity.getComponent(TransformComponent.class);
        if (renderer == null || transform == null) return;

        float dx = transform.getX() - ctx.getCameraX();
        float dy = transform.getY() - ctx.getCameraY();
        float dz = transform.getZ() - ctx.getCameraZ();
        Mesh lodMesh = lodGroup.selectLOD(dx * dx + dy * dy + dz * dz);
        currentLodLevel = lodGroup.getCurrentLevel();

        if (lodMesh != null) {
            renderer.setMesh(lodMesh);
            renderer.render(ctx);
        }
    }

    private void renderPbrEntities(RenderContext ctx) {
        if (pbrEntities.isEmpty() || pbrMaterials.size() != pbrEntities.size()
            || sphereMesh == null
            || !sphereMesh.isValid()) {
            return;
        }

        // 使用安全获取方法，避免抛出异常
        if (!ShaderType.PBR.isLoaded()) {
            return;
        }
        ShaderProgram shader = ShaderType.PBR.getOrNull();
        if (shader == null || !shader.isValid()) return;

        // 设置 PBR 渲染所需的 GL 状态
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        shader.use();
        ctx.setShader(shader);

        // Update GlobalUniforms UBO with projection and view matrices
        // PBR shader reads these from UBO (layout(std140, binding = 0))
        GlobalUniforms.INSTANCE.setProjection(MathUtils.toFloatBuffer(ctx.getProjMatrix()));
        GlobalUniforms.INSTANCE.setView(MathUtils.toFloatBuffer(ctx.getViewMatrix()));
        GlobalUniforms.INSTANCE.bind();
        GlobalUniforms.INSTANCE.bindToShader(shader);

        ctx.applyViewProjToShader();
        setNormalMatrixIdentity(shader);

        // Set default values for missing vertex attributes (tangent at location 3, color at location 4)
        // The sphere mesh only provides position, normal, texCoord (locations 0, 1, 2)
        GL20.glVertexAttrib3f(3, 1.0f, 0.0f, 0.0f); // Default tangent along X-axis
        GL20.glVertexAttrib4f(4, 1.0f, 1.0f, 1.0f, 0.0f); // Default color white, alpha 0 (signals no vertex color)

        // In camera-relative coordinates, camera is at origin
        shader.setUniformVec3("uCameraPos", 0.0f, 0.0f, 0.0f);
        shader.setUniformInt("uLightCount", 1);
        // Light position relative to camera
        shader.setUniformVec3("uLightPositions[0]", 3.0f, 4.0f, 2.0f);
        shader.setUniformVec3("uLightColors[0]", 3.0f, 3.0f, 3.0f);

        for (int i = 0; i < pbrEntities.size(); i++) {
            Entity entity = pbrEntities.get(i);
            TransformComponent transform = entity.getComponent(TransformComponent.class);
            if (transform == null) continue;

            // Convert world matrix to camera-relative coordinates (same as MeshRendererComponent)
            Matrix4f worldMat = transform.getWorldMatrix();
            Matrix4f cameraRelativeMat = new Matrix4f();
            cameraRelativeMat.load(worldMat);
            cameraRelativeMat.m30 -= ctx.getCameraX();
            cameraRelativeMat.m31 -= ctx.getCameraY();
            cameraRelativeMat.m32 -= ctx.getCameraZ();
            ctx.setModelMatrix(cameraRelativeMat);

            ctx.applyModelToShader();
            pbrMaterials.get(i)
                .apply(shader);
            sphereMesh.draw();
        }

        ShaderProgram.unbind();
    }

    private void renderParticleEntities(List<Entity> entities, float partialTicks) {
        for (Entity entity : entities) {
            ParticleSystemComponent particles = entity.getComponent(ParticleSystemComponent.class);
            if (particles != null && particles.shouldRender()) {
                particles.renderWithMinecraftCamera(partialTicks);
            }
        }
    }

    /**
     * Render floating text labels above each particle showcase station
     */
    private void renderShowcaseLabels(RenderContext ctx) {
        if (showcasePositions == null || showcaseLabels.isEmpty()) return;

        // Get camera position for billboard rendering
        double camX = ctx.getCameraX();
        double camY = ctx.getCameraY();
        double camZ = ctx.getCameraZ();

        // Calculate forward direction from base yaw
        float cosYaw = (float) Math.cos(baseYawRad + Math.PI / 2);
        float sinYaw = (float) Math.sin(baseYawRad + Math.PI / 2);
        float cosYawRight = (float) Math.cos(baseYawRad + Math.PI);
        float sinYawRight = (float) Math.sin(baseYawRad + Math.PI);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        for (int i = 0; i < showcaseLabels.size() && i < showcasePositions.length; i++) {
            float localX = showcasePositions[i][0];
            float localY = showcasePositions[i][1] + 2.0f; // Above the particle
            float localZ = showcasePositions[i][2];

            // Transform to world coordinates
            float worldX = (float) basePlayerX + cosYaw * localZ + cosYawRight * localX;
            float worldY = (float) basePlayerY + localY;
            float worldZ = (float) basePlayerZ + sinYaw * localZ + sinYawRight * localX;

            // Convert to camera-relative for rendering
            float relX = worldX - (float) camX;
            float relY = worldY - (float) camY;
            float relZ = worldZ - (float) camZ;

            // Render 3D text using MC's font renderer
            GL11.glPushMatrix();
            GL11.glTranslatef(relX, relY, relZ);

            // Billboard: face camera
            float yaw = ctx.getCameraYaw();
            float pitch = ctx.getCameraPitch();
            GL11.glRotatef(-yaw, 0, 1, 0);
            GL11.glRotatef(pitch, 1, 0, 0);
            GL11.glScalef(-0.025f, -0.025f, 0.025f); // Scale down and flip

            String label = showcaseLabels.get(i);
            int width = mc.fontRenderer.getStringWidth(label);

            // Draw background
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0, 0, 0, 0.5f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(-width / 2.0f - 2, -2, 0);
            GL11.glVertex3f(width / 2.0f + 2, -2, 0);
            GL11.glVertex3f(width / 2.0f + 2, 10, 0);
            GL11.glVertex3f(-width / 2.0f - 2, 10, 0);
            GL11.glEnd();

            // Draw text
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.fontRenderer.drawString(label, -width / 2, 0, 0xFFFFFF);

            GL11.glPopMatrix();
        }

        GL11.glPopAttrib();
    }

    /**
     * Render floating text labels above each bounce test station
     */
    private void renderBounceLabels(RenderContext ctx) {
        if (bounceTestPositions == null || bounceTestLabels.isEmpty()) return;

        double camX = ctx.getCameraX();
        double camY = ctx.getCameraY();
        double camZ = ctx.getCameraZ();

        float cosYaw = (float) Math.cos(baseYawRad + Math.PI / 2);
        float sinYaw = (float) Math.sin(baseYawRad + Math.PI / 2);
        float cosYawRight = (float) Math.cos(baseYawRad + Math.PI);
        float sinYawRight = (float) Math.sin(baseYawRad + Math.PI);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        for (int i = 0; i < bounceTestLabels.size() && i < bounceTestPositions.length; i++) {
            float localX = bounceTestPositions[i][0];
            float localY = bounceTestPositions[i][1] + 1.5f; // Above the emitter
            float localZ = bounceTestPositions[i][2];

            float worldX = (float) basePlayerX + cosYaw * localZ + cosYawRight * localX;
            float worldY = (float) basePlayerY + localY;
            float worldZ = (float) basePlayerZ + sinYaw * localZ + sinYawRight * localX;

            float relX = worldX - (float) camX;
            float relY = worldY - (float) camY;
            float relZ = worldZ - (float) camZ;

            GL11.glPushMatrix();
            GL11.glTranslatef(relX, relY, relZ);

            float yaw = ctx.getCameraYaw();
            float pitch = ctx.getCameraPitch();
            GL11.glRotatef(-yaw, 0, 1, 0);
            GL11.glRotatef(pitch, 1, 0, 0);
            GL11.glScalef(-0.025f, -0.025f, 0.025f);

            String label = bounceTestLabels.get(i);
            String[] lines = label.split("\n");

            // Calculate max width for background
            int maxWidth = 0;
            for (String line : lines) {
                int w = mc.fontRenderer.getStringWidth(line);
                if (w > maxWidth) maxWidth = w;
            }
            int totalHeight = lines.length * 10;

            // Draw background
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0, 0, 0, 0.6f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(-maxWidth / 2.0f - 3, -3, 0);
            GL11.glVertex3f(maxWidth / 2.0f + 3, -3, 0);
            GL11.glVertex3f(maxWidth / 2.0f + 3, totalHeight + 2, 0);
            GL11.glVertex3f(-maxWidth / 2.0f - 3, totalHeight + 2, 0);
            GL11.glEnd();

            // Draw text lines
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            for (int j = 0; j < lines.length; j++) {
                int w = mc.fontRenderer.getStringWidth(lines[j]);
                mc.fontRenderer.drawString(lines[j], -w / 2, j * 10, 0xFFFFFF);
            }

            GL11.glPopMatrix();
        }

        GL11.glPopAttrib();
    }

    /**
     * Render the magic circle with hexagram pattern (all particles)
     */
    private void renderMagicCircle(RenderContext ctx, float partialTicks) {
        // Render all particle-based magic circle entities
        Entity[] particleEntities = { magicCircleOuter, magicCircleInner, magicTriangle1, magicTriangle2,
            magicCircleParticles };
        for (Entity entity : particleEntities) {
            if (entity != null) {
                ParticleSystemComponent particles = entity.getComponent(ParticleSystemComponent.class);
                if (particles != null && particles.shouldRender()) {
                    particles.renderWithMinecraftCamera(partialTicks);
                }
            }
        }
    }

    private void renderInstancedBatch(RenderContext ctx, EntityPlayer player, float partialTicks) {
        if (instancedBatch == null || instancedShader == null || instancedBaseMesh == null) {
            return;
        }
        if (!InstancedBatch.isSupported() || !instancedShader.isValid()) {
            return;
        }
        if (player == null) return;

        // 设置 InstancedBatch 渲染所需的 GL 状态
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        // Calculate camera-relative position from base (anchored) position
        double camX = ctx.getCameraX();
        double camY = ctx.getCameraY();
        double camZ = ctx.getCameraZ();

        // Base position offset (in front of activation position)
        float baseOffsetX = (float) Math.sin(baseYawRad) * 5.0f;
        float baseOffsetZ = (float) (-Math.cos(baseYawRad)) * 5.0f;

        instancedBatch.begin();
        int grid = 8;
        float spacing = 0.6f;
        Matrix4f model = new Matrix4f();

        for (int x = 0; x < grid; x++) {
            for (int z = 0; z < grid; z++) {
                float fx = (x - grid / 2.0f) * spacing;
                float fz = (z - grid / 2.0f) * spacing;
                model.setIdentity();
                // World position anchored to base, then convert to camera-relative
                float worldX = (float) basePlayerX + baseOffsetX + fx;
                float worldY = (float) basePlayerY + 2.0f;
                float worldZ = (float) basePlayerZ + baseOffsetZ + fz;
                model.m30 = worldX - (float) camX;
                model.m31 = worldY - (float) camY;
                model.m32 = worldZ - (float) camZ;
                float[] rgb = hsvToRgb((time * 0.1f + (x + z) * 0.03f) % 1.0f, 0.8f, 1.0f);
                instancedBatch.addInstance(model, rgb[0], rgb[1], rgb[2], 0.9f);
            }
        }

        instancedBatch.end();

        instancedShader.use();
        ctx.setShader(instancedShader);
        ctx.applyViewProjToShader();
        instancedBatch.render(instancedShader);
        ShaderProgram.unbind();
    }

    private void setNormalMatrixIdentity(ShaderProgram shader) {
        if (shader == null) return;
        Matrix3f normal = new Matrix3f();
        normal.setIdentity();
        normalMatrixBuffer.clear();
        normal.store(normalMatrixBuffer);
        normalMatrixBuffer.flip();
        shader.setUniformMatrix3("uNormalMatrix", false, normalMatrixBuffer);
    }

    /**
     * Render scene to PiP framebuffer using debugCamera.
     * Uses the rendering framework (Shader, Mesh, Material) instead of fixed pipeline.
     * Demonstrates Camera.perspective() and Camera.lookAt() affecting actual rendering.
     */
    private void renderPipView(RenderContext ctx) {
        if (debugCamera == null || pipCubeMesh == null || pipRenderContext == null) return;
        // Use meshNormalShader which supports position+normal+texcoord format and uBaseColor uniform
        if (meshNormalShader == null || !meshNormalShader.isValid()) return;

        ShaderProgram shader = meshNormalShader;

        // Save current viewport
        IntBuffer viewportBuf = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
        int vx = viewportBuf.get(0);
        int vy = viewportBuf.get(1);
        int vw = viewportBuf.get(2);
        int vh = viewportBuf.get(3);

        // Bind PiP framebuffer
        pipFramebuffer.bind();
        GL11.glClearColor(0.15f, 0.15f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // Set up GL state for 3D rendering
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK); // Standard back-face culling (CCW = front)

        // Sync pipRenderContext from debugCamera
        pipRenderContext.syncFromCamera(debugCamera);
        pipRenderContext.setShader(shader);

        // Use the shader
        shader.use();

        // Apply view and projection matrices from debugCamera
        pipRenderContext.applyViewProjToShader();

        // Set shader-specific uniforms
        shader.setUniformBool("uUseTexture", false);
        shader.setUniformFloat("uAO", 1.0f);
        shader.setUniformFloat("uMetallic", 0.0f);
        shader.setUniformFloat("uRoughness", 0.5f);
        shader.setUniformFloat("uBlurScale", 0.0f);
        shader.setUniformVec4("uEmissive", 0.0f, 0.0f, 0.0f, 0.0f);
        shader.setUniformInt("uRenderMode", 0);

        // Render the cube at target position using framework
        org.lwjgl.util.vector.Vector3f target = debugCamera.getTarget();

        // Create model matrix for the cube (at target position)
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.setIdentity();
        modelMatrix.translate(new org.lwjgl.util.vector.Vector3f(target.x, target.y, target.z));

        // For PiP, we don't need camera-relative coordinates since we use debugCamera's view matrix directly
        pipRenderContext.setModelMatrix(modelMatrix);
        pipRenderContext.applyModelToShader();

        // Set color directly (orange) - bypass Material.apply for debugging
        shader.setUniformVec4("uBaseColor", 0.9f, 0.5f, 0.2f, 1.0f);
        shader.setUniformFloat("uAlpha", 1.0f);

        // Draw the cube mesh
        pipCubeMesh.draw();

        // Left cube (blue)
        modelMatrix.setIdentity();
        modelMatrix.translate(new org.lwjgl.util.vector.Vector3f(target.x - 2.0f, target.y, target.z));
        modelMatrix.scale(new org.lwjgl.util.vector.Vector3f(0.6f, 0.6f, 0.6f));
        pipRenderContext.setModelMatrix(modelMatrix);
        pipRenderContext.applyModelToShader();
        shader.setUniformVec4("uBaseColor", 0.3f, 0.5f, 0.9f, 1.0f);
        pipCubeMesh.draw();

        // Right cube (green)
        modelMatrix.setIdentity();
        modelMatrix.translate(new org.lwjgl.util.vector.Vector3f(target.x + 2.0f, target.y, target.z));
        modelMatrix.scale(new org.lwjgl.util.vector.Vector3f(0.6f, 0.6f, 0.6f));
        pipRenderContext.setModelMatrix(modelMatrix);
        pipRenderContext.applyModelToShader();
        shader.setUniformVec4("uBaseColor", 0.3f, 0.9f, 0.3f, 1.0f);
        pipCubeMesh.draw();

        // Front cube (purple)
        modelMatrix.setIdentity();
        modelMatrix.translate(new org.lwjgl.util.vector.Vector3f(target.x, target.y, target.z + 2.0f));
        modelMatrix.scale(new org.lwjgl.util.vector.Vector3f(0.6f, 0.6f, 0.6f));
        pipRenderContext.setModelMatrix(modelMatrix);
        pipRenderContext.applyModelToShader();
        shader.setUniformVec4("uBaseColor", 0.7f, 0.3f, 0.9f, 1.0f);
        pipCubeMesh.draw();

        // Unbind shader
        ShaderProgram.unbind();

        pipFramebuffer.unbind();

        // Restore viewport
        GL11.glViewport(vx, vy, vw, vh);
    }

    // ==================== 2D Rendering ====================

    private void render2DContent(int screenWidth, int screenHeight) {
        if (currentStage != TestStage.BATCH_SPRITE && currentStage != TestStage.COMPREHENSIVE) {
            return;
        }
        SpriteBatch batch = RenderSystem.getSpriteBatch();
        if (batch == null) return;

        batch.setProjectionOrtho(screenWidth, screenHeight);
        batch.begin();

        // Animated color bar at top
        for (int i = 0; i < 7; i++) {
            float hue = (time * 0.1f + (float) i / 7) % 1.0f;
            float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
            float pulse = (float) (Math.sin(time * 4 + i) * 0.2 + 0.8);
            batch.drawRect(10 + i * 18, 10, 15, 15 * pulse, rgb[0], rgb[1], rgb[2], 0.9f);
        }

        // Pulsing indicator
        float pulse = (float) (Math.sin(time * 3) * 0.3 + 0.7);
        batch.drawRect(screenWidth - 30, 10, 20, 20, 0.2f, 1.0f, 0.4f, pulse);

        batch.end();
    }

    /**
     * Draw PiP quad on screen (bottom-right corner)
     * Uses Minecraft's ScaledResolution coordinate system
     */
    private void renderPipQuad(int screenWidth, int screenHeight) {
        if (currentStage != TestStage.CAMERA_BASIC || pipFramebuffer == null) {
            return;
        }

        int pipDisplaySize = 180;
        int margin = 10;
        int x = screenWidth - pipDisplaySize - margin;
        int y = screenHeight - pipDisplaySize - margin - 40;

        // Use Minecraft's standard Gui drawing methods for reliability
        // These handle GL state correctly in the GUI context

        // Draw purple background using Gui.drawRect
        net.minecraft.client.gui.Gui.drawRect(x, y, x + pipDisplaySize, y + pipDisplaySize, 0xFF331A4D);

        // Draw border (4 rectangles for outline)
        int borderColor = 0xFF4D99FF;
        net.minecraft.client.gui.Gui.drawRect(x - 2, y - 2, x + pipDisplaySize + 2, y, borderColor); // top
        net.minecraft.client.gui.Gui
            .drawRect(x - 2, y + pipDisplaySize, x + pipDisplaySize + 2, y + pipDisplaySize + 2, borderColor); // bottom
        net.minecraft.client.gui.Gui.drawRect(x - 2, y, x, y + pipDisplaySize, borderColor); // left
        net.minecraft.client.gui.Gui
            .drawRect(x + pipDisplaySize, y, x + pipDisplaySize + 2, y + pipDisplaySize, borderColor); // right

        // Draw FBO texture if available
        int texId = pipFramebuffer.getTextureId();
        if (texId > 0) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glColor4f(1, 1, 1, 1);

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setColorRGBA_F(1.0f, 1.0f, 1.0f, 1.0f);
            // Note: UV flipped for FBO texture (FBO renders upside-down)
            tess.addVertexWithUV(x, y + pipDisplaySize, 0, 0, 0);
            tess.addVertexWithUV(x + pipDisplaySize, y + pipDisplaySize, 0, 1, 0);
            tess.addVertexWithUV(x + pipDisplaySize, y, 0, 1, 1);
            tess.addVertexWithUV(x, y, 0, 0, 1);
            tess.draw();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        // Draw labels
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        mc.fontRenderer.drawStringWithShadow("PiP: DebugCamera", x, y - 12, 0x80C0FF);
        if (debugCamera != null) {
            String fovInfo = String.format("FOV: %.0f", debugCamera.getFov());
            mc.fontRenderer.drawStringWithShadow(fovInfo, x, y + pipDisplaySize + 4, 0xFFFF80);
        }
    }

    // ==================== Test Info HUD ====================

    private void renderTestInfo(int screenWidth, int screenHeight) {
        int textY = 30;
        int lineHeight = 10;
        int textColor = 0xFFFFFF;

        World world = RenderSystem.getWorld();
        int entityCount = world != null ? world.getEntityCount() : 0;
        int componentCount = countTotalComponents(world);

        PostProcessor pp = RenderSystem.getPostProcessor();
        boolean bloomEnabled = pp != null && pp.isEnabled();

        List<String> lines = new ArrayList<>();
        lines.add("=== Render Framework Test ===");
        lines.add(
            String.format(
                "Stage: %d/%d %s | Auto: %s",
                currentStage.getStageNumber(),
                TestStage.getTotalStages(),
                currentStage.getNameEn(),
                autoAdvanceEnabled ? "ON" : "OFF"));
        lines.add("Desc: " + currentStage.getDescription());
        lines.add("Expected: " + currentStage.getExpectedResult());
        StageFeedback feedback = stageFeedback.get(currentStage);
        lines.add("Result: " + (feedback != null ? feedback.name() : "UNSET"));
        lines.add("Confirm: [Y] Pass [N] Fail [M] Note");
        if (lastFeedback != null && lastFeedbackStage != null) {
            lines.add("Last: " + lastFeedbackStage.getNameEn() + " -> " + lastFeedback.name());
        }
        lines.add(String.format("FPS: %d | Time: %.1fs | DeltaTime: %.3fs", currentFPS, time, deltaTime));
        lines.add(
            String.format(
                "ECS: %d entities, %d components | Meshes: %d | Particles: %d | Lines: %d",
                entityCount,
                componentCount,
                meshEntities.size(),
                particleEntities.size(),
                lineEntity != null ? 1 : 0));
        lines.add(String.format("Hierarchy: 1 parent + %d children", childEntities.size()));
        lines.add(
            String.format(
                "Bloom: %s | GlobalUniforms: %s",
                bloomEnabled ? "ON" : "OFF",
                GlobalUniforms.INSTANCE.isAvailable() ? "OK" : "N/A"));

        if (currentStage == TestStage.FRUSTUM_CULL) {
            lines.add(String.format("Frustum: visible=%d culled=%d", frustumVisibleCount, frustumCulledCount));
        }
        if (currentStage == TestStage.LOD_GROUP) {
            lines.add(String.format("LOD Level: %d", currentLodLevel));
        }
        if (currentStage == TestStage.INSTANCED_BATCH) {
            lines.add(
                String.format(
                    "Instanced: %s",
                    instancedBatch != null ? instancedBatch.getInstanceCount() + "/" + instancedBatch.getMaxInstances()
                        : "N/A"));
        }
        if (currentStage == TestStage.BLOOM_TEST) {
            lines.add(">>> SCREEN-SPACE BLOOM: " + (bloomEnabled ? "ENABLED" : "DISABLED") + " <<<");
            lines.add("White Cube | Yellow Sphere | Cyan Box");
            lines.add("Press [B] to toggle | Threshold: 0.6 | Intensity: 1.2");
        }
        if (currentStage == TestStage.ENVIRONMENT) {
            boolean textureOk = testTexture != null && testTexture.isValid();
            boolean fboOk = testFramebuffer != null && testFramebuffer.getFramebufferId() != 0;
            boolean computeOk = ShaderProgram.isComputeShaderSupported() && ShaderProgram.isSSBOSupported();
            lines.add(
                String.format("Texture2D: %s | Framebuffer: %s", textureOk ? "OK" : "FAIL", fboOk ? "OK" : "FAIL"));
            lines.add(String.format("Compute/SSBO: %s", computeOk ? "OK" : "N/A"));
        }
        if (currentStage == TestStage.SHADER_LOAD) {
            StringBuilder sb = new StringBuilder("Shaders: ");
            for (ShaderType type : ShaderType.values()) {
                try {
                    ShaderProgram shader = type.getOrNull();
                    boolean valid = shader != null && shader.isValid();
                    sb.append(type.name())
                        .append("=")
                        .append(valid ? "OK" : "FAIL")
                        .append(" ");
                } catch (Exception e) {
                    sb.append(type.name())
                        .append("=ERR ");
                }
            }
            lines.add(sb.toString());
        }
        if (currentStage == TestStage.CAMERA_BASIC) {
            Camera worldCam = RenderSystem.getWorldCamera();
            if (worldCam != null) {
                org.lwjgl.util.vector.Vector3f pos = worldCam.getPosition();
                org.lwjgl.util.vector.Vector3f target = worldCam.getTarget();
                lines.add(String.format("WorldCamera pos: (%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z));
                lines.add(String.format("WorldCamera target: (%.1f, %.1f, %.1f)", target.x, target.y, target.z));
                lines.add(
                    String.format(
                        "FOV: %.1f | Near: %.2f | Far: %.1f",
                        worldCam.getFov(),
                        worldCam.getNearPlane(),
                        worldCam.getFarPlane()));
                lines.add("Camera API: perspective/lookAt/syncFromMC = OK");
            }
            if (debugCamera != null) {
                org.lwjgl.util.vector.Vector3f dPos = debugCamera.getPosition();
                org.lwjgl.util.vector.Vector3f dTarget = debugCamera.getTarget();
                lines.add(String.format("DebugCamera pos: (%.1f, %.1f, %.1f)", dPos.x, dPos.y, dPos.z));
                lines.add(String.format("DebugCamera target: (%.1f, %.1f, %.1f)", dTarget.x, dTarget.y, dTarget.z));
            }
        }

        lines.add("[U] Toggle [I] Next [O] Prev [P] Auto");

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);

        for (int i = 0; i < lines.size(); i++) {
            mc.fontRenderer.drawStringWithShadow(lines.get(i), 5, textY + i * lineHeight, textColor);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    // ==================== Utility ====================

    /**
     * 统计所有实体的组件总数
     */
    private int countTotalComponents(World world) {
        if (world == null) return 0;
        int count = 0;
        for (Entity entity : world.getAllEntities()) {
            count += entity.getComponentCount();
        }
        return count;
    }

    private float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int hi = (int) (h * 6) % 6;
        switch (hi) {
            case 0:
                r = c;
                g = x;
                b = 0;
                break;
            case 1:
                r = x;
                g = c;
                b = 0;
                break;
            case 2:
                r = 0;
                g = c;
                b = x;
                break;
            case 3:
                r = 0;
                g = x;
                b = c;
                break;
            case 4:
                r = x;
                g = 0;
                b = c;
                break;
            default:
                r = c;
                g = 0;
                b = x;
                break;
        }
        return new float[] { r + m, g + m, b + m };
    }

    // ==================== Lifecycle ====================

    /**
     * Get the log file path
     */
    public String getLogFilePath() {
        return logger.getLogFilePath();
    }

    public void cleanup() {
        logger.logInfo("[RenderTest] Cleaning up...");

        // End log session if active
        if (logger.isOpen()) {
            logger.logInfo("Cleanup triggered - ending session");
            logger.endSession();
        }

        World world = RenderSystem.getWorld();

        // Remove all entities
        if (world != null) {
            for (Entity entity : meshEntities) {
                world.removeEntity(entity);
            }
            if (lineEntity != null) {
                world.removeEntity(lineEntity);
            }
            if (mcBlockEntity != null) {
                world.removeEntity(mcBlockEntity);
            }
            for (Entity entity : gradientLineEntities) {
                world.removeEntity(entity);
            }
            for (Entity entity : particleEntities) {
                world.removeEntity(entity);
            }
            for (Entity entity : advancedParticleEntities) {
                world.removeEntity(entity);
            }
            for (Entity entity : showcaseParticles) {
                world.removeEntity(entity);
            }
            // Remove magic circle entities
            if (magicCircleOuter != null) world.removeEntity(magicCircleOuter);
            if (magicCircleInner != null) world.removeEntity(magicCircleInner);
            if (magicTriangle1 != null) world.removeEntity(magicTriangle1);
            if (magicTriangle2 != null) world.removeEntity(magicTriangle2);
            if (magicCircleParticles != null) world.removeEntity(magicCircleParticles);
            if (parentEntity != null) {
                world.removeEntity(parentEntity);
            }
            for (Entity entity : childEntities) {
                world.removeEntity(entity);
            }
            if (dynamicMeshEntity != null) {
                world.removeEntity(dynamicMeshEntity);
            }
            if (lodEntity != null) {
                world.removeEntity(lodEntity);
            }
            for (Entity entity : pbrEntities) {
                world.removeEntity(entity);
            }
        }

        meshEntities.clear();
        lineEntity = null;
        mcBlockEntity = null;
        gradientLineEntities.clear();
        particleEntities.clear();
        advancedParticleEntities.clear();
        parentEntity = null;
        childEntities.clear();
        dynamicMeshEntity = null;
        lodEntity = null;
        pbrEntities.clear();
        pbrMaterials.clear();
        meshParticleEntities.clear();
        vortexEntity = null;
        attractorEntity = null;
        showcaseParticles.clear();
        showcaseLabels.clear();
        showcasePositions = null;
        bounceTestEntities.clear();
        bounceTestLabels.clear();
        bounceTestPositions = null;
        bloomTestEntities.clear();
        magicCircleOuter = null;
        magicCircleInner = null;
        magicTriangle1 = null;
        magicTriangle2 = null;
        magicCircleParticles = null;
        magicCircleRotation = 0;
        magicCircleBuildProgress = 0;
        magicCircleBuilding = false;
        outerCirclePoints.clear();
        innerCirclePoints.clear();
        triangle1Points.clear();
        triangle2Points.clear();

        // Delete meshes
        if (cubeMesh != null) {
            cubeMesh.delete();
            cubeMesh = null;
        }
        if (sphereMesh != null) {
            sphereMesh.delete();
            sphereMesh = null;
        }
        if (planeMesh != null) {
            planeMesh.delete();
            planeMesh = null;
        }
        if (customPyramidMesh != null) {
            customPyramidMesh.delete();
            customPyramidMesh = null;
        }
        if (customTorusMesh != null) {
            customTorusMesh.delete();
            customTorusMesh = null;
        }
        if (mcBlockMesh != null) {
            mcBlockMesh.delete();
            mcBlockMesh = null;
        }
        if (dynamicMesh != null) {
            dynamicMesh.delete();
            dynamicMesh = null;
        }
        if (instancedBaseMesh != null) {
            instancedBaseMesh.delete();
            instancedBaseMesh = null;
        }

        // Close World3DBatch
        if (worldBatch != null) {
            worldBatch.close();
            worldBatch = null;
        }
        if (instancedBatch != null) {
            instancedBatch.close();
            instancedBatch = null;
        }

        if (meshNormalShader != null) {
            meshNormalShader.close();
            meshNormalShader = null;
        }
        if (meshColorShader != null) {
            meshColorShader.close();
            meshColorShader = null;
        }
        if (instancedShader != null) {
            instancedShader.close();
            instancedShader = null;
        }

        if (testTexture != null) {
            testTexture.delete();
            testTexture = null;
        }
        if (testFramebuffer != null) {
            testFramebuffer.delete();
            testFramebuffer = null;
        }
        if (pipFramebuffer != null) {
            pipFramebuffer.delete();
            pipFramebuffer = null;
        }
        if (pipCubeMesh != null) {
            pipCubeMesh.delete();
            pipCubeMesh = null;
        }
        pipMaterial = null;
        pipRenderContext = null;

        // Disable bloom
        PostProcessor pp = RenderSystem.getPostProcessor();
        if (pp != null) {
            pp.setEnabled(false);
        }

        initialized = false;
        testActive = false;
        loggedEnvironment = false;
        lastTime = 0;
        time = 0;
        stageTimer = 0.0f;
        currentStage = TestStage.ENVIRONMENT;

        logger.logInfo("[RenderTest] Cleanup complete");
    }
}
