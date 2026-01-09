package io.github.mcengine.mceconomy.common.command;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the registration and execution of economy subcommands.
 */
public class MCEconomyCommandManager implements CommandExecutor {
    private final Map<String, IEconomyCommandHandle> subcommands = new HashMap<>();

    /**
     * Registers a new subcommand handler.
     * @param name The subcommand name (e.g., "get").
     * @param handler The logic handler for that command.
     */
    public void register(String name, IEconomyCommandHandle handler) {
        subcommands.put(name.toLowerCase(), handler);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Default to help if no args provided
        String subName = (args.length == 0) ? "help" : args[0].toLowerCase();
        IEconomyCommandHandle handle = subcommands.get(subName);

        if (handle != null) {
            // Clip the array: remove the subcommand name
            String[] subArgs = (args.length <= 1) ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            handle.invoke(sender, subArgs);
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /economy help.");
        }
        return true;
    }

    public Map<String, IEconomyCommandHandle> getSubcommands() {
        return subcommands;
    }
}