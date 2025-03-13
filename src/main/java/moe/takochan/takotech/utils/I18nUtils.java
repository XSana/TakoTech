package moe.takochan.takotech.utils;

import net.minecraft.client.resources.I18n;

public class I18nUtils {

    // 基础域名常量
    private static final String DOMAIN_TILE = "tile.takotech.%s.name";
    private static final String DOMAIN_GUI = "gui.takotech.%s";
    private static final String DOMAIN_TOOLTIP = "tooltip.takotech.%s";
    private static final String DOMAIN_KEY = "key.takotech.%s";

    // Tile 实体名称
    public static String tile(String key) {
        return format(DOMAIN_TILE, key);
    }

    // GUI 文字
    public static String gui(String key) {
        return format(DOMAIN_GUI, key);
    }

    // 物品提示信息
    public static String tooltip(String key) {
        return format(DOMAIN_TOOLTIP, key);
    }

    // 按键绑定名称
    public static String key(String key) {
        return format(DOMAIN_KEY, key);
    }

    // 带参数的格式化方法
    public static String tooltip(String key, Object... args) {
        return I18n.format(String.format(DOMAIN_TOOLTIP, key), args);
    }

    // 通用格式化方法
    private static String format(String domain, String key) {
        return I18n.format(domain.replaceFirst("%s", key));
    }
}
