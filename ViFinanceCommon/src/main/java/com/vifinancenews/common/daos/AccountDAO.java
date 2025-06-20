package com.vifinancenews.common.daos;

import com.vifinancenews.common.config.DatabaseConfig;
import com.vifinancenews.common.models.Account;
import com.vifinancenews.common.utilities.IDHash;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AccountDAO {

    // Method to insert a new account
    public static Account insertAccount(UUID identifierId, String userName, String avatarLink, String bio) throws SQLException {
        String hashedId = IDHash.hashUUID(identifierId);

        // Use ? placeholders for prepared statement
        String query = "INSERT INTO account (user_id, username, avatar_link, bio) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, hashedId);      // user_id (hashedId)
            pstmt.setString(2, userName);      // username
            pstmt.setString(3, avatarLink);    // avatar_link (can be null)
            pstmt.setString(4, bio);           // bio

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Rows inserted into account: " + rowsInserted);
            if (rowsInserted > 0) {
                return new Account(hashedId, userName, avatarLink, bio);
            }
        }
        return null;
    }

    // Method to get an account by user ID
    public static Account getAccountByUserId(UUID userId) throws SQLException {
        String hashedUserId = IDHash.hashUUID(userId);
        String query = "SELECT user_id, username, avatar_link, bio FROM account WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, hashedUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                        rs.getString("user_id"),
                        rs.getString("username"),
                        rs.getString("avatar_link"),
                        rs.getString("bio")
                    );
                }
            }
        }
        return null;
    }

    // Method to get an account by account ID
    public static Account getAccountByAccountId(String accountId) throws SQLException {
        String query = "SELECT user_id, username, avatar_link, bio FROM account WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String userId = rs.getString("user_id");
                String userName = rs.getString("username");
                String avatarLink = rs.getString("avatar_link");
                String bio = rs.getString("bio");

                return new Account(userId, userName, avatarLink, bio);
            }
        }

        return null;
    }

    // Method to move an account to the deleted_accounts table (soft delete)
    public static boolean moveAccountToDeleted(String userId) throws SQLException {
        String insertQuery = "INSERT INTO deleted_accounts (user_id, username, avatar_link, bio, deleted_at) " +
                             "SELECT user_id, username, avatar_link, bio, NOW() FROM account WHERE user_id = ?";
        String deleteQuery = "DELETE FROM account WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {

                insertStmt.setString(1, userId);
                deleteStmt.setString(1, userId);

                int inserted = insertStmt.executeUpdate();
                int deleted = deleteStmt.executeUpdate();

                if (inserted > 0 && deleted > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        }
    }

    public static Account getDeletedAccountByUserId(UUID identifierId) throws SQLException {
        String hashedId = IDHash.hashUUID(identifierId);
        String query = "SELECT * FROM deleted_accounts WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, hashedId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String userId = rs.getString("user_id");
                String userName = rs.getString("username");
                String avatarLink = rs.getString("avatar_link");
                String bio = rs.getString("bio");

                return new Account(userId, userName, avatarLink, bio);
            }
        }

        return null;
    }
    

    // Method to check if an account is in the deleted_accounts table
    public static boolean isAccountInDeleted(String userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM deleted_accounts WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Method to restore a deleted account
    public static boolean restoreUserById(String userId) throws SQLException {
        String restoreQuery = "INSERT INTO account (user_id, username, avatar_link, bio) " +
                              "SELECT user_id, username, avatar_link, bio FROM deleted_accounts WHERE user_id = ?";
        String deleteQuery = "DELETE FROM deleted_accounts WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement restoreStmt = conn.prepareStatement(restoreQuery);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {

                restoreStmt.setString(1, userId);
                deleteStmt.setString(1, userId);

                int restored = restoreStmt.executeUpdate();
                int deleted = deleteStmt.executeUpdate();

                if (restored > 0 && deleted > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        }
    }

    // Method to get the deleted_at timestamp of a deleted account
    public static Optional<LocalDateTime> getDeletedAccountDeletedAt(String userId) throws SQLException {
        String query = "SELECT deleted_at FROM deleted_accounts WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(rs.getTimestamp("deleted_at").toLocalDateTime());
            } else {
                return Optional.empty();
            }
        }
    }

    // Method to delete expired deleted accounts
    public static boolean deleteExpiredDeletedAccounts(int days) throws SQLException {
    String query = "DELETE FROM deleted_accounts WHERE deleted_at < NOW() - INTERVAL '" + days + " days'";

    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {

        return stmt.executeUpdate() > 0;
    }
}


    // Method to delete an account from the deleted_accounts table
    public static boolean deleteFromDeletedAccounts(String userId) throws SQLException {
        String query = "DELETE FROM deleted_accounts WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // Method to update username and bio together
    public static boolean updateUsernameAndBio(String userId, String userName, String bio) throws SQLException {
        List<String> updates = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (userName != null) {
            updates.add("username = ?");
            params.add(userName);
        }
        if (bio != null) {
            updates.add("bio = ?");
            params.add(bio);
        }

        // If nothing to update, return false
        if (updates.isEmpty()) {
            return false;
        }

        String query = "UPDATE account SET " + String.join(", ", updates) + " WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            int index = 1;
            for (Object param : params) {
                pstmt.setObject(index++, param);
            }
            pstmt.setString(index, userId);

            return pstmt.executeUpdate() > 0;
        }
    }

    // Method to update avatar link
    public static boolean updateAvatar(String userId, String avatarLink) throws SQLException {
        String query = "UPDATE account SET avatar_link = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, avatarLink);
            pstmt.setString(2, userId);

            return pstmt.executeUpdate() > 0;
        }
    }

    public static Map<String, Object> getSavedArticles(String userId, int page, int pageSize) {
        List<Map<String, Object>> articles = new ArrayList<>();
        int totalCount = 0;

        String dataQuery = """
                SELECT a.article_id, a.saved_at, ar.title, ar.url
                FROM account_article a
                JOIN article ar ON a.article_id = ar.article_id
                WHERE a.user_id = ?
                ORDER BY a.saved_at DESC
                LIMIT ? OFFSET ?
            """;

        String countQuery = "SELECT COUNT(*) FROM account_article WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Fetch articles
            try (PreparedStatement pstmt = conn.prepareStatement(dataQuery)) {
                int offset = (page - 1) * pageSize;
                pstmt.setString(1, userId);
                pstmt.setInt(2, pageSize);
                pstmt.setInt(3, offset);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> article = new HashMap<>();
                        article.put("articleId", rs.getString("article_id"));
                        article.put("savedAt", rs.getTimestamp("saved_at").toLocalDateTime());
                        article.put("title", rs.getString("title"));
                        article.put("url", rs.getString("url"));
                        articles.add(article);
                    }
                }
            }

            // Fetch total count
            try (PreparedStatement pstmt = conn.prepareStatement(countQuery)) {
                pstmt.setString(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        totalCount = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);
        result.put("totalCount", totalCount);
        return result;
        }

}
