package moe.takochan.takotech.client.gui.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

/**
 * 配置 GUI 的工厂
 */
public class TakoTechGuiConfigFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return TakoTechGuiConfig.class;
    }
}
