package moe.takochan.takotech.crossmod.waila.tile;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import moe.takochan.takotech.common.tile.ae.TileWebController;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.crossmod.waila.BaseWailaDataProvider;
import moe.takochan.takotech.utils.I18nUtils;

public class WebControllerWailaDataProvider extends BaseWailaDataProvider {

    @Override
    public List<String> getWailaBody(final ItemStack itemStack, final List<String> currentToolTip,
        final IWailaDataAccessor accessor, final IWailaConfigHandler config) {

        final TileEntity wte = accessor.getTileEntity();

        if (wte instanceof TileWebController te) {
            String controllerID = te.getData()
                .getControllerId();
            if (controllerID != null) {
                currentToolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC_1) + " : " + controllerID);
            }
        }

        return currentToolTip;
    }
}
