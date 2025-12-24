package moe.takochan.takotech.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扇形环顶点生成工具类。
 * 使用动态计算 + 缓存机制，避免硬编码顶点数据。
 */
public class SectorVertexUtils {

    /** 内圈半径 */
    public static final float RADIUS_IN = 30F;
    /** 外圈半径 */
    public static final float RADIUS_OUT = RADIUS_IN * 2F;
    /** 角度分割精度（度） */
    private static final float ANGLE_PRECISION = 5F;

    /** 顶点数据缓存：key = 缓存键, value = 扇区顶点数据 */
    private static final Map<CacheKey, List<List<float[][]>>> VERTEX_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存键，用于区分不同参数的顶点数据
     */
    private static class CacheKey {

        final int sectorCount;
        final float innerRadius;
        final float outerRadius;
        final float precision;

        CacheKey(int sectorCount, float innerRadius, float outerRadius, float precision) {
            this.sectorCount = sectorCount;
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
            this.precision = precision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey key = (CacheKey) o;
            return sectorCount == key.sectorCount && Float.compare(key.innerRadius, innerRadius) == 0
                && Float.compare(key.outerRadius, outerRadius) == 0
                && Float.compare(key.precision, precision) == 0;
        }

        @Override
        public int hashCode() {
            int result = sectorCount;
            result = 31 * result + Float.floatToIntBits(innerRadius);
            result = 31 * result + Float.floatToIntBits(outerRadius);
            result = 31 * result + Float.floatToIntBits(precision);
            return result;
        }
    }

    /**
     * 获取默认参数的扇区顶点数据（使用缓存）
     *
     * @param sectorCount 扇区数量
     * @return 扇区顶点数据，每个扇区包含多个四边形段
     */
    public static List<List<float[][]>> getSectorVertices(int sectorCount) {
        return getSectorVertices(sectorCount, RADIUS_IN, RADIUS_OUT, ANGLE_PRECISION);
    }

    /**
     * 获取指定参数的扇区顶点数据（使用缓存）
     *
     * @param sectorCount 扇区数量
     * @param innerRadius 内圈半径
     * @param outerRadius 外圈半径
     * @param precision   角度精度（度）
     * @return 扇区顶点数据
     */
    public static List<List<float[][]>> getSectorVertices(int sectorCount, float innerRadius, float outerRadius,
        float precision) {
        if (sectorCount <= 0) return new ArrayList<>();

        CacheKey key = new CacheKey(sectorCount, innerRadius, outerRadius, precision);
        return VERTEX_CACHE
            .computeIfAbsent(key, k -> generateSectorVertices(sectorCount, innerRadius, outerRadius, precision));
    }

    /**
     * 生成扇区顶点数据
     *
     * @param sectorCount 扇区数量
     * @param innerRadius 内圈半径
     * @param outerRadius 外圈半径
     * @param precision   角度精度（度）
     * @return 扇区顶点数据列表
     */
    private static List<List<float[][]>> generateSectorVertices(int sectorCount, float innerRadius, float outerRadius,
        float precision) {
        List<List<float[][]>> sectors = new ArrayList<>(sectorCount);

        float sectorAngle = 360F / sectorCount;

        for (int i = 0; i < sectorCount; i++) {
            List<float[][]> segments = new ArrayList<>();

            // 计算扇区起始和结束角度（从顶部开始，顺时针）
            float sectorStart = (((i - 0.5F) / sectorCount) + 0.25F) * 360F;
            float sectorEnd = (((i + 0.5F) / sectorCount) + 0.25F) * 360F;
            float angleRange = sectorEnd - sectorStart;

            // 计算分割段数
            int segmentCount = Math.max(1, (int) Math.ceil(angleRange / precision));
            float anglePerSegment = angleRange / segmentCount;

            // 生成每个小段的四边形顶点
            for (int s = 0; s < segmentCount; s++) {
                float startAngle = sectorStart + s * anglePerSegment;
                float endAngle = startAngle + anglePerSegment;

                float startRad = (float) Math.toRadians(startAngle);
                float endRad = (float) Math.toRadians(endAngle);

                float cosStart = (float) Math.cos(startRad);
                float sinStart = (float) Math.sin(startRad);
                float cosEnd = (float) Math.cos(endRad);
                float sinEnd = (float) Math.sin(endRad);

                // 四个顶点：外起点、内起点、内终点、外终点
                float[][] quad = new float[][] { { outerRadius * cosStart, outerRadius * sinStart },
                    { innerRadius * cosStart, innerRadius * sinStart }, { innerRadius * cosEnd, innerRadius * sinEnd },
                    { outerRadius * cosEnd, outerRadius * sinEnd } };

                segments.add(quad);
            }
            sectors.add(segments);
        }

        return sectors;
    }

    /**
     * 清除顶点缓存
     */
    public static void clearCache() {
        VERTEX_CACHE.clear();
    }

    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return VERTEX_CACHE.size();
    }
}
