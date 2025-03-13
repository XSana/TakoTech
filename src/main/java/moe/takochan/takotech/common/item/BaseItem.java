package moe.takochan.takotech.common.item;

import net.minecraft.item.Item;

import moe.takochan.takotech.common.Reference;

public abstract class BaseItem extends Item implements IBaseItem {

    protected String name;

    public BaseItem(String name) {
        this.name = name;
        this.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);
    }

    @Override
    public String getName() {
        return name;
    }
}
