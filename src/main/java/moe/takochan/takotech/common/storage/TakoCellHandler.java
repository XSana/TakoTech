package moe.takochan.takotech.common.storage;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.storage.*;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import moe.takochan.takotech.common.item.BaseAECellItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

public class TakoCellHandler implements ICellHandler {
    @Override
    public boolean isCell(ItemStack is) {
        return is != null && is.getItem() instanceof BaseAECellItem;
    }

    @Override
    public IMEInventoryHandler<?> getCellInventory(ItemStack is, ISaveProvider host, StorageChannel channel) {
        if (isCell(is) && is.getItem() instanceof BaseAECellItem aci) {
            try {
                return aci.getInventoryHandler(is, host);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public IIcon getTopTexture_Light() {
        return ExtraBlockTextures.BlockMEChestItems_Light.getIcon();
    }

    @Override
    public IIcon getTopTexture_Medium() {
        return ExtraBlockTextures.BlockMEChestItems_Medium.getIcon();
    }

    @Override
    public IIcon getTopTexture_Dark() {
        return ExtraBlockTextures.BlockMEChestItems_Dark.getIcon();
    }

    @Override
    public void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler cellHandler, IMEInventoryHandler inv, ItemStack is, StorageChannel chan) {
        Platform.openGUI(player, (TileEntity) chest, chest.getUp(), GuiBridge.GUI_ME);
    }

    @Override
    public int getStatusForCell(ItemStack is, IMEInventory handler) {
        if (handler instanceof TakoCellInventoryHandler ci) {
            return ci.getStatusForCell();
        }
        return 0;
    }

    @Override
    public double cellIdleDrain(ItemStack is, IMEInventory handler) {
        if (handler instanceof TakoCellInventoryHandler ci) {
            return ci.getCellInv().getIdleDrain();
        }
        return 0;
    }
}
