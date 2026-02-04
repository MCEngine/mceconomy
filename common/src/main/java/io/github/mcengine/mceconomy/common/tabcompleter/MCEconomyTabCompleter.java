package io.github.mcengine.mceconomy.common.tabcompleter;

import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tab completer for the MCEconomy command system.
 * Dynamically suggests subcommands from the Command Manager and common arguments like players/coins.
 */
public class MCEconomyTabCompleter implements TabCompleter {

    /**
     * The command manager used to retrieve registered subcommands and their permissions.
     */
    private final MCEconomyCommandManager manager;

    /**
     * The list of valid coin types available for tab completion suggestions.
     */
    private final List<String> coinTypes;

    /**
     * Constructs a new MCEconomyTabCompleter.
     * @param manager The command manager instance to reference for subcommands.
     */
    public MCEconomyTabCompleter(MCEconomyCommandManager manager) {
        this.manager = manager;
        // Dynamically load values from the Enum
        this.coinTypes = Arrays.stream(CurrencyType.values())
                               .map(CurrencyType::getName)
                               .collect(Collectors.toList());
    }

    /**
     * Handles the tab completion logic for the economy command.
     * @param sender  The source of the command.
     * @param command The command being executed.
     * @param label   The alias of the command used.
     * @param args    The arguments passed to the command.
     * @return A list of suggestions, or an empty list if no suggestions are available.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Argument 1: Subcommands (add, get, set, minus, send, help)
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return manager.getSubcommands().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(input))
                    .filter(entry -> entry.getValue().getPermission() == null || sender.hasPermission(entry.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // Arguments 2+: Context-aware suggestions based on the subcommand
        if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            IEconomyCommandHandle handle = manager.getSubcommands().get(subName);

            // Check if user has permission for this subcommand before suggesting further
            if (handle == null || (handle.getPermission() != null && !sender.hasPermission(handle.getPermission()))) {
                return completions;
            }

            switch (subName) {
                case "get":
                    // /economy get <coin type> [player]
                    if (args.length == 2) return filter(coinTypes, args[1]);
                    
                    // Suggest player at arg 3 if they have permission to see others
                    if (args.length == 3 && sender.hasPermission("mceconomy.get.other")) {
                        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
                    }
                    break;

                case "covert":
                    // /economy covert <coin type> <amount>
                    if (args.length == 2) {
                        return filter(coinTypes, args[1]);
                    }
                    break;

                case "add":
                case "set":
                case "minus":
                case "send":
                    // /economy <cmd> <player> <coin type> <amount>
                    if (args.length == 2) {
                        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
                    }
                    if (args.length == 3) {
                        return filter(coinTypes, args[2]);
                    }
                    break;
            }
        }

        return completions;
    }

    /**
     * Filters a list of strings based on whether they start with the provided input (case-insensitive).
     * @param list  The list of strings to filter.
     * @param input The current user input to match against.
     * @return A filtered list of strings.
     */
    private List<String> filter(List<String> list, String input) {
        String lowerInput = input.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}
