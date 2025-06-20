package com.vifinancenews.common.daos;

import com.vifinancenews.common.config.DatabaseConfig;
import com.vifinancenews.common.models.Identifier;
import com.vifinancenews.common.utilities.IDHash;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IdentifierDAO {

    public static Identifier getIdentifierByEmail(String email) throws SQLException {
        String query = "SELECT id, email, password_hash, login_method, created_at, last_login, failed_attempts, lockout_until FROM identifier WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Identifier(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("login_method"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null,
                        rs.getInt("failed_attempts"),
                        rs.getTimestamp("lockout_until") != null ? rs.getTimestamp("lockout_until").toLocalDateTime() : null
                    );
                }
            }
        }
        return null;
    }

    public static Identifier getIdentifierByAccountId(String accountId) throws SQLException {
        String query = """
            SELECT i.id, i.email, i.password_hash, i.login_method, i.created_at, i.last_login,
                   i.failed_attempts, i.lockout_until
            FROM identifier i
            JOIN account a ON i.id_hash = a.user_id
            WHERE a.id = ?
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
    
            pstmt.setString(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Identifier(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("login_method"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null,
                        rs.getInt("failed_attempts"),
                        rs.getTimestamp("lockout_until") != null ? rs.getTimestamp("lockout_until").toLocalDateTime() : null
                    );
                }
            }
        }
        return null;
    }

    public static void updateFailedAttempts(String email, int failedAttempts, LocalDateTime lockoutUntil) throws SQLException {
        String query = "UPDATE identifier SET failed_attempts = ?, lockout_until = ? WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, failedAttempts);
            pstmt.setTimestamp(2, lockoutUntil != null ? Timestamp.valueOf(lockoutUntil) : null);
            pstmt.setString(3, email);
            pstmt.executeUpdate();
        }
    }

    public static void updateLastLogin(String email) throws SQLException {
        String query = "UPDATE identifier SET last_login = ?, failed_attempts = 0 WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(2, email);
            pstmt.executeUpdate();
        }
    }

    public static void resetFailedAttempts(String email) throws SQLException {
        String query = "UPDATE identifier SET failed_attempts = 0 WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
        }
    }

    public static Identifier insertIdentifier(String email, String passwordHash, String loginMethod) throws SQLException {
        UUID userId = UUID.randomUUID(); // UUID is generated in Java for now
        LocalDateTime createdAt = LocalDateTime.now();

        String query = "INSERT INTO identifier (id, email, password_hash, login_method, created_at, last_login, failed_attempts, lockout_until) " +
                       "VALUES (?, ?, ?, ?, ?, NULL, 0, NULL)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, userId.toString());
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);
            pstmt.setString(4, loginMethod);
            pstmt.setTimestamp(5, Timestamp.valueOf(createdAt));

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                return new Identifier(userId, email, passwordHash, loginMethod, createdAt, null, 0, null);
            }
        }
        return null;
    }

    public static boolean deleteIdentifierByEmail(String email) throws SQLException {
        String query = "DELETE FROM identifier WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public static boolean deleteIdentifierByUserId(UUID identifierId) throws SQLException {
        String query = "DELETE FROM identifier WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setObject(1, identifierId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public static boolean deleteExpiredIdentifiers(int days) throws SQLException {
        String expiredAccountsQuery = "SELECT user_id FROM deleted_accounts WHERE deleted_at < NOW() - INTERVAL '" + days + " days'";
        Set<String> expiredUserIds = new HashSet<>();
    
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(expiredAccountsQuery);
             ResultSet rs = stmt.executeQuery()) {
    
            while (rs.next()) {
                expiredUserIds.add(rs.getString("user_id"));
            }
        }
    
        if (expiredUserIds.isEmpty()) return false;
    
        String identifiersQuery = "SELECT id FROM identifier";
        List<UUID> idsToDelete = new ArrayList<>();
    
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(identifiersQuery);
             ResultSet rs = stmt.executeQuery()) {
    
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                String hashed = IDHash.hashUUID(id);
                if (expiredUserIds.contains(hashed)) {
                    idsToDelete.add(id);
                }
            }
        }
    
        if (idsToDelete.isEmpty()) return false;
    
        // Build SQL DELETE query using IN clause
        String deleteQuery = "DELETE FROM identifier WHERE id = ANY (?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
    
            UUID[] uuidArray = idsToDelete.toArray(new UUID[0]);
            Array sqlArray = conn.createArrayOf("UUID", uuidArray);
            stmt.setArray(1, sqlArray);
    
            return stmt.executeUpdate() > 0;
        }
    }
    

    public static boolean updatePassword(String email, String newPasswordHash) throws SQLException {
        String query = "UPDATE identifier SET password_hash = ?, failed_attempts = 0, lockout_until = NULL WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
    
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, email);
    
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public static boolean changePassword(UUID userId, String newPasswordHash) throws SQLException {
        String query = "UPDATE identifier SET password_hash = ?, failed_attempts = 0, lockout_until = NULL WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
    
            pstmt.setString(1, newPasswordHash);
            pstmt.setObject(2, userId);
    
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    
}
