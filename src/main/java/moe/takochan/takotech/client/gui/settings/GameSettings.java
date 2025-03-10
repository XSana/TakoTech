package moe.takochan.takotech.client.gui.settings;

import net.minecraft.client.settings.KeyBinding;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.I18nUtils;
import org.lwjgl.input.Keyboard;

public class GameSettings {

    public static KeyBinding selectTool;

    public GameSettings() {
        selectTool = new KeyBinding(
            I18nUtils.key(NameConstants.KEY_TOOLBOX_PLUS),
            Keyboard.KEY_NONE,
            I18nUtils.key(NameConstants.KEY_CATEGORY));
    }

    public void register() {
        ClientRegistry.registerKeyBinding(selectTool);
        FMLCommonHandler.instance()
            .bus()
            .register(new KeyHandler());
    }
}
