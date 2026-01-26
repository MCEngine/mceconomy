package io.github.mcengine.mceconomy.common.database.sqlite;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.sql.*;

/**
 * SQLite implementation for MCEconomy.
 */
public class MCEconomySQLite implements IMCEconomyDB {
    /**
     * The active SQL connection instance.
     */
    private Connection conn;

    /**
     * Lock object for thread synchronization.
     */
    private final Object lock = new Object();

    /**
     * Constructs a new SQLite database handler.
     * Creates the plugin data folder and database file if they do not exist.
     *
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomySQLite(Plugin plugin) {
        File dataFolder = new File(plugin.getDataFolder(), "mceconomy.db");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the economy_accounts table if it does not already exist in the SQLite file.
     * Uses a composite primary key (account_uuid + account_type).
     *
     * @throws SQLException If an error occurs during table creation.
     */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS economy_accounts (" +
                     "account_uuid TEXT NOT NULL, " +
                     "account_type TEXT NOT NULL, " +
                     "coin INTEGER NOT NULL DEFAULT 0, " +
                     "copper INTEGER NOT NULL DEFAULT 0, " +
                     "silver INTEGER NOT NULL DEFAULT 0, " +
                     "gold INTEGER NOT NULL DEFAULT 0, " +
                     "PRIMARY KEY (account_uuid, account_type))";
        synchronized (lock) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Inserts an account into the database if it does not already exist.
     * Uses INSERT OR IGNORE for SQLite compatibility.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @return true if the operation executed successfully, false if a SQL error occurred.
     */
    @Override
    public boolean ensureAccountExist(String accountUuid, String accountType) {
        String sql = "INSERT OR IGNORE INTO economy_accounts (account_uuid, account_type) VALUES (?, ?)";
        synchronized (lock) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, accountUuid);
                pstmt.setString(2, accountType);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Retrieves the balance of a specific coin type for an account.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @return The amount found in the database, or 0 if an error occurs.
     */
    @Override
    public int getCoin(String accountUuid, String accountType, CurrencyType coinType) {
        synchronized (lock) {
            ensureAccountExist(accountUuid, accountType);
            String sql = "SELECT " + coinType.getName() + " FROM economy_accounts WHERE account_uuid = ? AND account_type = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, accountUuid);
                pstmt.setString(2, accountType);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Sets the balance of a specific coin type for an account to a specific amount.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The new value to set.
     * @return true if the update was successful, false on error.
     */
    @Override
    public boolean setCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        synchronized (lock) {
            ensureAccountExist(accountUuid, accountType);
            String sql = "UPDATE economy_accounts SET " + coinType.getName() + " = ? WHERE account_uuid = ? AND account_type = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, amount);
                pstmt.setString(2, accountUuid);
                pstmt.setString(3, accountType);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Adds an amount to the account's current balance for a specific coin type.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The amount to add.
     * @return true if successful, false on error.
     */
    @Override
    public boolean addCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        synchronized (lock) {
            return setCoin(accountUuid, accountType, coinType, getCoin(accountUuid, accountType, coinType) + amount);
        }
    }

    /**
     * Subtracts an amount from the account's current balance for a specific coin type.
     * Prevents the balance from dropping below 0.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The currency type.
     * @param amount      The amount to subtract.
     * @return true if transaction succeeded, false if insufficient funds or error.
     */
    @Override
    public boolean minusCoin(String accountUuid, String accountType, CurrencyType coinType, int amount) {
        synchronized (lock) {
            int currentBalance = getCoin(accountUuid, accountType, coinType);
            if (currentBalance >= amount) {
                return setCoin(accountUuid, accountType, coinType, currentBalance - amount);
            }
        }
        return false;
    }

    /**
     * Transfers an amount of a specific coin type from one account to another.
     *
     * @param senderUuid   The UUID of the sender.
     * @param senderType   The account type of the sender.
     * @param receiverUuid The UUID of the receiver.
     * @param receiverType The account type of the receiver.
     * @param coinType     The currency type.
     * @param amount       The amount to transfer.
     * @return true if transfer succeeded, false if sender has insufficient funds.
     */
    @Override
    public boolean sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, CurrencyType coinType, int amount) {
        synchronized (lock) {
            int senderBalance = getCoin(senderUuid, senderType, coinType);
            if (senderBalance >= amount) {
                boolean removed = setCoin(senderUuid, senderType, coinType, senderBalance - amount);
                if (removed) {
                    return addCoin(receiverUuid, receiverType, coinType, amount);
                }
            }
        }
        return false;
    }

    /**
     * Closes the SQLite connection and releases the file lock.
     */
    @Override
    public void close() {
        synchronized (lock) {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
