package io.github.mcengine.mceconomy.common.listener.util;

import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Listener that handles the redemption of physical coin items.
 * When a player right-clicks an item with valid coin data, the money is added to their balance.
 */
public class HandleCoinItem implements Listener {

    private final Plugin plugin;
    private final MCEconomyProvider provider;

    public HandleCoinItem(Plugin plugin, MCEconomyProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey keyType = new NamespacedKey(plugin, "coin_type");
        NamespacedKey keyAmount = new NamespacedKey(plugin, "coin_amount");

        // Check if item has the specific economy keys
        if (!pdc.has(keyType, PersistentDataType.STRING) || !pdc.has(keyAmount, PersistentDataType.INTEGER)) {
            return;
        }

        // Cancel the event to prevent placing the head or normal interaction
        event.setCancelled(true);

        String typeName = pdc.get(keyType, PersistentDataType.STRING);
        Integer value = pdc.get(keyAmount, PersistentDataType.INTEGER);

        if (typeName == null || value == null) {
            return;
        }

        CurrencyType currency = CurrencyType.fromName(typeName);
        if (currency == null) {
            event.getPlayer().sendMessage(Component.translatable("mceconomy.msg.error.item.data").color(NamedTextColor.RED));
            return;
        }

        // Deduct item from hand
        item.subtract(1);

        // Add money to player
        provider.addCoin(event.getPlayer().getUniqueId().toString(), "PLAYER", currency, value).thenAccept(success -> {
            if (success) {
                event.getPlayer().sendMessage(Component.translatable("mceconomy.msg.success.redeem")
                    .args(
                        Component.text(value),
                        Component.text(currency.getName())
                    )
                    .color(NamedTextColor.GREEN));
            } else {
                event.getPlayer().sendMessage(Component.translatable("mceconomy.msg.error.redeem").color(NamedTextColor.RED));
            }
        });
    }
}
