package moe.takochan.takotech.client.renderer.graphics.component;

import org.lwjgl.util.vector.Matrix4f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.renderer.graphics.core.RenderContext;
import moe.takochan.takotech.client.renderer.graphics.culling.AABB;
import moe.takochan.takotech.client.renderer.graphics.ecs.Component;
import moe.takochan.takotech.client.renderer.graphics.material.Material;
import moe.takochan.takotech.client.renderer.graphics.math.MathUtils;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;

/**
 * 网格渲染器组件。
 * 将 Mesh 和 Material 绑定到实体进行渲染。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     Entity cube = world.createEntity("Cube");
 *     cube.addComponent(new TransformComponent())
 *         .setPosition(0, 64, 0);
 *
 *     MeshRendererComponent renderer = cube.addComponent(new MeshRendererComponent());
 *     renderer.setMesh(StaticMesh.createCube());
 *     renderer.setMaterial(Material.color(1, 0, 0, 1));
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class MeshRendererComponent extends Component {

    /** 网格 */
    private Mesh mesh;

    /** 材质 */
    private Material material;

    /** 本地空间包围盒 */
    private final AABB localBounds = new AABB();

    /** 是否可见 */
    private boolean visible = true;

    /** 是否投射阴影（预留） */
    private boolean castShadows = true;

    /** 是否接收阴影（预留） */
    private boolean receiveShadows = true;

    /** 渲染排序优先级（越小越先渲染） */
    private int sortingOrder = 0;

    /** 渲染层（位掩码） */
    private int renderLayer = 1;

    // ==================== 构造函数 ====================

    /**
     * 创建空的网格渲染器组件
     */
    public MeshRendererComponent() {}

    /**
     * 创建带网格的渲染器组件
     *
     * @param mesh 网格
     */
    public MeshRendererComponent(Mesh mesh) {
        setMesh(mesh);
    }

    /**
     * 创建带网格和材质的渲染器组件
     *
     * @param mesh     网格
     * @param material 材质
     */
    public MeshRendererComponent(Mesh mesh, Material material) {
        setMesh(mesh);
        setMaterial(material);
    }

    // ==================== Mesh ====================

    /**
     * 获取网格
     *
     * @return 网格
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * 设置网格
     *
     * @param mesh 网格
     * @return this
     */
    public MeshRendererComponent setMesh(Mesh mesh) {
        this.mesh = mesh;
        updateLocalBounds();
        return this;
    }

    // ==================== Material ====================

    /**
     * 获取材质
     *
     * @return 材质
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * 设置材质
     *
     * @param material 材质
     * @return this
     */
    public MeshRendererComponent setMaterial(Material material) {
        this.material = material;
        return this;
    }

    // ==================== 包围盒 ====================

    /**
     * 获取本地空间包围盒
     *
     * @return 包围盒
     */
    public AABB getLocalBounds() {
        return localBounds;
    }

    /**
     * 设置本地包围盒
     *
     * @param minX 最小 X
     * @param minY 最小 Y
     * @param minZ 最小 Z
     * @param maxX 最大 X
     * @param maxY 最大 Y
     * @param maxZ 最大 Z
     * @return this
     */
    public MeshRendererComponent setLocalBounds(float minX, float minY, float minZ, float maxX, float maxY,
        float maxZ) {
        localBounds.set(minX, minY, minZ, maxX, maxY, maxZ);
        return this;
    }

    /**
     * 获取世界空间包围盒
     * 注意：当前实现返回本地包围盒，未来可添加矩阵变换支持
     *
     * @return 世界空间包围盒
     */
    public AABB getWorldBounds() {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null || !localBounds.isValid()) {
            return localBounds;
        }

        // TODO: 实现包围盒矩阵变换
        // 当前简单返回本地包围盒加上位移偏移
        AABB worldBounds = new AABB();
        float x = transform.getX();
        float y = transform.getY();
        float z = transform.getZ();
        worldBounds.set(
            localBounds.minX + x,
            localBounds.minY + y,
            localBounds.minZ + z,
            localBounds.maxX + x,
            localBounds.maxY + y,
            localBounds.maxZ + z);
        return worldBounds;
    }

    /**
     * 更新本地包围盒
     */
    private void updateLocalBounds() {
        // 未来可从 Mesh 顶点数据自动计算
        // 目前需要手动设置
    }

    // ==================== 可见性 ====================

    /**
     * 是否可见
     *
     * @return true 可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 设置可见性
     *
     * @param visible true 可见
     * @return this
     */
    public MeshRendererComponent setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * 判断是否应该渲染
     *
     * @return true 应该渲染
     */
    public boolean shouldRender() {
        return visible && isActive() && mesh != null && mesh.isValid();
    }

    // ==================== 阴影设置（预留） ====================

    /**
     * 是否投射阴影
     *
     * @return true 投射
     */
    public boolean isCastShadows() {
        return castShadows;
    }

    /**
     * 设置是否投射阴影
     *
     * @param castShadows true 投射
     * @return this
     */
    public MeshRendererComponent setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }

    /**
     * 是否接收阴影
     *
     * @return true 接收
     */
    public boolean isReceiveShadows() {
        return receiveShadows;
    }

    /**
     * 设置是否接收阴影
     *
     * @param receiveShadows true 接收
     * @return this
     */
    public MeshRendererComponent setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }

    // ==================== 排序 ====================

    /**
     * 获取排序优先级
     *
     * @return 优先级
     */
    public int getSortingOrder() {
        return sortingOrder;
    }

    /**
     * 设置排序优先级
     *
     * @param sortingOrder 优先级（越小越先渲染）
     * @return this
     */
    public MeshRendererComponent setSortingOrder(int sortingOrder) {
        this.sortingOrder = sortingOrder;
        return this;
    }

    /**
     * 获取渲染层
     *
     * @return 渲染层（位掩码）
     */
    public int getRenderLayer() {
        return renderLayer;
    }

    /**
     * 设置渲染层
     *
     * @param renderLayer 渲染层（位掩码）
     * @return this
     */
    public MeshRendererComponent setRenderLayer(int renderLayer) {
        this.renderLayer = renderLayer;
        return this;
    }

    // ==================== 渲染 ====================

    /**
     * 使用渲染上下文渲染
     *
     * @param ctx 渲染上下文
     */
    public void render(RenderContext ctx) {
        if (!shouldRender()) {
            return;
        }

        // 获取变换组件并设置模型矩阵
        // 注意: MC 的 ModelView 矩阵期望相机相对坐标，所以需要将世界坐标转换为相机相对坐标
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            Matrix4f worldMat = transform.getWorldMatrix();
            // 创建相机相对的模型矩阵：将位置偏移到相机坐标系
            Matrix4f cameraRelativeMat = new Matrix4f();
            cameraRelativeMat.load(worldMat);
            cameraRelativeMat.m30 -= ctx.getCameraX();
            cameraRelativeMat.m31 -= ctx.getCameraY();
            cameraRelativeMat.m32 -= ctx.getCameraZ();
            ctx.setModelMatrix(cameraRelativeMat);
        } else {
            ctx.resetModelMatrix();
        }

        // 应用材质
        ShaderProgram shader = ctx.getShader();
        if (material != null && shader != null) {
            material.apply(shader);
        }

        // 应用模型矩阵到着色器
        ctx.applyModelToShader();

        // 绘制
        mesh.draw();
    }

    /**
     * 使用着色器渲染（兼容旧 API）
     *
     * @param shader 着色器程序
     */
    public void render(ShaderProgram shader) {
        if (!shouldRender()) {
            return;
        }

        // 应用材质
        if (material != null && shader != null) {
            material.apply(shader);
        }

        // 设置模型矩阵
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null && shader != null) {
            shader.setUniformMatrix4("uModel", false, MathUtils.getMatrixBuffer(transform.getWorldMatrix()));
        }

        // 绘制
        mesh.draw();
    }

    /**
     * 简单渲染（不设置 uniforms）
     */
    public void renderSimple() {
        if (shouldRender()) {
            mesh.draw();
        }
    }

    @Override
    public String toString() {
        return String.format(
            "MeshRendererComponent[mesh=%s, material=%s, visible=%b]",
            mesh != null ? "valid" : "null",
            material != null ? material.getRenderMode() : "null",
            visible);
    }
}
