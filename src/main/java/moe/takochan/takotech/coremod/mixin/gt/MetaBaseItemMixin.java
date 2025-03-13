package moe.takochan.takotech.coremod.mixin.gt;

import static moe.takochan.takotech.client.gui.settings.GameSettings.selectTool;

import java.util.List;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gregtech.api.items.MetaBaseItem;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

@Mixin(MetaBaseItem.class)
public abstract class MetaBaseItemMixin {

    /**
     * 注入方法：在 MetaBaseItem 的 addInformation 方法返回时执行
     * <p>
     * 用于在工具提示中添加工具箱的相关信息
     *
     * @param aStack  物品堆栈
     * @param aPlayer 玩家
     * @param aList   信息列表
     * @param aF3_H   是否按下 F3+H
     * @param ci      回调信息
     */
    @Inject(method = "addInformation", at = @At("RETURN"))
    public final void addInformation(ItemStack aStack, EntityPlayer aPlayer, List<String> aList, boolean aF3_H,
        CallbackInfo ci) {
        // 检查物品是否为 MetaGeneratedTool
        if (aStack.getItem() instanceof MetaGeneratedTool) {
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
}
