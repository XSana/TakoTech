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
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

public class KeyHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (GameSettings.selectTool.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ItemStack heldItem = player.getHeldItem();
            if (isValidToolbox(player, heldItem)) {
                openToolboxGUI(player);
            }
        }
    }

    private boolean isValidToolbox(EntityPlayer player, ItemStack stack) {
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

    private void openToolboxGUI(EntityPlayer player) {
        World world = Minecraft.getMinecraft().theWorld;
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
