package moe.takochan.takotech.coremod.mixin.gc;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import micdoodle8.mods.galacticraft.core.client.model.ModelPlayerGC;
import moe.takochan.takotech.client.interfaces.IPlayerSkinModel;
import moe.takochan.takotech.client.interfaces.IPlayerSkinModelConfig;

/**
 * 让 GC 的玩家模型支持 64x64、双层皮肤与瘦臂。
 */
@Mixin(value = ModelPlayerGC.class, remap = false)
public abstract class ModelPlayerGCMixin extends ModelBiped implements IPlayerSkinModel, IPlayerSkinModelConfig {

    @Unique
    private ModelRenderer takotech$leftArmWear;
    @Unique
    private ModelRenderer takotech$rightArmWear;
    @Unique
    private ModelRenderer takotech$leftLegWear;
    @Unique
    private ModelRenderer takotech$rightLegWear;
    @Unique
    private ModelRenderer takotech$bodyWear;
    @Unique
    private boolean takotech$smallArms;
    @Unique
    private float takotech$modelSize;
    @Unique
    private boolean takotech$initialized;

    /**
     * 初始化时重建基础模型与外层。
     */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void takotech$onInit(float modelSize, CallbackInfo ci) {
        this.takotech$modelSize = modelSize;
        if (modelSize == 0.0F) {
            this.takotech$setupModel(false);
        }
    }

    /**
     * 同步外层与基础模型的旋转与位移。
     */
    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void takotech$afterSetRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float yaw,
        float pitch, float scaleFactor, Entity entity, CallbackInfo ci) {
        if (this.takotech$rightArmWear == null) {
            return;
        }

        takotech$copyModelAngles(this.bipedLeftArm, this.takotech$leftArmWear);
        takotech$copyModelAngles(this.bipedRightArm, this.takotech$rightArmWear);
        takotech$copyModelAngles(this.bipedLeftLeg, this.takotech$leftLegWear);
        takotech$copyModelAngles(this.bipedRightLeg, this.takotech$rightLegWear);
        takotech$copyModelAngles(this.bipedBody, this.takotech$bodyWear);
    }

    /**
     * 在 GC 主模型上渲染外层皮肤。
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void takotech$renderWearLayers(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
        float yaw, float pitch, float scale, CallbackInfo ci) {
        if (this.takotech$modelSize != 0.0F || this.takotech$rightArmWear == null) {
            return;
        }

        if (!(entity instanceof AbstractClientPlayer)) {
            return;
        }

        Render render = RenderManager.instance.getEntityClassRenderObject(EntityClientPlayerMP.class);
        if (!(render instanceof RenderPlayer)) {
            return;
        }

        ModelBiped modelBipedMain = ((RenderPlayer) render).modelBipedMain;
        if (!this.equals(modelBipedMain)) {
            return;
        }

        GL11.glPushMatrix();
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        if (!blendEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        if (this.isChild) {
            GL11.glScalef(0.5F, 0.5F, 0.5F);
            GL11.glTranslatef(0.0F, 24.0F * scale, 0.0F);
        }

        this.takotech$leftLegWear.render(scale);
        this.takotech$rightLegWear.render(scale);
        this.takotech$leftArmWear.render(scale);
        this.takotech$rightArmWear.render(scale);
        this.takotech$bodyWear.render(scale);

        if (!blendEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        GL11.glPopMatrix();
    }

    @Override
    public boolean takotech$isSmallArms() {
        return this.takotech$smallArms;
    }

    @Override
    public ModelRenderer takotech$getRightArmWear() {
        return this.takotech$rightArmWear;
    }

    /**
     * 切换瘦臂或普通臂配置。
     */
    @Override
    public void takotech$setSmallArms(boolean smallArms) {
        if (this.takotech$modelSize != 0.0F) {
            return;
        }

        if (this.takotech$initialized && this.takotech$smallArms == smallArms) {
            return;
        }

        this.takotech$setupModel(smallArms);
    }

    /**
     * 按当前瘦臂设置重建基础模型与外层。
     */
    @Unique
    private void takotech$setupModel(boolean smallArms) {
        this.takotech$smallArms = smallArms;
        this.takotech$initialized = true;

        float modelSize = this.takotech$modelSize;
        ModelBase modelBase = this;

        this.textureWidth = 64;
        this.textureHeight = 64;

        takotech$rebuildHead(modelSize);
        takotech$rebuildHeadwear(modelSize);
        takotech$rebuildBody(modelSize);
        takotech$rebuildRightArm(modelSize, smallArms);
        takotech$rebuildLeftArm(modelSize, smallArms);
        takotech$rebuildRightLeg(modelSize);
        takotech$rebuildLeftLeg(modelSize);

        this.takotech$leftArmWear = new ModelRenderer(modelBase, 48, 48);
        this.takotech$leftArmWear.setTextureSize(64, 64);
        this.takotech$leftArmWear.addBox(-1.0F, -2.0F, -2.0F, smallArms ? 3 : 4, 12, 4, modelSize + 0.25F);
        this.takotech$leftArmWear.setRotationPoint(5.0F, smallArms ? 2.5F : 2.0F, 0.0F);

        this.takotech$rightArmWear = new ModelRenderer(modelBase, 40, 32);
        this.takotech$rightArmWear.setTextureSize(64, 64);
        this.takotech$rightArmWear
            .addBox(smallArms ? -2.0F : -3.0F, -2.0F, -2.0F, smallArms ? 3 : 4, 12, 4, modelSize + 0.25F);
        this.takotech$rightArmWear.setRotationPoint(-5.0F, smallArms ? 2.5F : 2.0F, 0.0F);

        this.takotech$leftLegWear = new ModelRenderer(modelBase, 0, 48);
        this.takotech$leftLegWear.setTextureSize(64, 64);
        this.takotech$leftLegWear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.takotech$leftLegWear.setRotationPoint(1.9F, 12.0F, 0.0F);

        this.takotech$rightLegWear = new ModelRenderer(modelBase, 0, 32);
        this.takotech$rightLegWear.setTextureSize(64, 64);
        this.takotech$rightLegWear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.takotech$rightLegWear.setRotationPoint(-1.9F, 12.0F, 0.0F);

        this.takotech$bodyWear = new ModelRenderer(modelBase, 16, 32);
        this.takotech$bodyWear.setTextureSize(64, 64);
        this.takotech$bodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
        this.takotech$bodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    /**
     * 同步旋转与位移。
     */
    @Unique
    private void takotech$copyModelAngles(ModelRenderer source, ModelRenderer dest) {
        dest.rotateAngleX = source.rotateAngleX;
        dest.rotateAngleY = source.rotateAngleY;
        dest.rotateAngleZ = source.rotateAngleZ;
        dest.rotationPointX = source.rotationPointX;
        dest.rotationPointY = source.rotationPointY;
        dest.rotationPointZ = source.rotationPointZ;
    }

    @Unique
    private void takotech$rebuildHead(float modelSize) {
        if (this.bipedHead == null) {
            return;
        }

        this.bipedHead.cubeList.clear();
        this.bipedHead.setTextureSize(64, 64);
        this.bipedHead.setTextureOffset(0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize);
        this.bipedHead.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    @Unique
    private void takotech$rebuildHeadwear(float modelSize) {
        if (this.bipedHeadwear == null) {
            return;
        }

        this.bipedHeadwear.cubeList.clear();
        this.bipedHeadwear.setTextureSize(64, 64);
        this.bipedHeadwear.setTextureOffset(32, 0);
        this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, modelSize + 0.5F);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    @Unique
    private void takotech$rebuildBody(float modelSize) {
        if (this.bipedBody == null) {
            return;
        }

        this.bipedBody.cubeList.clear();
        this.bipedBody.setTextureSize(64, 64);
        this.bipedBody.setTextureOffset(16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize);
        this.bipedBody.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    @Unique
    private void takotech$rebuildRightArm(float modelSize, boolean smallArms) {
        if (this.bipedRightArm == null) {
            return;
        }

        this.bipedRightArm.mirror = false;
        this.bipedRightArm.cubeList.clear();
        this.bipedRightArm.setTextureSize(64, 64);
        this.bipedRightArm.setTextureOffset(40, 16);
        if (smallArms) {
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);
        } else {
            this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        }
    }

    @Unique
    private void takotech$rebuildLeftArm(float modelSize, boolean smallArms) {
        if (this.bipedLeftArm == null) {
            return;
        }

        this.bipedLeftArm.mirror = false;
        this.bipedLeftArm.cubeList.clear();
        this.bipedLeftArm.setTextureSize(64, 64);
        this.bipedLeftArm.setTextureOffset(32, 48);
        if (smallArms) {
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
        } else {
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        }
    }

    @Unique
    private void takotech$rebuildRightLeg(float modelSize) {
        if (this.bipedRightLeg == null) {
            return;
        }

        this.bipedRightLeg.mirror = false;
        this.bipedRightLeg.cubeList.clear();
        this.bipedRightLeg.setTextureSize(64, 64);
        this.bipedRightLeg.setTextureOffset(0, 16);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
    }

    @Unique
    private void takotech$rebuildLeftLeg(float modelSize) {
        if (this.bipedLeftLeg == null) {
            return;
        }

        this.bipedLeftLeg.mirror = false;
        this.bipedLeftLeg.cubeList.clear();
        this.bipedLeftLeg.setTextureSize(64, 64);
        this.bipedLeftLeg.setTextureOffset(16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
    }
}
