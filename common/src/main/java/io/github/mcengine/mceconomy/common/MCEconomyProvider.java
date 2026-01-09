package io.github.mcengine.mceconomy.common;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.common.database.mysql.MCEconomyMySQL;
import io.github.mcengine.mceconomy.common.database.sqlite.MCEconomySQLite;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.CompletableFuture;

/**
 * High-level provider that acts as a wrapper for the underlying database.
 * Methods that modify data are executed asynchronously and return Futures to track success.
 */
public class MCEconomyProvider {
    private final IMCEconomyDB db;
    private final Plugin plugin;
    private static final String DEFAULT_COIN = "coin";

    /**
     * Initializes the provider based on the config.yml settings.
     * @param plugin The Spigot Plugin instance.
     */
    public MCEconomyProvider(Plugin plugin) {
        this.plugin = plugin;
        String dbType = plugin.getConfig().getString("db.type", "sqlite").toLowerCase();
        
        switch (dbType) {
            case "mysql":
                this.db = new MCEconomyMySQL(plugin);
                break;
            case "sqlite":
            default:
                this.db = new MCEconomySQLite(plugin);
                break;
        }
    }

    // --- GETTERS (Synchronous) ---
    // Note: Getters remain sync because they return a value. 
    // Always call these from an async task in your own code to avoid lag.

    /**
     * Gets the balance of the default 'coin' type.
     * @param playerUuid The UUID of the player.
     * @return Current balance.
     */
    public int getCoin(String playerUuid) {
        return db.getCoin(playerUuid, DEFAULT_COIN);
    }

    /**
     * Gets the balance for a specific coin type.
     * @param playerUuid The UUID of the player.
     * @param coinType The type (coin, copper, silver, gold).
     * @return Current balance.
     */
    public int getCoin(String playerUuid, String coinType) {
        return db.getCoin(playerUuid, coinType);
    }

    // --- SETTERS (Asynchronous with Future) ---

    /**
     * Sets the balance of the default 'coin' type asynchronously.
     * @param playerUuid The UUID of the player.
     * @param amount The new amount.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String playerUuid, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.setCoin(playerUuid, DEFAULT_COIN, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    /**
     * Sets the balance for a specific coin type asynchronously.
     * @param playerUuid The UUID of the player.
     * @param coinType The type.
     * @param amount The new amount.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String playerUuid, String coinType, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.setCoin(playerUuid, coinType, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    // --- ADD (Asynchronous with Future) ---

    /**
     * Adds an amount to the default 'coin' balance asynchronously.
     * @param playerUuid The UUID of the player.
     * @param amount Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String playerUuid, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.addCoin(playerUuid, DEFAULT_COIN, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    /**
     * Adds an amount to a specific coin type asynchronously.
     * @param playerUuid The UUID of the player.
     * @param coinType The type.
     * @param amount Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String playerUuid, String coinType, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.addCoin(playerUuid, coinType, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    // --- MINUS (Asynchronous with Future) ---

    /**
     * Subtracts an amount from the default 'coin' balance asynchronously.
     * @param playerUuid The UUID of the player.
     * @param amount Amount to subtract.
     * @return A Future that completes with true if transaction succeeded, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String playerUuid, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.minusCoin(playerUuid, DEFAULT_COIN, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    /**
     * Subtracts an amount from a specific coin type asynchronously.
     * @param playerUuid The UUID of the player.
     * @param coinType The type.
     * @param amount Amount to subtract.
     * @return A Future that completes with true if transaction succeeded, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String playerUuid, String coinType, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.minusCoin(playerUuid, coinType, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    // --- SEND (Asynchronous with Future) ---

    /**
     * Sends default 'coin' currency from one player to another asynchronously.
     * @param senderUuid Sender player UUID.
     * @param receiverUuid Receiver player UUID.
     * @param amount Amount to transfer.
     * @return A Future that completes with true if successful, false if sender has insufficient funds.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String receiverUuid, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.sendCoin(senderUuid, receiverUuid, DEFAULT_COIN, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    /**
     * Sends a specific coin type from one player to another asynchronously.
     * @param senderUuid Sender player UUID.
     * @param receiverUuid Receiver player UUID.
     * @param coinType The type.
     * @param amount Amount to transfer.
     * @return A Future that completes with true if successful, false if sender has insufficient funds.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String receiverUuid, String coinType, int amount) {
        return CompletableFuture.supplyAsync(
            () -> db.sendCoin(senderUuid, receiverUuid, coinType, amount),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    // --- UTILITY ---

    /**
     * Ensures the player has an entry in the database asynchronously.
     * @param playerUuid The UUID of the player.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> ensurePlayerExist(String playerUuid) {
        return CompletableFuture.supplyAsync(
            () -> db.ensurePlayerExist(playerUuid),
            task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        );
    }

    /**
     * Properly shuts down the database connection.
     * Should be called in the Plugin's onDisable().
     */
    public void shutdown() {
        if (db != null) {
            db.close();
        }
    }
}
