package moe.takochan.takotech.client.gui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.network.NetworkHandler;
import moe.takochan.takotech.network.PacketToolboxOpenSelectGUI;
import moe.takochan.takotech.utils.CommonUtils;

public class KeyHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (GameSettings.selectTool.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ItemStack heldItem = player.inventory.getCurrentItem();
            if (isValidToolbox(heldItem)) {
                NetworkHandler.NETWORK.sendToServer(new PacketToolboxOpenSelectGUI());
            }
        }
    }

    private boolean isValidToolbox(ItemStack stack) {
        if (stack == null) return false;

        if (stack.getItem() instanceof ItemToolboxPlus) {
            return !ItemToolboxPlus.getToolItems(stack)
                .isEmpty();
        } else if (stack.getItem() instanceof MetaGeneratedTool) {
            return CommonUtils.openNbtData(stack)
                .hasKey(NBTConstants.TOOLBOX_DATA);
        }
        return false;
    }
}
