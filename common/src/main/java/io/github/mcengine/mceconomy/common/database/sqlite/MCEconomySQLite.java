package io.github.mcengine.mceconomy.common.database.sqlite;

import io.github.mcengine.mceconomy.api.database.IMCEconomyDB;
import io.github.mcengine.mceconomy.api.enums.CurrencyType;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.sql.*;
import java.util.Objects;

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
        String dbPath = envOrConfig("MCENGINE_MCECONOMY_SQLITE_PATH",
                                    "MCENGINE_SQLITE_PATH",
                                    "db.sqlite.path",
                                    plugin,
                                    "mceconomy.db");

        File dataFolder = resolvePath(plugin, dbPath);
        File parentDir = dataFolder.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();

        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolve configuration by preferring environment variables then falling back to plugin config.
     */
    private String envOrConfig(String primaryEnv, String secondaryEnv, String configPath, Plugin plugin, String defaultValue) {
        String value = System.getenv(primaryEnv);
        if (value != null && !value.isEmpty()) return value;

        value = System.getenv(secondaryEnv);
        if (value != null && !value.isEmpty()) return value;

        value = plugin.getConfig().getString(configPath);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }

    /**
     * Resolve a potentially absolute path; if relative, place inside the plugin data folder.
     */
    private File resolvePath(Plugin plugin, String path) {
        File candidate = new File(path);
        if (candidate.isAbsolute()) {
            return candidate;
        }
        return new File(plugin.getDataFolder(), path);
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
            String col = columnName(coinType);
            ensureAccountExist(accountUuid, accountType);
            String sql = "SELECT " + col + " FROM economy_accounts WHERE account_uuid = ? AND account_type = ?";
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
            if (amount < 0) return false;
            String col = columnName(coinType);
            ensureAccountExist(accountUuid, accountType);
            String sql = "UPDATE economy_accounts SET " + col + " = ? WHERE account_uuid = ? AND account_type = ?";
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
            if (amount <= 0) return false;
            int newAmount = getCoin(accountUuid, accountType, coinType) + amount;
            return setCoin(accountUuid, accountType, coinType, newAmount);
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
            if (amount <= 0) return false;
            String col = columnName(coinType);
            String sql = "UPDATE economy_accounts SET " + col + " = " + col + " - ? " +
                         "WHERE account_uuid = ? AND account_type = ? AND " + col + " >= ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, amount);
                pstmt.setString(2, accountUuid);
                pstmt.setString(3, accountType);
                pstmt.setInt(4, amount);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
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
     * @return true if the transfer was successful, false on error.
     */
    @Override
    public boolean sendCoin(String senderUuid, String senderType, String receiverUuid, String receiverType, CurrencyType coinType, int amount) {
        synchronized (lock) {
            if (amount <= 0) return false;
            String col = columnName(coinType);
            boolean prevAutoCommit = true;
            try {
                prevAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                ensureAccountExist(senderUuid, senderType);
                ensureAccountExist(receiverUuid, receiverType);

                String withdrawSql = "UPDATE economy_accounts SET " + col + " = " + col + " - ? " +
                                     "WHERE account_uuid = ? AND account_type = ? AND " + col + " >= ?";
                try (PreparedStatement withdraw = conn.prepareStatement(withdrawSql)) {
                    withdraw.setInt(1, amount);
                    withdraw.setString(2, senderUuid);
                    withdraw.setString(3, senderType);
                    withdraw.setInt(4, amount);
                    if (withdraw.executeUpdate() == 0) {
                        conn.rollback();
                        conn.setAutoCommit(prevAutoCommit);
                        return false;
                    }
                }

                String depositSql = "UPDATE economy_accounts SET " + col + " = " + col + " + ? WHERE account_uuid = ? AND account_type = ?";
                try (PreparedStatement deposit = conn.prepareStatement(depositSql)) {
                    deposit.setInt(1, amount);
                    deposit.setString(2, receiverUuid);
                    deposit.setString(3, receiverType);
                    deposit.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(prevAutoCommit);
                return true;
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                try { conn.setAutoCommit(prevAutoCommit); } catch (SQLException ignored) {}
                e.printStackTrace();
                return false;
            }
        }
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

    /**
     * Resolve the trusted column name for a currency type.
     */
    private String columnName(CurrencyType type) {
        Objects.requireNonNull(type, "currency type");
        return switch (type) {
            case COIN -> "coin";
            case COPPER -> "copper";
            case SILVER -> "silver";
            case GOLD -> "gold";
        };
    }
}
