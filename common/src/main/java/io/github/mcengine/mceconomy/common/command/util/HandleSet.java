package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HandleSet implements IEconomyCommandHandle {
    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleSet(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mceconomy.set.coin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy set <player> <coin type> <amount>");
            return;
        }

        String targetName = args[0];
        String coinType = args[1].toLowerCase();
        int amount;

        try { 
            amount = Integer.parseInt(args[2]); 
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            provider.setCoin(target.getUniqueId().toString(), coinType, amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.WHITE + targetName + ChatColor.GREEN + "'s " + coinType + " to " + ChatColor.WHITE + amount + ChatColor.GREEN + ".");
        });
    }

    /**
     * @return The help description for the set command.
     */
    @Override
    public String getHelp() {
        return "<player> <coin type> <amount> - Admin Set balance";
    }

    /**
     * @return The permission node required to set coins.
     */
    @Override
    public String getPermission() {
        return "mceconomy.set.coin";
    }
}