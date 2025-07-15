package moe.takochan.takotech.common.event;

import static moe.takochan.takotech.client.settings.GameSettings.selectTool;

import java.util.List;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemTooltipEventHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        List<String> tooltips = event.toolTip;
        if (stack != null) {
            if (stack.getItem() instanceof MetaGeneratedTool) {
                addToolBoxTip(stack, tooltips);
            }
        }

    }

    private void addToolBoxTip(ItemStack aStack, List<String> aList) {
        // 获取物品的 NBT 数据
        NBTTagCompound nbt = CommonUtils.openNbtData(aStack);
        // 检查是否存在工具箱数据和槽位数据
        if (!nbt.hasKey(NBTConstants.TOOLBOX_DATA) || !nbt.hasKey(NBTConstants.TOOLBOX_SLOT)) return;
        // 添加工具箱的描述信息
        aList.add("");
        aList.add(
            I18nUtils.tooltip(
                NameConstants.ITEM_TOOLBOX_PLUS_DESC,
                GameSettings.getKeyDisplayString(selectTool.getKeyCode())));
    }
}
