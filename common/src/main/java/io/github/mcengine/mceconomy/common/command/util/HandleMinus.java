package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HandleMinus implements IEconomyCommandHandle {
    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleMinus(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mceconomy.minus.coin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy minus <player> <coin type> <amount>");
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
            provider.minusCoin(target.getUniqueId().toString(), coinType, amount);
            sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + amount + " " + coinType + ChatColor.GREEN + " from " + ChatColor.WHITE + targetName + ChatColor.GREEN + ".");
        });
    }

    /**
     * @return The help description for the minus command.
     */
    @Override
    public String getHelp() {
        return "<player> <coin type> <amount> - Admin Remove money";
    }

    /**
     * @return The permission node required to remove coins.
     */
    @Override
    public String getPermission() {
        return "mceconomy.minus.coin";
    }
}