package moe.takochan.takotech.crossmod.waila.tile;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import moe.takochan.takotech.common.tile.ae.TileWebController;
import moe.takochan.takotech.crossmod.waila.BaseWailaDataProvider;

public class WebControllerWailaDataProvider extends BaseWailaDataProvider {

    @Override
    public List<String> getWailaBody(final ItemStack itemStack, final List<String> currentToolTip,
        final IWailaDataAccessor accessor, final IWailaConfigHandler config) {
        final TileEntity wte = accessor.getTileEntity();

        if (wte instanceof TileWebController te) {
            String controllerID = te.getData()
                .getControllerId();
            if (controllerID != null) {
                currentToolTip.add(controllerID);
            }
        }

        return currentToolTip;
    }
}
