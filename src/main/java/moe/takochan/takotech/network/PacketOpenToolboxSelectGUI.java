package moe.takochan.takotech.network;

import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ibxm.Player;
import io.netty.buffer.ByteBuf;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.utils.CommonUtils;

public class PacketOpenToolboxSelectGUI implements IMessage {

    public PacketOpenToolboxSelectGUI() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<PacketOpenToolboxSelectGUI, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenToolboxSelectGUI message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            CommonUtils.openGui(GuiType.GUI_TOOLBOX_PLUS_SELECT, player, null);
            return null;
        }
    }
}
