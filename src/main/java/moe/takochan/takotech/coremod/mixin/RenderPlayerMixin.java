package moe.takochan.takotech.coremod.mixin;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import moe.takochan.takotech.client.interfaces.IPlayerRendererSkinSelector;
import moe.takochan.takotech.client.interfaces.IPlayerSkinModel;
import moe.takochan.takotech.client.interfaces.ISkinTypeProvider;
import moe.takochan.takotech.client.model.ModelPlayerTT;

/**
 * 替换 RenderPlayer 的默认模型，支持双层皮肤与瘦臂。
 */
@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMixin {

    @Shadow
    public ModelBiped modelBipedMain;

    @Unique
    private ModelPlayerTT takotech$modelSteve;

    @Unique
    private ModelPlayerTT takotech$modelAlex;

    @Unique
    private boolean takotech$armOffsetApplied;

    @Unique
    private float takotech$armRotationPointX;

    /**
     * 构造后创建 Steve/Alex 模型。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructor(CallbackInfo ci) {
        // 创建 Steve（普通臂）与 Alex（瘦臂）模型
        this.takotech$modelSteve = new ModelPlayerTT(0.0F, false); // Steve：4px 臂
        this.takotech$modelAlex = new ModelPlayerTT(0.0F, true); // Alex：3px 臂

        // 默认使用 Steve
        ((RendererLivingEntityAccessor) this).takotech$setMainModel(this.takotech$modelSteve);
        this.modelBipedMain = this.takotech$modelSteve;
    }

    /**
     * 渲染前按皮肤类型选择模型。
     */
    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
    private void beforeDoRender(AbstractClientPlayer player, double x, double y, double z, float entityYaw,
        float partialTicks, CallbackInfo ci) {
        this.takotech$applySkinType(player);
    }

    /**
     * 第一人称手臂渲染，补外层并支持瘦臂。
     */
    @Inject(
        method = "renderFirstPersonArm(Lnet/minecraft/entity/player/EntityPlayer;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void renderFirstPersonArmTT(EntityPlayer player, CallbackInfo ci) {
        if (player instanceof AbstractClientPlayer) {
            this.takotech$applySkinType((AbstractClientPlayer) player);
        }

        if (!(this.modelBipedMain instanceof IPlayerSkinModel)) {
            return;
        }

        IPlayerSkinModel skinModel = (IPlayerSkinModel) this.modelBipedMain;
        ModelBiped model = this.modelBipedMain;
        if (skinModel.takotech$getRightArmWear() == null) {
            return;
        }

        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        model.onGround = 0.0F;
        model.isSneak = false;
        model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
        model.bipedRightArm.rotateAngleX = 0.0F;
        model.bipedRightArm.render(0.0625F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        skinModel.takotech$getRightArmWear().rotateAngleX = 0.0F;
        skinModel.takotech$getRightArmWear()
            .render(0.0625F);
        GL11.glDisable(GL11.GL_BLEND);
        ci.cancel();
    }

    /**
     * 渲染手持物前，为瘦臂补右臂偏移。
     */
    @Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At("HEAD"))
    private void beforeRenderEquippedItems(AbstractClientPlayer player, float partialTicks, CallbackInfo ci) {
        if (!(this.modelBipedMain instanceof IPlayerSkinModel)) {
            this.takotech$armOffsetApplied = false;
            return;
        }

        IPlayerSkinModel skinModel = (IPlayerSkinModel) this.modelBipedMain;
        if (!skinModel.takotech$isSmallArms()) {
            this.takotech$armOffsetApplied = false;
            return;
        }

        this.takotech$armOffsetApplied = true;
        this.takotech$armRotationPointX = this.modelBipedMain.bipedRightArm.rotationPointX;
        this.modelBipedMain.bipedRightArm.rotationPointX = this.takotech$armRotationPointX + 0.5F;
    }

    /**
     * 渲染手持物后，恢复右臂偏移。
     */
    @Inject(method = "renderEquippedItems(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V", at = @At("RETURN"))
    private void afterRenderEquippedItems(AbstractClientPlayer player, float partialTicks, CallbackInfo ci) {
        if (!this.takotech$armOffsetApplied) {
            return;
        }

        this.takotech$armOffsetApplied = false;

        this.modelBipedMain.bipedRightArm.rotationPointX = this.takotech$armRotationPointX;
    }

    /**
     * 根据皮肤类型切换 Steve/Alex 模型。
     */
    @Unique
    private void takotech$applySkinType(AbstractClientPlayer player) {
        if (this instanceof IPlayerRendererSkinSelector) {
            ((IPlayerRendererSkinSelector) this).takotech$selectModel(player);
            return;
        }

        String skinType = ((ISkinTypeProvider) player).takotech$getSkinType();

        if ("slim".equals(skinType)) {
            ((RendererLivingEntityAccessor) this).takotech$setMainModel(this.takotech$modelAlex);
            this.modelBipedMain = this.takotech$modelAlex;
        } else {
            ((RendererLivingEntityAccessor) this).takotech$setMainModel(this.takotech$modelSteve);
            this.modelBipedMain = this.takotech$modelSteve;
        }
    }
}
