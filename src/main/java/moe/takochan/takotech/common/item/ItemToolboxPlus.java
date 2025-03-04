package moe.takochan.takotech.common.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.Constants;

import appeng.util.Platform;
import com.gtnewhorizons.modularui.api.GlStateManager;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.items.MetaGeneratedTool;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.tool.HandHeldToolbox;
import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.client.render.ToolboxPlusRenderer;
import moe.takochan.takotech.client.tabs.TakoTechTabs;
import moe.takochan.takotech.constants.NBTConstants;
import moe.takochan.takotech.constants.NameConstants;
import moe.takochan.takotech.utils.CommonUtils;
import org.lwjgl.opengl.GL11;

public class ItemToolboxPlus extends BaseItem implements IHandHeldInventory {

    @SideOnly(Side.CLIENT)
    private static final RenderItem renderItem = RenderItem.getInstance();

    @SideOnly(Side.CLIENT)
    private IIcon baseIcon;

    public ItemToolboxPlus() {
        super(NameConstants.ITEM_TOOLBOX_PLUS);
        this.setMaxStackSize(1);
        this.setTextureName(CommonUtils.resource(NameConstants.ITEM_TOOLBOX_PLUS));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        this.baseIcon = register.registerIcon(this.getIconString());
    }

    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer) {
        if (IC2.platform.isSimulating()) {
            IC2.platform.launchGui(entityPlayer, this.getInventory(entityPlayer, itemStack));
        }

        return itemStack;
    }

    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    public void register() {
        GameRegistry.registerItem(this, NameConstants.ITEM_TOOLBOX_PLUS);
        setCreativeTab(TakoTechTabs.getInstance());
        // 注册动态渲染器
        MinecraftForgeClient.registerItemRenderer(this, new ToolboxPlusRenderer());
    }

    public IHasGui getInventory(EntityPlayer entityPlayer, ItemStack itemStack) {
        return new HandHeldToolbox(entityPlayer, itemStack, 9);
    }

    // 渲染物品栏图标
    @SideOnly(Side.CLIENT)
    private void renderInventory(ItemStack stack, ItemStack selectedStack, int selectedIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        // 绑定物品贴图
        mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // 重置颜色为纯白
        if (selectedStack == null) {
            // 如果没有选中物品，则直接绘制默认图标
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0, 16, 0, baseIcon.getMinU(), baseIcon.getMaxV());
            tessellator.addVertexWithUV(16, 16, 0, baseIcon.getMaxU(), baseIcon.getMaxV());
            tessellator.addVertexWithUV(16, 0, 0, baseIcon.getMaxU(), baseIcon.getMinV());
            tessellator.addVertexWithUV(0, 0, 0, baseIcon.getMinU(), baseIcon.getMinV());
            tessellator.draw();
        } else {
            // 先渲染选中物品的图标
            renderItem.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), selectedStack, 0, 0);
            // 再在左下角绘制缩小后的默认图标
            GlStateManager.pushMatrix();
            float scale = 0.5F;
            GlStateManager.scale(scale, scale, scale);
            // 注意：这里坐标需要根据实际情况进行微调，本例简单使用(0,0)作为左下角起点
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(0, 16, 0, baseIcon.getMinU(), baseIcon.getMaxV());
            tessellator.addVertexWithUV(16, 16, 0, baseIcon.getMaxU(), baseIcon.getMaxV());
            tessellator.addVertexWithUV(16, 0, 0, baseIcon.getMaxU(), baseIcon.getMinV());
            tessellator.addVertexWithUV(0, 0, 0, baseIcon.getMinU(), baseIcon.getMinV());
            tessellator.draw();
            GlStateManager.popMatrix();
        }
    }

    @SideOnly(Side.CLIENT)
    private void renderHandheld(ItemStack stack, ItemStack selectedStack, int selectedIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (selectedStack == null) {
            // 如果没有选中物品，则使用MC默认的手持物品渲染逻辑
            renderItem.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, 0, 0);
        } else {
            // 如果有选中物品，则仅渲染选中物品（不显示缩小的默认图标）
            renderItem.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), selectedStack, 0, 0);
        }
    }

    public int getSelectedIndex(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt.hasKey(NBTConstants.SELECTED_INDEX)) {
                return nbt.getInteger(NBTConstants.SELECTED_INDEX);
            }
        }
        return -1;
    }

    public ItemStack getSelectedItemStack(ItemStack toolboxStack, int selectedIndex) {
        if (selectedIndex >= 0) {
            List<ItemStack> tools = getToolItems(toolboxStack);
            if (selectedIndex < tools.size()) {
                return tools.get(selectedIndex);
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }


    public static List<ItemStack> getToolItems(ItemStack stack) {
        List<ItemStack> list = new ArrayList<>();
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            NBTTagList itemsTagList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < itemsTagList.tagCount(); i++) {
                NBTTagCompound itemTag = itemsTagList.getCompoundTagAt(i);
                ItemStack itemStack = ItemStack.loadItemStackFromNBT(itemTag);
                if (itemStack != null && itemStack.getItem() instanceof MetaGeneratedTool) {
                    list.add(itemStack);
                }
            }
        }
        return list;
    }

    public static void processSelection(EntityPlayer player, ItemStack selectedStack) {
        if (Platform.isClient()) return;
        if (selectedStack == null) return;
        ItemStack stack = player.getHeldItem();
        if (stack.getItem() instanceof ItemToolboxPlus) {
            List<ItemStack> toolItems = getToolItems(stack);

            int index = -1;
            if (!(selectedStack.getItem() instanceof ItemToolboxPlus)) {
                for (int i = 0; i < toolItems.size(); i++) {
                    if (toolItems.get(i).isItemEqual(selectedStack)) {
                        index = i;
                        break;
                    }
                }
            }
            // 执行后续物品切换逻辑
            updateToolboxContents(stack, player, index);
        }
    }

    private static void updateToolboxContents(ItemStack stack, EntityPlayer player, int index) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        // 根据索引更新手持物品逻辑
        TakoTechMod.LOG.info("selected toolbox: {}", index);
        if (index == -1) {
            nbt.removeTag(NBTConstants.SELECTED_INDEX);
        } else {
            nbt.setInteger(NBTConstants.SELECTED_INDEX, index);
        }
        player.inventory.markDirty();

        // 同步容器数据给客户端
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

}
