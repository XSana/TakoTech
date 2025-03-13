package moe.takochan.takotech.coremod;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;

@MCVersion("1.7.10")
public class FMLPlugin implements IFMLLoadingPlugin {

    /**
     * 获取 ASM 转换器类
     * <p>
     * 返回一个包含字节码转换器类名的数组
     *
     * @return 字节码转换器类名的数组
     */
    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "moe.takochan.takotech.coremod.transformer.ItemIC2Transformer" };
    }

    /**
     * 获取 Mod 容器类
     * <p>
     * 返回 Mod 容器的类名，如果不需要则返回 null
     *
     * @return Mod 容器的类名，或 null
     */
    @Override
    public String getModContainerClass() {
        return null;
    }

    /**
     * 获取设置类
     * <p>
     * 返回设置类的类名，如果不需要则返回 null
     *
     * @return 设置类的类名，或 null
     */
    @Override
    public String getSetupClass() {
        return null;
    }

    /**
     * 注入数据
     * <p>
     * 在 CoreMod 加载时调用，用于接收额外的数据
     *
     * @param data 包含额外数据的 Map
     */
    @Override
    public void injectData(Map<String, Object> data) {}

    /**
     * 获取访问权限转换器类
     * <p>
     * 返回访问权限转换器的类名，如果不需要则返回 null
     *
     * @return 访问权限转换器的类名，或 null
     */
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
