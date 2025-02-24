package moe.takochan.takotech.common.loader;

import moe.takochan.takotech.common.block.ae.BlockWebController;

/**
 * 方块注册
 */
public class BlockLoader implements Runnable {

    public static BlockWebController BLOCK_WEB_CONTROLLER;

    public BlockLoader() {
        BLOCK_WEB_CONTROLLER = new BlockWebController();
    }

    @Override
    public void run() {
        registerBlock();
    }

    private void registerBlock() {
        BLOCK_WEB_CONTROLLER.register();
    }
}
