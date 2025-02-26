package moe.takochan.takotech.common.block.ae;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import moe.takochan.takotech.common.block.BaseAEItemBlock;
import moe.takochan.takotech.common.data.WebControllerData;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemBlockWebController extends BaseAEItemBlock {

    public ItemBlockWebController(Block id) {
        super(id);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> toolTip,
                                      boolean advancedToolTips) {
        if (stack.hasTagCompound()) {
            WebControllerData data = new WebControllerData();
            data.readFormNBT(stack.getTagCompound());

            String controllerID = data.getControllerId();
            if (!controllerID.isEmpty()) {
                toolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC_1) + " : " + controllerID);
            }
        }
    }
}
