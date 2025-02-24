package moe.takochan.takotech.common.block.ae;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import appeng.block.AEBaseItemBlock;
import moe.takochan.takotech.common.block.BaseAETileBlock;
import moe.takochan.takotech.common.tile.BaseAETile;
import moe.takochan.takotech.common.tile.ae.TileWebController;
import moe.takochan.takotech.constants.NBTConstants;
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
        TileWebController te = getTileEntity(world, x, y, z);
        if (te != null) {
            ItemStack stack = new ItemStack(this);
            NBTTagCompound tileData = new NBTTagCompound();
            tileData.setString(NBTConstants.CONTROLLER_ID, te.getControllerId());
            stack.setTagCompound(tileData);
            return new ArrayList<>(Arrays.asList(stack));
        }
        return super.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, int x, int y, int z, int meta) {
        super.harvestBlock(worldIn, player, x, y, z, meta);
        worldIn.setBlockToAir(x, y, z);
    }

    @Override
    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase player, ItemStack is) {
        super.onBlockPlacedBy(w, x, y, z, player, is);
        if (is.hasTagCompound() && is.getTagCompound()
            .hasKey(NBTConstants.CONTROLLER_ID)) {
            String controllerId = is.getTagCompound()
                .getString(NBTConstants.CONTROLLER_ID);
            if (w.getTileEntity(x, y, z) instanceof BaseAETile) {
                TileWebController tile = getTileEntity(w, x, y, z);
                if (tile != null) {
                    tile.setControllerId(controllerId);
                    tile.markDirty();
                }
            }
        }
    }

    @Override
    public Class<? extends AEBaseItemBlock> getItemBlockClass() {
        return ItemBlockWebController.class;
    }
}
