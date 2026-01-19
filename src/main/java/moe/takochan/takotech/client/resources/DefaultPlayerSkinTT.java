package moe.takochan.takotech.client.resources;

import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 根据 UUID 判定默认皮肤类型。
 * 行为与 1.8+ 的 DefaultPlayerSkin 一致。
 */
@SideOnly(Side.CLIENT)
public class DefaultPlayerSkinTT {

    public static final ResourceLocation TEXTURE_STEVE = new ResourceLocation("takotech", "textures/entity/steve.png");
    public static final ResourceLocation TEXTURE_ALEX = new ResourceLocation("takotech", "textures/entity/alex.png");

    /**
     * 获取玩家默认皮肤（Alex/Steve）。
     *
     * @param playerUUID 玩家 UUID
     * @return 默认皮肤资源
     */
    public static ResourceLocation getDefaultSkin(UUID playerUUID) {
        return isSlimSkin(playerUUID) ? TEXTURE_ALEX : TEXTURE_STEVE;
    }

    /**
     * 根据 UUID 哈希判定皮肤类型（回退方案）。
     * 与 1.8+ 的算法一致。
     *
     * @param playerUUID 玩家 UUID
     * @return "slim" 表示 Alex，"default" 表示 Steve
     */
    public static String getSkinType(UUID playerUUID) {
        return isSlimSkin(playerUUID) ? "slim" : "default";
    }

    /**
     * UUID 哈希：奇数为 slim，偶数为 default。
     *
     * @param playerUUID 玩家 UUID
     * @return 是否使用 slim 皮肤
     */
    private static boolean isSlimSkin(UUID playerUUID) {
        return (playerUUID.hashCode() & 1) == 1;
    }
}
