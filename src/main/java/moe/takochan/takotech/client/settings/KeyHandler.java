package moe.takochan.takotech.client.settings;

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
import moe.takochan.takotech.utils.ToolboxHelper;

/**
 * 按键处理器类
 * <p>
 * 用于处理玩家按键输入事件，并执行相应的逻辑
 */
public class KeyHandler {

    /**
     * 按键输入事件监听方法
     * <p>
     * 当玩家按下绑定按键时触发
     *
     * @param event 按键输入事件
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyPress(InputEvent event) {
        if (event instanceof InputEvent.KeyInputEvent || event instanceof InputEvent.MouseInputEvent) {
            checkToolboxOpenRequest();
        }
    }

    private void checkToolboxOpenRequest() {
        // 如果按键被按下，执行逻辑
        if (GameSettings.selectTool.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ItemStack heldItem = player.inventory.getCurrentItem();
            if (isValidToolbox(heldItem)) {
                NetworkHandler.NETWORK.sendToServer(new PacketToolboxOpenSelectGUI());
            }
        }
    }

    /**
     * 检查物品是否为有效的工具箱
     * <p>
     * 判断物品是否为工具箱或包含工具箱数据
     *
     * @param stack 要检查的物品
     * @return 如果物品是有效的工具箱，返回true；否则返回false
     */
    private boolean isValidToolbox(ItemStack stack) {
        if (stack == null) return false;

        if (stack.getItem() instanceof ItemToolboxPlus) {
            return !ToolboxHelper.getToolItems(stack)
                .isEmpty();
        } else if (stack.getItem() instanceof MetaGeneratedTool) {
            return CommonUtils.openNbtData(stack)
                .hasKey(NBTConstants.TOOLBOX_DATA);
        }
        return false;
    }
}
