package io.github.mcengine.mceconomy.common.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the registration of all economy-related event listeners.
 */
public class MCEconomyListenerManager {
    /**
     * The plugin instance used to register events to the server.
     */
    private final Plugin plugin;

    /**
     * Internal list to track all listeners registered through this manager.
     */
    private final List<Listener> listeners = new ArrayList<>();

    /**
     * Constructs a new MCEconomyListenerManager.
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomyListenerManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a listener to the server and tracks it internally.
     * @param listener The listener implementation to register.
     */
    public void register(Listener listener) {
        listeners.add(listener);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    /**
     * Retrieves all listeners registered through this manager.
     * @return A list of registered listeners.
     */
    public List<Listener> getListeners() {
        return listeners;
    }
}
