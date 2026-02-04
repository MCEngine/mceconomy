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
import org.bukkit.plugin.Plugin;

/**
 * Command handler for sending coins from one player to another.
 */
public class HandleSend implements IEconomyCommandHandle {

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
            MCEconomyCommandManager.send(sender, Component.translatable("msg.only_players").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.usage.send").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String targetName = args[0];
        CurrencyType coinType = CurrencyType.fromName(args[1]);
        if (coinType == null) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.invalid.coin").color(NamedTextColor.RED));
            return;
        }

        int amount;

        try { 
            amount = Integer.parseInt(args[2]); 
        } catch (NumberFormatException e) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.invalid.amount").color(NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Validation check before attempting DB transaction
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.player.not.found")
                .args(Component.text(targetName))
                .color(NamedTextColor.RED));
            return;
        }

        // Updated: Added "PLAYER" account type for both Sender and Receiver
        provider.sendCoin(player.getUniqueId().toString(), "PLAYER", target.getUniqueId().toString(), "PLAYER", coinType, amount)
            .thenAccept(success -> {
                if (success) {
                    MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.success.send")
                        .args(
                            Component.text(amount),
                            Component.text(coinType.getName()),
                            Component.text(targetName)
                        )
                        .color(NamedTextColor.GREEN));
                } else {
                    MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.insufficient.funds").color(NamedTextColor.RED));
                }
            });
    }

    /**
     * @return The help description for the send command.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mceconomy.msg.help.send");
    }

    /**
     * @return null as send is available to all players.
     */
    @Override
    public String getPermission() {
        return null;
    }
}
