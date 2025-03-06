package moe.takochan.takotech.coremod.mixin.gt;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.IToolStats;
import gregtech.api.items.MetaBaseItem;
import gregtech.api.util.GTUtility;
import moe.takochan.takotech.common.loader.ItemLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

import gregtech.api.items.MetaGeneratedTool;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import static gregtech.api.items.MetaGeneratedTool.getToolDamage;
import static gregtech.api.items.MetaGeneratedTool.getToolMaxDamage;
import static gregtech.api.items.MetaGeneratedTool.setToolDamage;

@Mixin(value = MetaGeneratedTool.class, remap = false)
public abstract class MetaGeneratedToolMixin {

    @Shadow
    public abstract boolean isItemStackUsable(ItemStack aStack);

    @Shadow
    public abstract Long[] getElectricStats(ItemStack aStack);

    @Shadow
    public abstract IToolStats getToolStats(ItemStack aStack);


    @Accessor("playSound")
    public abstract boolean getPlaySound();

    /**
     * @author XSana
     * @reason 针对工具箱情况处理工具损坏
     */
    @Overwrite
    public final boolean doDamage(ItemStack aStack, long aAmount) {
        if (!isItemStackUsable(aStack)) return false;
        Long[] tElectric = getElectricStats(aStack);
        if (tElectric == null) {
            long tNewDamage = getToolDamage(aStack) + aAmount;
            setToolDamage(aStack, tNewDamage);
            if (tNewDamage >= getToolMaxDamage(aStack)) {
                IToolStats tStats = getToolStats(aStack);
                if (tStats == null || GTUtility.setStack(aStack, tStats.getBrokenItem(aStack)) == null) {
                    if (tStats != null && getPlaySound()) GTUtility.doSoundAtClient(tStats.getBreakingSound(), 1, 1.0F);
                    if (aStack.stackSize > 0) aStack.stackSize--;
                }
            }
            return true;
        }

        if (((MetaBaseItem)(Object)this).use(aStack, (int) aAmount, null)) {
            if (java.util.concurrent.ThreadLocalRandom.current()
                .nextInt(0, 25) == 0) {
                long tNewDamage = getToolDamage(aStack) + aAmount;
                setToolDamage(aStack, tNewDamage);
                if (tNewDamage >= getToolMaxDamage(aStack)) {
                    IToolStats tStats = getToolStats(aStack);
                    if (tStats == null || GTUtility.setStack(aStack, tStats.getBrokenItem(aStack)) == null) {
                        if (tStats != null && getPlaySound())
                            GTUtility.doSoundAtClient(tStats.getBreakingSound(), 1, 1.0F);
                        if (aStack.stackSize > 0) aStack.stackSize--;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
