package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class HandleHelp implements IEconomyCommandHandle {
    private final MCEconomyCommandManager manager;

    public HandleHelp(MCEconomyCommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("--- MCEconomy Help ---", NamedTextColor.GOLD, TextDecoration.BOLD));

        for (Map.Entry<String, IEconomyCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            IEconomyCommandHandle handle = entry.getValue();
            
            if (name.equalsIgnoreCase("help")) continue;

            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                sender.sendMessage(Component.text()
                    .append(Component.text("/economy " + name + " ", NamedTextColor.YELLOW))
                    .append(Component.text(handle.getHelp(), NamedTextColor.GRAY))
                    .build());
            }
        }
    }

    @Override
    public String getHelp() { return "- View this help menu"; }

    @Override
    public String getPermission() { return null; }
}