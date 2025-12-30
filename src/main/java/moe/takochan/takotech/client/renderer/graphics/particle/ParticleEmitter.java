package moe.takochan.takotech.client.renderer.graphics.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粒子发射器。
 * 定义粒子的发射参数、初始属性和生命周期行为。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     ParticleEmitter emitter = new ParticleEmitter().setPosition(0, 64, 0)
 *         .setShape(EmitterShape.SPHERE, 1.0f)
 *         .setRate(100)
 *         .setLifetime(1.0f, 3.0f)
 *         .setVelocity(0, 5, 0)
 *         .setColor(1, 0.5f, 0, 1);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class ParticleEmitter {

    // ==================== 随机数生成 ====================

    private final Random random = new Random();

    // ==================== 位置和方向 ====================

    /** 发射器位置 X */
    private float posX = 0;

    /** 发射器位置 Y */
    private float posY = 0;

    /** 发射器位置 Z */
    private float posZ = 0;

    /** 发射器旋转（欧拉角，弧度） */
    private float rotX = 0, rotY = 0, rotZ = 0;

    // ==================== 发射形状 ====================

    /** 发射形状 */
    private EmitterShape shape = EmitterShape.POINT;

    /** 形状参数1（半径/宽度等） */
    private float shapeParam1 = 1.0f;

    /** 形状参数2（内半径/高度等） */
    private float shapeParam2 = 0;

    /** 形状参数3（角度/深度等） */
    private float shapeParam3 = 0;

    /** 是否从表面发射（而非体积内） */
    private boolean emitFromSurface = false;

    /** 是否沿表面法线方向发射 */
    private boolean emitAlongNormal = true;

    // ==================== 发射控制 ====================

    /** 每秒发射数量 */
    private float emissionRate = 10;

    /** 一次性发射数量（burst） */
    private int burstCount = 0;

    /** burst 触发间隔（秒），0 表示只触发一次 */
    private float burstInterval = 0;

    /** 上次发射时间累计 */
    private float emissionAccumulator = 0;

    /** 上次 burst 时间累计 */
    private float burstAccumulator = 0;

    /** 是否已触发初始 burst */
    private boolean initialBurstTriggered = false;

    /** 是否启用发射 */
    private boolean emitting = true;

    // ==================== 粒子生命周期 ====================

    /** 最小生命周期（秒） */
    private float lifetimeMin = 1.0f;

    /** 最大生命周期（秒） */
    private float lifetimeMax = 1.0f;

    // ==================== 初始速度 ====================

    /** 基础速度 X */
    private float velocityX = 0;

    /** 基础速度 Y */
    private float velocityY = 0;

    /** 基础速度 Z */
    private float velocityZ = 0;

    /** 速度大小（用于径向发射） */
    private float speed = 0;

    /** 速度随机变化范围 (0-1) */
    private float velocityVariation = 0;

    /** 继承发射器速度的比例 */
    private float inheritVelocity = 0;

    // ==================== 初始大小 ====================

    /** 最小初始大小 */
    private float sizeMin = 0.1f;

    /** 最大初始大小 */
    private float sizeMax = 0.1f;

    // ==================== 初始颜色 ====================

    /** 颜色 R */
    private float colorR = 1;

    /** 颜色 G */
    private float colorG = 1;

    /** 颜色 B */
    private float colorB = 1;

    /** 颜色 A */
    private float colorA = 1;

    /** 是否使用随机颜色 */
    private boolean randomColor = false;

    /** 颜色渐变（用于随机采样初始颜色） */
    private Gradient colorGradient = null;

    // ==================== 初始旋转 ====================

    /** 最小初始旋转（弧度） */
    private float rotationMin = 0;

    /** 最大初始旋转（弧度） */
    private float rotationMax = 0;

    /** 角速度范围 */
    private float angularVelocityMin = 0;
    private float angularVelocityMax = 0;

    // ==================== 生命周期曲线 ====================

    /** 颜色生命周期曲线 */
    private ColorOverLifetime colorOverLifetime = null;

    /** 大小生命周期曲线 */
    private SizeOverLifetime sizeOverLifetime = null;

    // ==================== 力场 ====================

    /** 力场列表 */
    private final List<ParticleForce> forces = new ArrayList<>();

    // ==================== 碰撞 ====================

    /** 碰撞模式 */
    private CollisionMode collisionMode = CollisionMode.NONE;

    /** 碰撞响应 */
    private CollisionResponse collisionResponse = CollisionResponse.KILL;

    /** 碰撞弹性系数 (0-1) */
    private float bounciness = 0.5f;

    /** 碰撞摩擦系数 */
    private float friction = 0.1f;

    /** 弹跳概率 (0-1)，1.0 = 总是弹跳 */
    private float bounceChance = 1.0f;

    /** 弹跳速度扩散角度 (度)，0 = 完美反射 */
    private float bounceSpread = 0.0f;

    /** 碰撞平面法线 X */
    private float collisionPlaneNX = 0.0f;

    /** 碰撞平面法线 Y */
    private float collisionPlaneNY = 1.0f;

    /** 碰撞平面法线 Z */
    private float collisionPlaneNZ = 0.0f;

    /** 碰撞平面距离 (沿法线方向到原点的距离) */
    private float collisionPlaneD = 0.0f;

    // ==================== 子发射器 ====================

    /** 子发射器列表 */
    private final List<SubEmitterEntry> subEmitters = new ArrayList<>();

    // ==================== 纹理/渲染 ====================

    /** 纹理 ID */
    private int textureId = 0;

    /** 粒子类型 ID（用于着色器） */
    private int particleType = 0;

    /** 是否启用纹理动画 */
    private boolean textureAnimationEnabled = false;

    /** 纹理动画帧数 X */
    private int textureTilesX = 1;

    /** 纹理动画帧数 Y */
    private int textureTilesY = 1;

    /** 纹理动画 FPS */
    private float textureAnimationFps = 10;

    // ==================== 构造函数 ====================

    /**
     * 创建默认发射器
     */
    public ParticleEmitter() {}

    // ==================== 位置和方向设置 ====================

    /**
     * 设置发射器位置
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return this
     */
    public ParticleEmitter setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        return this;
    }

    /**
     * 设置发射器旋转
     *
     * @param rx X 轴旋转（弧度）
     * @param ry Y 轴旋转（弧度）
     * @param rz Z 轴旋转（弧度）
     * @return this
     */
    public ParticleEmitter setRotation(float rx, float ry, float rz) {
        this.rotX = rx;
        this.rotY = ry;
        this.rotZ = rz;
        return this;
    }

    public float getPositionX() {
        return posX;
    }

    public float getPositionY() {
        return posY;
    }

    public float getPositionZ() {
        return posZ;
    }

    // ==================== 发射形状设置 ====================

    /**
     * 设置发射形状
     *
     * @param shape 形状类型
     * @return this
     */
    public ParticleEmitter setShape(EmitterShape shape) {
        this.shape = shape;
        return this;
    }

    /**
     * 设置发射形状和参数
     *
     * @param shape  形状类型
     * @param param1 参数1（半径、宽度等）
     * @return this
     */
    public ParticleEmitter setShape(EmitterShape shape, float param1) {
        this.shape = shape;
        this.shapeParam1 = param1;
        return this;
    }

    /**
     * 设置发射形状和参数
     *
     * @param shape  形状类型
     * @param param1 参数1
     * @param param2 参数2
     * @return this
     */
    public ParticleEmitter setShape(EmitterShape shape, float param1, float param2) {
        this.shape = shape;
        this.shapeParam1 = param1;
        this.shapeParam2 = param2;
        return this;
    }

    /**
     * 设置发射形状和参数
     *
     * @param shape  形状类型
     * @param param1 参数1
     * @param param2 参数2
     * @param param3 参数3
     * @return this
     */
    public ParticleEmitter setShape(EmitterShape shape, float param1, float param2, float param3) {
        this.shape = shape;
        this.shapeParam1 = param1;
        this.shapeParam2 = param2;
        this.shapeParam3 = param3;
        return this;
    }

    /**
     * 设置是否从表面发射
     *
     * @param fromSurface true 从表面发射，false 从体积内发射
     * @return this
     */
    public ParticleEmitter setEmitFromSurface(boolean fromSurface) {
        this.emitFromSurface = fromSurface;
        return this;
    }

    /**
     * 设置是否沿法线方向发射
     *
     * @param alongNormal true 沿法线方向
     * @return this
     */
    public ParticleEmitter setEmitAlongNormal(boolean alongNormal) {
        this.emitAlongNormal = alongNormal;
        return this;
    }

    public EmitterShape getShape() {
        return shape;
    }

    public float getShapeParam1() {
        return shapeParam1;
    }

    public float getShapeParam2() {
        return shapeParam2;
    }

    public float getShapeParam3() {
        return shapeParam3;
    }

    // ==================== 发射控制设置 ====================

    /**
     * 设置每秒发射数量
     *
     * @param rate 每秒发射数量
     * @return this
     */
    public ParticleEmitter setRate(float rate) {
        this.emissionRate = rate;
        return this;
    }

    /**
     * 设置一次性发射数量
     *
     * @param count 发射数量
     * @return this
     */
    public ParticleEmitter setBurst(int count) {
        this.burstCount = count;
        return this;
    }

    /**
     * 设置 burst 触发间隔
     *
     * @param interval 间隔（秒），0 表示只触发一次
     * @return this
     */
    public ParticleEmitter setBurstInterval(float interval) {
        this.burstInterval = interval;
        return this;
    }

    /**
     * 设置是否启用发射
     *
     * @param emitting true 启用发射
     * @return this
     */
    public ParticleEmitter setEmitting(boolean emitting) {
        this.emitting = emitting;
        return this;
    }

    public float getEmissionRate() {
        return emissionRate;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public boolean isEmitting() {
        return emitting;
    }

    // ==================== 生命周期设置 ====================

    /**
     * 设置固定生命周期
     *
     * @param lifetime 生命周期（秒）
     * @return this
     */
    public ParticleEmitter setLifetime(float lifetime) {
        this.lifetimeMin = lifetime;
        this.lifetimeMax = lifetime;
        return this;
    }

    /**
     * 设置生命周期范围
     *
     * @param min 最小生命周期（秒）
     * @param max 最大生命周期（秒）
     * @return this
     */
    public ParticleEmitter setLifetime(float min, float max) {
        this.lifetimeMin = min;
        this.lifetimeMax = max;
        return this;
    }

    public float getLifetimeMin() {
        return lifetimeMin;
    }

    public float getLifetimeMax() {
        return lifetimeMax;
    }

    // ==================== 速度设置 ====================

    /**
     * 设置基础速度
     *
     * @param x X 方向速度
     * @param y Y 方向速度
     * @param z Z 方向速度
     * @return this
     */
    public ParticleEmitter setVelocity(float x, float y, float z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
        return this;
    }

    /**
     * 设置径向速度大小
     *
     * @param speed 速度大小
     * @return this
     */
    public ParticleEmitter setSpeed(float speed) {
        this.speed = speed;
        return this;
    }

    /**
     * 设置速度随机变化
     *
     * @param variation 变化范围 (0-1)
     * @return this
     */
    public ParticleEmitter setVelocityVariation(float variation) {
        this.velocityVariation = Math.max(0, Math.min(1, variation));
        return this;
    }

    /**
     * 设置继承发射器速度的比例
     *
     * @param inherit 继承比例
     * @return this
     */
    public ParticleEmitter setInheritVelocity(float inherit) {
        this.inheritVelocity = inherit;
        return this;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public float getVelocityZ() {
        return velocityZ;
    }

    public float getSpeed() {
        return speed;
    }

    // ==================== 大小设置 ====================

    /**
     * 设置固定大小
     *
     * @param size 大小
     * @return this
     */
    public ParticleEmitter setSize(float size) {
        this.sizeMin = size;
        this.sizeMax = size;
        return this;
    }

    /**
     * 设置大小范围
     *
     * @param min 最小大小
     * @param max 最大大小
     * @return this
     */
    public ParticleEmitter setSize(float min, float max) {
        this.sizeMin = min;
        this.sizeMax = max;
        return this;
    }

    public float getSizeMin() {
        return sizeMin;
    }

    public float getSizeMax() {
        return sizeMax;
    }

    // ==================== 颜色设置 ====================

    /**
     * 设置固定颜色
     *
     * @param r 红色 (0-1)
     * @param g 绿色 (0-1)
     * @param b 蓝色 (0-1)
     * @param a 透明度 (0-1)
     * @return this
     */
    public ParticleEmitter setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
        this.randomColor = false;
        return this;
    }

    /**
     * 设置固定颜色（不透明）
     *
     * @param r 红色 (0-1)
     * @param g 绿色 (0-1)
     * @param b 蓝色 (0-1)
     * @return this
     */
    public ParticleEmitter setColor(float r, float g, float b) {
        return setColor(r, g, b, 1.0f);
    }

    /**
     * 设置颜色（整数格式）
     *
     * @param color 0xRRGGBB 或 0xAARRGGBB 格式
     * @return this
     */
    public ParticleEmitter setColor(int color) {
        if ((color & 0xFF000000) == 0) {
            // 无 alpha，设为不透明
            this.colorA = 1.0f;
            this.colorR = ((color >> 16) & 0xFF) / 255.0f;
            this.colorG = ((color >> 8) & 0xFF) / 255.0f;
            this.colorB = (color & 0xFF) / 255.0f;
        } else {
            this.colorA = ((color >> 24) & 0xFF) / 255.0f;
            this.colorR = ((color >> 16) & 0xFF) / 255.0f;
            this.colorG = ((color >> 8) & 0xFF) / 255.0f;
            this.colorB = (color & 0xFF) / 255.0f;
        }
        this.randomColor = false;
        return this;
    }

    /**
     * 设置随机颜色（从渐变采样）
     *
     * @param gradient 颜色渐变
     * @return this
     */
    public ParticleEmitter setRandomColor(Gradient gradient) {
        this.colorGradient = gradient;
        this.randomColor = true;
        return this;
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getColorA() {
        return colorA;
    }

    // ==================== 旋转设置 ====================

    /**
     * 设置固定初始旋转
     *
     * @param rotation 旋转角度（弧度）
     * @return this
     */
    public ParticleEmitter setParticleRotation(float rotation) {
        this.rotationMin = rotation;
        this.rotationMax = rotation;
        return this;
    }

    /**
     * 设置初始旋转范围
     *
     * @param min 最小旋转（弧度）
     * @param max 最大旋转（弧度）
     * @return this
     */
    public ParticleEmitter setParticleRotation(float min, float max) {
        this.rotationMin = min;
        this.rotationMax = max;
        return this;
    }

    /**
     * 设置角速度范围
     *
     * @param min 最小角速度（弧度/秒）
     * @param max 最大角速度（弧度/秒）
     * @return this
     */
    public ParticleEmitter setAngularVelocity(float min, float max) {
        this.angularVelocityMin = min;
        this.angularVelocityMax = max;
        return this;
    }

    // ==================== 生命周期曲线设置 ====================

    /**
     * 设置颜色生命周期曲线
     *
     * @param colorOverLifetime 颜色曲线
     * @return this
     */
    public ParticleEmitter setColorOverLifetime(ColorOverLifetime colorOverLifetime) {
        this.colorOverLifetime = colorOverLifetime;
        return this;
    }

    /**
     * 设置大小生命周期曲线
     *
     * @param sizeOverLifetime 大小曲线
     * @return this
     */
    public ParticleEmitter setSizeOverLifetime(SizeOverLifetime sizeOverLifetime) {
        this.sizeOverLifetime = sizeOverLifetime;
        return this;
    }

    public ColorOverLifetime getColorOverLifetime() {
        return colorOverLifetime;
    }

    public SizeOverLifetime getSizeOverLifetime() {
        return sizeOverLifetime;
    }

    // ==================== 力场设置 ====================

    /**
     * 添加力场
     *
     * @param force 力场
     * @return this
     */
    public ParticleEmitter addForce(ParticleForce force) {
        if (force != null) {
            forces.add(force);
        }
        return this;
    }

    /**
     * 添加重力
     *
     * @param gravity 重力加速度（通常为负值表示向下）
     * @return this
     */
    public ParticleEmitter addGravity(float gravity) {
        forces.add(ParticleForce.gravity(0, gravity, 0));
        return this;
    }

    /**
     * 清除所有力场
     *
     * @return this
     */
    public ParticleEmitter clearForces() {
        forces.clear();
        return this;
    }

    public List<ParticleForce> getForces() {
        return forces;
    }

    // ==================== 碰撞设置 ====================

    /**
     * 设置碰撞模式
     *
     * @param mode 碰撞模式
     * @return this
     */
    public ParticleEmitter setCollisionMode(CollisionMode mode) {
        this.collisionMode = mode;
        return this;
    }

    /**
     * 设置碰撞响应
     *
     * @param response 碰撞响应类型
     * @return this
     */
    public ParticleEmitter setCollisionResponse(CollisionResponse response) {
        this.collisionResponse = response;
        return this;
    }

    /**
     * 设置碰撞参数
     *
     * @param mode       碰撞模式
     * @param response   碰撞响应
     * @param bounciness 弹性系数 (0-1)
     * @param friction   摩擦系数
     * @return this
     */
    public ParticleEmitter setCollision(CollisionMode mode, CollisionResponse response, float bounciness,
        float friction) {
        this.collisionMode = mode;
        this.collisionResponse = response;
        this.bounciness = bounciness;
        this.friction = friction;
        return this;
    }

    public CollisionMode getCollisionMode() {
        return collisionMode;
    }

    public CollisionResponse getCollisionResponse() {
        return collisionResponse;
    }

    public float getBounciness() {
        return bounciness;
    }

    public float getFriction() {
        return friction;
    }

    public float getBounceChance() {
        return bounceChance;
    }

    public float getBounceSpread() {
        return bounceSpread;
    }

    /**
     * 设置弹跳概率
     *
     * @param chance 弹跳概率 (0-1)，1.0 = 总是弹跳，0.0 = 从不弹跳
     * @return this
     */
    public ParticleEmitter setBounceChance(float chance) {
        this.bounceChance = Math.max(0, Math.min(1, chance));
        return this;
    }

    /**
     * 设置弹跳速度扩散
     *
     * @param spreadDegrees 扩散角度 (度)，0 = 完美反射，越大随机偏移越多
     * @return this
     */
    public ParticleEmitter setBounceSpread(float spreadDegrees) {
        this.bounceSpread = Math.max(0, spreadDegrees);
        return this;
    }

    /**
     * 设置碰撞平面
     *
     * @param nx 平面法线 X
     * @param ny 平面法线 Y
     * @param nz 平面法线 Z
     * @param d  平面距离（沿法线方向到原点的距离）
     * @return this
     */
    public ParticleEmitter setCollisionPlane(float nx, float ny, float nz, float d) {
        // 归一化法线
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001f) {
            this.collisionPlaneNX = nx / len;
            this.collisionPlaneNY = ny / len;
            this.collisionPlaneNZ = nz / len;
        } else {
            // 默认向上
            this.collisionPlaneNX = 0;
            this.collisionPlaneNY = 1;
            this.collisionPlaneNZ = 0;
        }
        this.collisionPlaneD = d;
        return this;
    }

    public float getCollisionPlaneNX() {
        return collisionPlaneNX;
    }

    public float getCollisionPlaneNY() {
        return collisionPlaneNY;
    }

    public float getCollisionPlaneNZ() {
        return collisionPlaneNZ;
    }

    public float getCollisionPlaneD() {
        return collisionPlaneD;
    }

    /**
     * 设置完整碰撞参数（包含弹跳概率和扩散）
     *
     * @param mode         碰撞模式
     * @param response     碰撞响应
     * @param bounciness   弹性系数 (0-1)
     * @param friction     摩擦系数
     * @param bounceChance 弹跳概率 (0-1)
     * @param bounceSpread 弹跳扩散角度 (度)
     * @return this
     */
    public ParticleEmitter setCollision(CollisionMode mode, CollisionResponse response, float bounciness,
        float friction, float bounceChance, float bounceSpread) {
        this.collisionMode = mode;
        this.collisionResponse = response;
        this.bounciness = bounciness;
        this.friction = friction;
        this.bounceChance = bounceChance;
        this.bounceSpread = bounceSpread;
        return this;
    }

    // ==================== 子发射器设置 ====================

    /**
     * 添加子发射器（粒子死亡时触发）
     *
     * @param emitter   子发射器
     * @param emitCount 每次触发发射数量
     * @return this
     */
    public ParticleEmitter addSubEmitterOnDeath(ParticleEmitter emitter, int emitCount) {
        subEmitters.add(new SubEmitterEntry(SubEmitterTrigger.DEATH, emitter, emitCount));
        return this;
    }

    /**
     * 添加子发射器（粒子碰撞时触发）
     *
     * @param emitter   子发射器
     * @param emitCount 每次触发发射数量
     * @return this
     */
    public ParticleEmitter addSubEmitterOnCollision(ParticleEmitter emitter, int emitCount) {
        subEmitters.add(new SubEmitterEntry(SubEmitterTrigger.COLLISION, emitter, emitCount));
        return this;
    }

    public List<SubEmitterEntry> getSubEmitters() {
        return subEmitters;
    }

    // ==================== 纹理/渲染设置 ====================

    /**
     * 设置纹理 ID
     *
     * @param textureId OpenGL 纹理 ID
     * @return this
     */
    public ParticleEmitter setTexture(int textureId) {
        this.textureId = textureId;
        return this;
    }

    /**
     * 设置粒子类型 ID
     *
     * @param type 类型 ID（用于着色器区分渲染方式）
     * @return this
     */
    public ParticleEmitter setParticleType(int type) {
        this.particleType = type;
        return this;
    }

    /**
     * 设置纹理动画
     *
     * @param tilesX X 方向帧数
     * @param tilesY Y 方向帧数
     * @param fps    播放速度
     * @return this
     */
    public ParticleEmitter setTextureAnimation(int tilesX, int tilesY, float fps) {
        this.textureAnimationEnabled = true;
        this.textureTilesX = tilesX;
        this.textureTilesY = tilesY;
        this.textureAnimationFps = fps;
        return this;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getParticleType() {
        return particleType;
    }

    // ==================== 粒子生成 ====================

    /**
     * 计算本帧需要发射的粒子数量
     *
     * @param deltaTime 时间增量（秒）
     * @return 需要发射的粒子数量
     */
    public int calculateEmitCount(float deltaTime) {
        if (!emitting) {
            return 0;
        }

        int count = 0;

        // 处理 burst
        if (burstCount > 0) {
            if (!initialBurstTriggered) {
                count += burstCount;
                initialBurstTriggered = true;
                burstAccumulator = 0;
            } else if (burstInterval > 0) {
                burstAccumulator += deltaTime;
                while (burstAccumulator >= burstInterval) {
                    burstAccumulator -= burstInterval;
                    count += burstCount;
                }
            }
        }

        // 处理持续发射
        if (emissionRate > 0) {
            emissionAccumulator += deltaTime * emissionRate;
            int rateCount = (int) emissionAccumulator;
            emissionAccumulator -= rateCount;
            count += rateCount;
        }

        return count;
    }

    /**
     * 生成单个粒子数据
     *
     * @return 16 个 float 的粒子数据 [pos(4), vel(4), color(4), params(4)]
     */
    public float[] generateParticle() {
        float[] particle = new float[16];

        // 生成位置
        float[] position = generatePosition();
        particle[0] = position[0];
        particle[1] = position[1];
        particle[2] = position[2];

        // 生成生命周期
        float lifetime = randomRange(lifetimeMin, lifetimeMax);
        particle[3] = lifetime; // position.w = 当前生命周期

        // 生成速度
        float[] velocity = generateVelocity(position);
        particle[4] = velocity[0];
        particle[5] = velocity[1];
        particle[6] = velocity[2];
        particle[7] = lifetime; // velocity.w = 最大生命周期

        // 生成颜色
        float[] color = generateColor();
        particle[8] = color[0];
        particle[9] = color[1];
        particle[10] = color[2];
        particle[11] = color[3];

        // 生成参数
        particle[12] = randomRange(sizeMin, sizeMax); // params.x = 大小
        particle[13] = randomRange(rotationMin, rotationMax); // params.y = 旋转
        particle[14] = particleType; // params.z = 类型
        particle[15] = randomRange(angularVelocityMin, angularVelocityMax); // params.w = 角速度

        return particle;
    }

    /**
     * 批量生成粒子数据
     *
     * @param count 粒子数量
     * @return 粒子数据数组
     */
    public float[] generateParticles(int count) {
        float[] particles = new float[count * 16];
        for (int i = 0; i < count; i++) {
            float[] p = generateParticle();
            System.arraycopy(p, 0, particles, i * 16, 16);
        }
        return particles;
    }

    /**
     * 重置发射状态
     */
    public void reset() {
        emissionAccumulator = 0;
        burstAccumulator = 0;
        initialBurstTriggered = false;
    }

    // ==================== 内部方法 ====================

    /**
     * 根据形状生成位置
     */
    private float[] generatePosition() {
        float[] pos = new float[3];
        float[] localPos;

        switch (shape) {
            case POINT:
                localPos = new float[] { 0, 0, 0 };
                break;

            case SPHERE:
                if (emitFromSurface) {
                    localPos = randomOnSphere(shapeParam1);
                } else {
                    localPos = randomInSphere(shapeParam1);
                }
                break;

            case SPHERE_SURFACE:
                localPos = randomOnSphere(shapeParam1);
                break;

            case HEMISPHERE:
                localPos = randomInHemisphere(shapeParam1);
                break;

            case CIRCLE:
                localPos = randomInCircle(shapeParam1);
                break;

            case RING:
                localPos = randomOnRing(shapeParam1, shapeParam2);
                break;

            case CONE:
                localPos = randomInCone(shapeParam1, shapeParam2, shapeParam3);
                break;

            case BOX:
                if (emitFromSurface) {
                    localPos = randomOnBox(shapeParam1, shapeParam2, shapeParam3);
                } else {
                    localPos = randomInBox(shapeParam1, shapeParam2, shapeParam3);
                }
                break;

            case BOX_EDGE:
                localPos = randomOnBoxEdge(shapeParam1, shapeParam2, shapeParam3);
                break;

            case CYLINDER:
                localPos = randomInCylinder(shapeParam1, shapeParam2);
                break;

            case LINE:
                localPos = randomOnLine(shapeParam1);
                break;

            case RECTANGLE:
                localPos = randomInRectangle(shapeParam1, shapeParam2);
                break;

            default:
                localPos = new float[] { 0, 0, 0 };
        }

        // 应用发射器旋转和位置
        pos[0] = localPos[0] + posX;
        pos[1] = localPos[1] + posY;
        pos[2] = localPos[2] + posZ;

        return pos;
    }

    /**
     * 生成速度
     */
    private float[] generateVelocity(float[] position) {
        float vx = velocityX;
        float vy = velocityY;
        float vz = velocityZ;

        // 如果设置了径向速度
        if (speed != 0 && emitAlongNormal) {
            // 计算从发射器中心到粒子位置的方向
            float dx = position[0] - posX;
            float dy = position[1] - posY;
            float dz = position[2] - posZ;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (len > 0.0001f) {
                dx /= len;
                dy /= len;
                dz /= len;
            } else {
                // 点发射，使用随机方向
                float[] dir = randomDirection();
                dx = dir[0];
                dy = dir[1];
                dz = dir[2];
            }

            vx += dx * speed;
            vy += dy * speed;
            vz += dz * speed;
        }

        // 应用速度随机变化
        if (velocityVariation > 0) {
            float var = 1.0f - velocityVariation + random.nextFloat() * velocityVariation * 2;
            vx *= var;
            vy *= var;
            vz *= var;
        }

        return new float[] { vx, vy, vz };
    }

    /**
     * 生成颜色
     */
    private float[] generateColor() {
        if (randomColor && colorGradient != null) {
            return colorGradient.evaluate(random.nextFloat());
        }
        return new float[] { colorR, colorG, colorB, colorA };
    }

    // ==================== 形状生成辅助方法 ====================

    private float[] randomInSphere(float radius) {
        // 使用拒绝采样生成均匀分布
        float x, y, z;
        do {
            x = random.nextFloat() * 2 - 1;
            y = random.nextFloat() * 2 - 1;
            z = random.nextFloat() * 2 - 1;
        } while (x * x + y * y + z * z > 1);
        return new float[] { x * radius, y * radius, z * radius };
    }

    private float[] randomOnSphere(float radius) {
        // 使用球面坐标
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        float phi = (float) Math.acos(2 * random.nextFloat() - 1);
        float sinPhi = (float) Math.sin(phi);
        return new float[] { radius * sinPhi * (float) Math.cos(theta), radius * (float) Math.cos(phi),
            radius * sinPhi * (float) Math.sin(theta) };
    }

    private float[] randomInHemisphere(float radius) {
        float[] pos = randomInSphere(radius);
        pos[1] = Math.abs(pos[1]); // 只取上半球
        return pos;
    }

    private float[] randomInCircle(float radius) {
        float r = radius * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), 0, r * (float) Math.sin(theta) };
    }

    private float[] randomOnRing(float outerRadius, float innerRadius) {
        float r = innerRadius + (outerRadius - innerRadius) * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), 0, r * (float) Math.sin(theta) };
    }

    private float[] randomInCone(float radius, float angle, float height) {
        float h = random.nextFloat() * height;
        float r = (h / height) * radius * (float) Math.tan(angle);
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        return new float[] { r * (float) Math.cos(theta), h, r * (float) Math.sin(theta) };
    }

    private float[] randomInBox(float width, float height, float depth) {
        return new float[] { (random.nextFloat() - 0.5f) * width, (random.nextFloat() - 0.5f) * height,
            (random.nextFloat() - 0.5f) * depth };
    }

    private float[] randomOnBox(float width, float height, float depth) {
        // 随机选择一个面
        int face = random.nextInt(6);
        float x = (random.nextFloat() - 0.5f) * width;
        float y = (random.nextFloat() - 0.5f) * height;
        float z = (random.nextFloat() - 0.5f) * depth;

        switch (face) {
            case 0:
                x = width / 2;
                break;
            case 1:
                x = -width / 2;
                break;
            case 2:
                y = height / 2;
                break;
            case 3:
                y = -height / 2;
                break;
            case 4:
                z = depth / 2;
                break;
            case 5:
                z = -depth / 2;
                break;
        }
        return new float[] { x, y, z };
    }

    private float[] randomOnBoxEdge(float width, float height, float depth) {
        // 随机选择一条边
        int edge = random.nextInt(12);
        float t = random.nextFloat();
        float hw = width / 2, hh = height / 2, hd = depth / 2;

        switch (edge) {
            case 0:
                return new float[] { -hw + t * width, -hh, -hd };
            case 1:
                return new float[] { -hw + t * width, -hh, hd };
            case 2:
                return new float[] { -hw + t * width, hh, -hd };
            case 3:
                return new float[] { -hw + t * width, hh, hd };
            case 4:
                return new float[] { -hw, -hh + t * height, -hd };
            case 5:
                return new float[] { hw, -hh + t * height, -hd };
            case 6:
                return new float[] { -hw, -hh + t * height, hd };
            case 7:
                return new float[] { hw, -hh + t * height, hd };
            case 8:
                return new float[] { -hw, -hh, -hd + t * depth };
            case 9:
                return new float[] { hw, -hh, -hd + t * depth };
            case 10:
                return new float[] { -hw, hh, -hd + t * depth };
            case 11:
                return new float[] { hw, hh, -hd + t * depth };
            default:
                return new float[] { 0, 0, 0 };
        }
    }

    private float[] randomInCylinder(float radius, float height) {
        float r = radius * (float) Math.sqrt(random.nextFloat());
        float theta = random.nextFloat() * 2 * (float) Math.PI;
        float y = (random.nextFloat() - 0.5f) * height;
        return new float[] { r * (float) Math.cos(theta), y, r * (float) Math.sin(theta) };
    }

    private float[] randomOnLine(float length) {
        return new float[] { (random.nextFloat() - 0.5f) * length, 0, 0 };
    }

    private float[] randomInRectangle(float width, float height) {
        return new float[] { (random.nextFloat() - 0.5f) * width, 0, (random.nextFloat() - 0.5f) * height };
    }

    private float[] randomDirection() {
        return randomOnSphere(1.0f);
    }

    private float randomRange(float min, float max) {
        if (min == max) return min;
        return min + random.nextFloat() * (max - min);
    }

    // ==================== 子发射器条目 ====================

    /**
     * 子发射器触发类型
     */
    public enum SubEmitterTrigger {
        BIRTH,
        DEATH,
        COLLISION,
        MANUAL
    }

    /**
     * 子发射器条目
     */
    public static class SubEmitterEntry {

        public final SubEmitterTrigger trigger;
        public final ParticleEmitter emitter;
        public final int emitCount;

        public SubEmitterEntry(SubEmitterTrigger trigger, ParticleEmitter emitter, int emitCount) {
            this.trigger = trigger;
            this.emitter = emitter;
            this.emitCount = emitCount;
        }
    }
}
