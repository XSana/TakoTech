package moe.takochan.takotech.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import appeng.block.AEBaseItemBlock;
import appeng.block.AEBaseTileBlock;
import appeng.client.render.BlockRenderInfo;
import appeng.client.texture.FlippableIcon;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.common.Reference;
import moe.takochan.takotech.common.tile.BaseAETile;

public abstract class BaseAETileBlock extends AEBaseTileBlock implements IBaseAETileBlock {

    protected final String name;
    protected IIcon[] icons;

    /**
     * 初始化方块名称、材质和 TileEntity 类型
     *
     * @param name           方块名称
     * @param materialIn     方块材质
     * @param tileEntityType 关联的 TileEntity 类型
     */
    protected BaseAETileBlock(String name, Material materialIn, Class<? extends BaseAETile> tileEntityType) {
        super(materialIn);
        this.icons = new IIcon[6];
        this.name = name;
        this.setBlockName(Reference.RESOURCE_ROOT_ID + "." + name);
        this.setTileEntity(tileEntityType);
    }

    /**
     * 注册方块的所有图标（重写AE原有贴图获取逻辑）。
     * 此方法仅在客户端环境中调用，用于渲染方块。
     *
     * @param register IIconRegister 用于注册方块的图标
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        // 0:bottom 1:top 2:north 3:south 4:west 5:east

        BlockRenderInfo info = this.getRendererInstance();

        // 默认图标
        FlippableIcon defaultIcon = optionalIcon(register, "default", null);
        // 顶部和底部的默认图标
        FlippableIcon topBottomIcon = optionalIcon(register, "top_bottom", defaultIcon);
        // 侧面的默认图标
        FlippableIcon sideIcon = optionalIcon(register, "side", defaultIcon);
        // 顶部和底部图标
        FlippableIcon bottomIcon = optionalIcon(register, "bottom", topBottomIcon);
        FlippableIcon topIcon = optionalIcon(register, "top", topBottomIcon);
        // 北、南、西、东的图标
        FlippableIcon northIcon = optionalIcon(register, "north", sideIcon);
        FlippableIcon southIcon = optionalIcon(register, "south", sideIcon);
        FlippableIcon westIcon = optionalIcon(register, "west", sideIcon);
        FlippableIcon eastIcon = optionalIcon(register, "east", sideIcon);

        this.blockIcon = defaultIcon;

        info.updateIcons(bottomIcon, topIcon, northIcon, southIcon, eastIcon, westIcon);

    }

    /**
     * 尝试获取并注册指定的图标。如果指定的图标不存在，则使用默认图标（重写AE原有贴图获取逻辑）。
     *
     * @param iconRegister IIconRegister 用于注册图标
     * @param textureName  纹理名称，用于构建资源路径
     * @param defaultIcon  默认图标，如果资源不可用则使用该图标
     * @return 注册的 FlippableIcon 实例
     */
    @SideOnly(Side.CLIENT)
    private FlippableIcon optionalIcon(IIconRegister iconRegister, String textureName, IIcon defaultIcon) {

        String texturePath = Reference.RESOURCE_ROOT_ID + ":" + this.name + "/" + textureName;

        if (defaultIcon != null) {
            try {
                // 贴图目录 textures/blocks/方块名称/贴图文件名
                ResourceLocation resourceLocation = new ResourceLocation(
                    Reference.RESOURCE_ROOT_ID,
                    String.format("%s/%s/%s%s", "textures/blocks", this.name, textureName, ".png"));
                // 尝试获取资源
                IResource res = Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(resourceLocation);
                if (res != null) {
                    return new FlippableIcon(iconRegister.registerIcon(texturePath));
                }
            } catch (Throwable e) {
                return new FlippableIcon(defaultIcon);
            }
        }

        return new FlippableIcon(iconRegister.registerIcon(texturePath));
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 注册当前方块及其关联的 TileEntity。
     * 使用 GameRegistry 进行注册以便在游戏中使用。
     */
    public void register() {
        GameRegistry.registerBlock(this, AEBaseItemBlock.class, this.getName());
        GameRegistry.registerTileEntity(this.getTileEntityClass(), this.getName());
        this.setCreativeTab(TakoTechTabs.getInstance());
    }
}
