package moe.takochan.takotech.mixin;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;

public class MixinPlugin implements IMixinConfigPlugin {

    /**
     * 该方法在插件加载时被调用。可以在这里进行插件的初始化操作，例如加载配置文件等。
     *
     * @param mixinPackage Mixin 包名
     */
    @Override
    public void onLoad(String mixinPackage) {
    }

    /**
     * 返回映射器配置的路径。该方法用于指定反向映射配置文件（如果有）。
     * 如果不需要映射器配置，可以返回空字符串。
     *
     * @return 映射器配置的路径，空字符串表示不需要配置
     */
    @Override
    public String getRefMapperConfig() {
        return "";
    }

    /**
     * 判断是否应用某个 Mixin。该方法接收目标类名和 Mixin 类名，根据条件返回是否应用该 Mixin。
     *
     * @param targetClassName 目标类的名称
     * @param mixinClassName  Mixin 类的名称
     * @return 是否应用该 Mixin
     */
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if ("net.minecraft.client.gui.GuiScreen".equals(targetClassName)
            && "moe.takochan.takotech.mixin.InputFixMixin".equals(mixinClassName)) {
            return true;
        }
        return false;
    }

    /**
     * 用于确定目标类的最终列表。可以在这里操作传入的目标类集，决定哪些类需要应用该 Mixin。
     *
     * @param myTargets    该插件应该处理的目标类名集合
     * @param otherTargets 其他插件目标类名集合
     */
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    /**
     * 返回需要应用的所有 Mixin 类的完整类名列表。该方法会被调用来加载所有 Mixin。
     *
     * @return 需要应用的 Mixin 类名列表
     */
    @Override
    public List<String> getMixins() {
        // 返回要应用的 Mixin 类名
        return Collections.emptyList();
    }

    /**
     * 在 Mixin 应用到目标类之前调用。可以在这里做一些预处理工作，例如修改目标类的字节码。
     *
     * @param s          目标类的名称
     * @param classNode  目标类的字节码节点
     * @param s1         Mixin 类的名称
     * @param iMixinInfo Mixin 信息
     */
    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }

    /**
     * 在 Mixin 应用到目标类之后调用。可以在这里进行后处理操作，例如输出日志或修改类的字节码。
     *
     * @param s          目标类的名称
     * @param classNode  目标类的字节码节点
     * @param s1         Mixin 类的名称
     * @param iMixinInfo Mixin 信息
     */
    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }
}
