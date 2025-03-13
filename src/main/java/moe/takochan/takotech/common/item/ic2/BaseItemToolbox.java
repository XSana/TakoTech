package moe.takochan.takotech.common.item.ic2;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.item.ItemToolbox;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.IBaseItem;

/**
 * 覆写IC2实现
 */
public abstract class BaseItemToolbox extends ItemToolbox implements IBaseItem {

    // 物品名称
    protected String name;
    // 未本地化名称
    private String unlocalizedName;

    public BaseItemToolbox(String name) {
        super(null);

        this.name = name;
        // 设置未本地化名称
        this.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);
    }

    /**
     * 获取物品名称
     *
     * @return 物品名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 注册物品图标
     *
     * @param register 图标注册器
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon(this.getIconString());
    }

    /**
     * 根据损坏值获取图标
     *
     * @param meta meta
     * @return 物品图标
     */
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        return this.itemIcon;
    }

    /**
     * 获取未本地化名称
     *
     * @return 未本地化名称
     */
    @Override
    public String getUnlocalizedName() {
        return "item." + this.unlocalizedName;
    }

    /**
     * 获取物品堆栈的未本地化名称
     *
     * @param itemStack 物品堆栈
     * @return 未本地化名称
     */
    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return this.getUnlocalizedName();
    }

    /**
     * 设置未本地化名称
     *
     * @param unlocalizedName 未本地化名称
     * @return 当前物品实例
     */
    @Override
    public Item setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
        return super.setUnlocalizedName(unlocalizedName);
    }

    /**
     * 获取物品堆栈的显示名称
     *
     * @param itemStack 物品堆栈
     * @return 显示名称
     */
    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        return ("" + StatCollector.translateToLocal(this.getUnlocalizedNameInefficiently(itemStack) + ".name")).trim();
    }
}
