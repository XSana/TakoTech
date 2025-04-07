package moe.takochan.takotech.client.gui.widget;

import java.awt.Rectangle;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ScrollBar {

    // 滚动条所在区域（相对于内容区域左上角）
    private final Rectangle area;

    // 内容总高度（实际所有内容的总高度）
    private int totalContentHeight;

    // 可视高度（滚动框实际能看到的区域高度）
    private final int visibleHeight;

    // 当前滚动偏移（单位：像素，范围：[0, maxScroll]）
    private int scrollOffset;

    // 拖动相关状态
    private boolean dragging = false;
    private int dragOffsetY = 0;

    /**
     * 构造一个滚动条。
     * 
     * @param area          滚动条区域
     * @param visibleHeight 当前显示区域的高度（不滚动时能看到的内容高度）
     */
    public ScrollBar(Rectangle area, int visibleHeight) {
        this.area = area;
        this.visibleHeight = visibleHeight;
        this.totalContentHeight = visibleHeight; // 默认不滚动
    }

    /**
     * 设置内容总高度。应在内容更新后调用。
     * 
     * @param height 总内容高度
     */
    public void setTotalContentHeight(int height) {
        this.totalContentHeight = Math.max(height, visibleHeight); // 至少不小于可视高度
    }

    /**
     * 获取当前的滚动偏移（像素）
     */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * 处理鼠标滚轮滚动。
     * 鼠标向上滚动时内容向下偏移。
     */
    public void handleMouseInput() {
        int dWheel = Mouse.getDWheel(); // 鼠标滚轮增量
        if (dWheel != 0 && isScrollable()) {
            scrollOffset -= Integer.signum(dWheel) * 6; // 每格滚动 6 像素
            clampScrollOffset(); // 限制滚动范围
        }
    }

    /**
     * 鼠标点击事件，处理拖动开始或点击跳转。
     */
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && isMouseOver(mouseX, mouseY)) {
            int barHeight = getBarHeight();
            int barY = getBarY();

            if (mouseY >= barY && mouseY <= barY + barHeight) {
                // 点击在滑块上：开始拖动
                dragging = true;
                dragOffsetY = mouseY - barY;
            } else {
                // 点击在轨道上：跳转到对应位置
                int clickOffset = mouseY - area.y - barHeight / 2;
                scrollOffset = (int) ((clickOffset / (float) (area.height - barHeight)) * getMaxScroll());
                clampScrollOffset();
            }
        }
    }

    /**
     * 鼠标释放事件，停止拖动。
     */
    public void mouseReleased() {
        dragging = false;
    }

    /**
     * 鼠标拖动事件，实时更新滚动位置。
     * 
     * @param mouseY 当前鼠标 Y 坐标（绝对坐标）
     */
    public void mouseDragged(int mouseY) {
        if (dragging) {
            int barHeight = getBarHeight();
            int newBarY = mouseY - area.y - dragOffsetY;

            // 限制滑块移动范围
            newBarY = Math.max(0, Math.min(newBarY, area.height - barHeight));

            // 反推出滚动位置
            scrollOffset = (int) ((newBarY / (float) (area.height - barHeight)) * getMaxScroll());
            clampScrollOffset();
        }
    }

    /**
     * 绘制滚动条（轨道与滑块）。
     */
    public void draw() {
        if (!isScrollable()) return;

        int barX = area.x + area.width - 6; // 滑块靠右对齐
        int barY = getBarY(); // 当前滑块位置
        int barHeight = getBarHeight(); // 当前滑块高度

        // 绘制滑块（半透明矩形）
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 0.6f);
        drawRect(barX, area.y + barY, barX + 6, area.y + barY + barHeight);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * 使用 OpenGL 绘制一个矩形（无贴图）
     */
    private void drawRect(int x1, int y1, int x2, int y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
    }

    /**
     * 判断鼠标是否在滚动条区域内。
     */
    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= area.x && mouseX <= area.x + area.width && mouseY >= area.y && mouseY <= area.y + area.height;
    }

    /**
     * 判断是否需要滚动（内容高度超过显示高度）
     */
    private boolean isScrollable() {
        return totalContentHeight > visibleHeight;
    }

    /**
     * 最大滚动值（允许的最大 scrollOffset）
     */
    private int getMaxScroll() {
        return totalContentHeight - visibleHeight;
    }

    /**
     * 限制 scrollOffset 不超过允许范围
     */
    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
    }

    /**
     * 计算滑块高度，依据内容高度和显示区域比例决定。
     */
    private int getBarHeight() {
        int h = (int) ((visibleHeight / (float) totalContentHeight) * area.height);
        return Math.max(h, 12); // 最小高度防止太小
    }

    /**
     * 计算当前滑块的 Y 位置（在滚动条轨道中的位置）
     */
    private int getBarY() {
        return (int) ((scrollOffset / (float) getMaxScroll()) * (area.height - getBarHeight()));
    }
}
