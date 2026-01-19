package moe.takochan.takotech.coremod.mixin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import net.minecraft.client.renderer.ImageBufferDownload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import moe.takochan.takotech.TakoTechMod;

/**
 * 将旧版 64x32 皮肤转换为 64x64，并修正透明像素。
 * 用于 1.7.10 兼容 1.8+ 皮肤格式。
 */
@Mixin(ImageBufferDownload.class)
public class ImageBufferDownloadMixin {

    /**
     * 注入 parseUserSkin，将 64x32 转换为 64x64。
     * 在皮肤下载并处理时触发。
     *
     * @param image 下载到的皮肤图像
     * @param cir   回调信息
     */
    @Inject(method = "parseUserSkin", at = @At("HEAD"), cancellable = true)
    private void convertSkinFormat(BufferedImage image, CallbackInfoReturnable<BufferedImage> cir) {
        if (image == null) {
            cir.setReturnValue(null);
            return;
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        if (imageWidth != 64 || (imageHeight != 32 && imageHeight != 64)) {
            TakoTechMod.LOG.warn("Invalid skin texture size: {}x{}, expected 64x32 or 64x64", imageWidth, imageHeight);
            cir.setReturnValue(image);
            return;
        }

        boolean isLegacyFormat = (imageHeight == 32);
        BufferedImage bufferedImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = bufferedImage.getGraphics();
        graphics.drawImage(image, 0, 0, null);

        if (isLegacyFormat) {
            graphics.setColor(new Color(0, 0, 0, 0));
            graphics.fillRect(0, 32, 64, 32);
            graphics.drawImage(bufferedImage, 24, 48, 20, 52, 4, 16, 8, 20, null);
            graphics.drawImage(bufferedImage, 28, 48, 24, 52, 8, 16, 12, 20, null);
            graphics.drawImage(bufferedImage, 20, 52, 16, 64, 8, 20, 12, 32, null);
            graphics.drawImage(bufferedImage, 24, 52, 20, 64, 4, 20, 8, 32, null);
            graphics.drawImage(bufferedImage, 28, 52, 24, 64, 0, 20, 4, 32, null);
            graphics.drawImage(bufferedImage, 32, 52, 28, 64, 12, 20, 16, 32, null);
            graphics.drawImage(bufferedImage, 40, 48, 36, 52, 44, 16, 48, 20, null);
            graphics.drawImage(bufferedImage, 44, 48, 40, 52, 48, 16, 52, 20, null);
            graphics.drawImage(bufferedImage, 36, 52, 32, 64, 48, 20, 52, 32, null);
            graphics.drawImage(bufferedImage, 40, 52, 36, 64, 44, 20, 48, 32, null);
            graphics.drawImage(bufferedImage, 44, 52, 40, 64, 40, 20, 44, 32, null);
            graphics.drawImage(bufferedImage, 48, 52, 44, 64, 52, 20, 56, 32, null);
        }

        graphics.dispose();

        int[] imageData = ((DataBufferInt) bufferedImage.getRaster()
            .getDataBuffer()).getData();
        setAreaOpaque(imageData, 64, 0, 0, 32, 16);

        if (isLegacyFormat) {
            setAreaTransparent(imageData, 64, 32, 0, 64, 32);
        }

        setAreaOpaque(imageData, 64, 0, 16, 64, 32);
        setAreaOpaque(imageData, 64, 16, 48, 48, 64);
        cir.setReturnValue(bufferedImage);
    }

    /**
     * 将指定区域设置为透明（若满足透明条件）。
     */
    private void setAreaTransparent(int[] imageData, int imageWidth, int x, int y, int width, int height) {
        for (int i = x; i < width; ++i) {
            for (int j = y; j < height; ++j) {
                int index = i + j * imageWidth;

                if ((imageData[index] >> 24 & 255) < 128) {
                    return;
                }
            }
        }

        for (int i = x; i < width; ++i) {
            for (int j = y; j < height; ++j) {
                int index = i + j * imageWidth;
                imageData[index] &= 16777215;
            }
        }
    }

    /**
     * 将指定区域设置为不透明。
     */
    private void setAreaOpaque(int[] imageData, int imageWidth, int x, int y, int width, int height) {
        for (int i = x; i < width; ++i) {
            for (int j = y; j < height; ++j) {
                int index = i + j * imageWidth;
                imageData[index] |= -16777216;
            }
        }
    }
}
