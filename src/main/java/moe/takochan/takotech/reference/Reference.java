package moe.takochan.takotech.reference;

import moe.takochan.takotech.Tags;

/**
 * 该类包含整个模组中使用的常量引用。
 */
public class Reference {

    /**
     * 模组版本。
     */
    public static final String VERSION = Tags.VERSION;

    /**
     * 模组的唯一标识符（Mod ID）。
     */
    public static final String MODID = "TakoTech";

    /**
     * 模组名称。
     */
    public static final String MODNAME = "TakoTech";

    /**
     * 模组的组名。
     */
    public static final String GROUPNAME = "moe.takochan.takotech";

    /**
     * GUI 工厂类的全限定名。
     * 用于初始化模组的图形用户界面配置。
     */
    public static final String GUI_FACTORY = "moe.takochan.takotech.client.gui.config.TakoTechGuiConfigFactory";

    /**
     * 资源根标识符。
     */
    public static final String RESOURCE_ROOT_ID = MODID.toLowerCase();

    /**
     * 模组的依赖关系列表。
     */
    public static final String DEPENDENCIES =
        "required-after:gtnhlib@[0.2.11,);"
            + " required-after:appliedenergistics2;"
            + " after:NotEnoughItems";
}
