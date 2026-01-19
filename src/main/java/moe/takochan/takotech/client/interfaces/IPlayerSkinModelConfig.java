package moe.takochan.takotech.client.interfaces;

/**
 * 允许渲染器切换瘦臂/普通臂配置。
 */
public interface IPlayerSkinModelConfig {

    /**
     * 更新为瘦臂（Alex）或普通臂（Steve）。
     *
     * @param smallArms true 表示瘦臂，false 表示普通臂
     */
    void takotech$setSmallArms(boolean smallArms);
}
