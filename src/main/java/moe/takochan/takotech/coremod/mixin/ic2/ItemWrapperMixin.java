package moe.takochan.takotech.coremod.mixin.ic2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.items.MetaGeneratedTool;
import ic2.api.item.ItemWrapper;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

@Mixin(value = ItemWrapper.class, remap = false)
public class ItemWrapperMixin {

    @Inject(method = "canBeStoredInToolbox", at = @At("RETURN"), cancellable = true)
    private static void canBeStoredInToolbox(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null) {
            if (stack.getItem() instanceof MetaGeneratedTool) {
                NBTTagCompound nbt = CommonUtils.openNbtData(stack);
                if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) cir.setReturnValue(false);
            }
        }
    }
}
