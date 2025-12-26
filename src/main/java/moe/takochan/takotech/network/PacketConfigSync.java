package moe.takochan.takotech.network;

import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.config.ServerConfig;

/**
 * 配置同步网络包
 * 服务端发送给客户端，用于同步服务端配置
 */
public class PacketConfigSync implements IMessage {

    // General
    private String[] oreDefs;

    // WebController
    private boolean webControllerEnabled;
    private boolean allowSelfHosting;
    private String defaultServerAddress;
    private int defaultPort;
    private boolean useSSL;

    public PacketConfigSync() {}

    /**
     * 从服务端配置创建同步包
     */
    public static PacketConfigSync fromServerConfig() {
        PacketConfigSync packet = new PacketConfigSync();
        packet.oreDefs = ServerConfig.oreDefs;
        packet.webControllerEnabled = ServerConfig.webControllerEnabled;
        packet.allowSelfHosting = ServerConfig.allowSelfHosting;
        packet.defaultServerAddress = ServerConfig.defaultServerAddress;
        packet.defaultPort = ServerConfig.defaultPort;
        packet.useSSL = ServerConfig.useSSL;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // oreDefs
        int oreDefsLength = buf.readInt();
        oreDefs = new String[oreDefsLength];
        for (int i = 0; i < oreDefsLength; i++) {
            int strLen = buf.readInt();
            byte[] bytes = new byte[strLen];
            buf.readBytes(bytes);
            oreDefs[i] = new String(bytes, StandardCharsets.UTF_8);
        }

        // WebController
        webControllerEnabled = buf.readBoolean();
        allowSelfHosting = buf.readBoolean();

        int addrLen = buf.readInt();
        byte[] addrBytes = new byte[addrLen];
        buf.readBytes(addrBytes);
        defaultServerAddress = new String(addrBytes, StandardCharsets.UTF_8);

        defaultPort = buf.readInt();
        useSSL = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // oreDefs
        buf.writeInt(oreDefs.length);
        for (String oreDef : oreDefs) {
            byte[] bytes = oreDef.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }

        // WebController
        buf.writeBoolean(webControllerEnabled);
        buf.writeBoolean(allowSelfHosting);

        byte[] addrBytes = defaultServerAddress.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(addrBytes.length);
        buf.writeBytes(addrBytes);

        buf.writeInt(defaultPort);
        buf.writeBoolean(useSSL);
    }

    /**
     * 客户端处理器
     */
    public static class Handler implements IMessageHandler<PacketConfigSync, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketConfigSync message, MessageContext ctx) {
            // 检查是否为远程服务器（非单人游戏）
            boolean isRemoteServer = !Minecraft.getMinecraft()
                .isSingleplayer();

            // 应用服务端配置到客户端
            ServerConfig.applyFromSync(
                message.oreDefs,
                message.webControllerEnabled,
                message.allowSelfHosting,
                message.defaultServerAddress,
                message.defaultPort,
                message.useSSL,
                isRemoteServer);

            if (isRemoteServer) {
                TakoTechMod.LOG.info("Received server config sync from remote server");
            } else {
                TakoTechMod.LOG.debug("Received server config sync from integrated server (single player)");
            }
            return null;
        }
    }
}
