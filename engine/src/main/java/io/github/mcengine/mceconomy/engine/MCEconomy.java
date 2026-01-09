package io.github.mcengine.mceconomy.engine;

import io.github.mcengine.mceconomy.common.MCEconomyProvider;
import io.github.mcengine.mceconomy.common.command.MCEconomyCommandManager;
import io.github.mcengine.mceconomy.common.command.util.*;
import io.github.mcengine.mceconomy.common.listener.MCEconomyListenerManager;
import io.github.mcengine.mceconomy.common.listener.util.HandleEnsurePlayerExist;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for MCEconomy.
 * Handles the lifecycle and registration of the economy system.
 */
public class MCEconomy extends JavaPlugin {

    private MCEconomyProvider provider;
    private MCEconomyCommandManager commandManager;
    private MCEconomyListenerManager listenerManager;

    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        saveDefaultConfig();

        // 2. Initialize Core Components
        this.provider = new MCEconomyProvider(this);
        this.commandManager = new MCEconomyCommandManager();
        this.listenerManager = new MCEconomyListenerManager(this);

        // 3. Register Managers as Bukkit Services
        // This allows Addons to find these instances and register their own logic
        Bukkit.getServicesManager().register(MCEconomyProvider.class, provider, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCEconomyCommandManager.class, commandManager, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(MCEconomyListenerManager.class, listenerManager, this, ServicePriority.Normal);

        // 4. Setup Commands and Listeners
        registerSubcommands();
        getCommand("economy").setExecutor(commandManager);
        registerListeners();

        getLogger().info("MCEconomy Engine has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown database connections
        if (provider != null) {
            provider.shutdown();
        }
        getLogger().info("MCEconomy Engine has been disabled!");
    }

    /**
     * Registers all subcommand handlers to the command manager.
     */
    private void registerSubcommands() {
        commandManager.register("help", new HandleHelp(commandManager));
        commandManager.register("get", new HandleGet(this, provider));
        commandManager.register("add", new HandleAdd(this, provider));
        commandManager.register("minus", new HandleMinus(this, provider));
        commandManager.register("set", new HandleSet(this, provider));
        commandManager.register("send", new HandleSend(this, provider));
    }

    /**
     * Registers all event listeners to the listener manager.
     */
    private void registerListeners() {
        listenerManager.register(new HandleEnsurePlayerExist(provider));
    }

    /**
     * @return The active economy provider.
     */
    public MCEconomyProvider getProvider() {
        return this.provider;
    }

    /**
     * @return The command manager for subcommands.
     */
    public MCEconomyCommandManager getCommandManager() {
        return this.commandManager;
    }

    /**
     * @return The listener manager for internal events.
     */
    public MCEconomyListenerManager getListenerManager() {
        return this.listenerManager;
    }
}