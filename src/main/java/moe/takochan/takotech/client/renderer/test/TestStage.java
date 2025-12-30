package moe.takochan.takotech.client.renderer.test;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 渲染框架测试阶段枚举。
 * 定义所有测试阶段及其描述信息。
 *
 * 按键: U=开关测试, I=下一阶段, O=上一阶段, P=自动推进
 */
@SideOnly(Side.CLIENT)
public enum TestStage {

    // ==================== 环境与基础 ====================

    ENVIRONMENT("Environment Check", "环境检测", "检测 OpenGL 版本、扩展支持、GPU 能力", "HUD 显示所有检测项状态", false, true),

    SHADER_LOAD("Shader Load", "Shader 加载", "测试所有 ShaderType 的加载与验证",
        "HUD 显示每个 Shader 状态: SIMPLE/BLUR/GUI_COLOR/UBER/WORLD_3D/LINE", false, false),

    // ==================== 批处理渲染 ====================

    BATCH_SPRITE("SpriteBatch", "SpriteBatch 2D", "测试 SpriteBatch 2D 矩形渲染", "屏幕左上角出现 7 个彩色方块动画", false, false),

    BATCH_WORLD3D("World3DBatch", "World3DBatch 3D", "测试 World3DBatch: drawLine, drawCircleXZ",
        "玩家脚下出现 3 层彩色魔法阵 + XYZ 坐标轴", true, false),

    // ==================== ECS 核心 ====================

    ECS_BASIC("ECS Basic", "ECS 基础", "测试 Entity/Component/World 创建与管理", "HUD 显示: Entity 数量, Component 数量", false,
        false),

    // ==================== Transform ====================

    TRANSFORM_BASIC("Transform Basic", "Transform 基础", "测试 TransformComponent: position/rotation/scale",
        "玩家前方 5 格出现一个旋转的立方体", true, false),

    // ==================== Mesh 渲染 ====================

    MESH_STATIC("StaticMesh", "StaticMesh", "测试 StaticMesh: Cube/Sphere/Plane", "玩家前方出现 3 个彩色几何体 (红立方体/绿球体/蓝平面)", true,
        false),

    MESH_CUSTOM("CustomMesh", "自定义 Mesh", "测试自定义几何体: Pyramid/Torus", "新增 2 个几何体 (金色金字塔/紫色环面)", true, false),

    MESH_DYNAMIC("DynamicMesh", "动态 Mesh", "测试 DynamicMesh 实时顶点更新", "出现一个波浪起伏的彩色网格平面", true, false),

    // ==================== 模型加载 ====================

    MODEL_MC("MC Model", "MC 模型加载", "测试 MCModelLoader.loadBlockModel()", "显示一个 MC 方块模型", true, false),

    // ==================== Material ====================

    MATERIAL_COLOR("Material Color", "材质颜色", "测试 Material.color() 和透明度", "物体显示不同颜色，部分半透明", true, false),

    MATERIAL_PBR("PBR Material", "PBR 材质", "测试 PBRMaterial: 金属度/粗糙度预设", "显示金/银/铜材质球体", true, false),

    // ==================== Transform 高级 ====================

    TRANSFORM_HIERARCHY("Transform Hierarchy", "Transform 层级", "测试父子实体变换传递", "白色立方体(父)旋转，4 个彩色球体(子)围绕其公转", true, false),

    ENTITY_CONTROL("Entity Control", "Entity 控制", "测试 Entity.setEnabled() 启用/禁用", "每 2 秒切换: 物体显示/隐藏", true, false),

    // ==================== Camera ====================

    CAMERA_BASIC("Camera", "Camera 系统", "测试 Camera: perspective/lookAt/syncFromMC",
        "HUD 显示 WorldCamera 位置/目标/FOV，验证 API 正常", true, false),

    FRUSTUM_CULL("Frustum Culling", "Frustum 剔除", "测试 Frustum.intersectsAABB()", "HUD 显示: 可见/剔除物体数量", true, false),

    // ==================== LOD ====================

    LOD_GROUP("LOD Group", "LOD 系统", "测试 LODGroup 距离切换", "靠近/远离物体时细节变化 (HUD 显示当前 LOD 级别)", true, false),

    // ==================== LineRenderer ====================

    LINE_BASIC("LineRenderer Basic", "线条渲染基础", "测试 LineRendererComponent: 点/宽度/颜色渐变", "玩家头顶出现彩虹色螺旋线", true, false),

    LINE_LOOP("LineRenderer Loop", "线条闭合", "测试 LineRendererComponent.setLoop(true)", "出现闭合的彩色圆形线", true, false),

    // ==================== InstancedBatch ====================

    INSTANCED_BATCH("InstancedBatch", "GPU 实例化", "测试 InstancedBatch GPU 实例化渲染", "前方出现 8x8 彩虹色立方体网格", true, false),

    // ==================== 粒子系统 ====================

    PARTICLE_SHOWCASE("Particle Showcase", "粒子展示", "10种粒子效果沿S形路径排列", "火焰/烟雾/形状/力场/网格粒子依次展示，带悬浮标签", true, false),

    PARTICLE_BOUNCE("Particle Bounce", "粒子弹跳", "测试弹跳概率和扩散角度", "3组粒子: 100%弹跳/50%弹跳+扩散/20%弹跳+大扩散", true, false),

    // ==================== 后处理 ====================

    BLOOM_TEST("Glow Test", "发光效果测试", "测试物体级发光效果 (多 pass 渲染)", "显示 3 个发光物体: 白色立方体/黄色球体/青色长方体", true, false),

    // ==================== 综合测试 ====================

    COMPREHENSIVE("Comprehensive", "综合测试", "所有功能同时运行", "完整场景: Mesh + 粒子 + 线条 + 后处理", true, false),

    // ==================== 完成 ====================

    COMPLETED("Completed", "测试完成", "所有测试阶段已完成", "查看日志文件获取详细测试报告", false, false);

    // ==================== 字段 ====================

    private final String nameEn;
    private final String nameZh;
    private final String description;
    private final String expectedResult;
    private final boolean requires3DRendering;
    private final boolean autoAdvance;

    TestStage(String nameEn, String nameZh, String description, String expectedResult, boolean requires3DRendering,
        boolean autoAdvance) {
        this.nameEn = nameEn;
        this.nameZh = nameZh;
        this.description = description;
        this.expectedResult = expectedResult;
        this.requires3DRendering = requires3DRendering;
        this.autoAdvance = autoAdvance;
    }

    // ==================== Getters ====================

    public String getNameEn() {
        return nameEn;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getDescription() {
        return description;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public boolean requires3DRendering() {
        return requires3DRendering;
    }

    public boolean isAutoAdvance() {
        return autoAdvance;
    }

    /**
     * 获取阶段编号 (1-based)
     */
    public int getStageNumber() {
        return ordinal() + 1;
    }

    /**
     * 获取总阶段数
     */
    public static int getTotalStages() {
        return values().length;
    }

    /**
     * 获取下一阶段
     */
    public TestStage next() {
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return COMPLETED;
        }
        return values()[nextOrdinal];
    }

    /**
     * 获取上一阶段
     */
    public TestStage previous() {
        int prevOrdinal = ordinal() - 1;
        if (prevOrdinal < 0) {
            return ENVIRONMENT;
        }
        return values()[prevOrdinal];
    }

    /**
     * 是否是最后一个阶段
     */
    public boolean isLast() {
        return this == COMPLETED;
    }

    /**
     * 是否是第一个阶段
     */
    public boolean isFirst() {
        return this == ENVIRONMENT;
    }

    /**
     * 根据索引获取阶段
     */
    public static TestStage fromIndex(int index) {
        if (index < 0) return ENVIRONMENT;
        if (index >= values().length) return COMPLETED;
        return values()[index];
    }
}
