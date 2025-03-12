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

@Mixin(value = NetHandlerPlayServer.class, remap = false)
public abstract class NetHandlerPlayServerMixin {

    @Shadow
    private EntityPlayerMP playerEntity;

    @Inject(
        method = "processPlayerBlockPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;",
            ordinal = 1,
            shift = At.Shift.BEFORE),
        remap = true,
        require = 1)
    private void onHandleItemStackZero(C08PacketPlayerBlockPlacement packetIn, CallbackInfo ci) {
        ItemStack itemstack = this.playerEntity.inventory.getCurrentItem();
        if (itemstack != null && itemstack.stackSize == 0) {
            if (ItemToolboxPlus.isMetaGeneratedTool(itemstack) && ItemToolboxPlus.isItemToolbox(itemstack)) {
                // 从 NBT 中恢复工具箱数据
                NBTTagCompound tag = CommonUtils.openNbtData(itemstack);
                NBTTagCompound toolboxItems = tag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                ItemStack toolbox = ItemStack.loadItemStackFromNBT(toolboxItems);

                // 更新玩家物品栏
                this.playerEntity.inventory.setInventorySlotContents(this.playerEntity.inventory.currentItem, toolbox);
            }
        }

    }

}
