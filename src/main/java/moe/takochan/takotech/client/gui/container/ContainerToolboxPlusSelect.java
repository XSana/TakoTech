package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;

public class ContainerToolboxPlusSelect extends BaseContainer {

    public ContainerToolboxPlusSelect(EntityPlayer player) {
        super(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
