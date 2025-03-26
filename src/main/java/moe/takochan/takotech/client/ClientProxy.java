package moe.takochan.takotech.client;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import moe.takochan.takotech.client.renderer.shader.ShaderType;
import moe.takochan.takotech.client.settings.GameSettings;
import moe.takochan.takotech.common.CommonProxy;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // 注册并编译Shader
        ShaderType.register();
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
        Runtime.getRuntime()
            .addShutdownHook(new Thread(ShaderType::cleanupAll));
    }
}
