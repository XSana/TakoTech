package moe.takochan.takotech.common.tile.ae;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import moe.takochan.takotech.common.data.WebControllerData;
import moe.takochan.takotech.common.tile.BaseAETile;
import moe.takochan.takotech.constants.NBTConstants;

public class TileWebController extends BaseAETile {

    /**
     * 存储控制器配置信息
     */
    private final WebControllerData data;

    public TileWebController() {
        this.getProxy()
            .setIdlePowerUsage(0);

        this.data = new WebControllerData();
    }

    /**
     * 从NBT加载数据（世界加载时调用）
     * 通过AE2的TileEvent机制触发数据恢复
     *
     * @param data 包含持久化数据的NBT标签
     */
    @Override
    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_AENetwork(final NBTTagCompound data) {
        super.readFromNBT_AENetwork(data);
        NBTTagCompound tag = data.getCompoundTag(NBTConstants.CONTROLLER_DATA);
        if (tag != null) {
            this.getData()
                .readFormNBT(tag);
        }
    }

    /**
     * 写入数据到NBT（世界保存时调用）
     * 通过AE2的TileEvent机制触发数据保存
     *
     * @param data 用于存储数据的NBT标签
     */
    @Override
    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_AENetwork(final NBTTagCompound data) {
        super.writeToNBT_AENetwork(data);

        NBTTagCompound dataTag = new NBTTagCompound();
        this.getData()
            .writeToNBT(dataTag);
        data.setTag(NBTConstants.CONTROLLER_DATA, dataTag);
    }

    /**
     * TileEntity就绪时回调
     * 确保控制器拥有唯一标识符，自动生成UUID（若缺失）
     */
    @Override
    public void onReady() {
        super.onReady();
        String controllerId = this.getData()
            .getControllerId();
        if (controllerId == null || controllerId.isEmpty()) {
            this.getData()
                .setControllerId(
                    UUID.randomUUID()
                        .toString());
            this.markDirty();
        }
    }

    /**
     * 获取数据访问接口
     * 注意：对返回对象的修改会自动同步到NBT
     *
     * @return 包含控制器配置数据的对象
     */
    public WebControllerData getData() {
        return data;
    }

}
