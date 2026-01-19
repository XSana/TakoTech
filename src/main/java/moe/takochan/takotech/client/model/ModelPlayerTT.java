package moe.takochan.takotech.client.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import moe.takochan.takotech.client.interfaces.IPlayerSkinModel;

/**
 * 支持双层皮肤与瘦臂的玩家模型。
 * 将 1.8+ 的玩家模型特性移植到 1.7.10。
 */
@SideOnly(Side.CLIENT)
public class ModelPlayerTT extends ModelBiped implements IPlayerSkinModel {

    // 外层模型（身体各部位的第二层）
    public ModelRenderer bipedLeftArmwear;
    public ModelRenderer bipedRightArmwear;
    public ModelRenderer bipedLeftLegwear;
    public ModelRenderer bipedRightLegwear;
    public ModelRenderer bipedBodyWear;

    private final boolean smallArms;

    /**
     * 创建玩家模型，可选瘦臂模式。
     *
     * @param modelSize   模型膨胀大小
     * @param smallArmsIn true 表示 Alex（3px 臂），false 表示 Steve（4px 臂）
     */
    public ModelPlayerTT(float modelSize, boolean smallArmsIn) {
        // 使用 64x64 贴图尺寸
        super(modelSize, 0.0F, 64, 64);
        this.smallArms = smallArmsIn;

        // 根据是否瘦臂创建手臂模型
        if (smallArmsIn) {
            // Alex 模型：3 像素手臂
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);

            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);

            // 瘦臂外层
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);

            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 0.0F);
        } else {
            // Steve 模型：4 像素手臂
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

            // 普通臂外层
            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);

            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 0.0F);
        }

        // 腿部模型（瘦臂与普通臂一致），覆盖父类贴图坐标
        this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);

        this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);

        this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
        this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);

        // 身体外层
        this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
        this.bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    }

    /**
     * 渲染模型并叠加外层。
     */
    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scale) {
        // 渲染基础模型（头、身、手臂、腿、头盔）
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        // 保存 GL 状态
        GL11.glPushMatrix();
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        if (!blendEnabled) {
            // 允许外层透明像素（1.8+ 行为）
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        if (this.isChild) {
            // 子模型缩放
            GL11.glScalef(0.5F, 0.5F, 0.5F);
            GL11.glTranslatef(0.0F, 24.0F * scale, 0.0F);
        }

        // 渲染外层
        this.bipedLeftLegwear.render(scale);
        this.bipedRightLegwear.render(scale);
        this.bipedLeftArmwear.render(scale);
        this.bipedRightArmwear.render(scale);
        this.bipedBodyWear.render(scale);

        if (!blendEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        // 恢复 GL 状态
        GL11.glPopMatrix();
    }

    /**
     * 设置模型各部位角度（包含外层）。
     */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
        float headPitch, float scaleFactor, Entity entityIn) {
        // 先由父类设置基础角度
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);

        // 同步外层角度
        copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
        copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
        copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
        copyModelAngles(this.bipedBody, this.bipedBodyWear);
    }

    /**
     * 设置各部位可见性（包含外层）。
     */
    public void setVisible(boolean visible) {
        // 基础部位可见性
        this.bipedHead.showModel = visible;
        this.bipedHeadwear.showModel = visible;
        this.bipedBody.showModel = visible;
        this.bipedRightArm.showModel = visible;
        this.bipedLeftArm.showModel = visible;
        this.bipedRightLeg.showModel = visible;
        this.bipedLeftLeg.showModel = visible;

        // 外层可见性
        this.bipedLeftArmwear.showModel = visible;
        this.bipedRightArmwear.showModel = visible;
        this.bipedLeftLegwear.showModel = visible;
        this.bipedRightLegwear.showModel = visible;
        this.bipedBodyWear.showModel = visible;
    }

    /**
     * 复制旋转角度与位移。
     */
    private void copyModelAngles(ModelRenderer source, ModelRenderer dest) {
        dest.rotateAngleX = source.rotateAngleX;
        dest.rotateAngleY = source.rotateAngleY;
        dest.rotateAngleZ = source.rotateAngleZ;
        dest.rotationPointX = source.rotationPointX;
        dest.rotationPointY = source.rotationPointY;
        dest.rotationPointZ = source.rotationPointZ;
    }

    /**
     * 是否为瘦臂模型。
     */
    public boolean isSmallArms() {
        return this.smallArms;
    }

    /**
     * 是否为瘦臂模型。
     */
    @Override
    public boolean takotech$isSmallArms() {
        return this.smallArms;
    }

    /**
     * 右臂外层模型（第一人称使用）。
     */
    @Override
    public ModelRenderer takotech$getRightArmWear() {
        return this.bipedRightArmwear;
    }
}
