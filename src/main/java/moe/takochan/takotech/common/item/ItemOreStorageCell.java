package moe.takochan.takotech.common.item;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.common.tabs.TakoTechTabs;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ItemOreStorageCell extends BaseAECellItem implements IStorageCell, IItemGroup {

    @SuppressWarnings("Guava")
    public ItemOreStorageCell() {
        this.setMaxStackSize(1);
        this.setUnlocalizedName(NameConstants.ITEM_ORE_STORAGE_CELL);
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_ORE_STORAGE_CELL));
        this.setFeature(EnumSet.of(AEFeature.StorageCells));
    }

    /**
     * 获取该物品所在组的本地化名称。
     *
     * @param otherItems 其他物品集合
     * @param is         当前物品
     * @return 物品组的本地化名称
     */
    @Override
    public String getUnlocalizedGroupName(Set<ItemStack> otherItems, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    /**
     * 客户端显示物品的详细信息，主要用于显示物品的工具提示（tooltips）。
     *
     * @param stack           物品堆栈
     * @param player          当前玩家
     * @param lines           工具提示文本列表
     * @param displayMoreInfo 是否显示更多信息（由 Shift 键控制）
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines, final boolean displayMoreInfo) {
        lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC)); // 添加物品的描述

        // 获取物品堆栈关联的存储单元库存处理器
        final IMEInventoryHandler<?> inventory = AEApi.instance().registries().cell()
            .getCellInventory(stack, null, StorageChannel.ITEMS);

        // 检查库存处理器是否是 ICellInventoryHandler 类型
        if (inventory instanceof ICellInventoryHandler handler) {
            final ICellInventory cellInventory = handler.getCellInv(); // 获取存储单元的库存

            if (cellInventory != null) {
                // 显示已用字节数和总字节数
                lines.add(NumberFormat.getInstance().format(cellInventory.getUsedBytes()) + " "
                    + GuiText.Of.getLocal() + ' '
                    + NumberFormat.getInstance().format(cellInventory.getTotalBytes())
                    + ' ' + GuiText.BytesUsed.getLocal());

                // 显示已存储的物品类型数量和总物品类型数量
                lines.add(NumberFormat.getInstance().format(cellInventory.getStoredItemTypes()) + " "
                    + GuiText.Of.getLocal() + ' '
                    + NumberFormat.getInstance().format(this.getTotalTypes(stack))
                    + ' ' + GuiText.Types.getLocal());

                // 如果存储单元启用了预格式化
                if (handler.isPreformatted()) {
                    String filter = cellInventory.getOreFilter(); // 获取矿物过滤器
                    if (filter.isEmpty()) {
                        // 显示包含或排除模式
                        final String list = (handler.getIncludeExcludeMode() == IncludeExclude.WHITELIST
                            ? GuiText.Included
                            : GuiText.Excluded).getLocal();

                        // 判断是否启用了模糊模式
                        if (handler.isFuzzy()) {
                            lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Fuzzy.getLocal());
                        } else {
                            lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Precise.getLocal());
                        }

                        // 如果 Shift 键被按下，显示过滤器详细信息
                        if (GuiScreen.isShiftKeyDown()) {
                            lines.add(GuiText.Filter.getLocal() + ": ");
                            for (int i = 0; i < cellInventory.getConfigInventory().getSizeInventory(); ++i) {
                                ItemStack s = cellInventory.getConfigInventory().getStackInSlot(i);
                                if (s != null) lines.add(s.getDisplayName()); // 显示物品名称
                            }
                        }
                    } else {
                        // 显示已设置的矿物过滤器
                        lines.add(GuiText.PartitionedOre.getLocal() + " : " + filter);
                    }
                }
            }
        }

        super.addCheckedInformation(stack, player, lines, displayMoreInfo); // 调用父类方法，添加其他信息
    }


    /**
     * 获取该存储单元的字节大小。
     *
     * @param cellItem 存储单元物品
     * @return 无限容量，返回最大值
     */
    @Override
    public int getBytes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    /**
     * 获取该存储单元的字节大小（返回长整型值）。
     *
     * @param cellItem 存储单元物品
     * @return 无限容量，返回最大值
     */
    @Override
    public long getBytesLong(final ItemStack cellItem) {
        return Long.MAX_VALUE;
    }

    /**
     * 每种物品类型占用的字节数。
     *
     * @param cellItem 存储单元物品
     * @return 每种类型占用的字节数
     */
    @Override
    public int BytePerType(ItemStack cellItem) {
        return 1;
    }

    /**
     * 获取每种类型的字节大小。
     *
     * @param cellItem 存储单元物品
     * @return 每种类型的字节大小
     */
    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return 1;
    }

    /**
     * 获取该存储单元支持的物品类型总数。
     *
     * @param cellItem 存储单元物品
     * @return 支持无限多的物品类型
     */
    @Override
    public int getTotalTypes(ItemStack cellItem) {
        // 应该没这么多矿物类型吧（恶臭）
        return 114514;
    }

    /**
     * 判断某个物品是否被黑名单所包含。
     *
     * @param cellItem          存储单元物品
     * @param requestedAddition 要添加的物品
     * @return 如果物品在黑名单中，返回 true，否则返回 false
     */
    @Override
    public boolean isBlackListed(ItemStack cellItem, IAEItemStack requestedAddition) {
        if (requestedAddition instanceof AEItemStack itemStack) {
            System.out.println("Storing ore: " + itemStack.getDisplayName() + " " + itemStack.isOre());
            return !itemStack.isOre();
        }
        return true;
    }

    /**
     * 判断该存储单元是否可以存储物品。
     *
     * @return 如果不可存储，返回 false
     */
    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    /**
     * 判断该物品是否为存储单元。
     *
     * @param i 物品堆栈
     * @return 如果是存储单元，返回 true，否则返回 false
     */
    @Override
    public boolean isStorageCell(ItemStack i) {
        return true;
    }

    /**
     * 获取该存储单元的空闲能量消耗（即空闲状态下的能量消耗）。
     *
     * @return 空闲状态下的能量消耗值
     */
    @Override
    public double getIdleDrain() {
        return 0;
    }

    /**
     * 判断该物品是否可以编辑。
     *
     * @param is 物品堆栈
     * @return 如果可编辑，返回 true；否则返回 false
     */
    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    /**
     * 获取物品的升级物品栏（如果有）。
     *
     * @param is 物品堆栈
     * @return 返回该物品的升级物品栏，若没有则返回 null
     */
    @Override
    public IInventory getUpgradesInventory(ItemStack is) {
        return new CellUpgrades(is, 2);
    }

    /**
     * 获取物品的配置物品栏（如果有）。
     *
     * @param is 物品堆栈
     * @return 返回该物品的配置物品栏，若没有则返回 null
     */
    @Override
    public IInventory getConfigInventory(ItemStack is) {
        return new CellConfig(is);
    }

    /**
     * 获取该物品的模糊模式设置（如果有）。
     *
     * @param is 物品堆栈
     * @return 返回该物品的模糊模式设置，若没有则返回 null
     */
    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        return FuzzyMode.fromItemStack(is);
    }

    /**
     * 设置该物品的模糊模式。
     *
     * @param is     物品堆栈
     * @param fzMode 目标模糊模式
     */
    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        Platform.openNbtData(is)
            .setString("FuzzyMode", fzMode.name());
    }

    @Override
    public String getOreFilter(ItemStack is) {
        return Platform.openNbtData(is).getString("OreFilter");
    }

    @Override
    public void setOreFilter(ItemStack is, String filter) {
        Platform.openNbtData(is).setString("OreFilter", filter);
    }

    @Override
    public void register() {
        GameRegistry.registerItem(this, "ore_storage_cell");
        setCreativeTab(TakoTechTabs.INSTANCE);
    }
}
