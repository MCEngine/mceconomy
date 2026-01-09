package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HandleSend implements IEconomyCommandHandle {
    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleSend(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can send coins.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy send <player> <coin type> <amount>");
            return;
        }

        Player player = (Player) sender;
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
            
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + targetName + ChatColor.RED + " not found.");
                return;
            }

            provider.sendCoin(player.getUniqueId().toString(), target.getUniqueId().toString(), coinType, amount);
            player.sendMessage(ChatColor.GREEN + "Sent " + ChatColor.WHITE + amount + " " + coinType + ChatColor.GREEN + " to " + ChatColor.WHITE + targetName + ChatColor.GREEN + ".");
        });
    }

    /**
     * @return The help description for the send command.
     */
    @Override
    public String getHelp() {
        return "<player> <coin type> <amount> - Send money to another player";
    }

    /**
     * @return null as this command is available to all players by default.
     */
    @Override
    public String getPermission() {
        return null;
    }
}