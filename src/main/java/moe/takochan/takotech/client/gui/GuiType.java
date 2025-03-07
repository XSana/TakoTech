package moe.takochan.takotech.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;
import moe.takochan.takotech.utils.CommonUtils;

public enum GuiType implements IGuiHandler {


    GUI_TOOLBOX_PLUS_SELECT(ContainerToolboxPlusSelect.class, GuiToolboxPlusSelect.class, null);

    private final Class<?> containerClass;
    private final Class<?> guiClass;
    public final Class<?> tileClass;

    GuiType(final Class<?> containerClass, final Class<?> guiClass, final Class<?> tileClass) {
        this.containerClass = containerClass;
        this.guiClass = guiClass;
        this.tileClass = tileClass;
    }


    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType guiType = values()[ID];
        try {
            return createContainer(guiType.tileClass, player, world, x, y, z);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType guiType = values()[ID];
        try {
            Object container = createContainer(guiType.tileClass, player, world, x, y, z);
            return guiClass.getConstructor(containerClass).newInstance(container);
        } catch (Exception ignored) {
        }
        return null;
    }

    public Object createContainer(Class<?> tileClass, EntityPlayer player, World world, int x, int y, int z) {
        try {
            if (tileClass != null) {
                TileEntity te = world.getTileEntity(x, y, z);
                return containerClass.getConstructor(EntityPlayer.class, TileEntity.class)
                    .newInstance(player, te);
            } else {
                return containerClass.getConstructor(EntityPlayer.class)
                    .newInstance(player);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create container for " + this.name(), e);
        }
    }

    public static void register() {
        for (GuiType type : values()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(TakoTechMod.instance, type);
        }
    }
}
