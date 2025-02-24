package moe.takochan.takotech.common.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import appeng.block.AEBaseItemBlock;

public abstract class BaseAEItemBlock extends AEBaseItemBlock {

    public BaseAEItemBlock(Block id) {
        super(id);
    }

    @Override
    public String getUnlocalizedName(ItemStack is) {
        return super.getUnlocalizedName(is);
    }
}
