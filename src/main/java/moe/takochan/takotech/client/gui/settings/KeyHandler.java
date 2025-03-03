package moe.takochan.takotech.client.gui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import moe.takochan.takotech.client.gui.GuiType;
import moe.takochan.takotech.common.item.ItemToolboxPlus;

public class KeyHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (GameSettings.selectTool.isPressed()) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            World world = Minecraft.getMinecraft().theWorld;
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemToolboxPlus) {
                NBTTagCompound nbt = heldItem.getTagCompound();
                if (nbt != null && nbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
                    NBTTagList list = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
                    if (list.tagCount() <= 0) {
                        return;
                    }
                    GuiType.openGuiWithClient(
                        GuiType.TOOLBOX_PLUS_SELECT,
                        player,
                        world,
                        ForgeDirection.UNKNOWN,
                        (int) player.posX,
                        (int) player.posY,
                        (int) player.posZ);
                }
            }
        }
    }
}
