package moe.takochan.takotech.coremod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/**
 * Mixin 插件入口与加载列表。
 */
public class MixinPlugin implements IMixinConfigPlugin {

    /**
     * 插件加载时调用，可用于初始化。
     *
     * @param mixinPackage Mixin 包名
     */
    @Override
    public void onLoad(String mixinPackage) {}

    /**
     * 返回 refmap 配置路径。
     *
     * @return refmap 配置路径，空字符串表示不使用
     */
    @Override
    public String getRefMapperConfig() {
        return "";
    }

    /**
     * 判断是否应用某个 mixin。
     *
     * @param targetClassName 目标类名
     * @param mixinClassName  Mixin 类名
     * @return 是否应用
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    /**
     * 接收并处理目标类列表。
     *
     * @param myTargets    当前插件的目标类集合
     * @param otherTargets 其他插件的目标类集合
     */
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    /**
     * 返回需要加载的 mixin 列表。
     *
     * @return mixin 列表
     */
    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        boolean isClient = FMLLaunchHandler.side()
            .isClient();

        // 如果已加载 InputFix 或 LWJGL3ify（Java17+），则跳过修复
        if (isClient && Stream.of("lain.mods.inputfix.InputFix", "me.eigenraven.lwjgl3ify.core.Lwjgl3ifyCoremod")
            .noneMatch(this::isClassExistSafe)) {
            mixins.add("InputFixMixin");
        }

        // 客户端：双层皮肤与瘦臂支持
        if (isClient) {
            mixins.add("AbstractClientPlayerMixin");
            mixins.add("SkinManagerMixin");
            mixins.add("ImageBufferDownloadMixin");
            mixins.add("RendererLivingEntityAccessor");
            mixins.add("RenderPlayerMixin");
        }

        mixins.add("NetHandlerPlayServerMixin");

        return mixins;
    }

    /**
     * mixin 应用前回调。
     *
     * @param s          目标类名
     * @param classNode  目标类节点
     * @param s1         mixin 类名
     * @param iMixinInfo mixin 信息
     */
    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    /**
     * mixin 应用后回调。
     *
     * @param s          目标类名
     * @param classNode  目标类节点
     * @param s1         mixin 类名
     * @param iMixinInfo mixin 信息
     */
    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

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
