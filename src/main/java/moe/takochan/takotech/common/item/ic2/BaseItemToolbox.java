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

public abstract class BaseItemToolbox extends ItemToolbox implements IBaseItem {

    protected String name;
    private String unlocalizedName;

    public BaseItemToolbox(String name) {
        super(null);

        this.name = name;
        this.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);

    }

    public String getName() {
        return name;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon(this.getIconString());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int p_77617_1_) {
        return this.itemIcon;
    }

    @Override
    public String getUnlocalizedName() {
        return "item." + this.unlocalizedName;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return this.getUnlocalizedName();
    }

    @Override
    public Item setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
        return super.setUnlocalizedName(unlocalizedName);
    }

    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        return ("" + StatCollector.translateToLocal(this.getUnlocalizedNameInefficiently(itemStack) + ".name")).trim();
    }
}
