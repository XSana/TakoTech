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
import moe.takochan.takotech.constants.NameConstants;

public class BlockWebController extends BaseAETileBlock {

    public BlockWebController() {
        super(NameConstants.BLOCK_WEB_CONTROLLER, Material.iron, TileWebController.class);

        this.setHardness(1.14f);
        this.setLightOpacity(255);
        this.setLightLevel(0);
        this.setHarvestLevel("pickaxe", 0);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>(1);
        drops.add(getItemStack(world, x, y, z));
        return drops;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (willHarvest) {
            dropBlockAsItem(world, x, y, z, getItemStack(world, x, y, z));
        }
        super.removedByPlayer(world, player, x, y, z, willHarvest);
        return false;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return getItemStack(world, x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase player, ItemStack is) {
        super.onBlockPlacedBy(w, x, y, z, player, is);

        TileEntity wte = getTileEntity(w, x, y, z);
        if (!(wte instanceof TileWebController te)) {
            return;
        }
        if (is.hasTagCompound()) {
            te.readFromNBT(is.getTagCompound());
        }
        te.markDirty();
    }

    @Override
    public Class<? extends AEBaseItemBlock> getItemBlockClass() {
        return ItemBlockWebController.class;
    }

    ItemStack getItemStack(World w, int x, int y, int z) {
        ItemStack drop = new ItemStack(this);
        TileEntity wte = w.getTileEntity(x, y, z);
        if (wte instanceof TileWebController te) {
            NBTTagCompound tag = new NBTTagCompound();
            te.writeToNBT(tag);
            tag.removeTag("x");
            tag.removeTag("y");
            tag.removeTag("z");
            tag.removeTag("orientation_up");
            tag.removeTag("web_controller");
            tag.removeTag("orientation_forward");
            tag.removeTag("proxy");
            tag.removeTag("id");
            drop.setTagCompound(tag);
        }
        return drop;
    }
}
