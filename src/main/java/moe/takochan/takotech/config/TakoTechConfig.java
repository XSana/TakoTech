package moe.takochan.takotech.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import moe.takochan.takotech.common.Reference;

@Config(modid = Reference.MODID, configSubDirectory = "TakoTech", filename = "config")
public class TakoTechConfig {

    @Config.Comment("匹配矿典前缀（不支持正则哦）")
    @Config.DefaultStringList(
        value = { "ore", // 矿石，粗矿oreRaw
            "rawOre", // 粗矿
            "crushed", // 粉碎，洗净，离心
            "dustImpure", // 含杂粉
            "dustPure" // 洁净粉
        })
    @Config.Sync
    public static String[] oreDefs;

    /**
     * 初始化配置。
     */
    public static void init() {
        try {
            ConfigurationManager.registerConfig(TakoTechConfig.class);
            ConfigurationManager.registerConfig(WebControllerConfig.class);
            ConfigurationManager.registerConfig(ToolboxConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }
}
