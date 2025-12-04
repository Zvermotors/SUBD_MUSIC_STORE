package com.example.musicstore.services;

import com.example.musicstore.utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AuthService {

    public boolean authenticate(String email, String password) {
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Если есть результат - аутентификация успешна

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerUser(String email, String password, String fullName, String phone) {
        // Сначала проверяем, нет ли уже пользователя с таким email
        String checkQuery = "SELECT * FROM users WHERE email = ?";
        String insertQuery = "INSERT INTO users (email, password, role, full_name, phone) VALUES (?, ?, 'user', ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

            // Проверка существования пользователя
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return false; // Пользователь уже существует
            }

            // Регистрация нового пользователя
            insertStmt.setString(1, email);
            insertStmt.setString(2, password);
            insertStmt.setString(3, fullName);
            insertStmt.setString(4, phone);

            return insertStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Регистрация администратора
     */
    public boolean registerAdmin(String email, String password, String firstName, String lastName) {
        try {
            // Проверяем, нет ли уже пользователя с таким email
            String checkQuery = "SELECT COUNT(*) as count FROM users WHERE email = '" + email.replace("'", "''") + "'";
            List<Map<String, Object>> result = com.example.musicstore.Database.executeQuery(checkQuery);

            if (!result.isEmpty() && Integer.parseInt(result.get(0).get("count").toString()) > 0) {
                System.out.println("Пользователь с email " + email + " уже существует");
                return false;
            }

            // Регистрируем администратора
            String query = "INSERT INTO users (email, password, first_name, last_name, role) VALUES ('" +
                    email.replace("'", "''") + "', '" + password.replace("'", "''") + "', '" +
                    firstName.replace("'", "''") + "', '" + lastName.replace("'", "''") + "', 'admin')";

            boolean success = com.example.musicstore.Database.executeUpdate(query);
            if (success) {
                System.out.println("Администратор зарегистрирован: " + email);
            }
            return success;

        } catch (Exception e) {
            System.err.println("Ошибка регистрации администратора: " + e.getMessage());
            return false;
        }
}}