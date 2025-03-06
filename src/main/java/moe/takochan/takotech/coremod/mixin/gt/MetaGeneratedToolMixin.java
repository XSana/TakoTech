package moe.takochan.takotech.coremod.mixin.gt;

import org.spongepowered.asm.mixin.Mixin;

import gregtech.api.items.MetaGeneratedTool;

@Mixin(MetaGeneratedTool.class)
public abstract class MetaGeneratedToolMixin {

    // @Inject(method = "getContainerItem*", at = @At("RETURN"), remap = false)
    // public final ItemStack getContainerItem(ItemStack aStack) {
    // }

    /**
     * @author XSana
     * @reason 合成替换
     */
    // @Overwrite(remap = false)
    // private ItemStack getContainerItem(ItemStack aStack, boolean playSound) {
    // return new ItemStack(ItemLoader.ITEM_TOOLBOX_PLUS);
    // }
    //
    // @Override
    // public final boolean doDamageToItem(ItemStack aStack, int aVanillaDamage) {
    // }
}
