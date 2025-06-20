package com.vifinancenews.user.services;

import com.vifinancenews.common.daos.AccountDAO;
import com.vifinancenews.common.daos.IdentifierDAO;
import com.vifinancenews.common.models.Account;
import com.vifinancenews.common.models.Identifier;
import com.vifinancenews.common.utilities.RedisCacheService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountService {

    // ========== User Profile ==========

    public Account getUserProfile(String accountId) throws SQLException {
        // Check cache
        Map<String, String> cachedData = RedisCacheService.getCachedUserData(accountId);
        if (cachedData != null) {
            return mapToAccount(cachedData, accountId);
        }

        // Fallback to DB
        Account account = AccountDAO.getAccountByAccountId(accountId);
        if (account != null) {
         // Make sure fields are not null before caching
            if (account.getUserName() != null && account.getAvatarLink() != null && account.getBio() != null) {
                RedisCacheService.cacheUserData(accountId, mapAccountToCacheData(account));
            }
        } else {
        // Log if account is null for further debugging
        System.out.println("Account not found for accountId: " + accountId);
        }

        return account;
    }

    public boolean updateUserNameAndBio(String accountId, String userName, String bio) throws SQLException {
        boolean updated = AccountDAO.updateUsernameAndBio(accountId, userName, bio);

        if (updated) {
            RedisCacheService.clearUserData(accountId);

            Account updatedAccount = AccountDAO.getAccountByAccountId(accountId);
            RedisCacheService.cacheUserData(accountId, mapAccountToCacheData(updatedAccount));
        }

        return updated;
    }

    public boolean updateAvatar(String accountId, String avatarLink) throws SQLException {
        boolean updated = AccountDAO.updateAvatar(accountId, avatarLink);

        if (updated) {
            RedisCacheService.clearUserData(accountId);
            Account updatedAccount = AccountDAO.getAccountByAccountId(accountId);
            RedisCacheService.cacheUserData(accountId, mapAccountToCacheData(updatedAccount));
        }

        return updated;
    }

    // ========== Account Deletion ==========

    public boolean softDeleteUser(String accountId) throws SQLException {
        boolean deleted = AccountDAO.moveAccountToDeleted(accountId);
        if (deleted) {
            RedisCacheService.clearUserData(accountId);
        }
        return deleted;
    }

    public boolean deleteUser(String accountId) throws SQLException {
        boolean accountDeleted = AccountDAO.deleteFromDeletedAccounts(accountId);
        if (!accountDeleted) {
            accountDeleted = AccountDAO.deleteFromDeletedAccounts(accountId);
        }

        Identifier user = IdentifierDAO.getIdentifierByAccountId(accountId);
        boolean identifierDeleted = user != null && IdentifierDAO.deleteIdentifierByUserId(user.getId());

        if (accountDeleted && identifierDeleted) {
            RedisCacheService.clearUserData(accountId);
        }

        return accountDeleted && identifierDeleted;
    }

    public static boolean permanentlyDeleteExpiredAccounts(int days) throws SQLException {
        boolean identifiersDeleted = IdentifierDAO.deleteExpiredIdentifiers(days);
        boolean accountsDeleted = AccountDAO.deleteExpiredDeletedAccounts(days);
        return identifiersDeleted || accountsDeleted;
    }

    public Map<String, Object> getSavedArticles(String userId, int page, int pageSize) {
        return AccountDAO.getSavedArticles(userId, page, pageSize);
}


    // ========== Helpers ==========

    private Map<String, String> mapAccountToCacheData(Account account) {
        Map<String, String> cacheData = new HashMap<>();
        
        cacheData.put("userName", account.getUserName() != null ? account.getUserName() : "");
        cacheData.put("avatarLink", account.getAvatarLink() != null ? account.getAvatarLink() : "");
        cacheData.put("bio", account.getBio() != null ? account.getBio() : "");
    
        return cacheData;
    }
    

    private Account mapToAccount(Map<String, String> data, String accountId) {
        String avatarLink = data.get("avatarLink");
        String bio = data.get("bio");

        if (avatarLink != null && avatarLink.isEmpty()) avatarLink = null;
        if (bio != null && bio.isEmpty()) bio = null;

        return new Account(accountId, data.get("userName"), avatarLink, bio);
    }
}
