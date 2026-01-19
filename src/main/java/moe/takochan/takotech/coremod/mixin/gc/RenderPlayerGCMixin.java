package moe.takochan.takotech.coremod.mixin.gc;

import micdoodle8.mods.galacticraft.core.client.model.ModelPlayerGC;
import micdoodle8.mods.galacticraft.core.client.render.entities.RenderPlayerGC;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import moe.takochan.takotech.client.interfaces.IPlayerRendererSkinSelector;
import moe.takochan.takotech.client.interfaces.IPlayerSkinModelConfig;
import moe.takochan.takotech.client.interfaces.ISkinTypeProvider;

/**
 * 让 Galacticraft 的 RenderPlayerGC 支持瘦臂与皮肤类型切换。
 */
@Mixin(value = RenderPlayerGC.class, remap = false)
public abstract class RenderPlayerGCMixin extends RenderPlayer implements IPlayerRendererSkinSelector {

    @Unique
    private ModelBiped takotech$modelSteve;
    @Unique
    private ModelBiped takotech$modelAlex;

    /**
     * 构造后创建 Alex/Steve 两套 GC 模型。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void takotech$onInit(CallbackInfo ci) {
        if (this.mainModel instanceof ModelBiped) {
            this.takotech$modelSteve = (ModelBiped) this.mainModel;
            takotech$configureModel(this.takotech$modelSteve, false);
        }

        this.takotech$modelAlex = new ModelPlayerGC(0.0F);
        takotech$configureModel(this.takotech$modelAlex, true);
    }

    /**
     * 根据皮肤类型切换当前使用的模型。
     */
    @Override
    public void takotech$selectModel(AbstractClientPlayer player) {
        if (this.takotech$modelSteve == null) {
            return;
        }

        String skinType = ((ISkinTypeProvider) player).takotech$getSkinType();
        ModelBiped target = "slim".equals(skinType) && this.takotech$modelAlex != null
            ? this.takotech$modelAlex
            : this.takotech$modelSteve;

        this.mainModel = target;
        this.modelBipedMain = target;
    }

    @Unique
    private void takotech$configureModel(ModelBiped model, boolean smallArms) {
        if (model instanceof IPlayerSkinModelConfig) {
            ((IPlayerSkinModelConfig) model).takotech$setSmallArms(smallArms);
        }
    }
}
