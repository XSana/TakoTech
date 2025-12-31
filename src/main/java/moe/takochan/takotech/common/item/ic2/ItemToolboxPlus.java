package moe.takochan.takotech.common.item.ic2;

import static moe.takochan.takotech.client.settings.GameSettings.selectTool;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.item.IHandHeldInventory;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import moe.takochan.takotech.utils.I18nUtils;

public class ItemToolboxPlus extends BaseItemToolbox implements IHandHeldInventory {

    // 基础图标
    @SideOnly(Side.CLIENT)
    private IIcon baseIcon;

    public ItemToolboxPlus() {
        super(NameConstants.ITEM_TOOLBOX_PLUS);
        // 设置最大堆叠数为1
        this.setMaxStackSize(1);
        // 设置材质路径
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_TOOLBOX_PLUS));
    }

    /**
     * 添加物品信息
     *
     * @param itemStack       物品堆栈
     * @param player          玩家
     * @param lines           信息列表
     * @param displayMoreInfo 是否显示更多信息
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack itemStack, final EntityPlayer player, final List<String> lines,
        final boolean displayMoreInfo) {
        lines.add(
            I18nUtils.tooltip(
                NameConstants.ITEM_TOOLBOX_PLUS_DESC,
                GameSettings.getKeyDisplayString(selectTool.getKeyCode())));
    }

    /**
     * 注册图标
     *
     * @param register 图标注册器
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        this.baseIcon = register.registerIcon(this.getIconString());
    }

    /**
     * 注册物品
     */
    @Override
    public void register() {
        GameRegistry.registerItem(this, NameConstants.ITEM_TOOLBOX_PLUS);
        setCreativeTab(TakoTechTabs.getInstance());
    }

    /**
     * 获取基础图标
     *
     * @return 基础图标
     */
    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }

}
