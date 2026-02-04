package io.github.mcengine.mceconomy.common.command.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.mcengine.mceconomy.api.command.IEconomyCommandHandle;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Command handler for converting currency into physical items with textures from config.
 */
public class HandleCovert implements IEconomyCommandHandle {

    /**
     * The plugin instance used for scheduling tasks and accessing configuration.
     */
    private final Plugin plugin;

    /**
     * The economy provider used for handling currency transactions.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleCovert command handler.
     *
     * @param plugin   The main plugin instance.
     * @param provider The MCEconomy provider instance.
     */
    public HandleCovert(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    /**
     * Executes the command logic to convert economy balance into physical items.
     * <p>
     * This method validates the sender, permissions, and arguments. It then checks the
     * configuration for the specific texture associated with the requested coin type.
     * Upon a successful transaction (deducting balance), it gives the player a textured
     * player head item.
     * </p>
     *
     * @param sender The source of the command (must be a Player).
     * @param args   The arguments passed to the command (coin type, amount).
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MCEconomyCommandManager.send(sender, Component.translatable("msg.only_players").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;

        if (player.getInventory().firstEmpty() == -1) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.inventory.full").color(NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.usage.covert").color(NamedTextColor.RED));
            return;
        }

        CurrencyType coinType = CurrencyType.fromName(args[0]);
        if (coinType == null) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.invalid.coin").color(NamedTextColor.RED));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.invalid.amount").color(NamedTextColor.RED));
            return;
        }

        String texture = plugin.getConfig().getString(coinType.getName().toLowerCase() + ".texture");
        if (texture == null) {
            MCEconomyCommandManager.send(sender, Component.translatable("mceconomy.msg.error.texture")
                .args(Component.text(coinType.getName()))
                .color(NamedTextColor.RED));
            return;
        }

        // Deduct coin and give item
        provider.minusCoin(player.getUniqueId().toString(), "PLAYER", coinType, amount).thenAccept(success -> {
            if (success) {
                // Switch to main thread for inventory operations
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) item.getItemMeta();

                    if (meta != null) {
                        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
                        profile.setProperty(new ProfileProperty("textures", texture));
                        meta.setPlayerProfile(profile);
                        
                        // Store coin data in PersistentDataContainer
                        NamespacedKey keyType = new NamespacedKey(plugin, "coin_type");
                        NamespacedKey keyAmount = new NamespacedKey(plugin, "coin_amount");
                        
                        meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, coinType.getName());
                        // Store the full amount in the item's data, rather than item count
                        meta.getPersistentDataContainer().set(keyAmount, PersistentDataType.INTEGER, amount);

                        meta.displayName(Component.translatable("mceconomy.item.coin.name")
                            .args(Component.text(coinType.getName()))
                            .color(NamedTextColor.GOLD));
                        item.setItemMeta(meta);
                    }

                    // Secondary check in case inventory filled up during database transaction
                    if (player.getInventory().firstEmpty() == -1) {
                        player.getWorld().dropItem(player.getLocation(), item);
                        MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.inventory.full.drop").color(NamedTextColor.YELLOW));
                    } else {
                        player.getInventory().addItem(item);
                    }

                    MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.success.convert")
                        .args(
                            Component.text(amount),
                            Component.text(coinType.getName())
                        )
                        .color(NamedTextColor.GREEN));
                });
            } else {
                MCEconomyCommandManager.send(player, Component.translatable("mceconomy.msg.insufficient.funds").color(NamedTextColor.RED));
            }
        });
    }

    /**
     * Gets the usage help string for this command.
     *
     * @return The help string.
     */
    @Override
    public String getHelp() {
        return "<coin type> <amount> - Convert currency to item";
    }

    /**
     * @return null as send is available to all players.
     */
    @Override
    public String getPermission() {
        return null;
    }
}
