package moe.takochan.takotech.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import moe.takochan.takotech.common.Reference;

@Config(modid = Reference.MODID, configSubDirectory = "TakoTech", filename = "config", category = "toolbox")
public class ToolboxConfig {

    @Config.Comment("工具选择界面是渲染工具tooltip。")
    @Config.DefaultBoolean(true)
    public static boolean renderToolTip;
}
