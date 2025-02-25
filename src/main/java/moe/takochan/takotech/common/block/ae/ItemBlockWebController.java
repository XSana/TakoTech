package moe.takochan.takotech.common.block.ae;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import moe.takochan.takotech.common.block.BaseAEItemBlock;
import moe.takochan.takotech.constants.NBTConstants;
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
            NBTTagCompound tag = stack.getTagCompound();
            String controllerID = tag.getString(NBTConstants.CONTROLLER_ID);
            if (!controllerID.isEmpty()) {
                toolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC_1) + " : " + controllerID);
            }
        }
    }
}
