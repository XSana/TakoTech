package moe.takochan.takotech.client.renderer.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.renderer.graphics.batch.RenderQueue;
import moe.takochan.takotech.client.renderer.graphics.batch.SpriteBatch;
import moe.takochan.takotech.client.renderer.graphics.camera.Camera;
import moe.takochan.takotech.client.renderer.graphics.camera.Frustum;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;
import moe.takochan.takotech.client.renderer.graphics.culling.BoundingSphere;
import moe.takochan.takotech.client.renderer.graphics.material.Material;
import moe.takochan.takotech.client.renderer.graphics.material.PBRMaterial;
import moe.takochan.takotech.client.renderer.graphics.material.RenderMode;
import moe.takochan.takotech.client.renderer.graphics.mesh.LODGroup;
import moe.takochan.takotech.client.renderer.graphics.scene.Scene;
import moe.takochan.takotech.client.renderer.graphics.scene.SceneNode;
import moe.takochan.takotech.client.renderer.graphics.scene.Transform;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.client.renderer.util.GLStateManager;

/**
 * 渲染框架测试类。
 * 在游戏内演示各种渲染组件的功能。
 *
 * <p>
 * 测试内容:
 * </p>
 * <ul>
 * <li>SpriteBatch - 2D 批量渲染</li>
 * <li>Material - 材质系统</li>
 * <li>Scene/Camera/Transform - 场景管理</li>
 * <li>LODGroup - LOD 选择</li>
 * <li>RenderQueue - 渲染队列</li>
 * <li>PBRMaterial - PBR 材质预设</li>
 * <li>AABB/BoundingSphere - 碰撞体</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class RenderingTestHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 是否启用测试渲染 */
    private boolean enabled = true;

    /** 测试场景 */
    private Scene testScene;
    private Camera testCamera;

    /** 测试 LOD 组 */
    private LODGroup testLODGroup;

    /** 测试渲染队列 */
    private RenderQueue testRenderQueue;

    /** 时间累加器（用于动画） */
    private float time = 0;

    /** 已初始化标记 */
    private boolean initialized = false;

    public RenderingTestHandler() {
        TakoTechMod.LOG.info("[RenderingTest] Test handler created");
    }

    /**
     * 初始化测试资源
     */
    private void initTestResources() {
        if (initialized) return;

        TakoTechMod.LOG.info("[RenderingTest] Initializing test resources...");

        // 1. 创建测试场景
        testScene = new Scene("TestScene");

        // 2. 创建测试相机
        testCamera = Camera.perspective(60.0f, 16.0f / 9.0f, 0.1f, 1000.0f);
        testCamera.setPosition(0, 5, 10);
        testCamera.setTarget(0, 0, 0);
        testScene.setMainCamera(testCamera);

        // 3. 创建场景节点测试 Transform
        SceneNode rootNode = testScene.getRoot();

        SceneNode childNode1 = new SceneNode("Child1");
        childNode1.getTransform()
            .setPosition(2, 0, 0);
        rootNode.addChild(childNode1);

        SceneNode childNode2 = new SceneNode("Child2");
        childNode2.getTransform()
            .setPosition(-2, 0, 0);
        childNode2.getTransform()
            .setScale(0.5f);
        rootNode.addChild(childNode2);

        // 4. 创建 LOD 组（使用 null mesh 演示逻辑）
        testLODGroup = new LODGroup();
        testLODGroup.addLevel(null, 0, 10); // LOD 0: 0-10 距离
        testLODGroup.addLevel(null, 10, 50); // LOD 1: 10-50 距离
        testLODGroup.addLevel(null, 50, 200); // LOD 2: 50-200 距离
        testLODGroup.setMaxCullDistance(200);

        // 5. 创建渲染队列
        testRenderQueue = new RenderQueue();

        initialized = true;
        TakoTechMod.LOG.info("[RenderingTest] Test resources initialized");
    }

    /**
     * HUD 渲染事件
     */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!RenderSystem.isInitialized() || !RenderSystem.isShaderSupported()) return;

        // 延迟初始化
        if (!initialized) {
            initTestResources();
        }

        // 更新时间
        time += 0.016f; // ~60fps

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        // 渲染测试 HUD
        renderTestHUD(width, height);
    }

    /**
     * 渲染测试 HUD
     */
    private void renderTestHUD(int screenWidth, int screenHeight) {
        // 使用 GLStateManager 保存和恢复 GL 状态
        try (GLStateManager glState = GLStateManager.save()) {
            // SpriteBatch 综合测试
            SpriteBatch batch = RenderSystem.getSpriteBatch();
            if (batch != null) {
                batch.setProjectionOrtho(screenWidth, screenHeight);
                batch.begin();

                // 测试 1: 基础 drawRect (RGB 纯色)
                batch.drawRect(10, 10, 40, 40, 1.0f, 0.0f, 0.0f, 0.8f); // 红色
                batch.drawRect(55, 10, 40, 40, 0.0f, 1.0f, 0.0f, 0.8f); // 绿色
                batch.drawRect(100, 10, 40, 40, 0.0f, 0.0f, 1.0f, 0.8f); // 蓝色

                // 测试 2: drawRect with RGBA int
                batch.drawRect(145, 10, 40, 40, 0xFFFF00FF); // 黄色 (RGBA)

                // 测试 3: drawQuad 任意四边形（动态旋转）
                float cx = 220, cy = 30;
                float size = 18;
                float angle = time * 2;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                batch.drawQuad(
                    cx + (-size * cos - -size * sin),
                    cy + (-size * sin + -size * cos),
                    cx + (size * cos - -size * sin),
                    cy + (size * sin + -size * cos),
                    cx + (size * cos - size * sin),
                    cy + (size * sin + size * cos),
                    cx + (-size * cos - size * sin),
                    cy + (-size * sin + size * cos),
                    1.0f,
                    0.5f,
                    0.0f,
                    0.9f);

                // 测试 4: 半透明叠加
                batch.drawRect(10, 55, 80, 30, 1.0f, 1.0f, 1.0f, 0.3f);
                batch.drawRect(30, 60, 80, 30, 0.0f, 0.5f, 1.0f, 0.5f);

                // 测试 5: 彩虹条（测试批量处理）
                for (int i = 0; i < 7; i++) {
                    float hue = i / 7.0f;
                    float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
                    batch.drawRect(250 + i * 15, 10, 12, 40, rgb[0], rgb[1], rgb[2], 0.9f);
                }

                // 测试 6: 脉冲动画方块
                float pulse = (float) (Math.sin(time * 4) * 0.3 + 0.7);
                batch.drawRect(360, 10, 40 * pulse, 40 * pulse, 0.8f, 0.2f, 0.8f, pulse);

                batch.end();
            }

            // 渲染测试信息文本（使用 MC 原生渲染）
            renderTestInfo(screenWidth, screenHeight);
        }
    }

    /**
     * HSV 转 RGB
     */
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

    /**
     * 渲染测试信息
     */
    private void renderTestInfo(int screenWidth, int screenHeight) {
        int textY = 95;
        int lineHeight = 10;
        int textColor = 0xFFFFFF;

        // === 1. Transform 测试 ===
        Transform transform = new Transform();
        transform.setPosition(1, 2, 3);
        transform.setScale(2.0f);
        transform.setRotationEuler(0, (float) Math.PI / 4, 0);

        // 测试 translate
        Transform transform2 = new Transform();
        transform2.setPosition(0, 0, 0);
        transform2.translate(5, 5, 5);

        // === 2. Camera 测试 ===
        Vector3f camPos = testCamera.getPosition();
        Camera orthoCam = Camera.orthographic(100, 100, -100, 100);
        String orthoType = orthoCam.getProjectionType()
            .name();

        // === 3. LOD 测试（多距离） ===
        testLODGroup.selectLOD(5 * 5); // 5 单位
        int lodAt5 = testLODGroup.getCurrentLevel();
        testLODGroup.selectLOD(25 * 25); // 25 单位
        int lodAt25 = testLODGroup.getCurrentLevel();
        testLODGroup.selectLOD(100 * 100); // 100 单位
        int lodAt100 = testLODGroup.getCurrentLevel();

        // === 4. AABB 测试 ===
        AABB testAABB = new AABB(-1, -1, -1, 1, 1, 1);
        AABB expandedAABB = new AABB(-1, -1, -1, 1, 1, 1);
        expandedAABB.expand(0.5f);

        AABB aabb1 = new AABB(0, 0, 0, 2, 2, 2);
        AABB aabb2 = new AABB(1, 1, 1, 3, 3, 3);
        boolean aabbIntersect = aabb1.intersects(aabb2);

        boolean pointInAABB = testAABB.contains(0, 0, 0);

        // === 5. BoundingSphere 测试 ===
        BoundingSphere testSphere = new BoundingSphere(0, 0, 0, 5);
        BoundingSphere sphere2 = new BoundingSphere(4, 0, 0, 2);
        boolean sphereIntersect = testSphere.intersects(sphere2);
        boolean sphereContainsPoint = testSphere.contains(1, 1, 1);

        // === 6. Frustum 测试 ===
        Frustum frustum = testCamera.getFrustum();
        boolean aabbInFrustum = frustum.intersectsAABB(testAABB);
        boolean sphereInFrustum = frustum.intersectsSphere(0, 0, -5, 2);
        Frustum.TestResult frustumTestResult = frustum.testAABB(testAABB);

        // === 7. Material 测试 ===
        Material colorMat = Material.color(1.0f, 0.5f, 0.2f, 1.0f);
        Material textureMat = Material.texture(1);
        Material blurMat = Material.blurHorizontal(1, 2.0f);
        RenderMode colorMode = colorMat.getRenderMode();
        RenderMode textureMode = textureMat.getRenderMode();
        RenderMode blurMode = blurMat.getRenderMode();

        // === 8. PBRMaterial 预设测试 ===
        PBRMaterial goldMat = PBRMaterial.gold();
        PBRMaterial silverMat = PBRMaterial.silver();
        PBRMaterial copperMat = PBRMaterial.copper();
        PBRMaterial ironMat = PBRMaterial.iron();
        PBRMaterial rubberMat = PBRMaterial.rubber();

        // === 9. Scene 测试 ===
        testScene.update(0.016f);
        testScene.collectVisibleRenderers();
        String sceneStats = testScene.getStats();
        SceneNode foundNode = testScene.findNode("Child1");
        boolean nodeFound = foundNode != null;

        // === 10. RenderQueue 测试 ===
        testRenderQueue.clear();
        testRenderQueue.setCameraPosition(0, 0, 0);
        // 不添加实际 renderer，只测试统计

        // === 11. ShaderType 测试 ===
        boolean simpleLoaded = false;
        boolean guiColorLoaded = false;
        boolean world3dLoaded = false;
        try {
            simpleLoaded = ShaderType.SIMPLE.get() != null && ShaderType.SIMPLE.get()
                .isValid();
            guiColorLoaded = ShaderType.GUI_COLOR.get() != null && ShaderType.GUI_COLOR.get()
                .isValid();
            world3dLoaded = ShaderType.WORLD_3D.get() != null && ShaderType.WORLD_3D.get()
                .isValid();
        } catch (Exception e) {
            // Shader not initialized
        }

        // === 12. SpriteBatch 状态测试 ===
        SpriteBatch sb = RenderSystem.getSpriteBatch();
        int maxQuads = sb != null ? sb.getMaxQuads() : 0;

        // 绘制信息
        String[] lines = { "=== Rendering Framework Test (MC 1.7.10 / GLSL 1.20) ===",
            String.format(
                "Transform: pos=(%.1f,%.1f,%.1f) scale=%.1f | translate test: (%.0f,%.0f,%.0f)",
                transform.getX(),
                transform.getY(),
                transform.getZ(),
                transform.getScale().x,
                transform2.getX(),
                transform2.getY(),
                transform2.getZ()),
            String.format(
                "Camera: persp pos=(%.1f,%.1f,%.1f) fov=%.0f | ortho type=%s",
                camPos.x,
                camPos.y,
                camPos.z,
                testCamera.getFov(),
                orthoType),
            String.format(
                "LOD: dist=5->L%d, dist=25->L%d, dist=100->L%d (total=%d levels)",
                lodAt5,
                lodAt25,
                lodAt100,
                testLODGroup.getLevelCount()),
            String.format(
                "AABB: valid=%b, size=(%.1f,%.1f,%.1f) | expand test: (%.1f,%.1f,%.1f)",
                testAABB.isValid(),
                testAABB.getSizeX(),
                testAABB.getSizeY(),
                testAABB.getSizeZ(),
                expandedAABB.getSizeX(),
                expandedAABB.getSizeY(),
                expandedAABB.getSizeZ()),
            String.format("AABB collision: intersect=%b | point(0,0,0) inside=%b", aabbIntersect, pointInAABB),
            String.format(
                "Sphere: r=%.1f | intersect=%b | contains(1,1,1)=%b",
                testSphere.radius,
                sphereIntersect,
                sphereContainsPoint),
            String.format(
                "Frustum: AABB inFrustum=%b, sphere inFrustum=%b, testResult=%s",
                aabbInFrustum,
                sphereInFrustum,
                frustumTestResult.name()),
            String.format(
                "Material: color=%s, texture=%s, blur=%s",
                colorMode.name(),
                textureMode.name(),
                blurMode.name()),
            String.format(
                "PBR: gold(M=%.1f,R=%.1f) silver(M=%.1f,R=%.1f) iron(M=%.1f,R=%.1f)",
                goldMat.getMetallic(),
                goldMat.getRoughness(),
                silverMat.getMetallic(),
                silverMat.getRoughness(),
                ironMat.getMetallic(),
                ironMat.getRoughness()),
            String.format(
                "PBR: copper(M=%.1f,R=%.1f) rubber(M=%.1f,R=%.1f)",
                copperMat.getMetallic(),
                copperMat.getRoughness(),
                rubberMat.getMetallic(),
                rubberMat.getRoughness()),
            String.format("%s | Child1 found=%b", sceneStats, nodeFound),
            String.format(
                "RenderQueue: opaque=%d, transparent=%d, overlay=%d",
                testRenderQueue.getOpaqueCount(),
                testRenderQueue.getTransparentCount(),
                testRenderQueue.getOverlayCount()),
            String.format(
                "Shaders: SIMPLE=%b GUI_COLOR=%b WORLD_3D=%b | SpriteBatch maxQuads=%d",
                simpleLoaded,
                guiColorLoaded,
                world3dLoaded,
                maxQuads),
            String.format("Time: %.2fs | ShaderSupport=%b", time, RenderSystem.isShaderSupported()) };

        // 使用 MC 字体渲染器绘制文本
        // 保存所有可能被修改的 GL 状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();

        // 显式设置 MC 字体渲染需要的状态
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);

        for (int i = 0; i < lines.length; i++) {
            mc.fontRenderer.drawStringWithShadow(lines[i], 5, textY + i * lineHeight, textColor);
        }

        // 恢复所有 GL 状态
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * 切换测试渲染
     */
    public void toggle() {
        enabled = !enabled;
        TakoTechMod.LOG.info("[RenderingTest] Test rendering {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置启用状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 释放资源（世界卸载时调用）
     */
    public void dispose() {
        // 清理测试对象（这些是纯 Java 对象，不包含 GL 资源，但为了一致性添加此方法）
        testScene = null;
        testCamera = null;
        testLODGroup = null;
        testRenderQueue = null;
        initialized = false;
        TakoTechMod.LOG.info("[RenderingTest] Resources disposed");
    }
}
