package moe.takochan.takotech.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import moe.takochan.takotech.common.Reference;

public class NetworkHandler {

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID);

    public static void init() {
        NETWORK.registerMessage(
            PacketToolboxOpenSelectGUI.Handler.class,
            PacketToolboxOpenSelectGUI.class,
            1,
            Side.SERVER);

        NETWORK.registerMessage(PacketToolboxSelected.Handler.class, PacketToolboxSelected.class, 2, Side.SERVER);

        // 配置同步包（服务端 -> 客户端）
        NETWORK.registerMessage(PacketConfigSync.Handler.class, PacketConfigSync.class, 3, Side.CLIENT);
    }

    /**
     * 向指定玩家发送配置同步包
     */
    public static void sendConfigSync(EntityPlayerMP player) {
        NETWORK.sendTo(PacketConfigSync.fromServerConfig(), player);
    }
}
