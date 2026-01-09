package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class HandleGet implements IEconomyCommandHandle {
    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleGet(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can check their balance.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /economy get <coin type>");
            return;
        }

        Player player = (Player) sender;
        String coinType = args[0].toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int balance = provider.getCoin(player.getUniqueId().toString(), coinType);
            player.sendMessage(ChatColor.GREEN + "Your " + coinType + " balance: " + ChatColor.WHITE + balance);
        });
    }

    /**
     * @return The help description for the get command.
     */
    @Override
    public String getHelp() {
        return "<coin type> - View your balance";
    }

    /**
     * @return null as this command is available to all players by default.
     */
    @Override
    public String getPermission() {
        return null;
    }
}