package io.github.mcengine.mceconomy.common.command;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the registration and execution of economy subcommands.
 * Also acts as a central utility for sending Cross-Platform messages.
 */
public class MCEconomyCommandManager implements CommandExecutor {
    /**
     * Storage for registered subcommand handles, keyed by their name.
     */
    private final Map<String, IEconomyCommandHandle> subcommands = new HashMap<>();

    /**
     * Serializer for Spigot (Legacy) platforms that don't support Components natively.
     */
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * Registers a new subcommand handler.
     * Prevents registration if a subcommand with the same name already exists.
     * @param name The subcommand name (e.g., "get").
     * @param handler The logic handler for that command.
     */
    public void register(String name, IEconomyCommandHandle handler) {
        if (subcommands.containsKey(name.toLowerCase())) {
            return;
        }
        subcommands.put(name.toLowerCase(), handler);
    }

    /**
     * Routes the base command to the appropriate subcommand handler.
     * @param sender Source of the command.
     * @param command Command which was executed.
     * @param label Alias of the command which was used.
     * @param args Passed command arguments.
     * @return true always to indicate the command was handled.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Default to help if no args provided
        String subName = (args.length == 0) ? "help" : args[0].toLowerCase();
        IEconomyCommandHandle handle = subcommands.get(subName);

        if (handle != null) {
            String permission = handle.getPermission();
            if (permission != null && !sender.hasPermission(permission)) {
                send(sender, Component.translatable("msg.permission.denied").color(NamedTextColor.RED));
                return true;
            }

            // Clip the array: remove the subcommand name
            String[] subArgs = (args.length <= 1) ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
            handle.invoke(sender, subArgs);
        } else {
            // Args: %s -> subName
            send(sender, Component.translatable("msg.command.unknown").args(Component.text(subName)).color(NamedTextColor.RED));
        }
        return true;
    }

    /**
     * Retrieves the map of all registered subcommands.
     * @return A map containing subcommand names and their respective handles.
     */
    public Map<String, IEconomyCommandHandle> getSubcommands() {
        return subcommands;
    }

    /**
     * Sends a Component to a CommandSender safely across platforms.
     * Detects if the sender is an Adventure Audience (Paper/Folia) or standard Bukkit (Spigot).
     * * @param sender The target to receive the message.
     * @param message The Adventure Component to send.
     */
    public static void send(CommandSender sender, Component message) {
        if (sender instanceof Audience) {
            // Paper, Folia, or Spigot with Adventure-Platform injected
            ((Audience) sender).sendMessage(message);
        } else {
            // Fallback for vanilla Spigot: Serialize to Legacy String (§ codes)
            sender.sendMessage(LEGACY_SERIALIZER.serialize(message));
        }
    }
}
