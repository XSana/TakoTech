package moe.takochan.takotech.client.settings;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.I18nUtils;

/**
 * 游戏设置类
 * <p>
 * 用于管理游戏中的按键绑定和设置相关逻辑
 */
public class GameSettings {

    // 工具箱选择工具的按键绑定实例
    public static KeyBinding selectTool;

    public GameSettings() {
        selectTool = new KeyBinding(
            I18nUtils.key(NameConstants.KEY_TOOLBOX_PLUS),
            Keyboard.KEY_NONE,
            I18nUtils.key(NameConstants.KEY_CATEGORY));
    }

    public void register() {
        // 注册按键绑定
        ClientRegistry.registerKeyBinding(selectTool);
        // 注册按键处理器
        FMLCommonHandler.instance()
            .bus()
            .register(new KeyHandler());
    }
}
