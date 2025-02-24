package moe.takochan.takotech.common.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.client.texture.ExtraBlockTextures;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import moe.takochan.takotech.common.item.BaseAECellItem;
import moe.takochan.takotech.common.item.IBaseAECellItem;

public class TakoCellHandler implements ICellHandler {

    /**
     * 检查物品是否由你的存储元件处理器处理
     *
     * @param is 要检查的物品
     * @return 如果提供的物品由你的存储元件处理器处理，返回true。（AE可能会选择跳过此方法，直接请求处理器）
     */
    @Override
    public boolean isCell(ItemStack is) {
        return is != null && is.getItem() instanceof BaseAECellItem;
    }

    /**
     * 获取存储元件库存处理器
     *
     * @param is      存储元件物品
     * @param host    每当存储元件的内容发生变化时，应该使用此方法请求保存。请注意，该值可能为null。
     * @param channel 请求的存储通道
     * @return 如果无法处理提供的物品，返回null，否则返回一个库存处理器
     */
    @Override
    public IMEInventoryHandler<?> getCellInventory(ItemStack is, ISaveProvider host, StorageChannel channel) {
        if (isCell(is) && is.getItem() instanceof IBaseAECellItem aci && channel == StorageChannel.ITEMS) {
            try {
                return new TakoCellInventoryHandler(aci.getCellInv(is, host));
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * @return 存储元件类型的ME箱子的光亮纹理，应该是 10x10 像素，16x16纹理上有3像素透明填充，如果存储元件不能用于ME箱子，返回null。
     */
    @Override
    public IIcon getTopTexture_Light() {
        return ExtraBlockTextures.BlockMEChestItems_Light.getIcon();
    }

    /**
     * @return 存储元件类型的ME箱子的中等纹理，应该是 10x10 像素，16x16纹理上有3像素透明填充，如果存储元件不能用于ME箱子，返回null。
     */
    @Override
    public IIcon getTopTexture_Medium() {
        return ExtraBlockTextures.BlockMEChestItems_Medium.getIcon();
    }

    /**
     * @return 存储元件类型的ME箱子的黑暗纹理，应该是 10x10 像素，16x16纹理上有3像素透明填充，如果存储元件不能用于ME箱子，返回null。
     */
    @Override
    public IIcon getTopTexture_Dark() {
        return ExtraBlockTextures.BlockMEChestItems_Dark.getIcon();
    }

    /**
     * 当存储元件被放置在ME箱子中，用户尝试打开终端侧面时调用。如果你的物品不能通过ME箱子使用，可以告知用户不能使用，或者做出其他提示。否则，你应该打开你的GUI并显示存储元件给用户。
     *
     * @param player      打开箱子GUI的玩家
     * @param chest       要打开的箱子
     * @param cellHandler 存储元件处理器
     * @param inv         库存处理器
     * @param is          物品
     * @param chan        存储通道
     */
    @Override
    public void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler cellHandler,
        IMEInventoryHandler inv, ItemStack is, StorageChannel chan) {
        Platform.openGUI(player, (TileEntity) chest, chest.getUp(), GuiBridge.GUI_ME);
    }

    /**
     * 0 - 存储元件缺失
     * <p>
     * 1 - 绿色，（通常意味着存储元件100%空闲）
     * <p>
     * 2 - 蓝色，（通常意味着有可用空间可以存储物品类型或物品）
     * <p>
     * 3 - 橙色，（通常意味着可以存储物品，但不能存储物品类型）
     * <p>
     * 4 - 红色，（通常意味着存储元件已满）
     *
     * @param is      存储元件物品
     * @param handler 存储元件的处理器，可以将其转换为你的处理器
     * @return 根据存储元件的内容返回其状态
     */
    @Override
    public int getStatusForCell(ItemStack is, IMEInventory handler) {
        if (handler instanceof TakoCellInventoryHandler ci) {
            return ci.getStatusForCell();
        }
        return 0;
    }

    /**
     * @return 存储元件在箱子/驱动器中空闲时的耗电量。
     */
    @Override
    public double cellIdleDrain(ItemStack is, IMEInventory handler) {
        if (handler instanceof TakoCellInventoryHandler ci) {
            return ci.getCellInv()
                .getIdleDrain();
        }
        return 0;
    }
}
