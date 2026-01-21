package moe.takochan.takotech.client.gui.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

import moe.takochan.takotech.common.Reference;

/**
 * 模组配置界面GUI
 */
public class TakoTechGuiConfig extends SimpleGuiConfig {

    public TakoTechGuiConfig(GuiScreen parent) throws ConfigException {
        // 使用无参版本，自动获取所有已注册的配置类
        super(parent, Reference.MODID, Reference.MODNAME);
    }
}
