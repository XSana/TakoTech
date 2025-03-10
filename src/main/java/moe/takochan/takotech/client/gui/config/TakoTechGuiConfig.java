package moe.takochan.takotech.client.gui.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.config.TakoTechConfig;

/**
 * 模组配置界面GUI
 */
public class TakoTechGuiConfig extends SimpleGuiConfig {

    public TakoTechGuiConfig(GuiScreen parent) throws ConfigException {
        super(parent, TakoTechConfig.class, Reference.MODID, Reference.MODNAME);
    }
}
