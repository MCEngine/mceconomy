package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for retrieving a player's balance.
 */
public class HandleGet implements IEconomyCommandHandle {

    /**
     * The economy provider for data operations.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleGet instance.
     * @param plugin The plugin instance.
     * @param provider The economy provider.
     */
    public HandleGet(Plugin plugin, MCEconomyProvider provider) {
        this.provider = provider;
    }

    /**
     * Executes the get command logic.
     * @param sender The sender of the command.
     * @param args The command arguments.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        // Minimum requirement: <coin type>
        if (args.length < 1) {
            if (sender.hasPermission("mceconomy.get.other")) {
                MCEconomyCommandManager.send(sender, Component.text("Usage: /economy get <coin type> [player]", NamedTextColor.RED));
            } else {
                MCEconomyCommandManager.send(sender, Component.text("Usage: /economy get <coin type>", NamedTextColor.RED));
            }
            return;
        }

        String coinType = args[0].toLowerCase();
        OfflinePlayer target;

        // Case 1: Checking own balance
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MCEconomyCommandManager.send(sender, Component.text("Console must specify a player: /economy get <coin type> <player>", NamedTextColor.RED));
                return;
            }
            target = (Player) sender;
        } 
        // Case 2: Checking another player's balance (OP/Admin)
        else {
            if (!sender.hasPermission("mceconomy.get.other")) {
                MCEconomyCommandManager.send(sender, Component.text("No permission to check other players' balances.", NamedTextColor.RED));
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        }
        
        // Validation check
        if (!target.hasPlayedBefore() && !target.isOnline()) {
             MCEconomyCommandManager.send(sender, Component.text()
                .append(Component.text("Player ", NamedTextColor.RED))
                .append(Component.text(target.getName() != null ? target.getName() : args[1], NamedTextColor.WHITE))
                .append(Component.text(" not found.", NamedTextColor.RED))
                .build());
            return;
        }

        // Capture name for lambda (OfflinePlayer#getName() can be null, handle gracefully)
        String targetName = target.getName() != null ? target.getName() : "Unknown";

        // Updated: Added "PLAYER" account type
        provider.getCoin(target.getUniqueId().toString(), "PLAYER", coinType).thenAccept(balance -> {
            // Message variation depending on if checking self or other
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
                MCEconomyCommandManager.send(sender, Component.text()
                    .append(Component.text("Your " + coinType + " balance: ", NamedTextColor.GREEN))
                    .append(Component.text(balance, NamedTextColor.WHITE))
                    .build());
            } else {
                MCEconomyCommandManager.send(sender, Component.text()
                    .append(Component.text(targetName + "'s " + coinType + " balance: ", NamedTextColor.GREEN))
                    .append(Component.text(balance, NamedTextColor.WHITE))
                    .build());
            }
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
     * @return null as this command is available to all players by default (for self).
     */
    @Override
    public String getPermission() {
        return null;
    }
}
