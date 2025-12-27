package moe.takochan.takotech.client.renderer.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.util.vector.Vector3f;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Minecraft 渲染辅助工具类。
 * 提供坐标转换、位置插值等 MC 特定的渲染工具方法。
 *
 * <p>
 * MC 渲染坐标系说明：
 * </p>
 * <ul>
 * <li>世界坐标：方块/实体的绝对位置</li>
 * <li>渲染坐标：相对于相机的位置（用于实际渲染）</li>
 * <li>RenderManager.renderPosX/Y/Z：当前渲染相机位置</li>
 * </ul>
 *
 * <p>
 * 转换公式：renderPos = worldPos - cameraPos
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class MCRenderHelper {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private MCRenderHelper() {}

    // ==================== 相机位置 ====================

    /**
     * 获取当前渲染相机的 X 坐标
     */
    public static double getCameraX() {
        return RenderManager.renderPosX;
    }

    /**
     * 获取当前渲染相机的 Y 坐标
     */
    public static double getCameraY() {
        return RenderManager.renderPosY;
    }

    /**
     * 获取当前渲染相机的 Z 坐标
     */
    public static double getCameraZ() {
        return RenderManager.renderPosZ;
    }

    /**
     * 获取当前渲染相机位置
     */
    public static Vector3f getCameraPosition() {
        return new Vector3f(
            (float) RenderManager.renderPosX,
            (float) RenderManager.renderPosY,
            (float) RenderManager.renderPosZ);
    }

    // ==================== 坐标转换 ====================

    /**
     * 将世界坐标转换为渲染坐标
     *
     * @param worldX 世界 X 坐标
     * @param worldY 世界 Y 坐标
     * @param worldZ 世界 Z 坐标
     * @return 渲染坐标 (x, y, z)
     */
    public static double[] worldToRender(double worldX, double worldY, double worldZ) {
        return new double[] { worldX - RenderManager.renderPosX, worldY - RenderManager.renderPosY,
            worldZ - RenderManager.renderPosZ };
    }

    /**
     * 将世界坐标转换为渲染坐标（返回 Vector3f）
     */
    public static Vector3f worldToRenderVec(double worldX, double worldY, double worldZ) {
        return new Vector3f(
            (float) (worldX - RenderManager.renderPosX),
            (float) (worldY - RenderManager.renderPosY),
            (float) (worldZ - RenderManager.renderPosZ));
    }

    /**
     * 将方块坐标转换为渲染坐标
     *
     * @param blockX 方块 X 坐标
     * @param blockY 方块 Y 坐标
     * @param blockZ 方块 Z 坐标
     * @return 渲染坐标 (x, y, z)
     */
    public static double[] blockToRender(int blockX, int blockY, int blockZ) {
        return worldToRender(blockX, blockY, blockZ);
    }

    // ==================== 实体位置插值 ====================

    /**
     * 获取实体的插值位置（避免渲染抖动）
     *
     * @param entity       实体
     * @param partialTicks 部分刻
     * @return 插值后的世界坐标 (x, y, z)
     */
    public static double[] getEntityInterpolatedPos(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        return new double[] { x, y, z };
    }

    /**
     * 获取实体的插值渲染位置
     *
     * @param entity       实体
     * @param partialTicks 部分刻
     * @return 相对于相机的渲染坐标 (x, y, z)
     */
    public static double[] getEntityRenderPos(Entity entity, float partialTicks) {
        double[] worldPos = getEntityInterpolatedPos(entity, partialTicks);
        return worldToRender(worldPos[0], worldPos[1], worldPos[2]);
    }

    /**
     * 获取玩家脚下的插值位置（世界坐标）
     *
     * @param player       玩家
     * @param partialTicks 部分刻
     * @return 脚下位置的世界坐标 (x, y, z)
     */
    public static double[] getPlayerFootPos(EntityPlayer player, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - player.yOffset;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        return new double[] { x, y, z };
    }

    /**
     * 获取玩家脚下的渲染位置
     *
     * @param player       玩家
     * @param partialTicks 部分刻
     * @return 相对于相机的渲染坐标 (x, y, z)
     */
    public static double[] getPlayerFootRenderPos(EntityPlayer player, float partialTicks) {
        double[] worldPos = getPlayerFootPos(player, partialTicks);
        return worldToRender(worldPos[0], worldPos[1], worldPos[2]);
    }

    /**
     * 获取玩家头顶位置（渲染坐标）
     *
     * @param player       玩家
     * @param partialTicks 部分刻
     * @return 相对于相机的渲染坐标 (x, y, z)
     */
    public static double[] getPlayerHeadRenderPos(EntityPlayer player, float partialTicks) {
        double[] footPos = getPlayerFootRenderPos(player, partialTicks);
        return new double[] { footPos[0], footPos[1] + player.height, footPos[2] };
    }

    // ==================== 距离计算 ====================

    /**
     * 计算相机到世界坐标点的距离
     */
    public static double distanceToCamera(double worldX, double worldY, double worldZ) {
        double dx = worldX - RenderManager.renderPosX;
        double dy = worldY - RenderManager.renderPosY;
        double dz = worldZ - RenderManager.renderPosZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 计算相机到世界坐标点的距离平方（用于比较，避免开方）
     */
    public static double distanceToCameraSq(double worldX, double worldY, double worldZ) {
        double dx = worldX - RenderManager.renderPosX;
        double dy = worldY - RenderManager.renderPosY;
        double dz = worldZ - RenderManager.renderPosZ;
        return dx * dx + dy * dy + dz * dz;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前客户端玩家
     */
    public static EntityPlayer getPlayer() {
        return mc.thePlayer;
    }

    /**
     * 检查客户端是否就绪（玩家和世界已加载）
     */
    public static boolean isClientReady() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getDisplayWidth() {
        return mc.displayWidth;
    }

    /**
     * 获取屏幕高度
     */
    public static int getDisplayHeight() {
        return mc.displayHeight;
    }
}
