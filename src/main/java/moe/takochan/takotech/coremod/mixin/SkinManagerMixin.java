package moe.takochan.takotech.coremod.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.interfaces.ISkinTypeProvider;

/**
 * 拦截皮肤加载并提取皮肤类型元数据。
 * 用于识别玩家是 slim（Alex）还是 default（Steve）。
 */
@Mixin(SkinManager.class)
public class SkinManagerMixin {

    /**
     * 注入皮肤加载流程，提取并保存皮肤类型。
     * 当皮肤从服务器加载时触发。
     *
     * @param profileTexture 包含元数据的皮肤纹理
     * @param type           纹理类型（SKIN 或 CAPE）
     * @param callback       皮肤可用回调（通常是 AbstractClientPlayer）
     * @param cir            回调信息
     */
    @Inject(method = "func_152789_a", at = @At("HEAD"))
    private void onSkinLoad(MinecraftProfileTexture profileTexture, MinecraftProfileTexture.Type type,
        SkinManager.SkinAvailableCallback callback, CallbackInfoReturnable<ResourceLocation> cir) {
        // 只处理 SKIN，不处理 CAPE
        if (type != MinecraftProfileTexture.Type.SKIN) {
            return;
        }

        // 仅处理 AbstractClientPlayer
        if (!(callback instanceof AbstractClientPlayer)) {
            return;
        }

        try {
            // 从元数据读取皮肤类型
            String skinType = profileTexture.getMetadata("model");

            // 元数据为 slim 则使用 Alex，否则使用 Steve
            if (skinType != null && skinType.equals("slim")) {
                ((ISkinTypeProvider) callback).takotech$setSkinType("slim");
                TakoTechMod.LOG.info(
                    "Player {} uses slim skin (Alex model)",
                    ((AbstractClientPlayer) callback).getCommandSenderName());
            } else {
                ((ISkinTypeProvider) callback).takotech$setSkinType("default");
                TakoTechMod.LOG.info(
                    "Player {} uses default skin (Steve model)",
                    ((AbstractClientPlayer) callback).getCommandSenderName());
            }
        } catch (Exception e) {
            // 出错时记录并继续（回退到 UUID 判定）
            TakoTechMod.LOG.warn("Failed to extract skin type metadata for player: " + e.getMessage());
        }
    }
}
