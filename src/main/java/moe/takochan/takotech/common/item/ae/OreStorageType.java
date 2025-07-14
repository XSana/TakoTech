package moe.takochan.takotech.common.item.ae;

import moe.takochan.takotech.config.TakoTechConfig;

/**
 * 表示矿物存储元件的分类类型。
 * <p>
 * 每种类型限定可存储的矿物词典前缀（includedPrefixes），
 * 并可选择性排除某些前缀（excludedPrefixes）。
 * 这些前缀仅在构造时解析一次，避免运行时反复 split 操作。
 * <p>
 * 通常通过 {@link #byMeta(int)} 以物品的 metadata 获取对应的类型。
 * 用于 {@link ItemOreStorageCell} 判断某个物品是否可被该元件接收。
 */
public enum OreStorageType {

    /**
     * 通用类型，不做固定前缀限制，使用配置文件定义的 oreDefs。
     */
    GENERAL(0, "", ""),

    /**
     * 原矿类型，包含 "ore" 和 "rawOre" 前缀。
     */
    RAW(1, "ore|rawOre", ""),

    /**
     * 粉碎矿类型，仅包含 "crushed" 前缀，但排除洗净矿和离心矿。
     */
    CRUSHED(2, "crushed", "crushedPurified|crushedCentrifuged"),

    /**
     * 洗净矿类型，仅包含 "crushedPurified" 前缀。
     */
    PURIFIED(3, "crushedPurified", ""),

    /**
     * 离心矿类型，仅包含 "crushedCentrifuged" 前缀。
     */
    CENTRIFUGED(4, "crushedCentrifuged", ""),

    /**
     * 含杂粉类型，仅包含 "dustImpure" 前缀。
     */
    DUST_IMPURE(5, "dustImpure", ""),

    /**
     * 洗净粉类型，仅包含 "dustPure" 前缀。
     */
    DUST_PURE(6, "dustPure", "");

    /**
     * 与物品 metadata 对应的编号。
     */
    private final int meta;

    /**
     * 此类型所允许接收的矿物词典前缀。
     */
    private final String[] includedPrefixes;

    /**
     * 此类型明确排除的矿物词典前缀。
     */
    private final String[] excludedPrefixes;

    /**
     * 构造方法，在加载阶段解析前缀字符串为 List。
     *
     * @param meta       与物品 metadata 对应的值
     * @param includeStr 用 '|' 分隔的允许前缀列表
     * @param excludeStr 用 '|' 分隔的排除前缀列表
     */
    OreStorageType(int meta, String includeStr, String excludeStr) {
        this.meta = meta;
        this.includedPrefixes = parseList(includeStr);
        this.excludedPrefixes = parseList(excludeStr);
    }

    /**
     * 将 '|' 分隔的字符串转为不可变前缀列表。
     *
     * @param str 原始前缀字符串
     * @return 解析后的前缀列表，若为空则返回空列表
     */
    private static String[] parseList(String str) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        }
        return str.split("\\|");
    }

    /**
     * 获取该类型对应的 metadata。
     *
     * @return 元数据值
     */
    public int getMeta() {
        return meta;
    }

    /**
     * 获取允许存储的前缀列表。
     *
     * @return 前缀列表，永不为 null
     */
    public String[] getIncludedPrefixes() {
        if (this == GENERAL) {
            return TakoTechConfig.oreDefs; // 动态获取
        }
        return includedPrefixes;
    }

    /**
     * 获取排除的前缀列表。
     *
     * @return 前缀列表，永不为 null
     */
    public String[] getExcludedPrefixes() {
        return excludedPrefixes;
    }

    /**
     * 根据 metadata 查找对应的类型。
     *
     * @param meta 物品的元数据值
     * @return 对应类型，若未匹配则返回 GENERAL
     */
    public static OreStorageType byMeta(int meta) {
        for (OreStorageType t : values()) {
            if (t.meta == meta) return t;
        }
        return GENERAL;
    }

    public String getJoinedIncludes() {
        return String.join(" | ", getIncludedPrefixes());
    }

    public String getJoinedExcludes() {
        return String.join(" | ", getExcludedPrefixes());
    }
}
