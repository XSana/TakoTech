package moe.takochan.takotech.network;

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
    }
}
