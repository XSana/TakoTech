package moe.takochan.takotech.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ToolboxSelectionPacket implements IMessage {


    private int selectedIndex;

    public ToolboxSelectionPacket() {
    }

    public ToolboxSelectionPacket(int index) {
        this.selectedIndex = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        selectedIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(selectedIndex);
    }


    public int getSelectedIndex() {
        return selectedIndex;
    }
}
