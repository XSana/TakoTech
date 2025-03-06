package moe.takochan.takotech.coremod.mixin.gt;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.ItemToolbox;
import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;

@Mixin(value = MTEHatchMaintenance.class, remap = false)
public abstract class MTEHatchMaintenanceMixin {

    @Shadow
    public abstract void onToolClick(ItemStack aStack, EntityLivingBase aPlayer, IInventory aToolboxInventory);

    /**
     * @author XSana
     * @reason 转为工具箱子类
     */
    @Overwrite
    private void applyToolbox(ItemStack aStack, EntityPlayer aPlayer) {
        Item item = aStack.getItem();
        if (!(item instanceof ItemToolbox || item instanceof ItemToolboxPlus)) return;
        IHandHeldInventory aToolbox = (IHandHeldInventory) item;
        final IHasGui aToolboxGUI = aToolbox.getInventory(aPlayer, aStack);
        for (int i = 0; i < aToolboxGUI.getSizeInventory(); i++) {
            if (aToolboxGUI.getStackInSlot(i) != null) {
                onToolClick(aToolboxGUI.getStackInSlot(i), aPlayer, aToolboxGUI);
                if (aToolboxGUI.getStackInSlot(i) != null && aToolboxGUI.getStackInSlot(i).stackSize <= 0)
                    aToolboxGUI.setInventorySlotContents(i, null);
            }
        }
    }

}
