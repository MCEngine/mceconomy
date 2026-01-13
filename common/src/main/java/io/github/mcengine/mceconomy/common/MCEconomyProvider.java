package io.github.mcengine.mceconomy.common;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * High-level provider that acts as a wrapper for the underlying database.
 * <p>
 * This class is platform-agnostic. It does not depend on Bukkit/Spigot directly,
 * instead using an injected {@link Executor} to handle asynchronous operations
 * in a way compatible with Spigot, Paper, and Folia.
 * </p>
 */
public class MCEconomyProvider {

    /**
     * The underlying database implementation (e.g., MySQL or SQLite).
     */
    private final IMCEconomyDB db;

    /**
     * The platform-specific executor used to run database tasks off the main thread.
     */
    private final Executor asyncExecutor;

    /**
     * The default currency identifier used when no specific coin type is provided.
     */
    private static final String DEFAULT_COIN = "coin";

    /**
     * Initializes the provider with a database implementation and an async executor.
     * * @param db            The database logic implementation.
     * @param asyncExecutor The executor (e.g., Bukkit scheduler or Folia async scheduler).
     */
    public MCEconomyProvider(IMCEconomyDB db, Executor asyncExecutor) {
        this.db = db;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Internal helper to wrap blocking database calls into a CompletableFuture.
     *
     * @param <T>      The return type.
     * @param supplier The database operation logic.
     * @return A CompletableFuture tracking the asynchronous task.
     */
    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    // --- GETTERS ---

    /**
     * Gets the balance of the default 'coin' type asynchronously.
     * * @param playerUuid The UUID of the player.
     * @return A Future that completes with the current balance.
     */
    public CompletableFuture<Integer> getCoin(String playerUuid) {
        return getCoin(playerUuid, DEFAULT_COIN);
    }

    /**
     * Gets the balance for a specific coin type asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param coinType   The type of currency (e.g., "gold", "silver").
     * @return A Future that completes with the current balance.
     */
    public CompletableFuture<Integer> getCoin(String playerUuid, String coinType) {
        return runAsync(() -> db.getCoin(playerUuid, coinType));
    }

    // --- SETTERS ---

    /**
     * Sets the balance of the default 'coin' type asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param amount     The new amount to set.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String playerUuid, int amount) {
        return setCoin(playerUuid, DEFAULT_COIN, amount);
    }

    /**
     * Sets the balance for a specific coin type asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param coinType   The type of currency.
     * @param amount     The new amount to set.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> setCoin(String playerUuid, String coinType, int amount) {
        return runAsync(() -> db.setCoin(playerUuid, coinType, amount));
    }

    // --- ADD ---

    /**
     * Adds an amount to the default 'coin' balance asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param amount     Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String playerUuid, int amount) {
        return addCoin(playerUuid, DEFAULT_COIN, amount);
    }

    /**
     * Adds an amount to a specific coin type balance asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param coinType   The type of currency.
     * @param amount     Amount to add.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> addCoin(String playerUuid, String coinType, int amount) {
        return runAsync(() -> db.addCoin(playerUuid, coinType, amount));
    }

    // --- MINUS ---

    /**
     * Subtracts an amount from the default 'coin' balance asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param amount     Amount to subtract.
     * @return A Future that completes with true if successful, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String playerUuid, int amount) {
        return minusCoin(playerUuid, DEFAULT_COIN, amount);
    }

    /**
     * Subtracts an amount from a specific coin type balance asynchronously.
     * * @param playerUuid The UUID of the player.
     * @param coinType   The type of currency.
     * @param amount     Amount to subtract.
     * @return A Future that completes with true if successful, false if insufficient funds.
     */
    public CompletableFuture<Boolean> minusCoin(String playerUuid, String coinType, int amount) {
        return runAsync(() -> db.minusCoin(playerUuid, coinType, amount));
    }

    // --- SEND ---

    /**
     * Sends default 'coin' currency from one player to another asynchronously.
     * * @param senderUuid   Sender player UUID.
     * @param receiverUuid Receiver player UUID.
     * @param amount       Amount to transfer.
     * @return A Future that completes with true if successful.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String receiverUuid, int amount) {
        return sendCoin(senderUuid, receiverUuid, DEFAULT_COIN, amount);
    }

    /**
     * Sends a specific coin type from one player to another asynchronously.
     * * @param senderUuid   Sender player UUID.
     * @param receiverUuid Receiver player UUID.
     * @param coinType     The type of currency.
     * @param amount       Amount to transfer.
     * @return A Future that completes with true if successful, false if sender has insufficient funds.
     */
    public CompletableFuture<Boolean> sendCoin(String senderUuid, String receiverUuid, String coinType, int amount) {
        return runAsync(() -> db.sendCoin(senderUuid, receiverUuid, coinType, amount));
    }

    // --- UTILITY ---

    /**
     * Ensures the player has an entry in the database asynchronously.
     * * @param playerUuid The UUID of the player.
     * @return A Future that completes with true if the player exists or was successfully created.
     */
    public CompletableFuture<Boolean> ensurePlayerExist(String playerUuid) {
        return runAsync(() -> db.ensurePlayerExist(playerUuid));
    }

    /**
     * Properly shuts down the database connection and releases resources.
     */
    public void shutdown() {
        if (db != null) {
            db.close();
        }
    }
}
