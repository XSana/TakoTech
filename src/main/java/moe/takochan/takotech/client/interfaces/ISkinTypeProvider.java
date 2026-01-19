package moe.takochan.takotech.client.interfaces;

/**
 * 玩家皮肤类型的读写接口。
 * 由 AbstractClientPlayerMixin 实现。
 */
public interface ISkinTypeProvider {

    /**
     * 获取皮肤类型。
     *
     * @return "slim" 表示 Alex，"default" 表示 Steve
     */
    String takotech$getSkinType();

    /**
     * 设置皮肤类型。
     *
     * @param skinType "slim" 表示 Alex，"default" 表示 Steve
     */
    void takotech$setSkinType(String skinType);
}
