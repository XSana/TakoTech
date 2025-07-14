package moe.takochan.takotech.common.item.ae;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.MinecraftForgeClient;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Upgrades;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.util.item.AEItemStack;
import appeng.util.item.OreHelper;
import appeng.util.item.OreReference;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.render.OreStorageCellRenderer;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.item.BaseAECellItem;
import moe.takochan.takotech.common.storage.ITakoCellInventory;
import moe.takochan.takotech.common.storage.ITakoCellInventoryHandler;
import moe.takochan.takotech.common.storage.inventory.OreStorageCellInventory;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

/**
 * 矿物存储元件
 * <p>
 * 只接受带有矿典的物品
 */
public class ItemOreStorageCell extends BaseAECellItem implements IStorageCell, IItemGroup {

    private static final EnumMap<OreStorageType, Map<String, Boolean>> oreWhitelistCache = new EnumMap<>(
        OreStorageType.class);

    private final IIcon[] overlayIcons = new IIcon[OreStorageType.values().length];

    private final int perType = 1;
    private final double idleDrain;

    @SuppressWarnings("Guava")
    public ItemOreStorageCell() {
        super(NameConstants.ITEM_ORE_STORAGE_CELL);

        idleDrain = 1.14;

        this.setMaxStackSize(1);
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_ORE_STORAGE_CELL));
        this.setFeature(EnumSet.of(AEFeature.StorageCells));

        this.setHasSubtypes(true);
        this.setMaxDamage(0);

        if (CommonUtils.isClient()) {
            MinecraftForgeClient.registerItemRenderer(this, new OreStorageCellRenderer());
        }
    }

    @Override
    public void getCheckedSubItems(final Item item, final CreativeTabs creativeTab, final List<ItemStack> itemStacks) {
        for (final OreStorageType type : OreStorageType.values()) {
            itemStacks.add(new ItemStack(item, 1, type.getMeta()));
        }
    }

    @Override
    public String getUnlocalizedName(final ItemStack itemStack) {
        return super.getUnlocalizedName() + "." + itemStack.getItemDamage();
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        for (final OreStorageType type : OreStorageType.values()) {
            this.overlayIcons[type.getMeta()] = register
                .registerIcon(Reference.RESOURCE_ROOT_ID + ":ore_" + type.getMeta());
        }
    }

    /**
     * 获取物品组未本地化字符串
     *
     * @param otherItems 其他物品
     * @param is         物品堆栈
     * @return 物品组未本地化字符串
     */
    @Override
    public String getUnlocalizedGroupName(Set<ItemStack> otherItems, ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    /**
     * 添加物品的详细信息
     *
     * @param itemStack       物品堆栈
     * @param player          玩家实体
     * @param lines           显示的详细信息
     * @param displayMoreInfo 是否显示更多信息
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(final ItemStack itemStack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC));

        // 获取物品堆栈关联的存储单元库存处理器
        final IMEInventoryHandler<?> inventory = AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(itemStack, null, StorageChannel.ITEMS);

        // 检查库存处理器是否是 ICellInventoryHandler 类型
        if (inventory instanceof ITakoCellInventoryHandler handler) {
            final ITakoCellInventory cellInventory = handler.getCellInv(); // 获取存储单元的库存

            if (cellInventory != null) {
                if (!cellInventory.getDiskID()
                    .isEmpty()) {
                    lines.add(cellInventory.getDiskID());
                }

                // 显示已存储的物品类型数量和总物品类型数量
                lines.add(
                    NumberFormat.getInstance()
                        .format(cellInventory.getStoredItemTypes()) + " "
                        + GuiText.Of.getLocal()
                        + ' '
                        + NumberFormat.getInstance()
                            .format(this.getTotalTypes(itemStack))
                        + ' '
                        + GuiText.Types.getLocal());

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
                            for (int i = 0; i < cellInventory.getConfigInventory()
                                .getSizeInventory(); ++i) {
                                ItemStack s = cellInventory.getConfigInventory()
                                    .getStackInSlot(i);
                                if (s != null) lines.add(s.getDisplayName()); // 显示物品名称
                            }
                        }
                    } else {
                        // 显示已设置的矿物过滤器
                        lines.add(GuiText.PartitionedOre.getLocal() + " : " + filter);
                    }
                }

                OreStorageType type = getStorageType(itemStack);
                String joinedIncludeDefs = type.getJoinedIncludes();
                String joinedExcludeDefs = type.getJoinedExcludes();

                if (!joinedIncludeDefs.isEmpty()) {
                    lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC_INCLUDE) + ": ");
                    lines.add(joinedIncludeDefs);
                }
                if (!joinedExcludeDefs.isEmpty()) {
                    lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC_EXCLUDE) + ": ");
                    lines.add(joinedExcludeDefs);
                }
            }
        }

        lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC_1));
        lines.add(I18nUtils.tooltip(NameConstants.ITEM_ORE_STORAGE_CELL_DESC_2));

        super.addCheckedInformation(itemStack, player, lines, displayMoreInfo); // 调用父类方法，添加其他信息
    }

    /**
     * 获取该存储单元可用的字节大小。
     *
     * @param cellItem 存储单元物品
     * @return 无限容量，返回最大值
     */
    @Override
    public int getBytes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    /**
     * 获取该存储单元的可用字节大小（返回长整型值）。
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
        return this.perType;
    }

    /**
     * 获取每种类型的字节大小。
     *
     * @param cellItem 存储单元物品
     * @return 每种类型的字节大小
     */
    @Override
    public int getBytesPerType(ItemStack cellItem) {
        return this.perType;
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
        if (!(requestedAddition instanceof AEItemStack itemStack)) {
            return true;
        }
        // 获取矿物信息
        OreReference oreReference = OreHelper.INSTANCE.isOre(itemStack.getItemStack());
        if (oreReference == null) {
            return true;
        }
        // 存在矿典标签，获列表
        Collection<String> oreDefs = oreReference.getEquivalents();
        OreStorageType storageType = getStorageType(cellItem);

        for (String oreDef : oreDefs) {
            if (isOreAllowed(storageType, oreDef)) {
                // 白名单命中
                return false;
            }
        }
        return true;
    }

    /**
     * 允许指定此存储单元是否可以存储在其他存储单元中，仅针对像物质炮这样不是通用存储的特殊物品设置。。
     *
     * @return 如果该存储单元可以存储在其他存储单元中，则返回 true，通常情况下返回 false，除非在特定情况下，例如物质炮。
     */
    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    /**
     * 判断该物品是否为存储单元。
     *
     * @param i 物品堆栈
     * @return 不作为默认存储单元
     */
    @Override
    public boolean isStorageCell(ItemStack i) {
        // 这里恒定为false，使AE再注册元件时不会将该物品注册为标准存储元件，以便于让自定义的CellInventory接管
        return false;
    }

    /**
     * 旧版空闲消耗 API。在基础 AE2 中未使用。
     *
     * @return 该存储单元将使用的 AE/t 消耗量。
     */
    @Override
    public double getIdleDrain() {
        return this.idleDrain;
    }

    /**
     * 如果返回 false，物品将不会被视为存储单元，且不能插入工作台。
     *
     * @param is 物品
     * @return 元件是否可插入元件工作台并编辑
     */
    @Override
    public boolean isEditable(ItemStack is) {
        return true;
    }

    /**
     * 获取存储单元的升级槽
     *
     * @param is 物品堆栈
     * @return 返回存储单元的升级槽，若没有则返回 null
     */
    @Override
    public IInventory getUpgradesInventory(ItemStack is) {
        return new CellUpgrades(is, 2);
    }

    /**
     * 用于提取或将工作台的内容镜像到存储单元中。
     *
     * @param is 物品堆栈
     * @return 返回存储单元的配置槽，若没有则返回 null
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
        CommonUtils.openNbtData(is)
            .setString("FuzzyMode", fzMode.name());
    }

    /**
     * 获取矿典过滤器, 需要矿典卡
     *
     * @param is 存储单元物品
     * @return 当前的矿物字典过滤器
     */
    @Override
    public String getOreFilter(ItemStack is) {
        return CommonUtils.openNbtData(is)
            .getString("OreFilter");
    }

    /**
     * 设置矿物字典过滤器
     *
     * @param is     存储单元物品
     * @param filter 矿典过滤器字符串
     */
    @Override
    public void setOreFilter(ItemStack is, String filter) {
        CommonUtils.openNbtData(is)
            .setString("OreFilter", filter);
    }

    /**
     * 获取库存管理实例
     *
     * @param o         物品堆栈
     * @param container 存储提供者，用于保存和管理数据
     * @return 库存管理实例
     * @throws AppEngException 如果无法获取库存或发生错误时抛出该异常
     */
    @Override
    public OreStorageCellInventory getCellInv(ItemStack o, ISaveProvider container) throws AppEngException {
        return new OreStorageCellInventory(o, container);
    }

    /**
     * 注册物品
     */
    @Override
    public void register() {
        GameRegistry.registerItem(this, this.name);
        setCreativeTab(TakoTechTabs.getInstance());

        for (OreStorageType type : OreStorageType.values()) {
            ItemStack itemStack = new ItemStack(this, 1, type.getMeta());
            Upgrades.INVERTER.registerItem(itemStack, 1);
            Upgrades.ORE_FILTER.registerItem(itemStack, 1);
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon(int meta) {
        if (meta >= 1 && meta < overlayIcons.length) {
            return overlayIcons[meta];
        }
        return null;
    }

    private OreStorageType getStorageType(ItemStack itemStack) {
        return OreStorageType.byMeta(itemStack.getItemDamage());
    }

    /**
     * 判断某个 oreDef 是否被指定的存储类型允许。
     * <p>
     * 使用 EnumMap 缓存每个 OreStorageType 对每个 oreDef 的判断结果， 避免重复进行 startsWith 匹配。
     * <p>
     * 匹配逻辑： - 若前缀匹配 Excluded，则视为不允许（黑名单优先） - 否则，若前缀匹配 Included，则视为允许 - 其余情况默认视为不允许
     *
     * @param type   存储元件类型
     * @param oreDef 矿典标签（如 "oreIron"）
     * @return 是否允许该矿典标签被存入此元件
     */
    private boolean isOreAllowed(OreStorageType type, String oreDef) {
        return oreWhitelistCache.computeIfAbsent(type, t -> new HashMap<>())
            .computeIfAbsent(oreDef, def -> {
                for (String exclude : type.getExcludedPrefixes()) {
                    if (def.startsWith(exclude)) return false;
                }
                for (String include : type.getIncludedPrefixes()) {
                    if (def.startsWith(include)) return true;
                }
                return false;
            });
    }
}
