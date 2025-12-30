package moe.takochan.takotech.client.renderer.graphics.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.renderer.graphics.mesh.Mesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.StaticMesh;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexAttribute;
import moe.takochan.takotech.client.renderer.graphics.mesh.VertexFormat;

/**
 * Minecraft 模型加载器。
 * 支持加载 MC JSON 方块模型和 OBJ 模型，并转换为 Mesh 格式。
 *
 * <p>
 * 使用示例:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // 加载 OBJ 模型
 *     Mesh mesh = MCModelLoader.loadOBJ(new ResourceLocation("modid", "models/custom_model.obj"));
 *
 *     // 加载 JSON 方块模型
 *     Mesh mesh = MCModelLoader.loadBlockModel(new ResourceLocation("minecraft", "models/block/cube.json"));
 *
 *     // 创建简单立方体
 *     Mesh cube = MCModelLoader.createCube(1.0f);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class MCModelLoader {

    private MCModelLoader() {}

    // ==================== OBJ 模型 ====================

    /**
     * 加载 OBJ 模型并转换为 Mesh
     *
     * @param location 资源位置
     * @return Mesh，如果失败返回 null
     */
    public static Mesh loadOBJ(ResourceLocation location) {
        try {
            IModelCustom model = AdvancedModelLoader.loadModel(location);
            return fromModelCustom(model);
        } catch (Exception e) {
            TakoTechMod.LOG.error("MCModelLoader: Failed to load OBJ model: {}", location, e);
            return null;
        }
    }

    /**
     * 从 IModelCustom 转换为 Mesh
     *
     * @param model Forge 模型
     * @return Mesh
     */
    public static Mesh fromModelCustom(IModelCustom model) {
        // IModelCustom 不直接提供顶点数据，需要使用显示列表方式渲染
        // 返回一个包装的 Mesh
        return new ModelCustomMesh(model);
    }

    // ==================== JSON 方块模型 ====================

    /**
     * 加载 MC JSON 方块模型并转换为 Mesh
     *
     * @param location 资源位置 (e.g., "minecraft:models/block/cube.json")
     * @return Mesh，如果失败返回 null
     */
    public static Mesh loadBlockModel(ResourceLocation location) {
        try {
            IResource resource = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(location);
            InputStream stream = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(reader)
                .getAsJsonObject();
            reader.close();

            return parseBlockModel(json);

        } catch (Exception e) {
            TakoTechMod.LOG.error("MCModelLoader: Failed to load block model: {}", location, e);
            return null;
        }
    }

    /**
     * 解析 JSON 方块模型
     *
     * @param json JSON 对象
     * @return Mesh
     */
    private static Mesh parseBlockModel(JsonObject json) {
        if (!json.has("elements")) {
            TakoTechMod.LOG.warn("MCModelLoader: Block model has no elements");
            return null;
        }

        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int indexOffset = 0;

        JsonArray elements = json.getAsJsonArray("elements");
        for (JsonElement elem : elements) {
            JsonObject element = elem.getAsJsonObject();

            // 获取 from 和 to
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");

            float x1 = from.get(0)
                .getAsFloat() / 16.0f;
            float y1 = from.get(1)
                .getAsFloat() / 16.0f;
            float z1 = from.get(2)
                .getAsFloat() / 16.0f;
            float x2 = to.get(0)
                .getAsFloat() / 16.0f;
            float y2 = to.get(1)
                .getAsFloat() / 16.0f;
            float z2 = to.get(2)
                .getAsFloat() / 16.0f;

            // 为每个面生成顶点
            JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : null;
            if (faces != null) {
                // 每个面
                String[] faceNames = { "down", "up", "north", "south", "west", "east" };
                for (String faceName : faceNames) {
                    if (faces.has(faceName)) {
                        indexOffset = addFace(vertices, indices, indexOffset, faceName, x1, y1, z1, x2, y2, z2);
                    }
                }
            }
        }

        if (vertices.isEmpty()) {
            return null;
        }

        // 转换为数组
        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }

        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        // 创建 Mesh (position3D + normal + texCoord)
        return new StaticMesh(
            vertexArray,
            indexArray,
            VertexFormat.POSITION_3D_NORMAL_TEX.getStride(),
            VertexFormat.POSITION_3D_NORMAL_TEX.getAttributes());
    }

    /**
     * 添加面到顶点和索引列表
     */
    private static int addFace(List<Float> vertices, List<Integer> indices, int indexOffset, String faceName, float x1,
        float y1, float z1, float x2, float y2, float z2) {
        float[] positions;
        float[] normal;

        switch (faceName) {
            case "down":
                positions = new float[] { x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2 };
                normal = new float[] { 0, -1, 0 };
                break;
            case "up":
                positions = new float[] { x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1 };
                normal = new float[] { 0, 1, 0 };
                break;
            case "north":
                positions = new float[] { x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1 };
                normal = new float[] { 0, 0, -1 };
                break;
            case "south":
                positions = new float[] { x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2 };
                normal = new float[] { 0, 0, 1 };
                break;
            case "west":
                positions = new float[] { x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1 };
                normal = new float[] { -1, 0, 0 };
                break;
            case "east":
                positions = new float[] { x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2 };
                normal = new float[] { 1, 0, 0 };
                break;
            default:
                return indexOffset;
        }

        float[] uvs = { 0, 0, 1, 0, 1, 1, 0, 1 };

        // 添加 4 个顶点 (每个顶点: pos3 + normal3 + uv2 = 8 floats)
        for (int i = 0; i < 4; i++) {
            // Position
            vertices.add(positions[i * 3]);
            vertices.add(positions[i * 3 + 1]);
            vertices.add(positions[i * 3 + 2]);
            // Normal
            vertices.add(normal[0]);
            vertices.add(normal[1]);
            vertices.add(normal[2]);
            // UV
            vertices.add(uvs[i * 2]);
            vertices.add(uvs[i * 2 + 1]);
        }

        // 添加 6 个索引 (两个三角形)
        indices.add(indexOffset);
        indices.add(indexOffset + 1);
        indices.add(indexOffset + 2);
        indices.add(indexOffset);
        indices.add(indexOffset + 2);
        indices.add(indexOffset + 3);

        return indexOffset + 4;
    }

    // ==================== 基本形状 ====================

    /**
     * 创建立方体 Mesh
     *
     * @param size 边长
     * @return Mesh
     */
    public static Mesh createCube(float size) {
        return createBox(size, size, size);
    }

    /**
     * 创建长方体 Mesh
     *
     * @param width  宽度 (X)
     * @param height 高度 (Y)
     * @param depth  深度 (Z)
     * @return Mesh
     */
    public static Mesh createBox(float width, float height, float depth) {
        float hw = width / 2;
        float hh = height / 2;
        float hd = depth / 2;

        // 顶点格式: position (3) + normal (3) + texcoord (2) = 8 floats per vertex
        float[] vertices = {
            // Back face (Z-)
            -hw, -hh, -hd, 0, 0, -1, 0, 0, hw, -hh, -hd, 0, 0, -1, 1, 0, hw, hh, -hd, 0, 0, -1, 1, 1, -hw, hh, -hd, 0,
            0, -1, 0, 1,
            // Front face (Z+)
            -hw, -hh, hd, 0, 0, 1, 0, 0, hw, -hh, hd, 0, 0, 1, 1, 0, hw, hh, hd, 0, 0, 1, 1, 1, -hw, hh, hd, 0, 0, 1, 0,
            1,
            // Left face (X-)
            -hw, -hh, -hd, -1, 0, 0, 0, 0, -hw, -hh, hd, -1, 0, 0, 1, 0, -hw, hh, hd, -1, 0, 0, 1, 1, -hw, hh, -hd, -1,
            0, 0, 0, 1,
            // Right face (X+)
            hw, -hh, -hd, 1, 0, 0, 0, 0, hw, -hh, hd, 1, 0, 0, 1, 0, hw, hh, hd, 1, 0, 0, 1, 1, hw, hh, -hd, 1, 0, 0, 0,
            1,
            // Bottom face (Y-)
            -hw, -hh, -hd, 0, -1, 0, 0, 0, hw, -hh, -hd, 0, -1, 0, 1, 0, hw, -hh, hd, 0, -1, 0, 1, 1, -hw, -hh, hd, 0,
            -1, 0, 0, 1,
            // Top face (Y+)
            -hw, hh, -hd, 0, 1, 0, 0, 0, hw, hh, -hd, 0, 1, 0, 1, 0, hw, hh, hd, 0, 1, 0, 1, 1, -hw, hh, hd, 0, 1, 0, 0,
            1 };

        // CCW winding order for all faces (front face = visible from outside)
        int[] indices = {
            // Back (Z-): viewed from -Z, vertices go CW, so reverse to CCW
            0, 2, 1, 0, 3, 2,
            // Front (Z+): viewed from +Z, vertices go CCW
            4, 5, 6, 4, 6, 7,
            // Left (X-): viewed from -X, vertices go CCW
            8, 9, 10, 8, 10, 11,
            // Right (X+): viewed from +X, vertices go CW, so reverse to CCW
            12, 14, 13, 12, 15, 14,
            // Bottom (Y-): viewed from -Y, vertices go CW, so reverse to CCW
            16, 18, 17, 16, 19, 18,
            // Top (Y+): viewed from +Y, vertices go CW, so reverse to CCW
            20, 22, 21, 20, 23, 22 };

        return new StaticMesh(
            vertices,
            indices,
            VertexFormat.POSITION_3D_NORMAL_TEX.getStride(),
            VertexFormat.POSITION_3D_NORMAL_TEX.getAttributes());
    }

    /**
     * 创建平面 Mesh
     *
     * @param width  宽度
     * @param height 高度
     * @return Mesh
     */
    public static Mesh createPlane(float width, float height) {
        float hw = width / 2;
        float hh = height / 2;

        float[] vertices = { -hw, 0, -hh, 0, 1, 0, 0, 0, hw, 0, -hh, 0, 1, 0, 1, 0, hw, 0, hh, 0, 1, 0, 1, 1, -hw, 0,
            hh, 0, 1, 0, 0, 1 };

        int[] indices = { 0, 1, 2, 0, 2, 3 };

        return new StaticMesh(
            vertices,
            indices,
            VertexFormat.POSITION_3D_NORMAL_TEX.getStride(),
            VertexFormat.POSITION_3D_NORMAL_TEX.getAttributes());
    }

    /**
     * 创建球体 Mesh
     *
     * @param radius   半径
     * @param segments 分段数
     * @param rings    环数
     * @return Mesh
     */
    public static Mesh createSphere(float radius, int segments, int rings) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int ring = 0; ring <= rings; ring++) {
            float v = (float) ring / rings;
            float phi = v * (float) Math.PI;

            for (int seg = 0; seg <= segments; seg++) {
                float u = (float) seg / segments;
                float theta = u * 2.0f * (float) Math.PI;

                float x = (float) (Math.cos(theta) * Math.sin(phi));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(theta) * Math.sin(phi));

                // Position
                vertices.add(x * radius);
                vertices.add(y * radius);
                vertices.add(z * radius);
                // Normal
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);
                // UV
                vertices.add(u);
                vertices.add(v);
            }
        }

        for (int ring = 0; ring < rings; ring++) {
            for (int seg = 0; seg < segments; seg++) {
                int curr = ring * (segments + 1) + seg;
                int next = curr + segments + 1;

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

    // ==================== ModelCustomMesh 包装类 ====================

    /**
     * 包装 IModelCustom 的 Mesh 实现
     */
    private static class ModelCustomMesh extends Mesh {

        private final IModelCustom model;
        private int displayList = -1;

        public ModelCustomMesh(IModelCustom model) {
            super(0, new VertexAttribute[0]);
            this.model = model;
            this.valid = model != null;
        }

        @Override
        public void draw() {
            if (model != null) {
                // 使用 IModelCustom 的渲染方法
                model.renderAll();
            }
        }

        @Override
        public void draw(int mode) {
            draw(); // IModelCustom 不支持指定绘制模式
        }

        @Override
        public void bind() {
            // IModelCustom 不需要绑定
        }

        @Override
        public void unbind() {
            // IModelCustom 不需要解绑
        }

        @Override
        protected void cleanup() {
            if (displayList != -1) {
                GL11.glDeleteLists(displayList, 1);
                displayList = -1;
            }
            valid = false;
        }
    }
}
