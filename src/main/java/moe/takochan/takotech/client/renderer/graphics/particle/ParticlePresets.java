package moe.takochan.takotech.client.renderer.graphics.particle;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子预设效果工厂。
 * 提供常用粒子效果的快速创建方法。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 创建火焰效果
 *     ParticleSystem fire = ParticlePresets.createFire(1.0f);
 *     fire.setPosition(x, y, z);
 *     fire.initialize();
 *
 *     // 创建爆炸效果
 *     ParticleSystem explosion = ParticlePresets.createExplosion(2.0f);
 *     explosion.setPosition(x, y, z);
 *     explosion.initialize();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class ParticlePresets {

    private ParticlePresets() {}

    // ==================== 火焰效果 ====================

    /**
     * 创建火焰效果
     *
     * @param intensity 强度 (0.5-2.0)
     * @return 粒子系统
     */
    public static ParticleSystem createFire(float intensity) {
        ParticleSystem system = new ParticleSystem((int) (5000 * intensity));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, 0.3f * intensity)
            .setRate(100 * intensity)
            .setLifetime(0.5f, 1.5f)
            .setVelocity(0, 2 * intensity, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.1f, 0.3f)
            .setColorOverLifetime(ColorOverLifetime.fire())
            .setSizeOverLifetime(SizeOverLifetime.shrink());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(2.0f, 1.0f * intensity));
        system.addForce(ParticleForce.gravity(0, 0.5f, 0)); // 上升力
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    /**
     * 创建火把效果（小型火焰）
     *
     * @return 粒子系统
     */
    public static ParticleSystem createTorch() {
        ParticleSystem system = new ParticleSystem(500);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.POINT)
            .setRate(30)
            .setLifetime(0.3f, 0.8f)
            .setVelocity(0, 1.5f, 0)
            .setVelocityVariation(0.2f)
            .setSize(0.05f, 0.1f)
            .setColorOverLifetime(ColorOverLifetime.fire())
            .setSizeOverLifetime(SizeOverLifetime.shrink());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(3.0f, 0.5f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    // ==================== 烟雾效果 ====================

    /**
     * 创建烟雾效果
     *
     * @param density 密度 (0.5-2.0)
     * @return 粒子系统
     */
    public static ParticleSystem createSmoke(float density) {
        ParticleSystem system = new ParticleSystem((int) (3000 * density));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, 0.2f)
            .setRate(50 * density)
            .setLifetime(2.0f, 4.0f)
            .setVelocity(0, 0.5f, 0)
            .setVelocityVariation(0.4f)
            .setSize(0.2f, 0.5f)
            .setColorOverLifetime(ColorOverLifetime.smoke())
            .setSizeOverLifetime(SizeOverLifetime.smoke());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(1.0f, 0.3f));
        system.addForce(ParticleForce.wind(0.5f, 0, 0, 0.3f, 0.2f));
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);

        return system;
    }

    // ==================== 爆炸效果 ====================

    /**
     * 创建爆炸效果
     *
     * @param scale 规模 (0.5-3.0)
     * @return 粒子系统
     */
    public static ParticleSystem createExplosion(float scale) {
        ParticleSystem system = new ParticleSystem((int) (2000 * scale));

        // 火球核心
        ParticleEmitter coreEmitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.1f * scale)
            .setBurst((int) (200 * scale))
            .setLifetime(0.2f, 0.5f)
            .setSpeed(8f * scale)
            .setVelocityVariation(0.3f)
            .setSize(0.3f * scale, 0.6f * scale)
            .setColorOverLifetime(ColorOverLifetime.explosion())
            .setSizeOverLifetime(SizeOverLifetime.explosion());

        // 火花
        ParticleEmitter sparkEmitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.05f * scale)
            .setBurst((int) (100 * scale))
            .setLifetime(0.5f, 1.2f)
            .setSpeed(15f * scale)
            .setVelocityVariation(0.5f)
            .setSize(0.02f * scale, 0.05f * scale)
            .setColor(1.0f, 0.9f, 0.5f, 1.0f)
            .setSizeOverLifetime(SizeOverLifetime.spark());

        // 烟雾
        ParticleEmitter smokeEmitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.3f * scale)
            .setBurst((int) (50 * scale))
            .setBurstInterval(0.1f)
            .setLifetime(1.0f, 3.0f)
            .setSpeed(3f * scale)
            .setVelocityVariation(0.4f)
            .setSize(0.5f * scale, 1.5f * scale)
            .setColorOverLifetime(ColorOverLifetime.smoke())
            .setSizeOverLifetime(SizeOverLifetime.smoke());

        system.addEmitter(coreEmitter);
        system.addEmitter(sparkEmitter);
        system.addEmitter(smokeEmitter);
        system.addForce(ParticleForce.drag(1.5f));
        system.addForce(ParticleForce.gravity(0, -5f, 0));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);
        system.setLooping(false);
        system.setDuration(3.0f);

        return system;
    }

    // ==================== 魔法效果 ====================

    /**
     * 创建能量球效果
     *
     * @param color 颜色 (0xRRGGBB)
     * @return 粒子系统
     */
    public static ParticleSystem createEnergyOrb(int color) {
        ParticleSystem system = new ParticleSystem(1000);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.SPHERE_SURFACE, 0.5f)
            .setRate(100)
            .setLifetime(0.5f, 1.0f)
            .setSpeed(-1.0f) // 向中心聚集
            .setSize(0.05f, 0.1f)
            .setColor(r, g, b, 1.0f)
            .setColorOverLifetime(ColorOverLifetime.energy())
            .setSizeOverLifetime(SizeOverLifetime.converge());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.attractor(0, 0, 0, 5.0f, 2.0f));
        system.addForce(ParticleForce.vortexY(0, 0, 0, 3.0f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    /**
     * 创建传送门效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createPortal() {
        ParticleSystem system = new ParticleSystem(3000);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.RING, 1.0f, 0.8f)
            .setRate(200)
            .setLifetime(0.5f, 1.5f)
            .setVelocity(0, 0.5f, 0)
            .setSize(0.08f, 0.12f)
            .setColorOverLifetime(ColorOverLifetime.portal())
            .setSizeOverLifetime(SizeOverLifetime.pulse());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 5.0f));
        system.addForce(ParticleForce.curlNoise(2.0f, 0.5f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    /**
     * 创建治愈效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createHealing() {
        ParticleSystem system = new ParticleSystem(500);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CYLINDER, 0.5f, 0.1f)
            .setRate(30)
            .setLifetime(1.5f, 2.5f)
            .setVelocity(0, 1.5f, 0)
            .setVelocityVariation(0.2f)
            .setSize(0.1f, 0.2f)
            .setColorOverLifetime(ColorOverLifetime.healing())
            .setSizeOverLifetime(SizeOverLifetime.pulse());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(1.5f, 0.3f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    /**
     * 创建召唤阵效果
     *
     * @param radius 半径
     * @return 粒子系统
     */
    public static ParticleSystem createSummoningCircle(float radius) {
        ParticleSystem system = new ParticleSystem(2000);

        // 外圈粒子
        ParticleEmitter outerEmitter = new ParticleEmitter().setShape(EmitterShape.RING, radius, radius * 0.95f)
            .setRate(100)
            .setLifetime(1.0f, 2.0f)
            .setVelocity(0, 0.3f, 0)
            .setSize(0.05f, 0.1f)
            .setColor(0.8f, 0.4f, 1.0f, 1.0f)
            .setSizeOverLifetime(SizeOverLifetime.pulse());

        // 内部光柱
        ParticleEmitter innerEmitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, radius * 0.3f)
            .setRate(50)
            .setLifetime(0.5f, 1.0f)
            .setVelocity(0, 3.0f, 0)
            .setSize(0.1f, 0.15f)
            .setColor(1.0f, 0.8f, 1.0f, 0.8f)
            .setSizeOverLifetime(SizeOverLifetime.shrink());

        system.addEmitter(outerEmitter);
        system.addEmitter(innerEmitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 2.0f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    // ==================== 自然效果 ====================

    /**
     * 创建下雨效果
     *
     * @param area      覆盖区域大小
     * @param intensity 强度 (0.5-2.0)
     * @return 粒子系统
     */
    public static ParticleSystem createRain(float area, float intensity) {
        ParticleSystem system = new ParticleSystem((int) (10000 * intensity));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.RECTANGLE, area, area)
            .setRate(500 * intensity)
            .setLifetime(1.5f, 2.5f)
            .setVelocity(0, -15f, 0)
            .setVelocityVariation(0.1f)
            .setSize(0.02f, 0.15f) // 细长雨滴
            .setColor(0.7f, 0.8f, 1.0f, 0.5f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.wind(0.5f, 0, 0, 0.5f, 0.3f));
        system.setCollision(CollisionMode.PLANE, CollisionResponse.KILL);
        system.setRenderMode(ParticleRenderer.RenderMode.STRETCHED_BILLBOARD);

        return system;
    }

    /**
     * 创建下雪效果
     *
     * @param area      覆盖区域大小
     * @param intensity 强度 (0.5-2.0)
     * @return 粒子系统
     */
    public static ParticleSystem createSnow(float area, float intensity) {
        ParticleSystem system = new ParticleSystem((int) (5000 * intensity));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.RECTANGLE, area, area)
            .setRate(100 * intensity)
            .setLifetime(5.0f, 10.0f)
            .setVelocity(0, -1.0f, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.05f, 0.15f)
            .setColor(1.0f, 1.0f, 1.0f, 0.8f)
            .setAngularVelocity(-1.0f, 1.0f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(0.5f, 0.5f));
        system.addForce(ParticleForce.wind(0.3f, 0, 0.2f, 0.3f, 0.2f));

        return system;
    }

    /**
     * 创建落叶效果
     *
     * @param area 覆盖区域大小
     * @return 粒子系统
     */
    public static ParticleSystem createFallingLeaves(float area) {
        ParticleSystem system = new ParticleSystem(500);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.RECTANGLE, area, area)
            .setRate(10)
            .setLifetime(8.0f, 15.0f)
            .setVelocity(0, -0.5f, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.1f, 0.2f)
            .setColor(0.8f, 0.6f, 0.2f, 1.0f)
            .setAngularVelocity(-2.0f, 2.0f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(0.3f, 1.0f));
        system.addForce(ParticleForce.wind(1.0f, 0, 0.5f, 0.5f, 0.3f));
        system.addForce(ParticleForce.drag(0.5f));

        return system;
    }

    // ==================== 工业效果 ====================

    /**
     * 创建蒸汽效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createSteam() {
        ParticleSystem system = new ParticleSystem(1000);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, 0.1f)
            .setRate(50)
            .setLifetime(1.0f, 2.5f)
            .setVelocity(0, 2.0f, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.1f, 0.3f)
            .setColor(1.0f, 1.0f, 1.0f, 0.4f)
            .setSizeOverLifetime(SizeOverLifetime.smoke());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(1.5f, 0.5f));
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);

        return system;
    }

    /**
     * 创建火花飞溅效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createSparks() {
        ParticleSystem system = new ParticleSystem(500);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CONE, 0.05f, 0.5f, 0.1f)
            .setRate(100)
            .setLifetime(0.3f, 0.8f)
            .setSpeed(5.0f)
            .setVelocityVariation(0.5f)
            .setSize(0.02f, 0.05f)
            .setColor(1.0f, 0.8f, 0.3f, 1.0f)
            .setSizeOverLifetime(SizeOverLifetime.spark());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.gravity(0, -9.8f, 0));
        system.addForce(ParticleForce.drag(0.5f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    // ==================== 光环效果 ====================

    /**
     * 创建光环效果
     *
     * @param color  颜色 (0xRRGGBB)
     * @param radius 半径
     * @return 粒子系统
     */
    public static ParticleSystem createAura(int color, float radius) {
        ParticleSystem system = new ParticleSystem(1000);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.SPHERE_SURFACE, radius)
            .setRate(50)
            .setLifetime(1.0f, 2.0f)
            .setSpeed(0)
            .setSize(0.05f, 0.1f)
            .setColor(r, g, b, 0.8f)
            .setColorOverLifetime(ColorOverLifetime.fadeOut())
            .setSizeOverLifetime(SizeOverLifetime.pulse());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 1.0f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    /**
     * 创建神圣光环效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createHolyAura() {
        ParticleSystem system = new ParticleSystem(1500);

        // 上升光点
        ParticleEmitter risingEmitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, 0.8f)
            .setRate(40)
            .setLifetime(2.0f, 3.0f)
            .setVelocity(0, 1.0f, 0)
            .setVelocityVariation(0.2f)
            .setSize(0.05f, 0.1f)
            .setColorOverLifetime(ColorOverLifetime.holy())
            .setSizeOverLifetime(SizeOverLifetime.pulse());

        // 环绕光点
        ParticleEmitter orbitEmitter = new ParticleEmitter().setShape(EmitterShape.RING, 1.0f, 0.9f)
            .setRate(30)
            .setLifetime(1.5f, 2.5f)
            .setSpeed(0)
            .setSize(0.08f, 0.12f)
            .setColor(1.0f, 0.95f, 0.7f, 1.0f)
            .setSizeOverLifetime(SizeOverLifetime.breathe());

        system.addEmitter(risingEmitter);
        system.addEmitter(orbitEmitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 2.0f));
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    // ==================== 水效果 ====================

    /**
     * 创建水花效果
     *
     * @param scale 规模
     * @return 粒子系统
     */
    public static ParticleSystem createWaterSplash(float scale) {
        ParticleSystem system = new ParticleSystem((int) (500 * scale));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.HEMISPHERE, 0.2f * scale)
            .setBurst((int) (100 * scale))
            .setLifetime(0.5f, 1.5f)
            .setSpeed(5.0f * scale)
            .setVelocityVariation(0.4f)
            .setSize(0.05f * scale, 0.15f * scale)
            .setColorOverLifetime(ColorOverLifetime.water())
            .setSizeOverLifetime(SizeOverLifetime.shrink());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.gravity(0, -9.8f, 0));
        system.addForce(ParticleForce.drag(0.3f));
        system.setLooping(false);
        system.setDuration(2.0f);

        return system;
    }

    /**
     * 创建气泡效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createBubbles() {
        ParticleSystem system = new ParticleSystem(200);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.CIRCLE, 0.3f)
            .setRate(20)
            .setLifetime(2.0f, 4.0f)
            .setVelocity(0, 1.0f, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.05f, 0.15f)
            .setColor(0.8f, 0.9f, 1.0f, 0.5f)
            .setSizeOverLifetime(SizeOverLifetime.bubble());

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(1.0f, 0.3f));

        return system;
    }

    // ==================== 电击效果 ====================

    /**
     * 创建电击效果
     *
     * @return 粒子系统
     */
    public static ParticleSystem createElectric() {
        ParticleSystem system = new ParticleSystem(500);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.3f)
            .setRate(150)
            .setLifetime(0.05f, 0.2f)
            .setSpeed(3.0f)
            .setVelocityVariation(0.8f)
            .setSize(0.02f, 0.05f)
            .setColorOverLifetime(ColorOverLifetime.lightning())
            .setSizeOverLifetime(SizeOverLifetime.flicker());

        system.addEmitter(emitter);
        system.setBlendMode(ParticleRenderer.BlendMode.ADDITIVE);

        return system;
    }

    // ==================== Mesh 粒子效果 ====================

    /**
     * 创建碎片爆炸效果（使用 Mesh 粒子）
     *
     * @param scale 规模
     * @return 粒子系统
     */
    public static ParticleSystem createDebrisExplosion(float scale) {
        ParticleSystem system = new ParticleSystem((int) (300 * scale));

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.2f * scale)
            .setBurst((int) (100 * scale))
            .setLifetime(2.0f, 4.0f)
            .setSpeed(8f * scale)
            .setVelocityVariation(0.5f)
            .setSize(0.08f * scale, 0.2f * scale)
            .setColor(0.6f, 0.5f, 0.4f, 1.0f)
            .setAngularVelocity(-5.0f, 5.0f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.gravity(0, -9.8f, 0));
        system.addForce(ParticleForce.drag(0.3f));
        system.setRenderMode(ParticleRenderer.RenderMode.MESH);
        system.setBuiltinMesh(ParticleRenderer.BuiltinMesh.TETRAHEDRON);
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);
        system.setLooping(false);
        system.setDuration(5.0f);

        return system;
    }

    /**
     * 创建立方体粒子效果
     *
     * @param color 颜色 (0xRRGGBB)
     * @return 粒子系统
     */
    public static ParticleSystem createCubeParticles(int color) {
        ParticleSystem system = new ParticleSystem(200);

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.SPHERE, 0.5f)
            .setRate(20)
            .setLifetime(3.0f, 5.0f)
            .setVelocity(0, 1.0f, 0)
            .setVelocityVariation(0.5f)
            .setSize(0.1f, 0.2f)
            .setColor(r, g, b, 1.0f)
            .setAngularVelocity(-2.0f, 2.0f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(1.0f, 0.5f));
        system.setRenderMode(ParticleRenderer.RenderMode.MESH);
        system.setBuiltinMesh(ParticleRenderer.BuiltinMesh.CUBE);

        return system;
    }

    /**
     * 创建八面体粒子效果（宝石风格）
     *
     * @return 粒子系统
     */
    public static ParticleSystem createGemParticles() {
        ParticleSystem system = new ParticleSystem(100);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.RING, 1.0f, 0.8f)
            .setRate(10)
            .setLifetime(4.0f, 6.0f)
            .setVelocity(0, 0.5f, 0)
            .setSize(0.15f, 0.25f)
            .setColorOverLifetime(ColorOverLifetime.rainbow())
            .setAngularVelocity(-1.0f, 1.0f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.vortexY(0, 0, 0, 1.5f));
        system.setRenderMode(ParticleRenderer.RenderMode.MESH);
        system.setBuiltinMesh(ParticleRenderer.BuiltinMesh.OCTAHEDRON);
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);

        return system;
    }

    /**
     * 创建二十面体粒子效果（能量球风格）
     *
     * @return 粒子系统
     */
    public static ParticleSystem createIcoParticles() {
        ParticleSystem system = new ParticleSystem(50);

        ParticleEmitter emitter = new ParticleEmitter().setShape(EmitterShape.POINT)
            .setRate(5)
            .setLifetime(5.0f, 8.0f)
            .setVelocity(0, 0.3f, 0)
            .setVelocityVariation(0.3f)
            .setSize(0.2f, 0.35f)
            .setColor(0.3f, 0.8f, 1.0f, 0.9f)
            .setAngularVelocity(-0.5f, 0.5f);

        system.addEmitter(emitter);
        system.addForce(ParticleForce.turbulence(0.5f, 0.3f));
        system.setRenderMode(ParticleRenderer.RenderMode.MESH);
        system.setBuiltinMesh(ParticleRenderer.BuiltinMesh.ICOSAHEDRON);
        system.setBlendMode(ParticleRenderer.BlendMode.ALPHA);

        return system;
    }
}
