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
 * Command handler for adding coins to a player's balance.
 */
public class HandleAdd implements IEconomyCommandHandle {

    /**
     * The economy provider for data operations.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleAdd instance.
     * @param plugin The plugin instance.
     * @param provider The economy provider.
     */
    public HandleAdd(Plugin plugin, MCEconomyProvider provider) {
        this.provider = provider;
    }

    /**
     * Executes the add command logic.
     * @param sender The sender of the command.
     * @param args The command arguments.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mceconomy.add.coin")) {
            MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.permission.denied").color(NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.usage.add").color(NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        CurrencyType coinType = CurrencyType.fromName(args[1]);
        if (coinType == null) {
            MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.invalid.coin").color(NamedTextColor.RED));
            return;
        }

        int amount;

        try { 
            amount = Integer.parseInt(args[2]); 
        } catch (NumberFormatException e) {
            MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.invalid.amount").color(NamedTextColor.RED));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Validation check before attempting DB transaction
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.player.not.found")
                .args(Component.text(targetName))
                .color(NamedTextColor.RED));
            return;
        }

        // Updated: Added "PLAYER" account type
        provider.addCoin(target.getUniqueId().toString(), "PLAYER", coinType, amount).thenAccept(success -> {
            if (success) {
                MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.success.add")
                    .args(
                        Component.text(amount),
                        Component.text(coinType.getName()),
                        Component.text(targetName)
                    )
                    .color(NamedTextColor.GREEN));
            } else {
                MCEconomyCommandManager.send(sender, Component.translatable("mcengine.mceconomy.msg.error.generic").color(NamedTextColor.RED));
            }
        });
    }

    /**
     * @return The help description for the add command.
     */
    @Override
    public Component getHelp() {
        return Component.translatable("mcengine.mceconomy.msg.help.add");
    }

    /**
     * @return The permission node required for this command.
     */
    @Override
    public String getPermission() {
        return "mceconomy.add.coin";
    }
}
