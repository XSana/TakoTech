package moe.takochan.takotech.common.block.ae;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import moe.takochan.takotech.common.block.BaseAEItemBlock;
import moe.takochan.takotech.common.data.WebControllerData;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

/**
 * WEB控制器方块的物品形式 处理物品形式的NBT数据交互和工具提示显示
 */
public class ItemBlockWebController extends BaseAEItemBlock {

    public ItemBlockWebController(Block id) {
        super(id);
    }

    /**
     * 添加物品详细信息（工具提示） 当玩家悬停查看物品时显示控制器ID信息
     *
     * @param itemStack        当前物品堆栈
     * @param player           当前玩家对象
     * @param toolTip          工具提示内容列表
     * @param advancedToolTips 是否显示高级信息（F3+H模式）
     */
    @Override
    public void addCheckedInformation(ItemStack itemStack, EntityPlayer player, List<String> toolTip,
        boolean advancedToolTips) {
        toolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC));
        if (itemStack.hasTagCompound()) {
            WebControllerData data = new WebControllerData();
            data.readFormNBT(CommonUtils.openNbtData(itemStack));

            String controllerID = data.getControllerId();
            if (controllerID != null && !controllerID.isEmpty()) {
                toolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC_1) + " : " + controllerID);
            }
        }
    }
}
