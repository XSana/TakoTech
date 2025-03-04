package moe.takochan.takotech.client.gui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.item.ItemToolboxPlus;

public class KeyHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (GameSettings.selectTool.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            World world = Minecraft.getMinecraft().theWorld;
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemToolboxPlus
                && !ItemToolboxPlus.getToolItems(heldItem)
                    .isEmpty()) {
                GuiType.openGuiWithClient(
                    GuiType.TOOLBOX_PLUS_SELECT,
                    player,
                    world,
                    ForgeDirection.UNKNOWN,
                    (int) player.posX,
                    (int) player.posY,
                    (int) player.posZ);
            }
        }
    }
}
