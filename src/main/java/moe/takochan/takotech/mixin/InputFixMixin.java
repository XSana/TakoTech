package moe.takochan.takotech.mixin;

import javax.swing.JOptionPane;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.LWJGLUtil;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiScreen.class)
public abstract class InputFixMixin {

    @Unique
    private static final boolean IS_WINDOWS = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS;

    @Shadow
    protected abstract void keyTyped(char typedChar, int keyCode);

    /**
     * 在 GuiScreen 中修复输入中文的问题
     *
     * @author XSana
     * @reason 修复输入中文的问题
     */
    @Overwrite
    public void handleKeyboardInput() {
        // 获取键盘输入字符
        char c = Keyboard.getEventCharacter();
        // 获取键盘按键的代码
        int k = Keyboard.getEventKey();

        // 判断是否是按下事件，且按键有效
        if (Keyboard.getEventKeyState() || (k == 0 && Character.isDefined(c))) {
            // 对于非 Windows 平台，处理特殊输入
            if (!IS_WINDOWS && k == 88) {
                takoTech$handleInputDialog();
                return; // 处理完后直接返回
            }
            // 其他情况，直接处理字符输入
            this.keyTyped(c, k);
        }
    }

    /**
     * 处理输入法对话框(非Windows平台)
     */
    @Unique
    private void takoTech$handleInputDialog() {
        String input = JOptionPane.showInputDialog("");
        if (input != null && !input.isEmpty()) {
            for (char c1 : input.toCharArray()) {
                this.keyTyped(c1, 0);
            }
        }
    }
}
