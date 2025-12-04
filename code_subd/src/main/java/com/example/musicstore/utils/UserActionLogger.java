package com.example.musicstore.utils;

import com.example.musicstore.Database;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserActionLogger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logAction(String userEmail, String actionType, String entityType, String details) {
        String ipAddress = getClientIpAddress();
        String timestamp = LocalDateTime.now().format(formatter);

        String query = String.format(
                "INSERT INTO user_actions (user_email, action_date, action_type, entity_type, action_details, ip_address) " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', '%s')",
                sanitize(userEmail), timestamp, sanitize(actionType),
                sanitize(entityType), sanitize(details), sanitize(ipAddress)
        );

        Database.executeUpdate(query);
    }

    private static String getClientIpAddress() {
        // В реальном приложении здесь должна быть логика получения IP адреса клиента
        // Для демонстрации возвращаем заглушку
        return "127.0.0.1";
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }
}