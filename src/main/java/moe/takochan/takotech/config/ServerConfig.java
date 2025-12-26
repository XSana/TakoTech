package moe.takochan.takotech.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import moe.takochan.takotech.TakoTechMod;

/**
 * 服务端配置
 * 单人游戏时使用本地配置，多人游戏时从服务端同步（不覆盖本地文件）
 */
public class ServerConfig {

    public static final String CATEGORY_ORE_STORAGE = "ore_storage_cell";
    public static final String CATEGORY_WEB_CONTROLLER = "webcontroller";

    private static Configuration config;

    // 本地配置值
    private static String[] localOreDefs;
    private static boolean localWebControllerEnabled;
    private static boolean localAllowSelfHosting;
    private static String localDefaultServerAddress;
    private static int localDefaultPort;
    private static boolean localUseSSL;

    // 运行时配置值
    public static String[] oreDefs;
    public static boolean webControllerEnabled;
    public static boolean allowSelfHosting;
    public static String defaultServerAddress;
    public static int defaultPort;
    public static boolean useSSL;

    private static boolean syncedFromServer = false;

    /**
     * 初始化配置
     */
    public static void init(File configDir) {
        File takoTechDir = new File(configDir, "TakoTech");
        if (!takoTechDir.exists()) {
            takoTechDir.mkdirs();
        }

        config = new Configuration(new File(takoTechDir, "server.cfg"));
        load();
    }

    /**
     * 加载配置
     */
    public static void load() {
        try {
            config.load();

            // Ore Storage Cell
            config.setCategoryComment(CATEGORY_ORE_STORAGE, "矿物存储元件配置");

            localOreDefs = config.getStringList(
                "oreDefs",
                CATEGORY_ORE_STORAGE,
                new String[] { "ore", "rawOre", "crushed", "dustImpure", "dustPure" },
                "矿物存储元件匹配的矿典前缀（不支持正则）");

            // WebController
            config.setCategoryComment(CATEGORY_WEB_CONTROLLER, "Web控制器配置");

            localWebControllerEnabled = config
                .getBoolean("isEnabled", CATEGORY_WEB_CONTROLLER, true, "是否启用 Web 控制器功能 [需要重启游戏]");

            localAllowSelfHosting = config
                .getBoolean("allowSelfHosting", CATEGORY_WEB_CONTROLLER, false, "是否允许玩家使用自托管服务器");

            localDefaultServerAddress = config
                .getString("defaultServerAddress", CATEGORY_WEB_CONTROLLER, "127.0.0.1", "默认服务器地址");

            localDefaultPort = config.getInt("defaultPort", CATEGORY_WEB_CONTROLLER, 11451, 0, 65535, "默认服务器端口");

            localUseSSL = config.getBoolean("useSSL", CATEGORY_WEB_CONTROLLER, false, "是否使用 SSL/TLS 加密连接");

            // 初始化运行时值为本地值
            resetToLocal();

        } catch (Exception e) {
            TakoTechMod.LOG.error("Failed to load server config", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    /**
     * 重置运行时值为本地配置值
     */
    public static void resetToLocal() {
        oreDefs = localOreDefs;
        webControllerEnabled = localWebControllerEnabled;
        allowSelfHosting = localAllowSelfHosting;
        defaultServerAddress = localDefaultServerAddress;
        defaultPort = localDefaultPort;
        useSSL = localUseSSL;
        syncedFromServer = false;
    }

    /**
     * 从服务端同步配置（仅覆盖运行时值，不修改本地配置文件）
     *
     * @param isRemoteServer 是否为远程服务器（true=多人模式，false=单人模式）
     */
    public static void applyFromSync(String[] syncOreDefs, boolean syncWebControllerEnabled,
        boolean syncAllowSelfHosting, String syncDefaultServerAddress, int syncDefaultPort, boolean syncUseSSL,
        boolean isRemoteServer) {
        oreDefs = syncOreDefs;
        webControllerEnabled = syncWebControllerEnabled;
        allowSelfHosting = syncAllowSelfHosting;
        defaultServerAddress = syncDefaultServerAddress;
        defaultPort = syncDefaultPort;
        useSSL = syncUseSSL;
        // 只有远程服务器才标记为已同步（多人模式）
        syncedFromServer = isRemoteServer;
    }

    /**
     * 是否已从服务端同步配置（用于判断是否在多人游戏中）
     */
    public static boolean isSyncedFromServer() {
        return syncedFromServer;
    }

    /**
     * 获取配置实例（用于 GUI）
     */
    public static Configuration getConfig() {
        return config;
    }

    /**
     * 重新加载配置文件
     */
    public static void reload() {
        if (config != null) {
            load();
            TakoTechMod.LOG.info("Server config reloaded");
        }
    }

    /**
     * 保存配置并同步静态字段
     */
    public static void save() {
        if (config == null) {
            return;
        }

        // 从 Configuration 对象同步到静态字段和本地值
        localOreDefs = config.getCategory(CATEGORY_ORE_STORAGE)
            .get("oreDefs")
            .getStringList();
        localWebControllerEnabled = config.getCategory(CATEGORY_WEB_CONTROLLER)
            .get("isEnabled")
            .getBoolean();
        localAllowSelfHosting = config.getCategory(CATEGORY_WEB_CONTROLLER)
            .get("allowSelfHosting")
            .getBoolean();
        localDefaultServerAddress = config.getCategory(CATEGORY_WEB_CONTROLLER)
            .get("defaultServerAddress")
            .getString();
        localDefaultPort = config.getCategory(CATEGORY_WEB_CONTROLLER)
            .get("defaultPort")
            .getInt();
        localUseSSL = config.getCategory(CATEGORY_WEB_CONTROLLER)
            .get("useSSL")
            .getBoolean();

        // 同步到运行时值（单人模式）
        oreDefs = localOreDefs;
        webControllerEnabled = localWebControllerEnabled;
        allowSelfHosting = localAllowSelfHosting;
        defaultServerAddress = localDefaultServerAddress;
        defaultPort = localDefaultPort;
        useSSL = localUseSSL;

        // 保存到文件
        config.save();
    }
}
