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
    private final Plugin plugin;
    private final List<Listener> listeners = new ArrayList<>();

    /**
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
     * @return A list of all listeners registered through this manager.
     */
    public List<Listener> getListeners() {
        return listeners;
    }
}