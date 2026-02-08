package moe.takochan.takotech.coremod.mixin;

import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.utils.ToolboxHelper;
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

@Mixin(value = NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {

    @Shadow
    private EntityPlayerMP playerEntity;

    /**
     * 注入方法：在 processPlayerBlockPlacement 方法中调用 getCurrentItem 之前执行
     * <p>
     * 用于处理玩家放置方块时物品堆栈为空的特殊情况，恢复工具箱数据
     *
     * @param packetIn 玩家放置方块的数据包
     * @param ci       回调信息
     */
    @Inject(
        method = "processPlayerBlockPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;",
            ordinal = 1,
            shift = At.Shift.BEFORE),
        cancellable = true,
        require = 1)
    private void onHandleItemStackZero(C08PacketPlayerBlockPlacement packetIn, CallbackInfo ci) {
        // 获取玩家当前手持的物品堆栈
        ItemStack itemstack = this.playerEntity.inventory.getCurrentItem();
        if (itemstack != null && itemstack.stackSize == 0) {
            // 检查物品是否为 MetaGeneratedTool 且为工具箱
            if (ToolboxHelper.isMetaGeneratedTool(itemstack) && ToolboxHelper.isItemToolbox(itemstack)) {
                // 验证工具确实已耗尽（防止其他 mod 异常调用）
                long damage = MetaGeneratedTool.getToolDamage(itemstack);
                long maxDamage = MetaGeneratedTool.getToolMaxDamage(itemstack);
                if (damage < maxDamage) {
                    // 工具未真正耗尽，不返还工具箱
                    return;
                }
                // 从 NBT 中恢复工具箱数据
                NBTTagCompound tag = CommonUtils.openNbtData(itemstack);
                NBTTagCompound toolboxItems = tag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                ItemStack toolbox = ItemStack.loadItemStackFromNBT(toolboxItems);

                // 更新玩家物品栏，将工具箱放回当前槽位
                this.playerEntity.inventory.setInventorySlotContents(this.playerEntity.inventory.currentItem, toolbox);
                ci.cancel();
            }
        }

    }

}
