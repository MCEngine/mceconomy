package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class HandleAdd implements IEconomyCommandHandle {
    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleAdd(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mceconomy.add.coin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy add <player> <coin type> <amount>");
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
            provider.addCoin(target.getUniqueId().toString(), coinType, amount);
            sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + amount + " " + coinType + ChatColor.GREEN + " to " + ChatColor.WHITE + targetName + ChatColor.GREEN + ".");
        });
    }

    @Override
    public String getHelp() {
        return "<player> <coin type> <amount> - Admin Add coins";
    }

    @Override
    public String getPermission() {
        return "mceconomy.add.coin";
    }
}