package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;

public class ContainerOreStorageCell extends BaseContainer {

    public ContainerOreStorageCell(EntityPlayer player) {
        super(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
