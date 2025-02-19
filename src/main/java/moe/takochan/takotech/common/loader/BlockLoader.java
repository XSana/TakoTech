package moe.takochan.takotech.common.loader;

import moe.takochan.takotech.common.block.ae.BlockWebClient;

/**
 * 方块注册
 */
public class BlockLoader implements Runnable {

    public static BlockWebClient BLOCK_WEB_CLIENT;

    public BlockLoader() {
        BLOCK_WEB_CLIENT = new BlockWebClient();
    }

    @Override
    public void run() {
        registerBlock();
    }

    private void registerBlock() {
        BLOCK_WEB_CLIENT.register();
    }
}
