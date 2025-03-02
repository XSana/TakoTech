package moe.takochan.takotech.client;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import moe.takochan.takotech.client.gui.settings.GameSettings;
import moe.takochan.takotech.common.CommonProxy;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        GameSettings gameSettings = new GameSettings();
        gameSettings.register();
    }
}
