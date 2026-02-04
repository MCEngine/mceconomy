package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
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
                MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.usage.get.other").color(NamedTextColor.RED));
            } else {
                MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.usage.get").color(NamedTextColor.RED));
            }
            return;
        }

        CurrencyType coinType = CurrencyType.fromName(args[0]);
        if (coinType == null) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.invalid.coin").color(NamedTextColor.RED));
            return;
        }

        OfflinePlayer target;

        // Case 1: Checking own balance
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.console.must.specify.player").color(NamedTextColor.RED));
                return;
            }
            target = (Player) sender;
        } 
        // Case 2: Checking another player's balance (OP/Admin)
        else {
            if (!sender.hasPermission("mceconomy.get.other")) {
                MCEconomyCommandManager.send(sender, Component.translatable("msg.permission.denied").color(NamedTextColor.RED));
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        }
        
        // Validation check
        if (!target.hasPlayedBefore() && !target.isOnline()) {
             MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.player.not.found")
                .args(Component.text(target.getName() != null ? target.getName() : args[1]))
                .color(NamedTextColor.RED));
            return;
        }

        // Capture name for lambda (OfflinePlayer#getName() can be null, handle gracefully)
        String targetName = target.getName() != null ? target.getName() : "Unknown";

        // Updated: Added "PLAYER" account type
        provider.getCoin(target.getUniqueId().toString(), "PLAYER", coinType).thenAccept(balance -> {
            // Message variation depending on if checking self or other
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
                MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.balance.self")
                    .args(
                        Component.text(coinType.getName()),
                        Component.text(balance)
                    )
                    .color(NamedTextColor.GREEN));
            } else {
                MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.balance.other")
                    .args(
                        Component.text(targetName),
                        Component.text(coinType.getName()),
                        Component.text(balance)
                    )
                    .color(NamedTextColor.GREEN));
            }
        });
    }

    /**
     * @return The help description for the get command.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mceconomy.msg.help.get");
    }

    /**
     * @return null as this command is available to all players by default (for self).
     */
    @Override
    public String getPermission() {
        return null;
    }
}
