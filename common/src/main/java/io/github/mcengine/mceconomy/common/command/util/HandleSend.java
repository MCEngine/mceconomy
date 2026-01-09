package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for sending coins from one player to another.
 */
public class HandleSend implements IEconomyCommandHandle {
    /**
     * The plugin instance for scheduling tasks.
     */
    private final Plugin plugin;
    
    /**
     * The economy provider for data operations.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleSend instance.
     * @param plugin The plugin instance.
     * @param provider The economy provider.
     */
    public HandleSend(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    /**
     * Executes the send command logic.
     * @param sender The sender of the command.
     * @param args The command arguments.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can send coins.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /economy send <player> <coin type> <amount>", NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String targetName = args[0];
        String coinType = args[1].toLowerCase();
        int amount;

        try { 
            amount = Integer.parseInt(args[2]); 
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Validation check before attempting DB transaction
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Component.text()
                .append(Component.text("Player ", NamedTextColor.RED))
                .append(Component.text(targetName, NamedTextColor.WHITE))
                .append(Component.text(" not found.", NamedTextColor.RED))
                .build());
            return;
        }

        provider.sendCoin(player.getUniqueId().toString(), target.getUniqueId().toString(), coinType, amount)
            .thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text()
                        .append(Component.text("Sent ", NamedTextColor.GREEN))
                        .append(Component.text(amount + " " + coinType, NamedTextColor.WHITE))
                        .append(Component.text(" to ", NamedTextColor.GREEN))
                        .append(Component.text(targetName, NamedTextColor.WHITE))
                        .append(Component.text(".", NamedTextColor.GREEN))
                        .build());
                } else {
                    player.sendMessage(Component.text("Transfer failed: Insufficient funds.", NamedTextColor.RED));
                }
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
     * @return null as send is available to all players.
     */
    @Override
    public String getPermission() {
        return null;
    }
}
