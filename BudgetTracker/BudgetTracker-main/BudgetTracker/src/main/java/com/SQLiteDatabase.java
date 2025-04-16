package com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SQLiteDatabase {
    private static final String DB_URL = "jdbc:sqlite:budgettracker.db";

    // Initialize database with required tables
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create transactions table if it doesn't exist
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "category TEXT NOT NULL," +
                            "amount REAL NOT NULL," +
                            "date TEXT NOT NULL," +
                            "type TEXT NOT NULL" + // 'income' or 'expense'
                            ")"
            );

            // Create balance table if it doesn't exist
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS balance (" +
                            "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                            "balance REAL NOT NULL" +
                            ")"
            );

            // Check if balance record exists, if not insert initial balance of 0
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM balance");
            if (rs.next() && rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO balance (id, balance) VALUES (1, 0.0)");
            }

            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Connect to SQLite database
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Insert transaction into the database
    public static boolean insertTransaction(String category, double amount, String date, boolean isIncome) {
        String type = isIncome ? "income" : "expense";
        String query = "INSERT INTO transactions (category, amount, date, type) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, category);
            stmt.setDouble(2, Math.abs(amount)); // Store positive values
            stmt.setString(3, date);
            stmt.setString(4, type);

            int rowsAffected = stmt.executeUpdate();

            // Update balance
            if (rowsAffected > 0) {
                updateBalanceForTransaction(Math.abs(amount), isIncome);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error inserting transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get current balance
    public static double getCurrentBalance() {
        double balance = 0.0;
        String query = "SELECT balance FROM balance WHERE id = 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                balance = rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.err.println("Error getting current balance: " + e.getMessage());
            e.printStackTrace();
        }
        return balance;
    }

    // Update balance
    public static boolean updateBalance(double newBalance) {
        String query = "UPDATE balance SET balance = ? WHERE id = 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, newBalance);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating balance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void updateBalanceForTransaction(double amount, boolean isIncome) {
        double currentBalance = getCurrentBalance();
        double newBalance = isIncome ? currentBalance + amount : currentBalance - amount;
        updateBalance(newBalance);
    }

    // Get all transactions
    public static List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT id, category, amount, date, type FROM transactions ORDER BY date DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                String date = rs.getString("date");
                String type = rs.getString("type");

                if ("expense".equals(type)) {
                    amount = -amount;
                }

                transactions.add(new Transaction(id, category, amount, date));
            }
        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }
    public static boolean deleteTransaction(int id) {
        String getQuery = "SELECT amount, type FROM transactions WHERE id = ?";
        String deleteQuery = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement getStmt = conn.prepareStatement(getQuery)) {
                getStmt.setInt(1, id);

                try (ResultSet rs = getStmt.executeQuery()) {
                    if (rs.next()) {
                        double amount = rs.getDouble("amount");
                        boolean isIncome = "income".equals(rs.getString("type"));

                        // Delete the transaction
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                            deleteStmt.setInt(1, id);
                            deleteStmt.executeUpdate();

                            double currentBalance = getCurrentBalance();
                            double newBalance = isIncome ?
                                    currentBalance - amount : // Subtract if was income
                                    currentBalance + amount;  // Add back if was expense
                            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE balance SET balance = ? WHERE id = 1")) {
                                updateStmt.setDouble(1, newBalance);
                                updateStmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            // Commit transaction
            conn.commit();
            updateBalance(getCurrentBalance());

            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static Map<String, Double> getMonthlyNetTotals() {
        Map<String, Double> monthlyNet = new LinkedHashMap<>();
        String query = "SELECT strftime('%Y-%m', date) as month, " +
                "SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END) as net " +
                "FROM transactions " +
                "GROUP BY month " +
                "ORDER BY month DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                monthlyNet.put(rs.getString("month"), rs.getDouble("net"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting monthly net totals: " + e.getMessage());
        }
        return monthlyNet;
    }

    public static List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT id, category, amount, date, type FROM transactions " +
                "ORDER BY date DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String category = rs.getString("category");
                    double amount = rs.getDouble("amount");
                    String date = rs.getString("date");
                    String type = rs.getString("type");

                    if ("expense".equals(type)) {
                        amount = -amount;
                    }

                    transactions.add(new Transaction(id, category, amount, date));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent transactions: " + e.getMessage());
        }
        return transactions;
    }
    public static Map<String, Double> getCategoryTotalsForMonth(boolean isIncome, String month) {
        Map<String, Double> categoryTotals = new HashMap<>();
        String type = isIncome ? "income" : "expense";
        String query = "SELECT category, SUM(amount) as total " +
                "FROM transactions " +
                "WHERE type = ? AND strftime('%Y-%m', date) = ? " +
                "GROUP BY category";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, type);
            stmt.setString(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categoryTotals.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting category totals for month: " + e.getMessage());
        }
        return categoryTotals;
    }

}