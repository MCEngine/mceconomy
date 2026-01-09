package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("Only players can check their balance.", NamedTextColor.RED));
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /economy get <coin type>", NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;
        String coinType = args[0].toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int balance = provider.getCoin(player.getUniqueId().toString(), coinType);
            player.sendMessage(Component.text()
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