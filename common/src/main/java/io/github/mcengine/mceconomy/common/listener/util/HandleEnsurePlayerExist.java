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
    private final MCEconomyProvider provider;

    /**
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
        
        // ensurePlayerExist is already asynchronous inside the MCEconomyProvider
        provider.ensurePlayerExist(uuid);
    }
}