package moe.takochan.takotech.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import moe.takochan.takotech.client.input.IMEControl;
import moe.takochan.takotech.client.input.IMEStateHandler;
import moe.takochan.takotech.client.renderer.RenderSystem;
import moe.takochan.takotech.client.settings.GameSettings;
import moe.takochan.takotech.common.CommonProxy;
import moe.takochan.takotech.common.event.ConfigChangeHandler;

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

        // 初始化 IME 控制 (每 tick 检查文本框焦点状态)
        IMEControl.init();
        if (IMEControl.isAvailable()) {
            FMLCommonHandler.instance()
                .bus()
                .register(new IMEStateHandler());
        }

        // 注册配置变更事件处理（ConfigChangedEvent 在 FML 事件总线上）
        FMLCommonHandler.instance()
            .bus()
            .register(new ConfigChangeHandler());

        // [测试代码] 渲染框架测试处理器 - 开发调试用，生产环境应注释掉
        // 按键: U=开关测试, I=下一阶段, O=上一阶段, P=自动推进, B=切换Bloom
        // moe.takochan.takotech.client.renderer.test.RenderFrameworkTestHandler testHandler = new
        // moe.takochan.takotech.client.renderer.test.RenderFrameworkTestHandler();
        // MinecraftForge.EVENT_BUS.register(testHandler);
        // FMLCommonHandler.instance().bus().register(testHandler);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        // 注册关闭钩子，清理渲染资源
        Runtime.getRuntime()
            .addShutdownHook(new Thread(RenderSystem::shutdown));
    }
}
