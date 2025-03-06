package moe.takochan.takotech.common.item.ic2;

import net.minecraftforge.common.util.EnumHelper;

import ic2.core.init.InternalName;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.ItemToolbox;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.IBaseItem;

public abstract class BaseItemToolbox extends ItemToolbox implements IHandHeldInventory, IBaseItem {

    static {
        // 添加新枚举值 itemToolboxPlus
        EnumHelper.addEnum(
            InternalName.class, // 目标枚举类
            "itemToolboxPlus", // 新枚举项名称
            new Class<?>[0], // 额外参数类型（空数组）
            new Object[0] // 额外参数值（空数组）
        );
    }

    protected String name;

    public BaseItemToolbox(String name) {
        super(getInternalName()); // 使用新枚举值

        this.name = name;
        this.setUnlocalizedName(Reference.RESOURCE_ROOT_ID + "." + name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUnlocalizedName() {
        return super.getUnlocalizedName().replace("ic2.", "");
    }

    private static InternalName getInternalName() {
        try {
            return InternalName.valueOf("itemToolboxPlus");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to find InternalName." + "itemToolboxPlus", e);
        }
    }
}
