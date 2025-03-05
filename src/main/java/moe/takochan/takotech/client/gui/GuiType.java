package moe.takochan.takotech.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.utils.CommonUtils;

public enum GuiType implements IGuiHandler {

    TOOLBOX_PLUS_SELECT((player, world, x, y, z) -> new ContainerToolboxPlusSelect(), (player, world, x, y,
        z) -> new GuiToolboxPlusSelect(new ContainerToolboxPlusSelect(), player.inventory.getCurrentItem()));

    private final ContainerProvider containerProvider;
    private final GuiProvider guiProvider;

    GuiType(ContainerProvider containerProvider, GuiProvider guiProvider) {
        this.containerProvider = containerProvider;
        this.guiProvider = guiProvider;
    }

    @Override
    public Container getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return values()[ID].containerProvider.create(player, world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiContainer getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return values()[ID].guiProvider.create(player, world, x, y, z);
    }

    @FunctionalInterface
    public interface ContainerProvider {

        Container create(EntityPlayer player, World world, int x, int y, int z);
    }

    @FunctionalInterface
    public interface GuiProvider {

        GuiContainer create(EntityPlayer player, World world, int x, int y, int z);
    }

    public static void register() {
        for (GuiType type : values()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(TakoTechMod.instance, type);
        }
    }

    public static void openGuiWithClient(GuiType type, EntityPlayer player, World world, ForgeDirection face, int x,
        int y, int z) {
        openGui(type, player, world, face, x, y, z, true);
    }

    public static void openGui(GuiType type, EntityPlayer player, World world, ForgeDirection face, int x, int y, int z,
        boolean force) {
        if (!force && CommonUtils.isClient()) {
            return;
        }
        player.openGui(TakoTechMod.instance, type.ordinal(), world, player.inventory.currentItem, 0, 0);
    }

}
