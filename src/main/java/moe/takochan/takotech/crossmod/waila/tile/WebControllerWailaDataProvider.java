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

/**
 * WEB控制器专用WAILA数据提供者 在WAILA提示中显示控制器的信息 继承自基础提供者实现核心数据展示逻辑
 */
public class WebControllerWailaDataProvider extends BaseWailaDataProvider {

    /**
     * 构建WAILA提示信息主体内容
     *
     * @param currentToolTip 当前已生成的提示文本列表（可修改）
     * @return 包含控制器ID信息的更新后提示列表
     */
    @Override
    public List<String> getWailaBody(final ItemStack itemStack, final List<String> currentToolTip,
        final IWailaDataAccessor accessor, final IWailaConfigHandler config) {

        // 从访问器获取TileEntity实例
        final TileEntity wte = accessor.getTileEntity();

        // 类型安全检查确保是WEB控制器
        if (wte instanceof TileWebController te) {
            // 从数据容器获取控制器ID
            String controllerID = te.getData()
                .getControllerId();
            // 当ID有效时添加本地化提示
            if (controllerID != null) {
                currentToolTip.add(I18nUtils.tooltip(NameConstants.BLOCK_WEB_CONTROLLER_DESC_1) + " : " + controllerID);
            }
        }

        return currentToolTip;
    }
}
