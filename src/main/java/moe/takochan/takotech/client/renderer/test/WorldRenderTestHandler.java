package moe.takochan.takotech.client.renderer.test;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.renderer.graphics.batch.World3DBatch;
import moe.takochan.takotech.client.renderer.util.GLStateManager;
import moe.takochan.takotech.client.renderer.util.MCRenderHelper;

/**
 * 世界渲染测试类。
 * 在游戏世界中演示各种 3D 渲染效果。
 *
 * <p>
 * 测试内容:
 * </p>
 * <ul>
 * <li>方块高亮与选择效果（线框 + 实心）</li>
 * <li>矿石发光效果</li>
 * <li>光束和螺旋特效</li>
 * <li>实心/线框混合渲染</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class WorldRenderTestHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * 是否启用测试渲染
     */
    private boolean enabled = true;

    /**
     * 3D 批量渲染器
     */
    private World3DBatch worldBatch;
    private boolean batchInitialized = false;

    /**
     * 时间累加器（用于动画）
     */
    private float time = 0;

    public WorldRenderTestHandler() {
        TakoTechMod.LOG.info("[WorldRenderTest] Test handler created");
    }

    private void initResources() {
        if (batchInitialized) return;
        TakoTechMod.LOG.info("[WorldRenderTest] Initializing render resources...");
        worldBatch = new World3DBatch(16384);
        batchInitialized = true;
        TakoTechMod.LOG.info("[WorldRenderTest] Render resources initialized");
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) return;
        if (!RenderSystem.isInitialized() || !RenderSystem.isShaderSupported()) return;
        if (!MCRenderHelper.isClientReady()) return;

        if (!batchInitialized) {
            initResources();
        }

        float partialTicks = event.partialTicks;
        time += 0.016f;

        // 使用 GLStateManager 保存和恢复 GL 状态
        try (GLStateManager glState = GLStateManager.save()) {
            EntityPlayer player = MCRenderHelper.getPlayer();

            // 获取相机位置
            double camX = MCRenderHelper.getCameraX();
            double camY = MCRenderHelper.getCameraY();
            double camZ = MCRenderHelper.getCameraZ();

            // 获取玩家脚下的渲染位置（使用工具类进行插值）
            double[] footPos = MCRenderHelper.getPlayerFootRenderPos(player, partialTicks);
            double footRelX = footPos[0];
            double footRelY = footPos[1];
            double footRelZ = footPos[2];

            // ============ 线段渲染 ============
            worldBatch.begin(GL11.GL_LINES);

            // 1. 玩家脚下的动态圆环
            renderPlayerCircles(footRelX, footRelY, footRelZ);

            // 2. 高亮目标方块（线框）
            renderTargetBlockWireframe(camX, camY, camZ);

            // 3. 周围矿石发光标记
            renderOreMarkers(camX, camY, camZ, player);

            // 4. 坐标轴（在玩家脚下）
            renderCoordinateAxes(footRelX, footRelY + 0.05, footRelZ);

            worldBatch.end();

            // ============ 实心渲染 ============
            worldBatch.begin(GL11.GL_TRIANGLES);

            // 5. 目标方块顶面发光
            renderTargetBlockGlow(camX, camY, camZ);

            // 6. 周围特殊方块效果
            renderSpecialBlockEffects(camX, camY, camZ, player);

            // 7. 玩家头顶悬浮菱形（在玩家头顶0.5米）
            // footRelY 已经是脚下位置（约 -1.62）
            // 头顶 = 脚下 + 身高 = footRelY + height
            // 菱形 = 头顶 + 0.5
            double diamondY = footRelY + player.height + 0.5;
            renderFloatingDiamond(footRelX, diamondY, footRelZ);

            worldBatch.end();

            // ============ 再次线段渲染（覆盖在实心之上）============
            worldBatch.begin(GL11.GL_LINES);

            // 8. 螺旋上升效果
            renderSpiralEffect(footRelX, footRelY, footRelZ);

            worldBatch.end();
        }
    }

    /**
     * 玩家脚下动态特效（终极版 - 魔法阵风格）
     */
    private void renderPlayerCircles(double footRelX, double footRelY, double footRelZ) {
        double y = footRelY + 0.02;
        double cx = footRelX, cz = footRelZ;

        // === 能量脉冲波（向外扩散） ===
        for (int wave = 0; wave < 3; wave++) {
            double waveTime = (time * 0.8 + wave * 0.33) % 1.0;
            double waveRadius = waveTime * 4.0;
            float waveAlpha = (float) ((1.0 - waveTime) * 0.6);
            float[] waveColor = hsvToRgb((time * 0.5f + wave * 0.2f) % 1.0f, 1.0f, 1.0f);
            if (waveAlpha > 0.05f) {
                worldBatch.drawCircleXZ(cx, y, cz, waveRadius, 72, waveColor[0], waveColor[1], waveColor[2], waveAlpha);
            }
        }

        // === 多层魔法阵圆环 ===
        double[] radii = { 0.5, 1.0, 1.6, 2.3, 3.0 };
        for (int i = 0; i < radii.length; i++) {
            float pulse = (float) (Math.sin(time * 4 + i * 0.8) * 0.15 + 0.85);
            float[] rgb = hsvToRgb((time * 0.15f + i * 0.12f) % 1.0f, 0.95f, 1.0f);
            double r = radii[i] * pulse;
            int segs = 48 + i * 16;
            worldBatch.drawCircleXZ(cx, y + i * 0.005, cz, r, segs, rgb[0], rgb[1], rgb[2], 0.8f - i * 0.1f);
        }

        // === 五芒星（正向旋转） ===
        drawPentagram(cx, y + 0.02, cz, 1.8, time * 1.2, 1.0f, 0.3f, 0.3f, 0.9f);

        // === 五芒星（反向旋转，较小） ===
        drawPentagram(cx, y + 0.025, cz, 1.2, -time * 1.8, 0.3f, 0.6f, 1.0f, 0.8f);

        // === 外圈符文标记（旋转） ===
        int numRunes = 8;
        double runeRadius = 2.5;
        for (int i = 0; i < numRunes; i++) {
            double angle = time * 0.8 + i * Math.PI * 2 / numRunes;
            double rx = cx + Math.cos(angle) * runeRadius;
            double rz = cz + Math.sin(angle) * runeRadius;
            float[] rgb = hsvToRgb((time * 0.4f + (float) i / numRunes) % 1.0f, 1.0f, 1.0f);
            // 符文形状（小型复杂图案）
            drawRuneSymbol(rx, y + 0.03, rz, 0.2, angle + time, rgb[0], rgb[1], rgb[2], 0.85f);
        }

        // === 内圈能量光点（快速旋转） ===
        int numOrbs = 16;
        for (int i = 0; i < numOrbs; i++) {
            double angle = time * 4 + i * Math.PI * 2 / numOrbs;
            double orbRadius = 0.7 + Math.sin(time * 3 + i) * 0.15;
            double ox = cx + Math.cos(angle) * orbRadius;
            double oz = cz + Math.sin(angle) * orbRadius;
            float[] rgb = hsvToRgb((float) i / numOrbs, 1.0f, 1.0f);
            // 光点连线到中心
            worldBatch.drawLine(cx, y + 0.04, cz, ox, y + 0.04, oz, rgb[0], rgb[1], rgb[2], 0.3f);
            // 光点本身
            worldBatch.drawCross(ox, y + 0.04, oz, 0.06, rgb[0], rgb[1], rgb[2], 0.9f);
        }

        // === 六芒星（双层交错） ===
        drawHexagram(cx, y + 0.015, cz, 2.0, time * 0.6, 1.0f, 0.9f, 0.2f, 0.7f);
        drawHexagram(cx, y + 0.018, cz, 2.0, -time * 0.6 + Math.PI / 6, 0.2f, 0.9f, 1.0f, 0.6f);

        // === 能量柱（环绕玩家） ===
        int numPillars = 6;
        for (int i = 0; i < numPillars; i++) {
            double angle = time * 0.5 + i * Math.PI * 2 / numPillars;
            double px = cx + Math.cos(angle) * 2.8;
            double pz = cz + Math.sin(angle) * 2.8;
            float pulse = (float) (Math.sin(time * 5 + i * 1.2) * 0.5 + 0.5);
            float height = 0.3f + pulse * 1.5f;
            float[] rgb = hsvToRgb((time * 0.3f + (float) i / numPillars) % 1.0f, 0.9f, 1.0f);
            // 能量柱
            for (int j = 0; j < 4; j++) {
                double offsetAngle = j * Math.PI / 2;
                double lineOffset = 0.05;
                worldBatch.drawLine(
                    px + Math.cos(offsetAngle) * lineOffset,
                    y,
                    pz + Math.sin(offsetAngle) * lineOffset,
                    px + Math.cos(offsetAngle) * lineOffset,
                    y + height,
                    pz + Math.sin(offsetAngle) * lineOffset,
                    rgb[0],
                    rgb[1],
                    rgb[2],
                    0.7f);
            }
            // 顶部光点
            worldBatch.drawCross(px, y + height, pz, 0.1, rgb[0], rgb[1], rgb[2], pulse);
        }

        // === 中心能量核心 ===
        float corePulse = (float) (Math.sin(time * 6) * 0.3 + 0.7);
        for (int ring = 0; ring < 3; ring++) {
            float[] coreColor = hsvToRgb((time * 0.8f + ring * 0.15f) % 1.0f, 1.0f, 1.0f);
            double coreRadius = 0.15 + ring * 0.08 + corePulse * 0.05;
            worldBatch.drawCircleXZ(
                cx,
                y + 0.05 + ring * 0.01,
                cz,
                coreRadius,
                24,
                coreColor[0],
                coreColor[1],
                coreColor[2],
                0.9f - ring * 0.2f);
        }
    }

    /** 绘制五芒星 */
    private void drawPentagram(double cx, double y, double cz, double radius, double rotation, float r, float g,
        float b, float a) {
        double[] px = new double[5];
        double[] pz = new double[5];
        for (int i = 0; i < 5; i++) {
            double angle = rotation + i * Math.PI * 2 / 5 - Math.PI / 2;
            px[i] = cx + Math.cos(angle) * radius;
            pz[i] = cz + Math.sin(angle) * radius;
        }
        // 连接形成五芒星
        for (int i = 0; i < 5; i++) {
            int next = (i + 2) % 5;
            worldBatch.drawLine(px[i], y, pz[i], px[next], y, pz[next], r, g, b, a);
        }
    }

    /** 绘制六芒星 */
    private void drawHexagram(double cx, double y, double cz, double radius, double rotation, float r, float g, float b,
        float a) {
        // 两个交错的等边三角形
        for (int tri = 0; tri < 2; tri++) {
            double baseAngle = rotation + tri * Math.PI / 6;
            double[] px = new double[3];
            double[] pz = new double[3];
            for (int i = 0; i < 3; i++) {
                double angle = baseAngle + i * Math.PI * 2 / 3;
                px[i] = cx + Math.cos(angle) * radius;
                pz[i] = cz + Math.sin(angle) * radius;
            }
            for (int i = 0; i < 3; i++) {
                worldBatch.drawLine(px[i], y, pz[i], px[(i + 1) % 3], y, pz[(i + 1) % 3], r, g, b, a);
            }
        }
    }

    /** 绘制符文符号 */
    private void drawRuneSymbol(double cx, double y, double cz, double size, double rotation, float r, float g, float b,
        float a) {
        // 简单的符文：菱形 + 十字
        double s = size;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        // 菱形四个点
        double[][] points = { { 0, -s }, { s, 0 }, { 0, s }, { -s, 0 } };
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double x1 = cx + points[i][0] * cos - points[i][1] * sin;
            double z1 = cz + points[i][0] * sin + points[i][1] * cos;
            double x2 = cx + points[next][0] * cos - points[next][1] * sin;
            double z2 = cz + points[next][0] * sin + points[next][1] * cos;
            worldBatch.drawLine(x1, y, z1, x2, y, z2, r, g, b, a);
        }
        // 中心十字
        worldBatch.drawCross(cx, y, cz, s * 0.5, r, g, b, a * 0.7f);
    }

    /**
     * 目标方块线框
     */
    private void renderTargetBlockWireframe(double camX, double camY, double camZ) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        // 转换为相对坐标（方块坐标 - 相机位置）
        double rx = mop.blockX - camX;
        double ry = mop.blockY - camY;
        double rz = mop.blockZ - camZ;

        // 脉冲扩展
        float pulse = (float) (Math.sin(time * 5) * 0.5 + 0.5);
        double expand = pulse * 0.03;

        worldBatch.drawWireBox(
            rx - expand,
            ry - expand,
            rz - expand,
            1 + expand * 2,
            1 + expand * 2,
            1 + expand * 2,
            1.0f,
            0.4f,
            0.4f,
            0.9f);

        // 中心十字
        worldBatch.drawCross(rx + 0.5, ry + 0.5, rz + 0.5, 0.2, 1.0f, 1.0f, 0.0f, 0.8f);
    }

    /**
     * 目标方块顶面发光（实心）
     */
    private void renderTargetBlockGlow(double camX, double camY, double camZ) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        double rx = mop.blockX - camX;
        double ry = mop.blockY - camY;
        double rz = mop.blockZ - camZ;

        float pulse = (float) (Math.sin(time * 4) * 0.3 + 0.5);
        worldBatch.drawBlockGlow(rx, ry, rz, 0.3f, 0.8f, 1.0f, pulse * 0.5f);
        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.08, 0.5f, 1.0f, 1.0f, pulse * 0.7f);
    }

    /**
     * 周围矿石高亮（线框）
     */
    private void renderOreMarkers(double camX, double camY, double camZ, EntityPlayer player) {
        World world = mc.theWorld;
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);
        int range = 8;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    int bx = px + dx, by = py + dy, bz = pz + dz;
                    Block block = world.getBlock(bx, by, bz);

                    float[] color = getOreColorByName(block);
                    if (color != null) {
                        // 转换为相对坐标
                        double rx = bx - camX;
                        double ry = by - camY;
                        double rz = bz - camZ;

                        // 距离衰减
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        float alpha = (float) (0.8 * (1 - dist / (range + 1)));

                        if (alpha > 0.1f) {
                            // 稍微缩小的线框
                            double shrink = 0.1;
                            worldBatch.drawWireBox(
                                rx + shrink,
                                ry + shrink,
                                rz + shrink,
                                1 - shrink * 2,
                                1 - shrink * 2,
                                1 - shrink * 2,
                                color[0],
                                color[1],
                                color[2],
                                alpha);
                        }
                    }
                }
            }
        }
    }

    /**
     * 特殊方块效果（实心面片）
     */
    private void renderSpecialBlockEffects(double camX, double camY, double camZ, EntityPlayer player) {
        World world = mc.theWorld;
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);
        int range = 8;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    int bx = px + dx, by = py + dy, bz = pz + dz;
                    Block block = world.getBlock(bx, by, bz);
                    String blockName = block.getUnlocalizedName()
                        .toLowerCase();

                    // 转换为相对坐标
                    double rx = bx - camX;
                    double ry = by - camY;
                    double rz = bz - camZ;

                    // 钻石矿：青色顶面发光
                    if (blockName.contains("diamond") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 4 + bx + bz) * 0.3 + 0.6);
                        worldBatch.drawBlockGlow(rx, ry, rz, 0.3f, 0.9f, 1.0f, pulse * 0.7f);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.12, 0.5f, 1.0f, 1.0f, pulse * 0.8f);
                    }
                    // 绿宝石矿：绿色顶面发光
                    else if (blockName.contains("emerald") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 4 + bx + bz) * 0.3 + 0.6);
                        worldBatch.drawBlockGlow(rx, ry, rz, 0.1f, 1.0f, 0.3f, pulse * 0.7f);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.12, 0.3f, 1.0f, 0.5f, pulse * 0.8f);
                    }
                    // 金矿：金色边缘发光 + 顶面发光
                    else if (blockName.contains("gold") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 3 + bx) * 0.2 + 0.6);
                        worldBatch.drawBlockGlow(rx, ry, rz, 1.0f, 0.85f, 0.0f, pulse * 0.5f);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.1, 1.0f, 0.9f, 0.2f, pulse * 0.7f);
                    }
                    // 铁矿：橙色边缘发光
                    else if (blockName.contains("iron") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 2.5 + bz) * 0.2 + 0.5);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.08, 0.9f, 0.6f, 0.4f, pulse * 0.5f);
                    }
                    // 红石矿：红色脉冲光柱
                    else if (blockName.contains("redstone") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 6 + by) * 0.5 + 0.5);
                        worldBatch.drawBlockGlow(rx, ry, rz, 1.0f, 0.1f, 0.1f, pulse * 0.5f);
                        worldBatch.drawRisingBeam(
                            rx + 0.5,
                            ry + 1.01,
                            rz + 0.5,
                            0.25 + pulse * 0.15,
                            0.5 + pulse * 0.8,
                            1.0f,
                            0.15f,
                            0.15f,
                            0.5f * pulse);
                    }
                    // 青金石矿：蓝色发光
                    else if (blockName.contains("lapis") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 3.5 + bx + by) * 0.25 + 0.6);
                        worldBatch.drawBlockGlow(rx, ry, rz, 0.1f, 0.2f, 0.9f, pulse * 0.6f);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.1, 0.3f, 0.4f, 1.0f, pulse * 0.7f);
                    }
                    // 煤矿：暗灰色边缘
                    else if (blockName.contains("coal") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 2 + bx) * 0.15 + 0.4);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.06, 0.3f, 0.3f, 0.3f, pulse * 0.4f);
                    }
                    // 铜矿：铜色发光
                    else if (blockName.contains("copper") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 3 + bx) * 0.2 + 0.5);
                        worldBatch.drawBlockGlow(rx, ry, rz, 0.8f, 0.5f, 0.2f, pulse * 0.5f);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.08, 0.9f, 0.6f, 0.3f, pulse * 0.6f);
                    }
                    // 锡矿：银白色发光
                    else if (blockName.contains("tin") && blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 2.5 + bz) * 0.2 + 0.5);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.08, 0.8f, 0.8f, 0.9f, pulse * 0.5f);
                    }
                    // 通用矿石检测：任何包含 "ore" 的方块
                    else if (blockName.contains("ore")) {
                        float pulse = (float) (Math.sin(time * 3 + bx + by + bz) * 0.2 + 0.5);
                        worldBatch.drawBlockEdgeGlow(rx, ry, rz, 0.06, 0.7f, 0.7f, 0.7f, pulse * 0.4f);
                    }
                }
            }
        }
    }

    /**
     * 悬浮菱形（在玩家头顶）- 终极版：能量核心
     */
    private void renderFloatingDiamond(double rx, double ry, double rz) {
        float bob = (float) Math.sin(time * 2) * 0.2f;
        float spin = time * 3;
        double cy = ry + bob;

        // === 主能量核心（大菱形） ===
        float corePulse = (float) (Math.sin(time * 4) * 0.2 + 0.8);
        float[] coreColor = hsvToRgb((time * 0.25f) % 1.0f, 0.9f, 1.0f);
        worldBatch.drawDiamond(rx, cy, rz, 0.25 * corePulse, coreColor[0], coreColor[1], coreColor[2], 0.9f);

        // === 双层环绕菱形（反向旋转） ===
        for (int layer = 0; layer < 2; layer++) {
            int count = 4 + layer * 2;
            double orbitRadius = 0.5 + layer * 0.3;
            double layerSpin = layer == 0 ? spin : -spin * 1.5;
            double vertOffset = layer == 0 ? 0 : Math.sin(time * 3) * 0.15;
            for (int i = 0; i < count; i++) {
                double angle = layerSpin + i * Math.PI * 2 / count;
                double ox = rx + Math.cos(angle) * orbitRadius;
                double oz = rz + Math.sin(angle) * orbitRadius;
                float[] color = hsvToRgb((time * 0.4f + (float) i / count + layer * 0.3f) % 1.0f, 1.0f, 1.0f);
                double size = 0.1 - layer * 0.02;
                worldBatch.drawDiamond(ox, cy + vertOffset, oz, size, color[0], color[1], color[2], 0.7f);
                // 连接到核心的能量线
                worldBatch.drawLine(rx, cy, rz, ox, cy + vertOffset, oz, color[0], color[1], color[2], 0.25f);
            }
        }

        // === 能量光环 ===
        for (int ring = 0; ring < 3; ring++) {
            float ringPulse = (float) (Math.sin(time * 5 + ring) * 0.15 + 0.85);
            float[] ringColor = hsvToRgb((time * 0.3f + ring * 0.2f) % 1.0f, 0.95f, 1.0f);
            double ringRadius = (0.6 + ring * 0.15) * ringPulse;
            // 水平环
            worldBatch
                .drawCircleXZ(rx, cy, rz, ringRadius, 32, ringColor[0], ringColor[1], ringColor[2], 0.5f - ring * 0.1f);
        }

        // === 垂直能量射线 ===
        float rayPulse = (float) (Math.sin(time * 6) * 0.4 + 0.6);
        float[] rayColor = hsvToRgb((time * 0.5f) % 1.0f, 1.0f, 1.0f);
        worldBatch.drawLine(
            rx,
            cy - 0.8 * rayPulse,
            rz,
            rx,
            cy + 0.8 * rayPulse,
            rz,
            rayColor[0],
            rayColor[1],
            rayColor[2],
            0.7f);

        // === 四方向水平射线 ===
        for (int i = 0; i < 4; i++) {
            double angle = spin * 0.5 + i * Math.PI / 2;
            double rayLen = 0.5 * rayPulse;
            double ex = rx + Math.cos(angle) * rayLen;
            double ez = rz + Math.sin(angle) * rayLen;
            float[] hRayColor = hsvToRgb((time * 0.4f + i * 0.25f) % 1.0f, 1.0f, 1.0f);
            worldBatch.drawLine(rx, cy, rz, ex, cy, ez, hRayColor[0], hRayColor[1], hRayColor[2], 0.6f);
        }
    }

    /**
     * 螺旋上升效果 - 终极版：能量龙卷风
     */
    private void renderSpiralEffect(double footRelX, double footRelY, double footRelZ) {
        double baseY = footRelY + 0.1;
        double cx = footRelX, cz = footRelZ;
        double maxHeight = 3.0;

        // === 三重螺旋龙卷风 ===
        for (int spiral = 0; spiral < 3; spiral++) {
            double phaseOffset = spiral * Math.PI * 2 / 3;
            float[] color = hsvToRgb((time * 0.2f + spiral * 0.33f) % 1.0f, 0.9f, 1.0f);

            int segments = 64;
            double prevX = 0, prevY = 0, prevZ = 0;
            for (int i = 0; i <= segments; i++) {
                double t = (double) i / segments;
                double h = baseY + t * maxHeight;
                // 半径随高度增加而减小（龙卷风形状）
                double radius = 0.8 * (1 - t * 0.6) + Math.sin(time * 2 + t * 10) * 0.1;
                double angle = t * Math.PI * 6 + time * 3 + phaseOffset;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;

                if (i > 0) {
                    float alpha = (float) (0.8 * (1 - t * 0.5));
                    worldBatch.drawLine(prevX, prevY, prevZ, x, h, z, color[0], color[1], color[2], alpha);
                }
                prevX = x;
                prevY = h;
                prevZ = z;
            }
        }

        // === 水平环带（多层） ===
        int numRings = 8;
        for (int ring = 0; ring < numRings; ring++) {
            double t = (double) ring / numRings;
            double h = baseY + t * maxHeight;
            double radius = 0.8 * (1 - t * 0.6);
            float ringPulse = (float) (Math.sin(time * 4 + ring * 0.5) * 0.2 + 0.8);
            float[] ringColor = hsvToRgb((float) ((time * 0.3f + t) % 1.0), 1.0f, 1.0f);
            worldBatch.drawCircleXZ(
                cx,
                h,
                cz,
                radius * ringPulse,
                24,
                ringColor[0],
                ringColor[1],
                ringColor[2],
                (float) (0.4 * (1 - t * 0.5)));
        }

        // === 上升能量粒子 ===
        int particles = 12;
        for (int i = 0; i < particles; i++) {
            double t = ((time * 0.4 + i * (1.0 / particles)) % 1.0);
            double h = baseY + t * maxHeight;
            double radius = 0.8 * (1 - t * 0.6);
            double angle = t * Math.PI * 8 + i * 0.5;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            float alpha = (float) ((1 - t) * 0.9);
            float[] pColor = hsvToRgb((time * 0.6f + (float) i / particles) % 1.0f, 1.0f, 1.0f);
            // 发光粒子
            worldBatch.drawCross(px, h, pz, 0.1 * (1 - t * 0.5), pColor[0], pColor[1], pColor[2], alpha);
            // 粒子拖尾
            if (t > 0.1) {
                double prevT = t - 0.1;
                double prevH = baseY + prevT * maxHeight;
                double prevRadius = 0.8 * (1 - prevT * 0.6);
                double prevAngle = prevT * Math.PI * 8 + i * 0.5;
                double prevPx = cx + Math.cos(prevAngle) * prevRadius;
                double prevPz = cz + Math.sin(prevAngle) * prevRadius;
                worldBatch.drawLine(prevPx, prevH, prevPz, px, h, pz, pColor[0], pColor[1], pColor[2], alpha * 0.5f);
            }
        }

        // === 顶部能量爆发 ===
        double topY = baseY + maxHeight;
        float burstPulse = (float) (Math.sin(time * 5) * 0.3 + 0.7);
        int burstRays = 8;
        for (int i = 0; i < burstRays; i++) {
            double angle = time * 2 + i * Math.PI * 2 / burstRays;
            double rayLen = 0.4 * burstPulse;
            double ex = cx + Math.cos(angle) * rayLen;
            double ez = cz + Math.sin(angle) * rayLen;
            float[] rayColor = hsvToRgb((time * 0.7f + (float) i / burstRays) % 1.0f, 1.0f, 1.0f);
            worldBatch
                .drawLine(cx, topY, cz, ex, topY + 0.3 * burstPulse, ez, rayColor[0], rayColor[1], rayColor[2], 0.8f);
        }
    }

    /**
     * 坐标轴
     */
    private void renderCoordinateAxes(double rx, double ry, double rz) {
        worldBatch.drawLine(rx, ry, rz, rx + 1, ry, rz, 1, 0, 0, 1); // X 红
        worldBatch.drawLine(rx, ry, rz, rx, ry + 1, rz, 0, 1, 0, 1); // Y 绿
        worldBatch.drawLine(rx, ry, rz, rx, ry, rz + 1, 0, 0, 1, 1); // Z 蓝
    }

    /**
     * 根据方块名称获取矿石颜色（支持 GTNH 模组矿石）
     */
    private float[] getOreColorByName(Block block) {
        String name = block.getUnlocalizedName()
            .toLowerCase();

        // 不是矿石直接返回
        if (!name.contains("ore")) return null;

        // 根据名称匹配颜色
        if (name.contains("coal")) return new float[] { 0.2f, 0.2f, 0.2f };
        if (name.contains("iron")) return new float[] { 0.85f, 0.7f, 0.6f };
        if (name.contains("gold")) return new float[] { 1.0f, 0.85f, 0.0f };
        if (name.contains("diamond")) return new float[] { 0.4f, 0.9f, 1.0f };
        if (name.contains("emerald")) return new float[] { 0.2f, 1.0f, 0.4f };
        if (name.contains("redstone")) return new float[] { 1.0f, 0.1f, 0.1f };
        if (name.contains("lapis")) return new float[] { 0.2f, 0.3f, 0.9f };
        if (name.contains("quartz")) return new float[] { 1.0f, 1.0f, 0.95f };
        if (name.contains("copper")) return new float[] { 0.8f, 0.5f, 0.2f };
        if (name.contains("tin")) return new float[] { 0.8f, 0.8f, 0.9f };
        if (name.contains("silver")) return new float[] { 0.85f, 0.85f, 0.9f };
        if (name.contains("lead")) return new float[] { 0.4f, 0.4f, 0.5f };
        if (name.contains("nickel")) return new float[] { 0.7f, 0.7f, 0.6f };
        if (name.contains("uranium")) return new float[] { 0.3f, 0.9f, 0.3f };
        if (name.contains("titanium")) return new float[] { 0.7f, 0.7f, 0.8f };
        if (name.contains("platinum")) return new float[] { 0.9f, 0.9f, 0.85f };
        if (name.contains("iridium")) return new float[] { 0.95f, 0.95f, 1.0f };

        // 通用矿石颜色
        return new float[] { 0.6f, 0.6f, 0.6f };
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

    public void toggle() {
        enabled = !enabled;
        TakoTechMod.LOG.info("[WorldRenderTest] Test rendering {}", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void dispose() {
        if (worldBatch != null) {
            worldBatch.close();
            worldBatch = null;
        }
        batchInitialized = false;
    }
}
