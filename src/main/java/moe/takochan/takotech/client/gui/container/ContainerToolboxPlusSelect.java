package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerToolboxPlusSelect extends Container {

    public ContainerToolboxPlusSelect() {
        super();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return false;
    }
}
