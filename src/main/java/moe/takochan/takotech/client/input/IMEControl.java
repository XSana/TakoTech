package moe.takochan.takotech.client.input;

import org.lwjgl.LWJGLUtil;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;

/**
 * Windows IME (输入法) 控制工具类。
 * 用于在游戏非输入状态时禁用输入法，避免 WASD 等按键被输入法拦截。
 *
 * <p>
 * 通过 KeyboardMixin hook {@code Keyboard.enableRepeatEvents()} 实现自动控制:
 * </p>
 * <ul>
 * <li>enableRepeatEvents(true) → 启用 IME (需要文本输入)</li>
 * <li>enableRepeatEvents(false) → 禁用 IME (不需要文本输入)</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class IMEControl {

    private static final boolean IS_WINDOWS = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS;

    /** IMM32 库接口 */
    private interface Imm32 extends StdCallLibrary {

        Imm32 INSTANCE = IS_WINDOWS ? Native.load("imm32", Imm32.class) : null;

        /**
         * 获取窗口的输入法上下文
         */
        Pointer ImmGetContext(HWND hwnd);

        /**
         * 释放输入法上下文
         */
        boolean ImmReleaseContext(HWND hwnd, Pointer hIMC);

        /**
         * 关联输入法上下文到窗口
         *
         * @param hwnd 窗口句柄
         * @param hIMC 输入法上下文，null 表示禁用
         * @return 之前关联的上下文
         */
        Pointer ImmAssociateContext(HWND hwnd, Pointer hIMC);

        /**
         * 关联输入法上下文（扩展版本）
         *
         * @param hwnd  窗口句柄
         * @param hIMC  输入法上下文
         * @param flags 标志位
         */
        boolean ImmAssociateContextEx(HWND hwnd, Pointer hIMC, int flags);
    }

    /** ImmAssociateContextEx 标志：恢复默认 IME 上下文 */
    private static final int IACE_DEFAULT = 0x0010;

    /** 保存的原始 IME 上下文 */
    private static Pointer savedIMC = null;

    /** 当前 IME 状态 */
    private static boolean imeEnabled = true;

    /** 是否已初始化 */
    private static boolean initialized = false;

    private IMEControl() {}

    /**
     * 初始化 IME 控制
     * 应在游戏启动时调用一次
     */
    public static void init() {
        if (!IS_WINDOWS || initialized) {
            return;
        }

        try {
            // 验证 JNA 可用
            if (Imm32.INSTANCE == null) {
                TakoTechMod.LOG.warn("[IMEControl] Failed to load imm32.dll");
                return;
            }

            initialized = true;
            TakoTechMod.LOG.info("[IMEControl] Initialized successfully");
        } catch (Throwable e) {
            TakoTechMod.LOG.error("[IMEControl] Failed to initialize", e);
        }
    }

    /**
     * 启用输入法
     * 在打开聊天框、命令输入等需要中文输入时调用
     */
    public static void enableIME() {
        if (!IS_WINDOWS || !initialized || imeEnabled) {
            return;
        }

        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return;
            }

            if (savedIMC != null) {
                // 恢复保存的 IME 上下文
                Imm32.INSTANCE.ImmAssociateContext(hwnd, savedIMC);
                savedIMC = null;
            } else {
                // 使用默认恢复方式
                Imm32.INSTANCE.ImmAssociateContextEx(hwnd, null, IACE_DEFAULT);
            }

            imeEnabled = true;
            TakoTechMod.LOG.debug("[IMEControl] IME enabled");
        } catch (Throwable e) {
            TakoTechMod.LOG.error("[IMEControl] Failed to enable IME", e);
        }
    }

    /**
     * 禁用输入法
     * 在游戏世界中（无输入框）时调用，防止输入法拦截 WASD 等按键
     */
    public static void disableIME() {
        if (!IS_WINDOWS || !initialized || !imeEnabled) {
            return;
        }

        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return;
            }

            // 保存当前 IME 上下文
            savedIMC = Imm32.INSTANCE.ImmGetContext(hwnd);
            if (savedIMC != null) {
                Imm32.INSTANCE.ImmReleaseContext(hwnd, savedIMC);
            }

            // 禁用 IME (设置上下文为 null)
            Imm32.INSTANCE.ImmAssociateContext(hwnd, null);

            imeEnabled = false;
            TakoTechMod.LOG.debug("[IMEControl] IME disabled");
        } catch (Throwable e) {
            TakoTechMod.LOG.error("[IMEControl] Failed to disable IME", e);
        }
    }

    /**
     * 检查 IME 控制是否可用
     */
    public static boolean isAvailable() {
        return IS_WINDOWS && initialized;
    }

    /**
     * 检查 IME 当前是否启用
     */
    public static boolean isEnabled() {
        return imeEnabled;
    }

    /**
     * 窗口失去焦点时恢复 IME
     * 确保切换到其他应用时输入法正常工作
     */
    public static void restoreIME() {
        if (!IS_WINDOWS || !initialized || imeEnabled) {
            return;
        }

        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return;
            }

            if (savedIMC != null) {
                Imm32.INSTANCE.ImmAssociateContext(hwnd, savedIMC);
                savedIMC = null;
            } else {
                Imm32.INSTANCE.ImmAssociateContextEx(hwnd, null, IACE_DEFAULT);
            }
            imeEnabled = true;
            TakoTechMod.LOG.debug("[IMEControl] IME restored");
        } catch (Throwable e) {
            TakoTechMod.LOG.error("[IMEControl] Failed to restore IME", e);
        }
    }
}
