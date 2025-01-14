package moe.takochan.takotech.client.gui.config;

import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;
import net.minecraft.client.gui.GuiScreen;

/**
 * 配置 GUI 的工厂
 */
public class TakoTechGuiConfigFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return TakoTechGuiConfig.class;
    }
}
