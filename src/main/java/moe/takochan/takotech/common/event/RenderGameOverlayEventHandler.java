package moe.takochan.takotech.common.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import moe.takochan.takotech.client.gui.GuiToolboxPlusSelect;

public class RenderGameOverlayEventHandler {

    /**
     * 取消渲染 HUD 时的准星
     */
    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiToolboxPlusSelect) {
            event.setCanceled(true);
        }
    }
}
