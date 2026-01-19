package moe.takochan.takotech.client.interfaces;

import net.minecraft.client.model.ModelRenderer;

/**
 * 暴露玩家模型的可选扩展（外层与瘦臂）。
 */
public interface IPlayerSkinModel {

    /**
     * @return 是否为瘦臂（Alex）模型
     */
    boolean takotech$isSmallArms();

    /**
     * @return 右臂外层模型，不存在则为 null
     */
    ModelRenderer takotech$getRightArmWear();
}
