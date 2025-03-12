package moe.takochan.takotech.coremod.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import moe.takochan.takotech.common.item.ic2.ItemToolboxPlus;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {

    @Shadow
    private EntityPlayerMP playerEntity;

    @Inject(
        method = "processPlayerBlockPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;",
            shift = At.Shift.BEFORE))
    private void onInventorySetNull(C08PacketPlayerBlockPlacement packet, CallbackInfo ci) {
        ItemStack currentItem = this.playerEntity.inventory.getCurrentItem();
        if (currentItem != null && currentItem.stackSize == 0) {
            if (ItemToolboxPlus.isMetaGeneratedTool(currentItem) && ItemToolboxPlus.isItemToolbox(currentItem)) {
                NBTTagCompound tag = CommonUtils.openNbtData(currentItem);
                final NBTTagCompound toolboxItems = tag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                final ItemStack toolbox = ItemStack.loadItemStackFromNBT(toolboxItems);
                playerEntity.inventory.setInventorySlotContents(playerEntity.inventory.currentItem, toolbox);
            }
        }
    }
}
