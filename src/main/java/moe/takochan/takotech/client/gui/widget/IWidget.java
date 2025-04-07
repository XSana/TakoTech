package moe.takochan.takotech.client.gui.widget;

public interface IWidget {

    /**
     * 渲染组件内容。调用者应在适当的 OpenGL 坐标变换后调用本方法。
     * <p>
     * 一般由容器或 GUI 主类负责坐标变换。
     */
    void draw();

    /**
     * 当鼠标点击时触发。
     *
     * @param mouseX 相对于容器的 X 坐标
     * @param mouseY 相对于容器的 Y 坐标
     * @param button 鼠标按钮编号（0=左键，1=右键）
     */
    void mouseClicked(int mouseX, int mouseY, int button);

    /**
     * 鼠标释放事件。
     */
    void mouseReleased();

    /**
     * 鼠标拖动事件。
     *
     * @param mouseY 当前 Y 坐标（相对于组件）
     */
    void mouseDragged(int mouseY);

    /**
     * 判断鼠标是否悬停在组件范围内。
     *
     * @param mouseX 相对于组件左上角的 X 坐标
     * @param mouseY 相对于组件左上角的 Y 坐标
     * @return 若鼠标在组件内，则返回 true
     */
    boolean isMouseOver(int mouseX, int mouseY);

    /**
     * 获取组件的 Y 坐标（相对于其父容器或 GUI 区域）。
     */
    int getX();

    /**
     * @return 相对于容器顶部的 Y 坐标（用于裁剪判断）
     */
    int getY();

    /**
     * 获取组件宽度（像素）
     */
    int getWidth();

    /**
     * 获取组件高度（像素）
     */
    int getHeight();
}
