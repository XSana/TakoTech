package moe.takochan.takotech.client;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.settings.GameSettings;
import moe.takochan.takotech.common.CommonProxy;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // 初始化渲染系统（包括 Shader 注册）
        RenderSystem.init();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 创建游戏设置实例并注册按键绑定
        GameSettings gameSettings = new GameSettings();
        gameSettings.register();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        // 注册关闭钩子，清理渲染资源
        Runtime.getRuntime()
            .addShutdownHook(new Thread(RenderSystem::shutdown));
    }
}
