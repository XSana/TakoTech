package moe.takochan.takotech.common.block.ae;

import net.minecraft.block.material.Material;

import moe.takochan.takotech.common.block.BaseAETileBlock;
import moe.takochan.takotech.common.tile.TileWebController;
import moe.takochan.takotech.constants.NameConstants;

public class BlockWebClient extends BaseAETileBlock {

    public BlockWebClient() {
        super(NameConstants.BLOCK_WEB_CONTROLLER, Material.iron, TileWebController.class);

        this.setHardness(1.14f);
        this.setLightOpacity(255);
        this.setLightLevel(0);
        this.setHarvestLevel("pickaxe", 0);
    }

}
