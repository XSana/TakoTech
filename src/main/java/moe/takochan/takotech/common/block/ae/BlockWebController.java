package moe.takochan.takotech.common.block.ae;

import java.util.ArrayList;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import appeng.block.AEBaseItemBlock;
import moe.takochan.takotech.common.block.BaseAETileBlock;
import moe.takochan.takotech.common.tile.ae.TileWebController;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;

public class BlockWebController extends BaseAETileBlock {

    public BlockWebController() {
        super(NameConstants.BLOCK_WEB_CONTROLLER, Material.iron, TileWebController.class);

        this.setHardness(1.14f); // 硬度
        this.setLightOpacity(255); // 完全不透光
        this.setLightLevel(0); // 不自发光
        this.setHarvestLevel("pickaxe", 0); // 可用镐破坏
    }

    /**
     * 获取方块掉落物（保留数据存储）
     *
     * @param world    当前世界
     * @param x        X 坐标
     * @param y        Y 坐标
     * @param z        Z 坐标
     * @param metadata 元数据
     * @param fortune  时运等级
     * @return 包含该方块所有掉落物品的ArrayList
     */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>(1);
        drops.add(getItemStack(world, x, y, z));
        return drops;
    }

    /**
     * 当玩家破坏方块时调用。该方法负责实际销毁方块，且调用时方块仍保持完整状态。
     * 无论玩家是否能够采集该方块，此方法都会被触发。
     * 在多人游戏环境下，该方法会在客户端和服务端同步调用！
     *
     * @param world       当前世界
     * @param player      正在破坏方块的玩家（可能为null）
     * @param x           X 坐标
     * @param y           Y 坐标
     * @param z           Z 坐标
     * @param willHarvest 当返回值为true时，后续会调用Block.harvestBlock方法。
     * @return 若方块实际被销毁则返回true
     */
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (willHarvest) {
            dropBlockAsItem(world, x, y, z, getItemStack(world, x, y, z));
        }
        super.removedByPlayer(world, player, x, y, z, willHarvest);
        return false;
    }

    /**
     * 获取玩家创造模式下选取时的物品形式
     *
     * @param target 玩家正在注视的完整目标对象
     * @param world  当前世界
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @param player 当前玩家
     * @return 要添加到玩家物品栏的ItemStack，如果不应添加任何内容则返回null
     */
    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        // 创造模式获取不保留nbt数据，防止id重复
        return getItemStack(world, x, y, z, true);
    }

    /**
     * 放置方块时调用
     *
     * @param world  当前世界
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @param player 当前玩家
     * @param stack  物品堆栈
     */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        TileEntity wte = world.getTileEntity(x, y, z);
        if (!(wte instanceof TileWebController te)) {
            return;
        }
        if (stack.hasTagCompound()) {
            te.getData()
                .readFormNBT(stack.getTagCompound());
        }
        te.markDirty();
        super.onBlockPlacedBy(world, x, y, z, player, stack);
    }

    /**
     * 获取对应ItemBlock类型
     *
     * @return ItemBlockWebController
     */
    @Override
    public Class<? extends AEBaseItemBlock> getItemBlockClass() {
        return ItemBlockWebController.class;
    }

    /**
     * 获取方块物品（非创造模式，保留nbt）
     *
     * @param world 当前世界
     * @param x     X 坐标
     * @param y     Y 坐标
     * @param z     Z 坐标
     * @return 掉落物品堆栈
     */
    ItemStack getItemStack(World world, int x, int y, int z) {
        return getItemStack(world, x, y, z, false);
    }

    /**
     * 获取方块物品
     *
     * @param world      当前世界
     * @param x          X 坐标
     * @param y          Y 坐标
     * @param z          Z 坐标
     * @param isCreative 是否为创造模式
     * @return 掉落物品堆栈
     */
    ItemStack getItemStack(World world, int x, int y, int z, boolean isCreative) {
        ItemStack drop = new ItemStack(this);
        TileEntity wte = world.getTileEntity(x, y, z);
        if (!isCreative && wte instanceof TileWebController te) {
            NBTTagCompound tag = new NBTTagCompound();
            te.writeToNBT(tag);
            drop.setTagCompound(tag.getCompoundTag(NBTConstants.CONTROLLER_DATA));
        }
        return drop;
    }
}
