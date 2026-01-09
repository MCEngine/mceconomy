package io.github.mcengine.mceconomy.common.listener.util;

import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener to ensure a player has a record in the database upon joining.
 */
public class HandleEnsurePlayerExist implements Listener {
    /**
     * The economy provider for data operations.
     */
    private final MCEconomyProvider provider;

    /**
     * Constructs a new HandleEnsurePlayerExist instance.
     * @param provider The economy provider to handle database operations.
     */
    public HandleEnsurePlayerExist(MCEconomyProvider provider) {
        this.provider = provider;
    }

    /**
     * Triggers when a player joins. 
     * Uses MONITOR priority to ensure it runs even if other plugins cancel events, 
     * as we just want to ensure the DB record exists.
     * * @param event The PlayerJoinEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        
        // ensurePlayerExist is asynchronous inside the MCEconomyProvider.
        // We use thenAccept to log a warning if the database record could not be created.
        provider.ensurePlayerExist(uuid).thenAccept(success -> {
            if (!success) {
                // You could log this to console or notify admins
                System.err.println("[MCEconomy] Failed to ensure database record for: " + event.getPlayer().getName());
            }
        });
    }
}
