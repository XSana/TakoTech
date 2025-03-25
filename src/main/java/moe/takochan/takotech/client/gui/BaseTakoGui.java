package moe.takochan.takotech.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import moe.takochan.takotech.client.gui.container.BaseContainer;
import moe.takochan.takotech.common.Reference;
import org.lwjgl.opengl.GL11;

public abstract class BaseTakoGui<T extends BaseContainer> extends GuiContainer {

    private final static Minecraft mc = Minecraft.getMinecraft();

    private final ResourceLocation GUI_TEXTURE = new ResourceLocation(Reference.RESOURCE_ROOT_ID,
        "textures/guis/base_gui.png");

    private final T container;

    private final String title;
    private final int windowWidth;
    private final int windowHeight;
    private int titleBarWidth;

    public BaseTakoGui(T container, String title, int windowWidth, int windowHeight) {
        super(container);
        this.container = container;
        this.title = title;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        titleBarWidth = 0;
    }

    public T getContainer() {
        return container;
    }

    public String getTitle() {
        return title;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    @Override
    public void initGui() {
        this.xSize = windowWidth;
        this.ySize = windowHeight + 16;

        super.initGui();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制标题栏
        drawTitleBar(guiLeft, guiTop, windowWidth);

        // 绘制主体
        drawBody(guiLeft, guiTop + 16, windowWidth, windowHeight);

        // 绘制底部
        drawBottom(guiLeft, guiTop + windowHeight - 3, windowWidth);
    }

    private void drawTitleBar(int x, int y, int width) {
        int titleWidth = fontRendererObj.getStringWidth(title);
        int padding = 8; // 文字两侧留白
        int leftWidth = 3; // 左侧固定3像素
        int rightBaseWidth = 15; // 右侧基础部分15像素

        // 中间段最小保持4像素
        int middleWidth = Math.max(titleWidth + padding, 4);

        // 计算基础部分总宽度
        titleBarWidth = leftWidth + middleWidth + rightBaseWidth;

        // 绘制左侧 (1-3像素)
        drawTexturedModalRect(x, y, 0, 0, 3, 16);

        // 中间拉伸段 (4号像素)
        drawModalRectWithCustomSizedTexture(x + 3, y, 3, 0, middleWidth, 16, 1, 16);

        // 右侧基础部分 (5-20像素中的前16像素)
        drawTexturedModalRect(x + 3 + middleWidth, y, 4, 0, 16, 16);

        // 计算延伸区域剩余空间
        int remaining = width - titleBarWidth - 2; // 总宽-基础部分-2px边距
        if (remaining > 0) {
            // 右侧延伸部分 (16,0开始的1像素宽纹理)
            drawModalRectWithCustomSizedTexture(x + titleBarWidth, y, 20, 0, // 新纹理坐标
                remaining, 16, 1, 16  // 1像素宽纹理横向拉伸
            );
        }

        // 标题文字居中绘制
        int textX = x + 4 + (middleWidth - titleWidth) / 2;
        int textY = y + 5; // 垂直居中
        //        fontRendererObj.drawString(title, textX, textY, 0x404040);
    }

    private void drawBody(int x, int y, int width, int height) {

        // 尺寸约束计算
        int actualWidth = Math.max(width, titleBarWidth); // 保证最小宽度
        int actualHeight = Math.max(height - 3 - 16, 3);  // 最小高度3px

        // 左侧装饰（20,0-20,1）
        drawModalRectWithCustomSizedTexture(x, y, // 绘制起点
            0, 16,   // 纹理起点
            2, actualHeight, // 绘制尺寸
            2, 1      // 纹理截取尺寸（1x2像素）
        );

        // 中间平铺区域（16,0-18,2）
        int middleWidth = actualWidth - 2 - 1; // 总宽 - 左右装饰
        int segmentWidth = Math.max(3, middleWidth);
        drawModalRectWithCustomSizedTexture(x + 2, y, 0, 17, segmentWidth, 3, 1, 3);
        drawModalRectWithCustomSizedTexture(x + 2, y + 3, 2, 16,       // 纹理起点
            segmentWidth, actualHeight - 3, 1, 1         // 使用3x3纹理块
        );

        // 右侧装饰
        if (actualWidth > titleBarWidth) {
            drawTexturedModalRect(x + actualWidth - 2, y, 1, 17, 3, 3);
            drawModalRectWithCustomSizedTexture(x + actualWidth - 1, y + 3, 3, 16, 2, actualHeight - 3, 2, 1);
        } else {
            drawModalRectWithCustomSizedTexture(x + actualWidth - 1, y, 3, 16, 2, actualHeight, 2, 1);
        }
    }

    private void drawBottom(int x, int y, int width) {
        // 底部左 (16x16)
        drawTexturedModalRect(x, y, 4, 17, 3, 3);
        // 底部中 (动态拉伸)
        drawModalRectWithCustomSizedTexture(x + 3, y-1, 7, 17, width - 3 - 2, 4, 1, 3);
        // 底部右 (16x16)
        drawTexturedModalRect(x + width - 2, y, 8, 17, 3, 3);
    }

    private void drawModalRectWithCustomSizedTexture(int x, int y, int textureX, int textureY, int width, int height,
        int textureWidth, int textureHeight) {
        float uScale = 1.0F / 256.0F; // 贴图宽度比例
        float vScale = 1.0F / 256.0F; // 贴图高度比例
        float u = textureX * uScale;
        float v = textureY * vScale;
        float u2 = (textureX + textureWidth) * uScale;
        float v2 = (textureY + textureHeight) * vScale;

        // 绘制动态拉伸部分
        drawTexturedModalRect2(x, y, width, height, u, v, u2, v2);
    }

    private void drawTexturedModalRect2(int x, int y, int width, int height, float u, float v, float u2, float v2) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, this.zLevel, u, v2);
        tessellator.addVertexWithUV(x + width, y + height, this.zLevel, u2, v2);
        tessellator.addVertexWithUV(x + width, y, this.zLevel, u2, v);
        tessellator.addVertexWithUV(x, y, this.zLevel, u, v);
        tessellator.draw();
    }

}
