package moe.takochan.takotech.utils;

import net.minecraft.client.resources.I18n;

public class I18nUtils {

    // 获取 Tile 名称
    public static String tile(String key) {
        return I18n.format("tile.takotech." + key + ".name");
    }

    // 获取 GUI 名称
    public static String gui(String key) {
        return I18n.format("gui.takotech." + key);
    }

    // 获取 GUI Tooltip
    public static String tooltip(String key) {
        return I18n.format("tooltip.takotech." + key);
    }

    public static String key(String key) {
        return I18n.format("key.takotech." + key);
    }
}
