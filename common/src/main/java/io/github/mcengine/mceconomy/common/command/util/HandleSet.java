package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /economy set <player> <coin type> <amount>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        String coinType = args[1].toLowerCase();
        int amount;

        try { 
            amount = Integer.parseInt(args[2]); 
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            provider.setCoin(target.getUniqueId().toString(), coinType, amount);
            
            sender.sendMessage(Component.text()
                .append(Component.text("Set ", NamedTextColor.GREEN))
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text("'s " + coinType + " to ", NamedTextColor.GREEN))
                .append(Component.text(amount, NamedTextColor.WHITE))
                .append(Component.text(".", NamedTextColor.GREEN))
                .build());
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