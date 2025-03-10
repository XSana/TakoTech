package moe.takochan.takotech.client.gui.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.items.MetaGeneratedTool;
import ic2.core.IHasGui;
import moe.takochan.takotech.common.data.ToolData;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;


public class ContainerToolboxPlusSelect extends Container {

    private final EntityPlayer player;

    private final ItemStack currentItem;

    private final List<ToolData> items = new ArrayList<>(); // 存储的可选物品列表


    public ContainerToolboxPlusSelect(EntityPlayer player) {
        this.player = player;
        currentItem = player.inventory.getCurrentItem();
        loadItems();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public boolean isItemToolbox() {
        return currentItemIsToolboxPlus() || (currentItemIsMetaGeneratedTool() &&
            CommonUtils.openNbtData(currentItem).hasKey(NBTConstants.TOOLBOX_DATA));
    }

    public List<ToolData> getGTTools() {
        return getToolbox().map(toolbox -> {
            if (toolbox.getItem() instanceof ItemToolboxPlus toolboxPlus) {
                return toolboxPlus.getGTTools(toolbox, player);
            }
            return null;
        }).orElse(null);
    }

    public void selectTool(int slot) {
        getToolbox().ifPresent(toolbox -> {
            if (toolbox.getItem() instanceof ItemToolboxPlus toolboxPlus) {
                final IHasGui toolboxGUI = toolboxPlus.getInventory(player, toolbox);
                if (currentItemIsToolboxPlus()) {
                    handleToolSelection(toolboxGUI, slot, toolbox, null);
                } else if (currentItemIsMetaGeneratedTool()) {
                    handleMetaToolSelection(toolboxGUI, slot, toolbox);
                }
            }
        });
    }

    private void handleToolSelection(IHasGui toolboxGUI, int slot, ItemStack toolbox, NBTTagCompound nbt) {
        ItemStack tool = toolboxGUI.getStackInSlot(slot).copy();
        toolboxGUI.setInventorySlotContents(slot, null);
        NBTTagCompound toolNbt = new NBTTagCompound();
        toolbox.writeToNBT(toolNbt);
        CommonUtils.openNbtData(tool).setTag(NBTConstants.TOOLBOX_DATA, toolNbt);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, tool);
    }

    private void handleMetaToolSelection(IHasGui toolboxGUI, int slot, ItemStack toolbox) {
        NBTTagCompound nbt = CommonUtils.openNbtData(currentItem);
        int currentSlot = nbt.getInteger(NBTConstants.TOOLBOX_TOOLS_SLOT);
        nbt.removeTag(NBTConstants.TOOLBOX_DATA);
        nbt.removeTag(NBTConstants.TOOLBOX_TOOLS_SLOT);
        toolboxGUI.setInventorySlotContents(currentSlot, currentItem);
        if (slot == -1) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
        } else {
            handleToolSelection(toolboxGUI, slot, toolbox, nbt);
        }
    }

    private Optional<ItemStack> getToolbox() {
        if (!isItemToolbox()) {
            return Optional.empty();
        }
        if (currentItemIsToolboxPlus()) {
            return Optional.of(currentItem);
        }
        NBTTagCompound nbt = CommonUtils.openNbtData(currentItem);
        if (nbt.hasKey(NBTConstants.TOOLBOX_DATA)) {
            return Optional.ofNullable(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(NBTConstants.TOOLBOX_DATA)));
        }
        return Optional.empty();
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public boolean currentItemIsToolboxPlus() {
        return currentItem.getItem() instanceof ItemToolboxPlus;
    }

    public boolean currentItemIsMetaGeneratedTool() {
        return currentItem.getItem() instanceof MetaGeneratedTool;
    }

    public List<ToolData> getItems() {
        return items;
    }

    private void loadItems() {
        final List<ToolData> list = getGTTools();
        if (list == null || list.isEmpty()) return;
        if (currentItemIsMetaGeneratedTool()) {
            this.items.add(new ToolData(-1, ItemToolboxPlus.DEFAULT_ITEM));
        }
        this.items.addAll(list);
    }
}
