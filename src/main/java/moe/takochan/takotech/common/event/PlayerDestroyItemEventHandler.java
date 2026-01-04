package moe.takochan.takotech.common.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.items.MetaGeneratedTool;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.utils.CommonUtils;

public class PlayerDestroyItemEventHandler {

    /**
     * 处理工具损坏事件
     * <p>
     * 当玩家破坏物品时触发，检查是否为 GT 工具，并恢复工具箱
     * <p>
     * 注意：某些 mod（如龙之研究）会调用 destroyCurrentEquippedItem() 来放置物品，
     * 这也会触发此事件。需要检查工具是否真的耗尽才能返还工具箱。
     *
     * @param event 玩家破坏物品事件
     */
    @SubscribeEvent
    public void onToolBroken(PlayerDestroyItemEvent event) {
        // 获取被破坏的物品堆栈
        ItemStack brokenStack = event.original;
        // 检查物品是否为 GT 工具
        if (brokenStack != null && brokenStack.getItem() instanceof MetaGeneratedTool) {
            // 获取物品的 NBT 数据
            NBTTagCompound tag = CommonUtils.openNbtData(brokenStack);
            // 获取玩家实例
            EntityPlayer player = event.entityPlayer;
            // 检查玩家是否有效且物品包含工具箱数据
            if (player != null && tag.hasKey(NBTConstants.TOOLBOX_DATA)) {
                // 验证工具确实已耗尽（防止其他 mod 调用 destroyCurrentEquippedItem 触发误判）
                long damage = MetaGeneratedTool.getToolDamage(brokenStack);
                long maxDamage = MetaGeneratedTool.getToolMaxDamage(brokenStack);
                if (damage < maxDamage) {
                    // 工具未真正耗尽，不返还工具箱
                    return;
                }

                // 获取工具箱数据并加载工具箱物品
                final NBTTagCompound toolboxItems = tag.getCompoundTag(NBTConstants.TOOLBOX_DATA);
                final ItemStack toolbox = ItemStack.loadItemStackFromNBT(toolboxItems);
                // 将工具箱放入玩家当前手持的槽位
                player.inventory.setInventorySlotContents(player.inventory.currentItem, toolbox);
                // 如果玩家是服务器端玩家，则同步容器数据
                if (player instanceof EntityPlayerMP playerMP) {
                    playerMP.sendContainerToPlayer(player.inventoryContainer);
                }
            }
        }
    }

}
