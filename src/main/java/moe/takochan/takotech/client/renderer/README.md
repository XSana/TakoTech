# TakoTech 渲染框架文档

## 概述

TakoTech 渲染框架是为 Minecraft 1.7.10 设计的现代化渲染系统，提供基于 Shader 的高效 2D/3D 渲染能力。框架主要使用 OpenGL 3.3+ (GLSL 3.30) 特性，部分着色器提供 GLSL 1.20 兼容模式。可与 Angelica/Sodium 等优化 mod 共存。

## 目录

- [设计原则](#设计原则)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [核心组件](#核心组件)
- [使用指南](#使用指南)
- [注意事项](#注意事项)
- [目录结构](#目录结构)

---

## 设计原则

### 1. GL 状态隔离
**核心原则：不信任前置 GL 状态。**

MC 1.7.10 的渲染环境复杂，多个 mod 可能修改 GL 状态。框架要求：
- 渲染前保存所有 GL 状态
- 显式设置所需的 GL 状态
- 渲染后恢复 GL 状态

### 2. 延迟初始化
VAO/VBO 等 GL 资源采用延迟初始化，避免在 FML 初始化阶段创建的资源被其他 mod 污染。

### 3. 资源自动管理
所有 GL 资源类实现 `AutoCloseable`，支持 try-with-resources 语法自动释放。

### 4. OpenGL 版本支持
框架支持 OpenGL 3.3 至 4.5，根据着色器类型使用不同的 GLSL 版本：

**GLSL 3.30 (OpenGL 3.3+)** - 主要着色器：
- `simple`, `blur`, `gui_color`, `uber`, `pbr`
- 使用现代语法：`in` / `out`、`texture()`、`layout` 限定符

**GLSL 1.20 (OpenGL 2.1)** - 兼容着色器：
- `world3d` - 用于最大兼容性的 3D 世界渲染
- 使用传统语法：`attribute` / `varying`、`texture2D()`、`gl_FragColor`

**硬件要求**：
- 最低：OpenGL 2.1 + ARB_shader_objects（仅 world3d 可用）
- 推荐：OpenGL 3.3+（全部功能可用）
- 完整支持：OpenGL 4.5（可使用 UBO、实例化渲染等高级特性）

### 5. 批量渲染优化
通过 `SpriteBatch` 和 `World3DBatch` 合并绘制调用，减少 Draw Call 开销。

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      RenderSystem                           │
│  (入口点：初始化、资源管理、全局服务)                          │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  SpriteBatch  │    │ World3DBatch  │    │    Camera     │
│  (2D 批量渲染) │    │ (3D 批量渲染) │    │  (相机系统)   │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Graphics Layer                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │  Mesh   │ │ Shader  │ │Material │ │ Scene   │          │
│  │ System  │ │ System  │ │ System  │ │ Graph   │          │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Utility Layer                           │
│  ┌───────────────┐              ┌───────────────┐          │
│  │GLStateManager │              │MCRenderHelper │          │
│  │ (GL 状态管理)  │              │ (MC 坐标转换) │          │
│  └───────────────┘              └───────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 1. 初始化（在 ClientProxy 中）

```java
@Override
public void preInit(FMLPreInitializationEvent event) {
    super.preInit(event);
    RenderSystem.init();
}

@Override
public void postInit(FMLPostInitializationEvent event) {
    super.postInit(event);
    // 注册关闭钩子
    Runtime.getRuntime().addShutdownHook(new Thread(RenderSystem::shutdown));
}
```

### 2. HUD 渲染（RenderGameOverlayEvent）

```java
@SubscribeEvent
public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
    if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
    if (!RenderSystem.isShaderSupported()) return;

    // 保存 GL 状态
    try (GLStateManager glState = GLStateManager.save()) {
        SpriteBatch batch = RenderSystem.getSpriteBatch();
        batch.setProjectionOrtho(screenWidth, screenHeight);
        batch.begin();

        // 绘制红色矩形
        batch.drawRect(10, 10, 100, 50, 1.0f, 0.0f, 0.0f, 0.8f);

        batch.end();
    }
}
```

### 3. 世界渲染（RenderWorldLastEvent）

```java
@SubscribeEvent
public void onRenderWorld(RenderWorldLastEvent event) {
    if (!RenderSystem.isShaderSupported()) return;

    try (GLStateManager glState = GLStateManager.save()) {
        // 获取相机位置
        double camX = MCRenderHelper.getCameraX();
        double camY = MCRenderHelper.getCameraY();
        double camZ = MCRenderHelper.getCameraZ();

        // 计算方块的相对坐标
        double rx = blockX - camX;
        double ry = blockY - camY;
        double rz = blockZ - camZ;

        World3DBatch batch = RenderSystem.getWorld3DBatch();
        batch.begin(GL11.GL_LINES);

        // 绘制方块线框
        batch.drawWireBox(rx, ry, rz, 1, 1, 1, 1.0f, 1.0f, 0.0f, 0.8f);

        batch.end();
    }
}
```

---

## 核心组件

### RenderSystem

渲染系统入口点，负责初始化和管理全局渲染资源。

| 方法 | 说明 |
|------|------|
| `init()` | 初始化渲染系统 |
| `shutdown()` | 关闭并释放资源 |
| `isShaderSupported()` | 检查 Shader 支持 |
| `getSpriteBatch()` | 获取共享的 2D 批量渲染器 |
| `getWorld3DBatch()` | 获取共享的 3D 批量渲染器 |
| `getGuiCamera()` | 获取 GUI 相机 |
| `getWorldCamera()` | 获取世界相机 |

### SpriteBatch

2D 批量渲染器，用于 HUD、GUI 等场景。

```java
// 基本用法
batch.setProjectionOrtho(width, height);
batch.begin();
batch.drawRect(x, y, w, h, r, g, b, a);      // 矩形
batch.drawQuad(x1,y1, x2,y2, x3,y3, x4,y4, r,g,b,a);  // 四边形
batch.end();
```

### World3DBatch

3D 世界批量渲染器，用于在 MC 世界中渲染图元。

```java
// 线段模式
batch.begin(GL11.GL_LINES);
batch.drawLine(x1, y1, z1, x2, y2, z2, r, g, b, a);
batch.drawWireBox(x, y, z, w, h, d, r, g, b, a);
batch.drawCircleXZ(cx, y, cz, radius, segments, r, g, b, a);
batch.end();

// 三角形模式
batch.begin(GL11.GL_TRIANGLES);
batch.drawDiamond(x, y, z, size, r, g, b, a);
batch.drawBlockGlow(x, y, z, r, g, b, a);
batch.end();
```

### GLStateManager

GL 状态保存/恢复管理器。

```java
// 方式 1：try-with-resources
try (GLStateManager state = GLStateManager.save()) {
    // 渲染代码
}

// 方式 2：手动管理
GLStateManager state = new GLStateManager();
state.saveState();
try {
    // 渲染代码
} finally {
    state.restoreState();
}

// 预设 GL 状态
GLStateManager.setupForHUD();      // 2D HUD 渲染
GLStateManager.setupForWorld3D();  // 3D 世界渲染
GLStateManager.setupForText();     // MC 文字渲染
```

### MCRenderHelper

MC 坐标转换和实体位置工具。

```java
// 相机位置
double camX = MCRenderHelper.getCameraX();
double camY = MCRenderHelper.getCameraY();
double camZ = MCRenderHelper.getCameraZ();

// 世界坐标 -> 渲染坐标
double[] renderPos = MCRenderHelper.worldToRender(worldX, worldY, worldZ);

// 方块坐标 -> 渲染坐标
double[] renderPos = MCRenderHelper.blockToRender(blockX, blockY, blockZ);

// 实体插值位置（避免抖动）
double[] entityPos = MCRenderHelper.getEntityRenderPos(entity, partialTicks);

// 玩家脚下位置
double[] footPos = MCRenderHelper.getPlayerFootRenderPos(player, partialTicks);
```

### BatchConfig

批量渲染器配置，基于 OpenGL 硬件能力自动检测推荐值。

```java
// 获取配置
BatchConfig config = RenderSystem.getBatchConfig();

// 查询 GL 硬件限制
int maxVertices = config.getGLMaxVertices();   // GL_MAX_ELEMENTS_VERTICES
int maxIndices = config.getGLMaxIndices();     // GL_MAX_ELEMENTS_INDICES

// 获取推荐值
int spriteQuads = config.getRecommendedSpriteQuads();
int world3DVerts = config.getRecommendedWorld3DVertices();

// 用户自定义（覆盖推荐值）
config.setSpriteMaxQuads(1024);
config.setWorld3DMaxVertices(32768);

// 重置为推荐值
config.resetToRecommended();
```

**配置项说明：**

| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `spriteMaxQuads` | 256 | 64 - 65536 | SpriteBatch 最大四边形数 |
| `world3DMaxVertices` | 8192 | 1024 - 262144 | World3DBatch 最大顶点数 |
| `instancedMaxInstances` | 1024 | 128 - 65536 | InstancedBatch 最大实例数 |

### ShaderType

预定义的着色器类型枚举。

| 类型 | GLSL | 用途 |
|------|------|------|
| `SIMPLE` | 3.30 | 基础 2D 纹理绘制 |
| `BLUR` | 3.30 | 高斯模糊效果 |
| `GUI_COLOR` | 3.30 | GUI 纯色渲染（SpriteBatch 使用） |
| `UBER` | 3.30 | 统一着色器，支持多种模式 |
| `PBR` | 3.30 | 物理渲染（金属度/粗糙度工作流） |
| `WORLD_3D` | 1.20 | 3D 世界渲染（World3DBatch 使用，最大兼容） |

### Camera

相机系统，支持透视和正交投影。

```java
// 透视相机
Camera perspCamera = Camera.perspective(fov, aspect, near, far);
perspCamera.setPosition(x, y, z);
perspCamera.setTarget(tx, ty, tz);

// 正交相机
Camera orthoCamera = Camera.orthographic(width, height, near, far);

// 同步 MC 相机
camera.syncFromMinecraft(partialTicks);
```

### Scene & SceneNode

场景图系统，用于管理 3D 对象层次结构。

```java
Scene scene = new Scene("MyScene");
scene.setMainCamera(camera);

SceneNode root = scene.getRoot();
SceneNode child = new SceneNode("Child");
child.getTransform().setPosition(x, y, z);
child.getTransform().setRotation(pitch, yaw, roll);
child.getTransform().setScale(scale);
root.addChild(child);
```

---

## 使用指南

### HUD 渲染流程

1. 监听 `RenderGameOverlayEvent.Post`
2. 检查 `event.type == ElementType.ALL`
3. 检查 `RenderSystem.isShaderSupported()`
4. 使用 `GLStateManager.save()` 保存状态
5. 获取 `SpriteBatch` 并设置投影
6. 调用 `begin()` / `draw*()` / `end()`
7. 状态自动恢复

### 世界渲染流程

1. 监听 `RenderWorldLastEvent`
2. 检查 `RenderSystem.isShaderSupported()`
3. 使用 `GLStateManager.save()` 保存状态
4. 使用 `MCRenderHelper` 获取相机位置
5. 计算相对坐标：`renderPos = worldPos - cameraPos`
6. 获取 `World3DBatch`
7. 调用 `begin(mode)` / `draw*()` / `end()`
8. 状态自动恢复

### 坐标系统

MC 世界渲染使用**相对坐标系**：

```
渲染坐标 = 世界坐标 - 相机位置

相机位置来自：
- RenderManager.renderPosX/Y/Z
- 或 MCRenderHelper.getCameraX/Y/Z()
```

**示例：渲染方块线框**
```java
// 方块世界坐标
int blockX = 100, blockY = 64, blockZ = 200;

// 相机位置
double camX = MCRenderHelper.getCameraX();
double camY = MCRenderHelper.getCameraY();
double camZ = MCRenderHelper.getCameraZ();

// 渲染坐标
double rx = blockX - camX;
double ry = blockY - camY;
double rz = blockZ - camZ;

// 绘制
batch.drawWireBox(rx, ry, rz, 1, 1, 1, r, g, b, a);
```

---

## 注意事项

### 必须遵守

1. **始终保存/恢复 GL 状态**
   ```java
   try (GLStateManager state = GLStateManager.save()) {
       // 渲染代码
   }
   ```

2. **检查 Shader 支持**
   ```java
   if (!RenderSystem.isShaderSupported()) return;
   ```

3. **正确配对 begin/end**
   ```java
   batch.begin();
   // draw calls...
   batch.end();  // 必须调用
   ```

4. **使用相对坐标**
   - 世界渲染必须减去相机位置
   - 使用 `MCRenderHelper` 简化计算

### 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 渲染不显示 | 未设置正确的 GL 状态 | 使用 `GLStateManager` |
| MC UI 损坏 | 未恢复 GL 状态 | 确保 `restoreState()` 调用 |
| 位置偏移 | 使用了世界坐标 | 转换为相对坐标 |
| 闪烁/抖动 | 未插值实体位置 | 使用 `getEntityRenderPos()` |
| 崩溃 | VAO 被其他 mod 污染 | 延迟初始化已处理此问题 |

### 性能建议

1. **复用批量渲染器**
   - 使用 `RenderSystem.getSpriteBatch()` 获取共享实例
   - 避免每帧创建新实例

2. **批量绘制**
   - 在单个 `begin()/end()` 块中绘制多个图元
   - 避免频繁切换绘制模式

3. **合理设置容量**
   ```java
   // 大量绘制时增加容量
   World3DBatch batch = new World3DBatch(32768);
   ```

---

## 目录结构

```
client/renderer/
├── RenderSystem.java           # 渲染系统入口
├── graphics/
│   ├── batch/
│   │   ├── BatchConfig.java    # 批量渲染器配置（GL 限制查询）
│   │   ├── SpriteBatch.java    # 2D 批量渲染器
│   │   ├── World3DBatch.java   # 3D 世界渲染器
│   │   ├── InstancedBatch.java # 实例化渲染器
│   │   └── RenderQueue.java    # 渲染队列
│   ├── buffer/
│   │   ├── UniformBuffer.java  # UBO 管理
│   │   └── GlobalUniforms.java # 全局 Uniform
│   ├── camera/
│   │   ├── Camera.java         # 相机系统
│   │   ├── Frustum.java        # 视锥体剔除
│   │   └── Plane.java          # 平面定义
│   ├── culling/
│   │   ├── AABB.java           # 轴对齐包围盒
│   │   └── BoundingSphere.java # 包围球
│   ├── material/
│   │   ├── Material.java       # 基础材质
│   │   ├── PBRMaterial.java    # PBR 材质
│   │   └── RenderMode.java     # 渲染模式
│   ├── math/
│   │   └── MathUtils.java      # 数学工具
│   ├── mesh/
│   │   ├── Mesh.java           # 网格基类
│   │   ├── StaticMesh.java     # 静态网格
│   │   ├── DynamicMesh.java    # 动态网格
│   │   ├── LODGroup.java       # LOD 组
│   │   ├── VertexFormat.java   # 顶点格式
│   │   └── VertexAttribute.java# 顶点属性
│   ├── scene/
│   │   ├── Scene.java          # 场景
│   │   ├── SceneNode.java      # 场景节点
│   │   ├── Transform.java      # 变换组件
│   │   └── MeshRenderer.java   # 网格渲染器
│   ├── shader/
│   │   ├── ShaderProgram.java  # Shader 程序
│   │   └── ShaderType.java     # Shader 类型枚举
│   ├── texture/
│   │   └── Texture2D.java      # 2D 纹理
│   └── framebuffer/
│       └── Framebuffer.java    # 帧缓冲
├── util/
│   ├── GLStateManager.java     # GL 状态管理
│   └── MCRenderHelper.java     # MC 渲染辅助
└── test/
    ├── RenderingTestHandler.java    # HUD 渲染测试
    └── WorldRenderTestHandler.java  # 世界渲染测试
```

---

## 版本信息

- **目标环境**: Minecraft 1.7.10 + GTNH 2.8.0
- **OpenGL 版本**: 3.3+ (推荐)，最低 2.1
- **GLSL 版本**: 3.30 (主要)，1.20 (兼容)
- **支持特性**: VAO, VBO, UBO, 实例化渲染, 帧缓冲
- **兼容性**: Angelica, Sodium, Embeddium

---

## 参考示例

完整示例请参考 `test/` 目录：
- `RenderingTestHandler.java` - HUD 渲染示例
- `WorldRenderTestHandler.java` - 世界渲染示例（魔法阵、矿石高亮等）
