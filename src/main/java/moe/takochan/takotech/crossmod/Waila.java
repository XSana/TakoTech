package moe.takochan.takotech.crossmod;

import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import moe.takochan.takotech.common.tile.BaseAETile;
import moe.takochan.takotech.crossmod.waila.TileWailaDataProvider;

public class Waila {

    /**
     * 初始化WAILA集成
     * 通过FML的跨模组通信系统发送注册请求
     * 应在模组初始化阶段调用
     */
    public static void run() {
        FMLInterModComms.sendMessage("Waila", "register", Waila.class.getName() + ".register");
    }

    /**
     * WAILA回调注册方法
     * 由WAILA模组在加载时主动调用
     *
     * @param registrar WAILA注册接口，用于添加数据提供者
     */
    public static void register(final IWailaRegistrar registrar) {
        // 创建自定义TileEntity数据提供者实例
        final IWailaDataProvider tile = new TileWailaDataProvider();

        // 1. 主体信息显示 - 控制WAILA提示框主要内容
        registrar.registerBodyProvider(tile, BaseAETile.class);
        // 2. NBT数据同步 - 确保客户端能获取服务端数据
        registrar.registerNBTProvider(tile, BaseAETile.class);
    }

}
