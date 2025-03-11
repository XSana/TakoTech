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

    @Inject(method = "addInformation", at = @At("RETURN"))
    public final void addInformation(ItemStack aStack, EntityPlayer aPlayer, List<String> aList, boolean aF3_H,
        CallbackInfo ci) {
        if (aStack.getItem() instanceof MetaGeneratedTool) {
            NBTTagCompound nbt = CommonUtils.openNbtData(aStack);
            if (!nbt.hasKey(NBTConstants.TOOLBOX_DATA) || !nbt.hasKey(NBTConstants.TOOLBOX_SELECTED_INDEX)) return;
            aList.add("");
            aList.add(
                I18nUtils.tooltip(
                    NameConstants.ITEM_TOOLBOX_PLUS_DESC,
                    GameSettings.getKeyDisplayString(selectTool.getKeyCode())));
        }
    }
}
