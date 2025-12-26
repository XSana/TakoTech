package moe.takochan.takotech.common.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import moe.takochan.takotech.TakoTechMod;
import moe.takochan.takotech.config.ServerConfig;
import moe.takochan.takotech.network.NetworkHandler;

/**
 * 配置重载命令
 * 用法: /takotech reload
 */
public class CommandReloadConfig extends CommandBase {

    private static final List<String> SUB_COMMANDS = Collections.singletonList("reload");

    @Override
    public String getCommandName() {
        return "takotech";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/takotech reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP 权限
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB_COMMANDS.toArray(new String[0]));
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            // 重新加载服务端配置
            ServerConfig.reload();

            // 向所有在线玩家同步配置
            @SuppressWarnings("unchecked")
            List<EntityPlayerMP> players = MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList;
            for (EntityPlayerMP player : players) {
                NetworkHandler.sendConfigSync(player);
            }

            String message = EnumChatFormatting.GREEN + "[TakoTech] " + EnumChatFormatting.RESET + "配置已重载并同步到所有玩家";
            sender.addChatMessage(new ChatComponentText(message));
            TakoTechMod.LOG.info("Config reloaded by {}", sender.getCommandSenderName());
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "未知子命令: " + args[0]));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
        }
    }
}
