package moe.takochan.takotech.client.gui;

import static moe.takochan.takotech.utils.SectorVertexUtils.RADIUS_IN;
import static moe.takochan.takotech.utils.SectorVertexUtils.RADIUS_OUT;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import moe.takochan.takotech.utils.ToolboxHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderProgram;
import moe.takochan.takotech.client.renderer.graphics.shader.ShaderType;
import moe.takochan.takotech.client.settings.GameSettings;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.common.loader.ItemLoader;
import moe.takochan.takotech.config.ClientConfig;
import moe.takochan.takotech.network.NetworkHandler;
import moe.takochan.takotech.network.PacketToolboxSelected;
import moe.takochan.takotech.utils.SectorVertexUtils;

@SideOnly(Side.CLIENT)
public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    // 物品显示位置半径
    public final static float ITEM_RADIUS = (RADIUS_IN + RADIUS_OUT) * 0.5F;
    // 获取当前游戏实例
    private final static Minecraft mc = Minecraft.getMinecraft();
    // 默认物品实例
    private final static ItemStack DEFAULT_ITEM = new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);

    // 存储的可选物品列表
    private final List<ToolData> items = new ArrayList<>();
    // 关联的容器实例
    private final ContainerToolboxPlusSelect container;
    // 当前选中的物品索引
    private int selectIndex = -1;

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container) {
        super(container);
        // 设置 GUI 尺寸（可根据实际需求调整）
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;

        this.container = container;

    }

    /**
     * 初始化 GUI
     */
    @Override
    public void initGui() {
        super.initGui();
        EntityPlayer player = container.getPlayer();
        loadItems(player.inventory.getCurrentItem());
    }

    /**
     * 绘制背景层（主要绘制逻辑）
     *
     * @param partialTicks 部分刻（用于动画效果）
     * @param mouseX       鼠标 X 坐标
     * @param mouseY       鼠标 Y 坐标
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        // 如果没有可选物品则返回
        if (items.isEmpty()) return;

        // 计算选中项
        int xCenter = width / 2;
        int yCenter = height / 2;

        // 计算鼠标相对中心的角度和距离
        double dx = mouseX - xCenter;
        double dy = mouseY - yCenter;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 当前选中的物品索引
        selectIndex = -1;
        int n = items.size();
        for (int i = 0; i < items.size(); i++) {
            float sectorStart = (((i - 0.5F) / n) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / n) + 0.25F) * 360F;

            if (angle < sectorStart) angle += 360;
            if (angle >= sectorStart && angle < sectorEnd && distance >= RADIUS_IN && distance < RADIUS_OUT) {
                selectIndex = i;
                break;
            }
        }

        // 绘制扇区（临时使用固定管线渲染测试）
        drawSectors(xCenter, yCenter, n);

        // 绘制物品图标
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < items.size(); i++) {
            float angleRad = (((i) / (float) items.size()) + 0.25F) * 2F * (float) Math.PI;
            float posX = xCenter - 8 + ITEM_RADIUS * (float) Math.cos(angleRad);
            float posY = yCenter - 8 + ITEM_RADIUS * (float) Math.sin(angleRad);

            ItemStack toolItemStack = items.get(i)
                .getItemStack();

            if (toolItemStack != null) {
                // 渲染物品图标和效果
                itemRender.renderItemAndEffectIntoGUI(
                    mc.fontRenderer,
                    mc.getTextureManager(),
                    toolItemStack,
                    (int) posX,
                    (int) posY);
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    /**
     * 使用现代 GL 绘制扇区
     */
    private void drawSectors(int xCenter, int yCenter, int n) {
        // 保存当前状态
        int savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int savedVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int savedEbo = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        // 获取扇区数据
        List<List<float[][]>> sectors = SectorVertexUtils.getSectorVertices(n);

        // 计算顶点和索引数量
        int totalQuads = 0;
        for (List<float[][]> segments : sectors) {
            totalQuads += segments.size();
        }

        // 每个四边形 4 顶点，每顶点 6 floats (x, y, r, g, b, a)
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(totalQuads * 4 * 6);
        // 每个四边形 6 索引（2 个三角形）
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(totalQuads * 6);

        int vertexIndex = 0;
        for (int i = 0; i < n; i++) {
            int color = (i == selectIndex) ? 0xFFFFDD60 : 0x33333380;
            float r = ((color >> 24) & 0xFF) / 255f;
            float g = ((color >> 16) & 0xFF) / 255f;
            float b = ((color >> 8) & 0xFF) / 255f;
            float a = (color & 0xFF) / 255f;

            List<float[][]> segments = sectors.get(i);
            for (float[][] segment : segments) {
                float[] pos1Out = segment[0];
                float[] pos1In = segment[1];
                float[] pos2In = segment[2];
                float[] pos2Out = segment[3];

                // 顶点 0
                vertexBuffer.put(xCenter + pos1Out[0])
                    .put(yCenter + pos1Out[1]);
                vertexBuffer.put(r)
                    .put(g)
                    .put(b)
                    .put(a);
                // 顶点 1
                vertexBuffer.put(xCenter + pos1In[0])
                    .put(yCenter + pos1In[1]);
                vertexBuffer.put(r)
                    .put(g)
                    .put(b)
                    .put(a);
                // 顶点 2
                vertexBuffer.put(xCenter + pos2In[0])
                    .put(yCenter + pos2In[1]);
                vertexBuffer.put(r)
                    .put(g)
                    .put(b)
                    .put(a);
                // 顶点 3
                vertexBuffer.put(xCenter + pos2Out[0])
                    .put(yCenter + pos2Out[1]);
                vertexBuffer.put(r)
                    .put(g)
                    .put(b)
                    .put(a);

                // 索引（两个三角形）
                indexBuffer.put(vertexIndex)
                    .put(vertexIndex + 1)
                    .put(vertexIndex + 2);
                indexBuffer.put(vertexIndex)
                    .put(vertexIndex + 2)
                    .put(vertexIndex + 3);
                vertexIndex += 4;
            }
        }
        vertexBuffer.flip();
        indexBuffer.flip();

        // 创建 VAO/VBO/EBO
        int testVao = GL30.glGenVertexArrays();
        int testVbo = GL15.glGenBuffers();
        int testEbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(testVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, testVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, testEbo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_DYNAMIC_DRAW);

        // 设置顶点属性
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 24, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 24, 8);

        // 保存渲染状态
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // 设置渲染状态
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // 使用 Shader 并绘制
        ShaderProgram shader = ShaderType.GUI_COLOR.get();
        if (shader != null && shader.isValid()) {
            shader.use();

            // 设置投影矩阵
            ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            FloatBuffer projMatrix = BufferUtils.createFloatBuffer(16);
            // 正交投影矩阵
            float left = 0, right = screenWidth, bottom = screenHeight, top = 0, near = -1, far = 1;
            projMatrix.put(2.0f / (right - left))
                .put(0)
                .put(0)
                .put(0);
            projMatrix.put(0)
                .put(2.0f / (top - bottom))
                .put(0)
                .put(0);
            projMatrix.put(0)
                .put(0)
                .put(-2.0f / (far - near))
                .put(0);
            projMatrix.put(-(right + left) / (right - left))
                .put(-(top + bottom) / (top - bottom))
                .put(-(far + near) / (far - near))
                .put(1);
            projMatrix.flip();

            shader.setUniformMatrix4("uProjection", false, projMatrix);

            GL11.glDrawElements(GL11.GL_TRIANGLES, totalQuads * 6, GL11.GL_UNSIGNED_INT, 0);
        }

        // 恢复渲染状态
        GL11.glPopAttrib();

        // 解绑 Shader
        GL20.glUseProgram(0);

        // 解绑 VAO
        GL30.glBindVertexArray(0);

        // 在 VAO 0 状态下禁用顶点属性
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);

        // 解绑 VBO/EBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        // 删除资源
        GL30.glDeleteVertexArrays(testVao);
        GL15.glDeleteBuffers(testVbo);
        GL15.glDeleteBuffers(testEbo);

        // 恢复状态
        GL30.glBindVertexArray(savedVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedEbo);
        GL20.glUseProgram(savedProgram);
    }

    /**
     * 绘制前景层
     *
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 若存在工具提示物品则绘制其提示文本
        if (ClientConfig.renderToolTip && selectIndex > -1) {
            ToolData toolData = items.get(selectIndex);
            if (toolData.getSlot() == -1) return;

            // 将鼠标坐标转换为 GUI 坐标系
            int guiMouseX = mouseX - guiLeft;
            int guiMouseY = mouseY - guiTop;

            ItemStack tooltipItem = toolData.getItemStack();
            renderToolTip(tooltipItem, guiMouseX, guiMouseY);
        }
    }

    @Override
    public void drawDefaultBackground() {}

    /**
     * 处理输入事件
     */
    @Override
    public void handleInput() {
        super.handleInput();

        // 获取 KeyBinding 的键码
        int keyCode = GameSettings.selectTool.getKeyCode();

        // 判断是否是鼠标按键
        boolean isMouseButton = keyCode < 0;

        // 检测按键是否松开
        boolean isKeyReleased;
        if (isMouseButton) {
            // 鼠标按键检测（需要转换键码）
            int mouseButton = keyCode + 100;
            isKeyReleased = !Mouse.isButtonDown(mouseButton);
        } else {
            // 键盘按键检测
            isKeyReleased = !Keyboard.isKeyDown(keyCode);
        }

        // 当选择按键松开时执行操作
        if (isKeyReleased) {
            if (selectIndex != -1) {
                int slot = items.get(selectIndex)
                    .getSlot();
                NetworkHandler.NETWORK.sendToServer(new PacketToolboxSelected(slot));
            }
            mc.thePlayer.closeScreen();
        }

    }

    // region NEI
    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        // 隐藏NEI界面
        currentVisibility.showNEI = false;
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button) {
        return true;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return true;
    }
    // endregion

    /**
     * 加载物品到 GUI 中
     *
     * @param stack 玩家当前手持的物品
     */
    private void loadItems(ItemStack stack) {
        items.clear();
        if (ToolboxHelper.isMetaGeneratedTool(stack)) {
            this.items.add(new ToolData(-1, DEFAULT_ITEM));
        }
        final List<ToolData> list = getGTTools(stack);
        if (list == null || list.isEmpty()) return;
        this.items.addAll(list);
    }

    /**
     * 获取 GT 工具箱中的工具
     *
     * @param stack 玩家当前手持的物品
     * @return 工具列表，如果不存在则返回 null
     */
    public List<ToolData> getGTTools(ItemStack stack) {
        return ToolboxHelper.getToolbox(stack)
            .map(toolbox -> {
                if (toolbox.getItem() instanceof ItemToolboxPlus) {
                    return ToolboxHelper.getToolItems(toolbox);
                }
                return null;
            })
            .orElse(null);
    }
}
