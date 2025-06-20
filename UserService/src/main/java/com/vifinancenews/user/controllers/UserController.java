package com.vifinancenews.user.controllers;

import com.vifinancenews.user.services.AccountService;
import com.vifinancenews.common.models.Account;
import com.vifinancenews.common.utilities.RedisSessionManager;
import io.javalin.http.Handler;

import java.util.Map;

public class UserController {

    private static final AccountService accountService = new AccountService();

    // Handler for retrieving user profile
    public static Handler getUserProfile = ctx -> {
        String sessionId = ctx.cookie("SESSION_ID");
        if (sessionId == null) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        Map<String, Object> sessionData = RedisSessionManager.getSession(sessionId);
        if (sessionData == null || !sessionData.containsKey("userId")) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        String userId = (String) sessionData.get("userId");
        Account account = accountService.getUserProfile(userId);
        if (account == null) {
            ctx.status(404).result("User not found");
        } else {
            ctx.json(account);
        }
    };

    // Handler for updating username and bio
    public static Handler updateInfo = ctx -> {
        String sessionId = ctx.cookie("SESSION_ID");
        if (sessionId == null) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        Map<String, Object> sessionData = RedisSessionManager.getSession(sessionId);
        if (sessionData == null || !sessionData.containsKey("userId")) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        String userId = (String) sessionData.get("userId");
        Map<String, String> requestData = ctx.bodyAsClass(Map.class);

        String userName = requestData.get("userName");
        String bio = requestData.get("bio");

        if ((userName == null || userName.isBlank()) && bio == null) {
            ctx.status(400).result("Username or bio must be provided");
            return;
        }

        if (userName != null && userName.isBlank()) {
            ctx.status(400).result("Username cannot be empty");
            return;
        }

        boolean success = accountService.updateUserNameAndBio(userId, userName, bio);

        if (success) {
            ctx.status(200).result("Username and/or bio updated");
        } else {
            ctx.status(400).result("Update failed");
        }
    };

    // Handler for updating avatar link
    public static Handler updateAvatar = ctx -> {
        String sessionId = ctx.cookie("SESSION_ID");
        if (sessionId == null) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        Map<String, Object> sessionData = RedisSessionManager.getSession(sessionId);
        if (sessionData == null || !sessionData.containsKey("userId")) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        String userId = (String) sessionData.get("userId");
        Map<String, String> requestData = ctx.bodyAsClass(Map.class);

        String avatarLink = requestData.get("avatarLink");

        if (avatarLink == null || avatarLink.isBlank()) {
            ctx.status(400).result("Avatar link is required");
            return;
        }

        boolean success = accountService.updateAvatar(userId, avatarLink);

        if (success) {
            ctx.status(200).result("Avatar updated");
        } else {
            ctx.status(400).result("Update failed");
        }
    };

    // Handler for soft deleting user
    public static Handler deleteUser = ctx -> {
        String sessionId = ctx.cookie("SESSION_ID");
        if (sessionId == null) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        Map<String, Object> sessionData = RedisSessionManager.getSession(sessionId);
        if (sessionData == null || !sessionData.containsKey("userId")) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        String userId = (String) sessionData.get("userId");
        boolean softDeleted = accountService.softDeleteUser(userId);

        if (softDeleted) {
            RedisSessionManager.destroySession(sessionId);
            ctx.status(200).result("Your account has been deactivated for 30 days before permanent deletion. You can restore it during this period.");
        } else {
            ctx.status(400).result("Failed to soft delete user.");
        }
    };

    // Handler to get paginated list of saved articles
    public static Handler getSavedArticles = ctx -> {
        String sessionId = ctx.cookie("SESSION_ID");
        if (sessionId == null) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        Map<String, Object> sessionData = RedisSessionManager.getSession(sessionId);
        if (sessionData == null || !sessionData.containsKey("userId")) {
            ctx.status(401).result("Unauthorized");
            return;
        }

        String userId = (String) sessionData.get("userId");

        // Parse page query param, default to 1 if missing or invalid
        int page;
        try {
            String pageParam = ctx.queryParam("page");
            page = (pageParam == null) ? 1 : Integer.parseInt(pageParam);
            if (page < 1) page = 1;
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid page number");
            return;
        }

        // Fetch paginated saved articles
        var articles = accountService.getSavedArticles(userId, page, 5); // 5 articles per page

        ctx.json(articles);
    };

}
