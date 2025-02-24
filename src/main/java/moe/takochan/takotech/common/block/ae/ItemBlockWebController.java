package moe.takochan.takotech.common.block.ae;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import moe.takochan.takotech.common.block.BaseAEItemBlock;
import moe.takochan.takotech.constants.NBTConstants;

public class ItemBlockWebController extends BaseAEItemBlock {

    public ItemBlockWebController(Block id) {
        super(id);
    }

    @Override
    public void addCheckedInformation(ItemStack itemStack, EntityPlayer player, List<String> toolTip,
        boolean advancedToolTips) {
        NBTTagCompound tag = itemStack.getTagCompound();
        String controllerID = tag.getString(NBTConstants.CONTROLLER_ID);
        if (!controllerID.isEmpty()) {
            toolTip.add(controllerID);
        }
    }
}
