package moe.takochan.takotech;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.TakoTech.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();

        boolean isClient = FMLLaunchHandler.side()
            .isClient();

        Map<String, ModContainer> map = Loader.instance()
            .getIndexedModList();

        if (isClient && map.containsKey("gregtech")) {
            mixins.add("gt.MetaGeneratedToolRendererMixin");
            mixins.add("gt.MetaBaseItemMixin");
        }

        return mixins;
    }
}
