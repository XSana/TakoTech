package moe.takochan.takotech.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import moe.takochan.takotech.TakoTechMod;

/**
 * 客户端配置（不同步，仅客户端本地）
 * 包含渲染、UI、输入法等纯客户端设置
 */
public class ClientConfig {

    private static Configuration config;

    // Toolbox UI
    public static boolean renderToolTip;

    // IME
    public static boolean enableIMEControl;

    /**
     * 初始化配置
     */
    public static void init(File configDir) {
        File takoTechDir = new File(configDir, "TakoTech");
        if (!takoTechDir.exists()) {
            takoTechDir.mkdirs();
        }

        config = new Configuration(new File(takoTechDir, "client.cfg"));
        load();
    }

    /**
     * 加载配置
     */
    public static void load() {
        try {
            config.load();

            // Toolbox UI
            config.setCategoryComment("toolbox", "工具箱界面配置");
            renderToolTip = config.getBoolean("renderToolTip", "toolbox", true, "工具选择界面显示工具提示");

            // IME
            config.setCategoryComment("ime", "输入法控制配置\n" + "自动管理 Windows 输入法状态，防止中文输入法干扰游戏操作");
            enableIMEControl = config.getBoolean(
                "enableIMEControl",
                "ime",
                true,
                "启用输入法自动控制\n" + "游戏内无 GUI 时自动禁用输入法，防止 WASD 等按键被拦截\n" + "打开 GUI 时自动启用输入法，方便输入中文\n" + "仅在 Windows 系统上生效");

        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to load client config", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    /**
     * 获取配置实例（用于 GUI）
     */
    public static Configuration getConfig() {
        return config;
    }

    /**
     * 保存配置并同步静态字段
     */
    public static void save() {
        if (config == null) {
            return;
        }

        // 从 Configuration 对象同步到静态字段
        renderToolTip = config.getCategory("toolbox")
            .get("renderToolTip")
            .getBoolean();
        enableIMEControl = config.getCategory("ime")
            .get("enableIMEControl")
            .getBoolean();

        // 保存到文件
        config.save();
    }
}
