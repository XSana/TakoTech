package moe.takochan.takotech.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import moe.takochan.takotech.common.Reference;

@Config(modid = Reference.MODID, configSubDirectory = "TakoTech", filename = "client", category = "ime")
public class ClientConfig {

    @Config.Comment({ "启用 IME（输入法）自动控制功能", "开启后，游戏内无 GUI 时自动禁用输入法，防止 WASD 等按键被输入法拦截", "打开 GUI 时自动启用输入法，方便输入中文",
        "仅在 Windows 系统上生效", "Enable automatic IME (Input Method Editor) control",
        "When enabled, IME will be disabled when no GUI is open in-game,",
        "preventing keys like WASD from being intercepted by the input method",
        "IME will be enabled when a GUI is open for Chinese input", "Only works on Windows" })
    @Config.DefaultBoolean(true)
    public static boolean enableIMEControl;
}
