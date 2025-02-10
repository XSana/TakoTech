package moe.takochan.takotech.common.storage;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.storage.*;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.common.item.BaseAECellItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

public class CellHandler implements ICellHandler {
    @Override
    public boolean isCell(ItemStack is) {
        TakoTechMod.LOG.info(is.getItem().getUnlocalizedName());
        if (is.getItem() instanceof BaseAECellItem) TakoTechMod.LOG.info("isCell");
        else TakoTechMod.LOG.info("isCell false");
        return is != null && is.getItem() instanceof BaseAECellItem;
    }

    @Override
    public IMEInventoryHandler<?> getCellInventory(ItemStack is, ISaveProvider host, StorageChannel channel) {
        if (isCell(is)) {
            try {
                return new CellInventoryHandler(new OreStorageCellInventory(is, host));
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
        if (handler instanceof CellInventoryHandler ci) {
            return ci.getStatusForCell();
        }
        return 0;
    }

    @Override
    public double cellIdleDrain(ItemStack is, IMEInventory handler) {
        final IBaseCellInventory inv = ((IBaseCellInventoryHandler) handler).getCellInv();
        return inv.getIdleDrain();
    }
}
