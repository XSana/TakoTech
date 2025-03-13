package moe.takochan.takotech.common.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.items.AEBaseItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.common.Reference;

public abstract class BaseAECellItem extends AEBaseItem implements IBaseAECellItem {

    protected String name;

    public BaseAECellItem(String name) {
        this.name = name;
        this.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);
    }

    /**
     * 客户端显示物品的额外信息（如提示）。 这个方法仅在客户端侧调用。
     *
     * @param itemStack       当前物品的堆叠
     * @param player          当前玩家实例
     * @param lines           信息列表，用于显示物品描述
     * @param displayMoreInfo 是否显示更多信息
     */
    @Override
    @SideOnly(Side.CLIENT)
    protected void addCheckedInformation(ItemStack itemStack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        super.addCheckedInformation(itemStack, player, lines, displayMoreInfo);
    }

    @Override
    public String getName() {
        return name;
    }
}
