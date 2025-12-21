# Shader Rendering Framework

TakoTech 的现代 OpenGL 渲染框架，提供 shader 程序管理、网格渲染和 GL 状态管理功能。

## 架构概览

```
client/renderer/
├── RenderSystem.java              # 渲染系统入口
└── graphics/
    ├── shader/
    │   ├── ShaderProgram.java     # Shader 程序封装
    │   └── ShaderType.java        # Shader 类型枚举
    ├── mesh/
    │   ├── VertexFormat.java      # 顶点格式定义
    │   ├── VertexAttribute.java   # 顶点属性
    │   ├── Mesh.java              # 网格基类
    │   ├── StaticMesh.java        # 静态网格
    │   └── DynamicMesh.java       # 动态网格
    ├── state/
    │   └── RenderState.java       # GL 状态管理
    ├── batch/
    │   └── SpriteBatch.java       # 批量渲染器
    ├── framebuffer/
    │   └── Framebuffer.java       # 帧缓冲
    └── texture/
        └── Texture2D.java         # 纹理
```

## 核心组件

### RenderSystem

渲染系统入口，负责初始化和清理渲染资源。

```java
// 初始化（在 ClientProxy.preInit 中调用）
RenderSystem.init();

// 检查 shader 支持
if (RenderSystem.isShaderSupported()) {
    // 使用 shader 渲染
}

// 获取共享的 SpriteBatch
SpriteBatch batch = RenderSystem.getSpriteBatch();

// 清理（在 mod 卸载时调用）
RenderSystem.shutdown();
```

### ShaderProgram

Shader 程序封装，支持从资源文件加载着色器。

```java
// 从资源文件创建
ShaderProgram shader = new ShaderProgram("takotech", "shaders/my_shader.vert", "shaders/my_shader.frag");

// 使用 shader
shader.use();

// 设置 uniform
shader.setUniformFloat("uTime", time);
shader.setUniformVec4("uColor", r, g, b, a);
shader.setUniformMatrix4("uProjection", false, projMatrix);

// 解绑
ShaderProgram.unbind();

// 释放资源
shader.close();
```

### VertexFormat

预定义的顶点格式，用于配置顶点属性布局。

```java
// 预定义格式
VertexFormat.POSITION_2D          // vec2 pos
VertexFormat.POSITION_COLOR       // vec2 pos + vec4 color
VertexFormat.POSITION_TEX         // vec2 pos + vec2 uv
VertexFormat.POSITION_TEX_COLOR   // vec2 pos + vec2 uv + vec4 color
VertexFormat.POSITION_3D          // vec3 pos
VertexFormat.POSITION_3D_TEX      // vec3 pos + vec2 uv
VertexFormat.POSITION_3D_COLOR    // vec3 pos + vec4 color

// 自定义格式
VertexFormat custom = VertexFormat.builder()
    .position3D()
    .normal()
    .texCoord()
    .colorFloat()
    .build();
```

### Mesh

网格基类，封装 VAO/VBO/EBO 操作。

```java
// 静态网格（一次上传，多次绘制）
StaticMesh mesh = new StaticMesh(vertexData, indexData, VertexFormat.POSITION_COLOR);
mesh.draw();
mesh.close();

// 动态网格（每帧更新）
DynamicMesh mesh = new DynamicMesh(maxVertices, maxIndices, VertexFormat.POSITION_COLOR);
mesh.updateData(vertexData, indexData);
mesh.draw();
mesh.close();
```

### RenderState

GL 状态管理器，提供状态保存/恢复功能。

```java
// 保存当前状态
RenderState.StateSnapshot snapshot = RenderState.save();

// 设置渲染状态
RenderState.setBlendAlpha();      // 标准 alpha 混合
RenderState.setBlendAdditive();   // 加法混合
RenderState.disableDepthTest();
RenderState.disableCullFace();
RenderState.disableTexture2D();

// 恢复状态
RenderState.restore(snapshot);
```

### SpriteBatch

高效的 2D 批量渲染器。

```java
SpriteBatch batch = new SpriteBatch();

// 设置投影
batch.setProjectionOrtho(width, height);

// 开始批量渲染
batch.begin();

// 添加图元
batch.drawRect(x, y, w, h, r, g, b, a);
batch.drawQuad(x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);

// 结束并提交
batch.end();

// 释放资源
batch.close();
```

## 使用示例

### GUI 渲染

```java
public class MyGui extends GuiScreen {
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (RenderSystem.isShaderSupported()) {
            drawWithShader();
        } else {
            drawWithTessellator();  // 固定管线回退
        }
    }

    private void drawWithShader() {
        SpriteBatch batch = RenderSystem.getSpriteBatch();
        batch.setProjectionOrtho(width, height);
        batch.begin();

        // 绘制背景
        batch.drawRect(0, 0, width, height, 0.1f, 0.1f, 0.1f, 0.8f);

        // 绘制按钮
        batch.drawRect(buttonX, buttonY, buttonW, buttonH, 0.3f, 0.3f, 0.3f, 1.0f);

        batch.end();
    }
}
```

### 自定义 Shader

1. 创建着色器文件:

**shaders/my_effect.vert**
```glsl
#version 330 core

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec4 aColor;

uniform mat4 uProjection;

out vec4 vColor;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vColor = aColor;
}
```

**shaders/my_effect.frag**
```glsl
#version 330 core

in vec4 vColor;
out vec4 FragColor;

uniform float uTime;

void main() {
    float pulse = sin(uTime * 3.0) * 0.5 + 0.5;
    FragColor = vColor * vec4(1.0, 1.0, 1.0, pulse);
}
```

2. 在 ShaderType 中注册:

```java
public enum ShaderType {
    // ...
    MY_EFFECT("shaders/my_effect.vert", "shaders/my_effect.frag");
}
```

3. 使用:

```java
ShaderProgram shader = ShaderType.MY_EFFECT.get();
shader.use();
shader.setUniformFloat("uTime", time);
// ...
```

## 兼容性

### Minecraft 固定管线

框架设计为与 Minecraft 的固定管线渲染兼容：
- 自动保存/恢复 GL 状态
- 绘制完成后禁用顶点属性数组
- 支持回退到 Tessellator

### Angelica/Embeddium

针对 Angelica/Embeddium 的 GLStateManager 进行了兼容性处理：
- 显式绑定 VBO 和 EBO（不依赖 VAO 状态缓存）
- 每次绘制前重新设置顶点属性

### Shader 支持检测

```java
// 检查系统是否支持 shader
if (ShaderProgram.isSupported()) {
    // 使用 shader
} else {
    // 回退方案
}

// 或使用 RenderSystem
if (RenderSystem.isShaderSupported() && RenderSystem.isInitialized()) {
    // 使用 shader
}
```

## 注意事项

1. **资源管理**: 所有渲染资源实现 `AutoCloseable`，推荐使用 try-with-resources 或显式调用 `close()`

2. **线程安全**: 所有 OpenGL 调用必须在渲染线程中执行

3. **顶点顺序**: 四边形顶点应按逆时针顺序排列，或禁用背面剔除

4. **投影矩阵**: SpriteBatch 使用左上角原点的正交投影，Y 轴向下

5. **状态恢复**: 始终在渲染完成后恢复 GL 状态，确保不影响其他渲染代码
