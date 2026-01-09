package io.github.mcengine.mceconomy.api.command;

import org.bukkit.command.CommandSender;

/**
 * Interface for all economy subcommands.
 */
public interface IEconomyCommandHandle {
    /**
     * Executes the specific subcommand logic.
     * @param sender The person who ran the command.
     * @param args The arguments passed (clipped array).
     */
    void invoke(CommandSender sender, String[] args);
    String getHelp();
    String getPermission();
}
