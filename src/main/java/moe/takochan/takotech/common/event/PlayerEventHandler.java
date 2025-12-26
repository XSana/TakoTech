package moe.takochan.takotech.common.event;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.config.ServerConfig;
import moe.takochan.takotech.network.NetworkHandler;

/**
 * 玩家事件处理器
 * 处理玩家登录时的配置同步和断开连接时的配置重置
 */
public class PlayerEventHandler {

    /**
     * 玩家登录时同步服务端配置
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            NetworkHandler.sendConfigSync((EntityPlayerMP) event.player);
            TakoTechMod.LOG.debug("Sent config sync to player: {}", event.player.getDisplayName());
        }
    }

    /**
     * 客户端断开连接时重置为本地配置
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ServerConfig.resetToLocal();
        TakoTechMod.LOG.debug("Client disconnected, reset server config to local values");
    }
}
