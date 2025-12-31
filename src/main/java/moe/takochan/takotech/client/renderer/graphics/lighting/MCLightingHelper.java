package moe.takochan.takotech.client.renderer.graphics.lighting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Utility class for integrating Minecraft's lightmap lighting system
 * with custom shaders.
 *
 * <p>
 * MC uses a 16x16 lightmap texture where:
 * </p>
 * <ul>
 * <li>U axis (0-15): Block light level (torches, glowstone, etc.)</li>
 * <li>V axis (0-15): Sky light level (affected by time of day)</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Get light coordinates at a world position
 *     float[] lightUV = MCLightingHelper.getLightmapCoords(world, x, y, z);
 *
 *     // Bind MC lightmap to texture slot 9
 *     MCLightingHelper.bindLightmap(MCLightingHelper.LIGHTMAP_TEXTURE_SLOT);
 *     shader.setUniformInt("uLightmap", MCLightingHelper.LIGHTMAP_TEXTURE_SLOT);
 *     shader.setUniformVec2("uLightCoord", lightUV[0], lightUV[1]);
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class MCLightingHelper {

    /** Recommended texture slot for MC lightmap in custom shaders (avoids PBR slots 0-8) */
    public static final int LIGHTMAP_TEXTURE_SLOT = 9;

    /** Maximum light level in MC */
    public static final int MAX_LIGHT_LEVEL = 15;

    /** Lightmap texture size */
    public static final int LIGHTMAP_SIZE = 16;

    private MCLightingHelper() {
        // Utility class
    }

    /**
     * Get lightmap UV coordinates for a world position.
     * The returned values are normalized to 0-1 range for texture sampling.
     *
     * @param world The world instance
     * @param x     Block X coordinate
     * @param y     Block Y coordinate
     * @param z     Block Z coordinate
     * @return float[2] where [0] = blockLight UV, [1] = skyLight UV (both 0-1 range)
     */
    public static float[] getLightmapCoords(World world, int x, int y, int z) {
        if (world == null) {
            return new float[] { 1.0f, 1.0f }; // Full brightness fallback
        }

        int packed = world.getLightBrightnessForSkyBlocks(x, y, z, 0);
        return unpackLightCoords(packed);
    }

    /**
     * Get lightmap UV coordinates for a world position with minimum light level.
     *
     * @param world    The world instance
     * @param x        Block X coordinate
     * @param y        Block Y coordinate
     * @param z        Block Z coordinate
     * @param minLight Minimum block light level (0-15)
     * @return float[2] where [0] = blockLight UV, [1] = skyLight UV (both 0-1 range)
     */
    public static float[] getLightmapCoords(World world, int x, int y, int z, int minLight) {
        if (world == null) {
            return new float[] { 1.0f, 1.0f };
        }

        int packed = world.getLightBrightnessForSkyBlocks(x, y, z, minLight);
        return unpackLightCoords(packed);
    }

    /**
     * Unpack MC's packed light value to UV coordinates.
     *
     * <p>
     * MC packs light values as: (skyLight << 20) | (blockLight << 4)
     * </p>
     *
     * @param packedLight Packed light value from getLightBrightnessForSkyBlocks
     * @return float[2] where [0] = blockLight UV, [1] = skyLight UV (both 0-1 range)
     */
    public static float[] unpackLightCoords(int packedLight) {
        // MC 1.7.10 packs light as: skyLight in bits 20-23, blockLight in bits 4-7
        int blockLight = (packedLight & 0xFFFF) >> 4;
        int skyLight = (packedLight >> 16) & 0xFFFF;
        skyLight = skyLight >> 4;

        // Normalize to 0-1 range
        float blockLightU = Math.min(blockLight, MAX_LIGHT_LEVEL) / (float) MAX_LIGHT_LEVEL;
        float skyLightV = Math.min(skyLight, MAX_LIGHT_LEVEL) / (float) MAX_LIGHT_LEVEL;

        return new float[] { blockLightU, skyLightV };
    }

    /**
     * Get raw light levels (0-15) at a world position.
     *
     * @param world The world instance
     * @param x     Block X coordinate
     * @param y     Block Y coordinate
     * @param z     Block Z coordinate
     * @return int[2] where [0] = blockLight (0-15), [1] = skyLight (0-15)
     */
    public static int[] getRawLightLevels(World world, int x, int y, int z) {
        if (world == null) {
            return new int[] { MAX_LIGHT_LEVEL, MAX_LIGHT_LEVEL };
        }

        int packed = world.getLightBrightnessForSkyBlocks(x, y, z, 0);

        int blockLight = (packed & 0xFFFF) >> 4;
        int skyLight = ((packed >> 16) & 0xFFFF) >> 4;

        return new int[] { Math.min(blockLight, MAX_LIGHT_LEVEL), Math.min(skyLight, MAX_LIGHT_LEVEL) };
    }

    /**
     * Bind MC's lightmap texture to the specified texture slot.
     * Call this before rendering with custom shaders that need MC lighting.
     *
     * @param textureSlot The texture slot to bind to (e.g., LIGHTMAP_TEXTURE_SLOT)
     */
    public static void bindLightmap(int textureSlot) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer == null) {
            return;
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureSlot);

        // MC's lightmap is managed by EntityRenderer
        // We need to get the current lightmap texture that MC is using
        // The lightmap is typically already bound to OpenGlHelper.lightmapTexUnit
        // We copy the binding to our custom slot

        // Save current state
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        // Get MC's lightmap texture ID from the lightmap texture unit
        GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
        int lightmapTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // Bind to our custom slot
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureSlot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmapTextureId);

        // Restore previous active texture
        GL13.glActiveTexture(previousActiveTexture);
    }

    /**
     * Unbind lightmap from the specified texture slot.
     *
     * @param textureSlot The texture slot to unbind
     */
    public static void unbindLightmap(int textureSlot) {
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureSlot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(previousActiveTexture);
    }

    /**
     * Get the current sky light multiplier based on time of day.
     * This represents the overall brightness of the sky.
     *
     * @return Sky light multiplier (0.0 = night, 1.0 = noon)
     */
    public static float getSkyLightMultiplier() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return 1.0f;
        }
        return mc.theWorld.getSunBrightness(1.0f);
    }

    /**
     * Get the current world time (0-24000).
     *
     * @return World time in ticks
     */
    public static long getWorldTime() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return 0;
        }
        return mc.theWorld.getWorldTime() % 24000;
    }

    /**
     * Check if it's currently daytime in the world.
     *
     * @return true if daytime (time 0-12000)
     */
    public static boolean isDaytime() {
        long time = getWorldTime();
        return time < 12000;
    }

    /**
     * Calculate interpolated light coordinates for smooth lighting.
     * Useful for entities or objects between block positions.
     *
     * @param world The world instance
     * @param x     World X coordinate (can be fractional)
     * @param y     World Y coordinate (can be fractional)
     * @param z     World Z coordinate (can be fractional)
     * @return float[2] interpolated light UV coordinates
     */
    public static float[] getInterpolatedLightCoords(World world, double x, double y, double z) {
        if (world == null) {
            return new float[] { 1.0f, 1.0f };
        }

        // Get the block position and fractional parts
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);

        // For simplicity, just sample the current block
        // A more advanced implementation could interpolate between 8 surrounding blocks
        return getLightmapCoords(world, bx, by, bz);
    }

    /**
     * Convert separate block and sky light levels to packed format.
     *
     * @param blockLight Block light level (0-15)
     * @param skyLight   Sky light level (0-15)
     * @return Packed light value compatible with MC format
     */
    public static int packLightLevels(int blockLight, int skyLight) {
        blockLight = Math.max(0, Math.min(MAX_LIGHT_LEVEL, blockLight));
        skyLight = Math.max(0, Math.min(MAX_LIGHT_LEVEL, skyLight));
        return (skyLight << 20) | (blockLight << 4);
    }

    /**
     * Create UV coordinates from separate light levels.
     *
     * @param blockLight Block light level (0-15)
     * @param skyLight   Sky light level (0-15)
     * @return float[2] normalized UV coordinates
     */
    public static float[] createLightCoords(int blockLight, int skyLight) {
        float u = Math.max(0, Math.min(MAX_LIGHT_LEVEL, blockLight)) / (float) MAX_LIGHT_LEVEL;
        float v = Math.max(0, Math.min(MAX_LIGHT_LEVEL, skyLight)) / (float) MAX_LIGHT_LEVEL;
        return new float[] { u, v };
    }

    /**
     * Full brightness light coordinates (15, 15).
     */
    public static final float[] FULL_BRIGHT = { 1.0f, 1.0f };

    /**
     * No light coordinates (0, 0).
     */
    public static final float[] NO_LIGHT = { 0.0f, 0.0f };
}
