package moe.takochan.takotech.coremod.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import moe.takochan.takotech.client.interfaces.ISkinTypeProvider;
import moe.takochan.takotech.client.resources.DefaultPlayerSkinTT;

/**
 * 为 AbstractClientPlayer 增加皮肤类型的存取。
 * 用于记录玩家是 slim（Alex）还是 default（Steve）。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements ISkinTypeProvider {

    @Shadow
    public static ResourceLocation locationStevePng;

    /**
     * 保存玩家皮肤类型。
     * "slim" 表示 Alex，"default" 表示 Steve。
     */
    @Unique
    private String takotech$skinType = null;

    /**
     * 获取皮肤类型。
     * 优先级：1) 服务器元数据，2) UUID 哈希回退。
     *
     * @return "slim" 表示 Alex，"default" 表示 Steve
     */
    @Unique
    public String takotech$getSkinType() {
        // 优先使用服务器元数据
        if (this.takotech$skinType != null) {
            return this.takotech$skinType;
        }

        // 回退方案：用 UUID 哈希判定
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        return DefaultPlayerSkinTT.getSkinType(self.getUniqueID());
    }

    /**
     * 设置皮肤类型。
     * 由 SkinManagerMixin 在加载皮肤元数据时调用。
     *
     * @param skinType "slim" 表示 Alex，"default" 表示 Steve
     */
    @Unique
    public void takotech$setSkinType(String skinType) {
        this.takotech$skinType = skinType;
    }

    /**
     * 修正默认皮肤资源为 Alex/Steve 对应纹理。
     */
    @Inject(method = "getLocationSkin", at = @At("RETURN"), cancellable = true)
    private void takotech$fixDefaultSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (cir.getReturnValue() == locationStevePng) {
            String skinType = this.takotech$getSkinType();
            cir.setReturnValue(
                "slim".equals(skinType) ? DefaultPlayerSkinTT.TEXTURE_ALEX : DefaultPlayerSkinTT.TEXTURE_STEVE);
        }
    }
}
