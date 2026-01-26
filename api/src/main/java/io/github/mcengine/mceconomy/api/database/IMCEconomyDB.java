package io.github.mcengine.mceconomy.api.database;

import io.github.mcengine.mceconomy.api.enums.CurrencyType;

/**
 * Interface for MCEconomy database operations.
 * Supports multiple account types (Player, Clan, Guild) and coin types.
 */
public interface IMCEconomyDB {

    /**
     * Retrieves the balance for a specific coin type.
     * @param accountUuid The UUID of the account (Player, Clan, Guild).
     * @param accountType The type of account (e.g., "PLAYER", "CLAN", "GUILD").
     * @param coinType The currency type (coin, copper, silver, gold).
     * @return The amount stored in the database.
     */
    int getCoin(String accountUuid, String accountType, CurrencyType coinType);

    /**
     * Sets an account's balance to a specific amount.
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType The currency type.
     * @param amount The new balance.
     * @return true if the update was successful, false otherwise.
     */
    boolean setCoin(String accountUuid, String accountType, CurrencyType coinType, int amount);

    /**
     * Increases an account's balance.
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType The currency type.
     * @param amount The amount to add.
     * @return true if the addition was successful, false otherwise.
     */
    boolean addCoin(String accountUuid, String accountType, CurrencyType coinType, int amount);

    /**
     * Decreases an account's balance. 
     * Implementations should check for sufficient funds.
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType The currency type.
     * @param amount The amount to subtract.
     * @return true if the subtraction was successful, false if insufficient funds or error.
     */
    boolean minusCoin(String accountUuid, String accountType, CurrencyType coinType, int amount);

    /**
     * Transfers coins from one account to another.
     * @param senderUuid The UUID of the sender.
     * @param senderType The account type of the sender.
     * @param receiverUuid The UUID of the receiver.
     * @param receiverType The account type of the receiver.
     * @param coinType The currency type.
     * @param amount The amount to transfer.
     * @return true if the transfer was completed successfully, false otherwise.
     */
    boolean sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, CurrencyType coinType, int amount);

    /**
     * Checks if an account exists in the database; if not, creates a record with default values.
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account (e.g., "PLAYER", "CLAN").
     * @return true if the account exists or was successfully created, false if a database error occurred.
     */
    boolean ensureAccountExist(String accountUuid, String accountType);

    /**
     * Closes the database connection safely.
     */
    void close();
}
