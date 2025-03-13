package moe.takochan.takotech.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.gui.container.ContainerToolboxPlusSelect;

/**
 * GUI 类型枚举类
 * <p>
 * 用于管理不同类型的 GUI，并实现 IGuiHandler 接口以处理 GUI 的创建和注册
 */
public enum GuiType implements IGuiHandler {

    // 高级工具箱选择界面的 GUI 类型
    GUI_TOOLBOX_PLUS_SELECT(ContainerToolboxPlusSelect.class, GuiToolboxPlusSelect.class, null);

    // 关联的 TileEntity 类
    public final Class<?> tileClass;
    // 关联的 TileEntity 类
    private final Class<?> containerClass;
    // 关联的 GUI 类
    private final Class<?> guiClass;

    /**
     * 构造函数
     *
     * @param containerClass 关联的容器类
     * @param guiClass       关联的 GUI 类
     * @param tileClass      关联的 TileEntity 类
     */
    GuiType(final Class<?> containerClass, final Class<?> guiClass, final Class<?> tileClass) {
        this.containerClass = containerClass;
        this.guiClass = guiClass;
        this.tileClass = tileClass;
    }

    /**
     * 注册所有 GUI 类型
     */
    public static void register() {
        for (GuiType type : values()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(TakoTechMod.instance, type);
        }
    }

    /**
     * 获取服务器端的 GUI 元素（容器）
     *
     * @param ID     GUI 类型的 ID
     * @param player 玩家实例
     * @param world  世界实例
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @return 服务器端的容器实例
     */
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType guiType = values()[ID];
        try {
            return createContainer(guiType.tileClass, player, world, x, y, z);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 获取客户端的 GUI 元素（界面）
     *
     * @param ID     GUI 类型的 ID
     * @param player 玩家实例
     * @param world  世界实例
     * @param x      X 坐标
     * @param y      Y 坐标
     * @param z      Z 坐标
     * @return 客户端的 GUI 实例
     */
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        GuiType guiType = values()[ID];
        try {
            Object container = createContainer(guiType.tileClass, player, world, x, y, z);
            return guiClass.getConstructor(containerClass)
                .newInstance(container);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 创建容器实例
     *
     * @param tileClass 关联的 TileEntity 类
     * @param player    玩家实例
     * @param world     世界实例
     * @param x         X 坐标
     * @param y         Y 坐标
     * @param z         Z 坐标
     * @return 容器实例
     */
    public Object createContainer(Class<?> tileClass, EntityPlayer player, World world, int x, int y, int z) {
        try {
            if (tileClass != null) {
                // 如果关联了 TileEntity，则获取 TileEntity 并创建容器
                TileEntity te = world.getTileEntity(x, y, z);
                return containerClass.getConstructor(EntityPlayer.class, TileEntity.class)
                    .newInstance(player, te);
            } else {
                // 否则直接创建容器
                return containerClass.getConstructor(EntityPlayer.class)
                    .newInstance(player);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create container for " + this.name(), e);
        }
    }
}
