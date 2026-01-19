package moe.takochan.takotech.coremod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

/**
 * 按已加载模组延迟注册 mixin。
 */
@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    /**
     * 提供延迟 mixin 配置文件名。
     */
    @Override
    public String getMixinConfig() {
        return "mixins.TakoTech.late.json";
    }

    /**
     * 根据已加载模组返回需要应用的 mixin 列表。
     */
    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();

        boolean isClient = FMLLaunchHandler.side()
            .isClient();

        Map<String, ModContainer> map = Loader.instance()
            .getIndexedModList();

        // GT5U 相关 mixin
        if (map.containsKey("gregtech")) {
            if (isClient) {
                mixins.add("gt.MetaGeneratedToolRendererMixin");
            }
            mixins.add("gt.MetaGeneratedToolMixin");
        }

        // IC2 相关 mixin
        if (map.containsKey("IC2")) {
            mixins.add("ic2.ItemWrapperMixin");
        }

        // Galacticraft 相关 mixin
        if (isClient && map.containsKey("GalacticraftCore")) {
            mixins.add("gc.ModelPlayerGCMixin");
            mixins.add("gc.RenderPlayerGCMixin");
        }

        return mixins;
    }

    /**
     * 安全判断类是否存在。
     */
    private boolean isClassExistSafe(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
