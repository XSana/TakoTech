package moe.takochan.takotech.client.interfaces;

import net.minecraft.client.entity.AbstractClientPlayer;

/**
 * 允许自定义玩家渲染器根据皮肤类型切换模型。
 */
public interface IPlayerRendererSkinSelector {

    /**
     * 根据玩家皮肤类型选择正确的模型。
     *
     * @param player 玩家实例
     */
    void takotech$selectModel(AbstractClientPlayer player);
}
