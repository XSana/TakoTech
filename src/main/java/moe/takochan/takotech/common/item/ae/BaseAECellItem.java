package moe.takochan.takotech.common.item.ae;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.items.AEBaseItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.IBaseItem;

public abstract class BaseAECellItem extends AEBaseItem implements IBaseItem, IBaseAECellItem {

    /**
     * 重写 setUnlocalizedName 方法来设置物品的非本地化名称。
     * 通过拼接 mod 的资源根标识符和传入的名称来确保唯一性。
     *
     * @param name 物品的名称
     * @return 当前物品实例，以便链式调用
     */
    @Override
    public Item setUnlocalizedName(String name) {
        super.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);
        return this;
    }

    /**
     * 客户端显示物品的额外信息（如提示）。
     * 这个方法仅在客户端侧调用。
     *
     * @param stack           当前物品的堆叠
     * @param player          当前玩家实例
     * @param lines           信息列表，用于显示物品描述
     * @param displayMoreInfo 是否显示更多信息
     */
    @Override
    @SideOnly(Side.CLIENT)
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
    }

}
