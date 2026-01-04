package moe.takochan.takotech.client.input;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.config.ClientConfig;

/**
 * IME 状态处理器。
 * 简单策略：游戏内有 GUI 时启用 IME，无 GUI 时检查输入状态。
 * 游戏外（主菜单等）不管理 IME 状态。
 * 窗口失去焦点时自动恢复 IME，确保其他应用可以正常输入。
 */
@SideOnly(Side.CLIENT)
public class IMEStateHandler {

    /** 上一次 IME 状态 */
    private boolean lastIMEEnabled = true;

    /** 上一次窗口焦点状态 */
    private boolean lastWindowFocused = true;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // 检查配置是否启用
        if (!ClientConfig.enableIMEControl) {
            // 功能禁用时，确保 IME 恢复启用状态
            if (!lastIMEEnabled) {
                lastIMEEnabled = true;
                IMEControl.enableIME();
            }
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        // 游戏外（主菜单、设置等）不管理，保持 IME 启用
        if (mc.theWorld == null) {
            if (!lastIMEEnabled) {
                lastIMEEnabled = true;
                IMEControl.enableIME();
            }
            return;
        }

        // 使用 LWJGL Display.isActive() 检查窗口焦点
        boolean windowFocused = Display.isActive();

        // 窗口失去焦点时恢复 IME
        if (lastWindowFocused && !windowFocused) {
            IMEControl.restoreIME();
            lastIMEEnabled = true;
        }
        lastWindowFocused = windowFocused;

        // 窗口无焦点时不管理 IME
        if (!windowFocused) {
            return;
        }

        // 游戏内：有 GUI 直接启用，无 GUI 检查输入状态
        boolean shouldEnable = mc.currentScreen != null || Keyboard.areRepeatEventsEnabled();

        if (shouldEnable != lastIMEEnabled) {
            lastIMEEnabled = shouldEnable;

            if (shouldEnable) {
                IMEControl.enableIME();
            } else {
                IMEControl.disableIME();
            }
        }
    }
}
