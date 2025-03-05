package moe.takochan.takotech.common.data;

import net.minecraft.nbt.NBTTagCompound;

public class WebControllerData {

    /**
     * 控制器唯一标识符
     */
    public static final String CONTROLLER_ID = "controller_id";

    /**
     * 当前控制器的唯一标识字符串
     */
    private String controllerId;

    /**
     * 获取控制器标识符
     *
     * @return 格式为字符串的唯一ID，可能为空字符串
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * 设置控制器标识符
     *
     * @param controllerId 要设置的唯一ID字符串
     */
    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    /**
     * 将数据写入NBT标签
     *
     * @param data 目标NBT标签，必须为非空
     */
    public void writeToNBT(NBTTagCompound data) {
        data.setString(CONTROLLER_ID, controllerId);
    }

    /**
     * 从NBT标签读取数据
     *
     * @param nbt 来源NBT标签，允许为null（此时不执行读取）
     */
    public void readFormNBT(NBTTagCompound nbt) {
        controllerId = nbt.getString(CONTROLLER_ID);
    }
}
