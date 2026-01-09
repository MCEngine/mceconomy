package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for removing coins from a player's balance.
 */
public class HandleMinus implements IEconomyCommandHandle {
    /**
     * The plugin instance for scheduling tasks.
     */
    private final Plugin plugin;
    
    /**
     * The economy provider for data operations.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleMinus instance.
     * @param plugin The plugin instance.
     * @param provider The economy provider.
     */
    public HandleMinus(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    /**
     * Executes the minus command logic.
     * @param sender The sender of the command.
     * @param args The command arguments.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mceconomy.minus.coin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /economy minus <player> <coin type> <amount>", NamedTextColor.RED));
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
            provider.minusCoin(target.getUniqueId().toString(), coinType, amount);
            
            sender.sendMessage(Component.text()
                .append(Component.text("Removed ", NamedTextColor.GREEN))
                .append(Component.text(amount + " " + coinType, NamedTextColor.WHITE))
                .append(Component.text(" from ", NamedTextColor.GREEN))
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(".", NamedTextColor.GREEN))
                .build());
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
     * @return The permission node required for this command.
     */
    @Override
    public String getPermission() {
        return "mceconomy.minus.coin";
    }
}
