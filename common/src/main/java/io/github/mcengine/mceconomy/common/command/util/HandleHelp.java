package io.github.mcengine.mceconomy.common.command.util;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Command handler for displaying the help menu.
 */
public class HandleHelp implements IEconomyCommandHandle {
    /**
     * The manager used to retrieve registered subcommands.
     */
    private final MCEconomyCommandManager manager;

    /**
     * Constructs a new HandleHelp instance.
     * @param manager The command manager.
     */
    public HandleHelp(MCEconomyCommandManager manager) {
        this.manager = manager;
    }

    /**
     * Executes the help command logic.
     * Displays an interactive list of commands that players can click to autofill.
     * @param sender The sender of the command.
     * @param args The command arguments.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        sender.sendMessage(Component.text("--- MCEconomy Help ---", NamedTextColor.GOLD, TextDecoration.BOLD));

        for (Map.Entry<String, IEconomyCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            IEconomyCommandHandle handle = entry.getValue();
            
            if (name.equalsIgnoreCase("help")) continue;

            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                String fullCommand = "/economy " + name;

                sender.sendMessage(Component.text()
                    .append(Component.text(fullCommand + " ", NamedTextColor.YELLOW)
                        // Click to autofill the command in the chat bar
                        .clickEvent(ClickEvent.suggestCommand(fullCommand + " "))
                        // Show a tooltip when hovering
                        .hoverEvent(HoverEvent.showText(Component.text("Click to autofill command", NamedTextColor.GREEN))))
                    .append(Component.text(handle.getHelp(), NamedTextColor.GRAY))
                    .build());
            }
        }
    }

    /**
     * @return The help description for the help command.
     */
    @Override
    public String getHelp() { return "- View this help menu"; }

    /**
     * @return null as help is available to all players.
     */
    @Override
    public String getPermission() { return null; }
}
