package moe.takochan.takotech.config;

import java.io.File;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import moe.takochan.takotech.reference.Reference;
import net.minecraftforge.common.config.Configuration;

@Config(modid = Reference.MODID, configSubDirectory = "TakoTech", filename = "config")
public class TakoTechConfig {

    /**
     * 初始化配置。
     */
    public static void init() {
        try {
            ConfigurationManager.registerConfig(TakoTechConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }
}
