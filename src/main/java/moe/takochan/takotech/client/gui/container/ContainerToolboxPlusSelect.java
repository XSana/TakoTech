package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

public class ContainerToolboxPlusSelect extends Container {

    private ItemStack stack;
    private EntityPlayer player;

    public ContainerToolboxPlusSelect(EntityPlayer player, ItemStack stack) {
        super();
        this.stack = stack;
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return false;
    }
}
