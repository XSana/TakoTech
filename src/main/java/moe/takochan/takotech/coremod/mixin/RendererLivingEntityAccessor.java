package moe.takochan.takotech.coremod.mixin;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 RendererLivingEntity 的主模型字段。
 */
@Mixin(RendererLivingEntity.class)
public interface RendererLivingEntityAccessor {

    /**
     * 设置主渲染模型。
     */
    @Accessor("mainModel")
    void takotech$setMainModel(ModelBase model);
}
