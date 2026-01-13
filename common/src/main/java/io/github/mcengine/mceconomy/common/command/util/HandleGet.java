package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (!(sender instanceof Player)) {
            MCEconomyCommandManager.send(sender, Component.text("Only players can check their balance.", NamedTextColor.RED));
            return;
        }
        if (args.length < 1) {
            MCEconomyCommandManager.send(sender, Component.text("Usage: /economy get <coin type>", NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String coinType = args[0].toLowerCase();

        // The provider now handles the async task. We just handle the result.
        provider.getCoin(player.getUniqueId().toString(), coinType).thenAccept(balance -> {
            MCEconomyCommandManager.send(player, Component.text()
                .append(Component.text("Your " + coinType + " balance: ", NamedTextColor.GREEN))
                .append(Component.text(balance, NamedTextColor.WHITE))
                .build());
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
