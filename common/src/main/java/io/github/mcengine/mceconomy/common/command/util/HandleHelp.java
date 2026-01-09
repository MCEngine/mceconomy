package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class HandleHelp implements IEconomyCommandHandle {
    private final MCEconomyCommandManager manager;

    public HandleHelp(MCEconomyCommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "--- MCEconomy Help ---");

        for (Map.Entry<String, IEconomyCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            IEconomyCommandHandle handle = entry.getValue();
            
            // Don't show the help command in the help list
            if (name.equalsIgnoreCase("help")) continue;

            // Check if player has permission for this specific subcommand
            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                sender.sendMessage(ChatColor.YELLOW + "/economy " + name + " " + ChatColor.GRAY + handle.getHelp());
            }
        }
    }

    @Override
    public String getHelp() { return "- View this help menu"; }

    @Override
    public String getPermission() { return null; } // Everyone can use help
}