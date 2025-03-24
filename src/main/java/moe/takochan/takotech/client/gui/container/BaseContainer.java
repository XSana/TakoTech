package moe.takochan.takotech.client.gui.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public abstract class BaseContainer extends Container {

    private final EntityPlayer player;

    public BaseContainer(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
