package moe.takochan.takotech.client.gui.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.config.ClientConfig;
import moe.takochan.takotech.config.ServerConfig;

/**
 * 模组配置界面 GUI
 * 分为服务端配置和客户端配置两大类
 * 多人模式下服务端配置为只读（显示从服务端同步的值）
 */
public class TakoTechGuiConfig extends GuiConfig {

    public TakoTechGuiConfig(GuiScreen parent) {
        super(parent, getConfigElements(), Reference.MODID, false, false, getTitle());
    }

    private static String getTitle() {
        return Reference.MODNAME + " 配置";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();

        boolean isMultiplayer = ServerConfig.isSyncedFromServer();

        // 服务端配置（多人模式下不显示，避免信息泄露）
        if (!isMultiplayer) {
            Configuration serverConfig = ServerConfig.getConfig();
            if (serverConfig != null) {
                List<IConfigElement> serverElements = new ArrayList<>();
                serverElements.add(new ConfigElement(serverConfig.getCategory(ServerConfig.CATEGORY_ORE_STORAGE)));
                serverElements.add(new ConfigElement(serverConfig.getCategory(ServerConfig.CATEGORY_WEB_CONTROLLER)));
                list.add(new DummyCategoryElement("服务端配置", "takotech.config.server", serverElements));
            }
        }

        // 客户端配置（始终显示）
        Configuration clientConfig = ClientConfig.getConfig();
        if (clientConfig != null) {
            List<IConfigElement> clientElements = new ArrayList<>();
            clientElements.add(new ConfigElement(clientConfig.getCategory("toolbox")));
            clientElements.add(new ConfigElement(clientConfig.getCategory("ime")));
            list.add(new DummyCategoryElement("客户端配置", "takotech.config.client", clientElements));
        }

        return list;
    }
}
