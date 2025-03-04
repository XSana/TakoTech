package moe.takochan.takotech.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.ItemToolboxPlus;

public class NetworkHandler {

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID);

    public static void init() {
        NETWORK.registerMessage(ServerHandler.class, ToolboxSelectionPacket.class, 2, Side.SERVER);
    }

    public static class ServerHandler implements IMessageHandler<ToolboxSelectionPacket, IMessage> {

        @Override
        public IMessage onMessage(ToolboxSelectionPacket message, MessageContext ctx) {
            // 获取玩家实体（1.7.10专用方式）
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            // 在主服务端线程执行
            if (player.getHeldItem() != null && player.getHeldItem()
                .getItem() instanceof ItemToolboxPlus) {
                ItemToolboxPlus.processSelection(player, message.getStack());
            }

            return null;
        }
    }
}
