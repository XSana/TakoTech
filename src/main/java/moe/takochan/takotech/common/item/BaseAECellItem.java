package moe.takochan.takotech.common.item;

import appeng.api.exceptions.AppEngException;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.items.AEBaseItem;
import moe.takochan.takotech.common.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public abstract class BaseAECellItem extends AEBaseItem implements IBaseItem {

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

    @Override
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
    }

    public abstract IMEInventoryHandler<?> getInventoryHandler(ItemStack o, ISaveProvider container)
        throws AppEngException;
}
