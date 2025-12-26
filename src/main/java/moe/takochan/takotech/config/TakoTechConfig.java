package moe.takochan.takotech.config;

import java.io.File;

import moe.takochan.takotech.utils.CommonUtils;

/**
 * TakoTech 配置管理器
 * 统一管理服务端配置和客户端配置的初始化
 */
public class TakoTechConfig {

    /**
     * 初始化所有配置
     */
    public static void init(File configDir) {
        // 服务端配置（双端都需要加载，但客户端的值会被服务端同步覆盖）
        ServerConfig.init(configDir);

        // 客户端配置（仅客户端加载）
        if (CommonUtils.isClient()) {
            ClientConfig.init(configDir);
        }
    }

    /**
     * 保存所有配置
     * 多人模式下只保存客户端配置
     */
    public static void save() {
        // 单人模式下保存服务端配置
        if (!ServerConfig.isSyncedFromServer()) {
            ServerConfig.save();
        }
        // 客户端配置始终保存
        if (CommonUtils.isClient()) {
            ClientConfig.save();
        }
    }
}
