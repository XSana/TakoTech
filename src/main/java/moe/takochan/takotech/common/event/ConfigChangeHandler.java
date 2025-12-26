package moe.takochan.takotech.common.event;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.config.TakoTechConfig;

/**
 * 配置变更事件处理器
 */
public class ConfigChangeHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Reference.MODID.equals(event.modID)) {
            TakoTechConfig.save();
        }
    }
}
