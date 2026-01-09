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

    /**
     * Retrieves the help description for the command.
     * @return A string describing usage and purpose.
     */
    String getHelp();

    /**
     * Retrieves the permission node required to execute the command.
     * @return The permission string, or null if no permission is required.
     */
    String getPermission();
}
