package io.github.mcengine.mceconomy.common.database.mysql;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import org.bukkit.plugin.Plugin;
import java.sql.*;

/**
 * MySQL implementation for MCEconomy.
 */
public class MCEconomyMySQL implements IMCEconomyDB {
    /**
     * The active SQL connection instance.
     */
    private Connection conn;

    /**
     * Constructs a new MySQL database handler.
     * Initializes connection settings from the plugin configuration and attempts to connect.
     *
     * @param plugin The Bukkit/Spigot plugin instance.
     */
    public MCEconomyMySQL(Plugin plugin) {
        String dbUser = plugin.getConfig().getString("db.mysql.user");
        String dbPass = plugin.getConfig().getString("db.mysql.password");
        String dbHost = plugin.getConfig().getString("db.mysql.host");
        String dbName = plugin.getConfig().getString("db.mysql.database");
        String dbPort = plugin.getConfig().getString("db.mysql.port", "3306");
        String dbSsl = plugin.getConfig().getString("db.mysql.ssl", "false");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=" + dbSsl, dbUser, dbPass);
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the economy_accounts table if it does not already exist.
     * Uses a composite primary key (account_uuid + account_type).
     *
     * @throws SQLException If an error occurs during table creation.
     */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS economy_accounts (" +
                     "account_uuid VARCHAR(36) NOT NULL, " +
                     "account_type VARCHAR(32) NOT NULL, " +
                     "coin BIGINT NOT NULL DEFAULT 0, " +
                     "copper BIGINT NOT NULL DEFAULT 0, " +
                     "silver BIGINT NOT NULL DEFAULT 0, " +
                     "gold BIGINT NOT NULL DEFAULT 0, " +
                     "PRIMARY KEY (account_uuid, account_type))";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Validates if the provided coin type is a valid column name.
     * Use this to prevent SQL Injection attacks via column name manipulation.
     *
     * @param coinType The coin type string to check.
     * @return true if valid, false otherwise.
     */
    private boolean isValidCoinType(String coinType) {
        if (coinType == null) return false;
        return coinType.equalsIgnoreCase("coin") ||
               coinType.equalsIgnoreCase("copper") ||
               coinType.equalsIgnoreCase("silver") ||
               coinType.equalsIgnoreCase("gold");
    }

    /**
     * Inserts an account into the database if it does not already exist.
     * Uses INSERT IGNORE to handle existing primary keys gracefully.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @return true if the operation executed successfully, false if a SQL error occurred.
     */
    @Override
    public boolean ensureAccountExist(String accountUuid, String accountType) {
        String sql = "INSERT IGNORE INTO economy_accounts (account_uuid, account_type) VALUES (?, ?)";
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

    /**
     * Retrieves the balance of a specific coin type for an account.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The column name (coin, copper, silver, gold).
     * @return The amount found in the database, or 0 if an error occurs.
     */
    @Override
    public int getCoin(String accountUuid, String accountType, String coinType) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);

        ensureAccountExist(accountUuid, accountType);
        String sql = "SELECT " + coinType + " FROM economy_accounts WHERE account_uuid = ? AND account_type = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountUuid);
            pstmt.setString(2, accountType);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Sets the balance of a specific coin type for an account to a specific amount.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The column name.
     * @param amount      The new value to set.
     * @return true if the update was successful, false on error.
     */
    @Override
    public boolean setCoin(String accountUuid, String accountType, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);

        ensureAccountExist(accountUuid, accountType);
        String sql = "UPDATE economy_accounts SET " + coinType + " = ? WHERE account_uuid = ? AND account_type = ?";
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

    /**
     * Adds an amount to the account's current balance for a specific coin type.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The column name.
     * @param amount      The amount to add.
     * @return true if successful, false on error.
     */
    @Override
    public boolean addCoin(String accountUuid, String accountType, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        return setCoin(accountUuid, accountType, coinType, getCoin(accountUuid, accountType, coinType) + amount);
    }

    /**
     * Subtracts an amount from the account's current balance for a specific coin type.
     * Prevents the balance from dropping below 0.
     *
     * @param accountUuid The UUID of the account.
     * @param accountType The type of account.
     * @param coinType    The column name.
     * @param amount      The amount to subtract.
     * @return true if transaction succeeded, false if insufficient funds or error.
     */
    @Override
    public boolean minusCoin(String accountUuid, String accountType, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        int currentBalance = getCoin(accountUuid, accountType, coinType);
        if (currentBalance >= amount) {
            return setCoin(accountUuid, accountType, coinType, currentBalance - amount);
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
     * @param coinType     The column name.
     * @param amount       The amount to transfer.
     * @return true if transfer succeeded, false if sender has insufficient funds.
     */
    @Override
    public boolean sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, String coinType, int amount) {
        if (!isValidCoinType(coinType)) throw new IllegalArgumentException("Invalid coin type: " + coinType);
        int senderBalance = getCoin(senderUuid, senderType, coinType);
        if (senderBalance >= amount) {
            boolean removed = setCoin(senderUuid, senderType, coinType, senderBalance - amount);
            if (removed) {
                return addCoin(receiverUuid, receiverType, coinType, amount);
            }
        }
        return false;
    }

    /**
     * Closes the MySQL connection and releases resources.
     */
    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
