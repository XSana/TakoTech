package moe.takochan.takotech.crossmod.waila;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.google.common.collect.Lists;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import moe.takochan.takotech.crossmod.waila.tile.WebControllerWailaDataProvider;

public class TileWailaDataProvider implements IWailaDataProvider {

    private final List<IWailaDataProvider> providers;

    public TileWailaDataProvider() {
        final IWailaDataProvider webController = new WebControllerWailaDataProvider();

        this.providers = Lists.newArrayList(webController);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        for (final IWailaDataProvider provider : this.providers) {
            provider.getWailaHead(itemStack, currentTip, accessor, config);
        }

        return currentTip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        for (final IWailaDataProvider provider : this.providers) {
            provider.getWailaBody(itemStack, currentTip, accessor, config);
        }
        return currentTip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        for (final IWailaDataProvider provider : this.providers) {
            provider.getWailaTail(itemStack, currentTip, accessor, config);
        }

        return currentTip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
        int y, int z) {
        for (final IWailaDataProvider provider : this.providers) {
            provider.getNBTData(player, te, tag, world, x, y, z);
        }
        return tag;
    }
}
