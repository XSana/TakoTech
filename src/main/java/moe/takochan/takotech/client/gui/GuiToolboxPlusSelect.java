package moe.takochan.takotech.client.gui;

import static org.lwjgl.BufferUtils.createFloatBuffer;
import static org.lwjgl.opengl.GL11.*;

import java.nio.FloatBuffer;
import java.util.List;

import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import cpw.mods.fml.common.Loader;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.client.gui.shader.ShaderProgram;
import moe.takochan.takotech.common.Reference;

public class GuiToolboxPlusSelect extends GuiContainer implements INEIGuiHandler {

    private static final ShaderProgram SHADER = new ShaderProgram(
        Reference.RESOURCE_ROOT_ID,
        Reference.RESOURCE_ROOT_ID,
        "shaders/ToolboxPlusSelectGui.vert",
        "shaders/ToolboxPlusSelectGui.frag");

    // 顶点缓冲区对象 (VBO)
    private static final int VBO = GL15.glGenBuffers();
    private final ItemStack stack;
    private static final Minecraft mc = Minecraft.getMinecraft();

    // 缩放因子（根据分辨率动态计算）
    private int scaleFactor = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
    private static final boolean hasLwjgl3 = Loader.isModLoaded("lwjgl3ify");

    // !!! 需要修改：将固定6扇区改为动态值，建议从配置或着色器参数获取 !!!
    private int sectorCount = 0; // 此处应替换为动态值，例如从配置读取

    private NBTTagList itemsTagList; // 存储从 NBT 中读取的物品列表

    public GuiToolboxPlusSelect(ContainerToolboxPlusSelect container, ItemStack itemStack) {
        super(container);
        this.xSize = mc.displayWidth;
        this.ySize = mc.displayHeight;
        // this.xSize = 256;

        this.stack = itemStack;
        loadItemsFromNBT();

        if (sectorCount <= 1) {
            // 立即关闭GUI
            Minecraft.getMinecraft()
                .displayGuiScreen(this);
        }
    }

    private void loadItemsFromNBT() {
        NBTTagCompound nbt = this.stack.getTagCompound();
        if (nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            itemsTagList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            sectorCount = itemsTagList.tagCount() + 1;
        } else {
            itemsTagList = new NBTTagList(); // 空列表
            sectorCount = 0;
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        scaleFactor = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        float x = mc.displayWidth, y = mc.displayHeight;
        float[] vertices = {0, 0, 0, y, x, y, x, 0};

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length)
            .put(vertices);
        vertexBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        FloatBuffer buf1 = createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf1);
        FloatBuffer buf2 = createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buf2);
        SHADER.use();
        GL20.glUniformMatrix4(GL20.glGetUniformLocation(SHADER.getProgram(), "modelview"), false, buf1);
        GL20.glUniformMatrix4(GL20.glGetUniformLocation(SHADER.getProgram(), "projection"), false, buf2);
        GL20.glUniform2f(GL20.glGetUniformLocation(SHADER.getProgram(), "iResolution"), x, y);
        GL20.glUniform1f(GL20.glGetUniformLocation(SHADER.getProgram(), "scaleFactor"), scaleFactor);
        GL20.glUniform1f(GL20.glGetUniformLocation(SHADER.getProgram(), "sectorCount"), sectorCount);
        SHADER.clear();
    }

    @Override
    public void handleInput() {
        if (Mouse.isCreated()) {
            while (Mouse.next()) {
                this.handleMouseInput();
            }
        }

        if (Keyboard.isCreated()) {
            while (Keyboard.next()) {
                this.handleKeyboardInput();
            }
            if (!Keyboard.isKeyDown(GameSettings.selectTool.getKeyCode())) {
                // TakoTechMod.NETWORK
                // .sendToServer(new MultiItemToolMessage(updateMode(Mouse.getX(), Mouse.getY())));
                mc.thePlayer.closeScreen();
            }
        }
    }

    @Override
    public void onGuiClosed() {
    }

    @Override
    public void drawDefaultBackground() {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        SHADER.use();

        GL20.glUniform2f(GL20.glGetUniformLocation(SHADER.getProgram(), "iMouse"), Mouse.getX(), Mouse.getY());

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, VBO);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);

        GL11.glDrawArrays(GL_QUADS, 0, 4);

        GL20.glDisableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        SHADER.clear();

        glDisable(GL11.GL_BLEND);
        glEnable(GL11.GL_DEPTH_TEST);

        GL11.glPopMatrix();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GL11.glPushMatrix();
        float innerRadius = 20.0f;
        float outerRadius = 45.0f;
        int centerX = mc.displayWidth / 2;
        int centerY = mc.displayHeight / 2;
        float sectorSize = (float) (2 * Math.PI) / sectorCount;
        float startAngleOffset = (float) (3 * Math.PI + Math.PI / 6);

        // 遍历所有扇区
        for (short i = 0; i < sectorCount; i++) {
            float centerAngle = startAngleOffset + i * sectorSize;
            float iconRadius = (innerRadius + outerRadius) / 2;
            int iconX = centerX + (int) (iconRadius * Math.cos(-centerAngle));
            int iconY = centerY - (int) (iconRadius * Math.sin(-centerAngle));

            if (i < itemsTagList.tagCount()) {
                NBTTagCompound itemTag = itemsTagList.getCompoundTagAt(i);
                ItemStack itemStack = ItemStack.loadItemStackFromNBT(itemTag);
                // 渲染物品
                itemRender.renderItemAndEffectIntoGUI(
                    this.fontRendererObj,
                    mc.getTextureManager(),
                    itemStack,
                    iconX - 8,
                    iconY - 8);
            } else {
                itemRender.renderItemAndEffectIntoGUI(
                    this.fontRendererObj,
                    mc.getTextureManager(),
                    this.stack,
                    iconX - 8,
                    iconY - 8);
            }
        }

        GL11.glPopMatrix();
    }

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
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
}
