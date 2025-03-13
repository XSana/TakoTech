package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerToolboxPlusSelect extends Container {

    private final EntityPlayer player;

    public ContainerToolboxPlusSelect(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
