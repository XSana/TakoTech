package moe.takochan.takotech.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import moe.takochan.takotech.common.Reference;

@Config(modid = Reference.MODID, configSubDirectory = "TakoTech", filename = "config", category = "webcontroller")
public class WebControllerConfig {

    @Config.Comment("表示功能是否启用，若设置为 true，则启用该功能")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean isEnabled;

    @Config.Comment("表示是否允许玩家使用自托管（自建）服务器进行连接。")
    @Config.DefaultBoolean(false)
    public static boolean allowSelfHosting;

    @Config.Comment("默认的服务器 IP 地址或域名。")
    @Config.DefaultString("127.0.0.1")
    public static String defaultServerAddress;

    @Config.Comment("服务器连接的默认端口号。")
    @Config.DefaultInt(11451)
    @Config.RangeInt(min = 0, max = 65535)
    public static int defaultPort;

    @Config.Comment("是否使用 (SSL/TLS) 加密连接。如果服务端启用了https则启用。")
    @Config.DefaultBoolean(false)
    public static boolean useSSL;
}
