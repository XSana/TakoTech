package moe.takochan.takotech.client.gui.container;

import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

public class ContainerToolboxPlusSelect extends Container {

    private final EntityPlayer player;

    private final ItemStack currentItem;

    public ContainerToolboxPlusSelect(EntityPlayer player) {
        this.player = player;
        currentItem = player.inventory.getCurrentItem();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public boolean isItemToolbox(){
        return currentItem.getItem() instanceof ItemToolboxPlus;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }
}
