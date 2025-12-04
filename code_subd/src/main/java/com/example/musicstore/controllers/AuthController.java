package com.example.musicstore.controllers;

import com.example.musicstore.Database;
import com.example.musicstore.services.AuthService;
import com.example.musicstore.utils.Validation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AuthController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label loginError;

    private AuthService authService = new AuthService();

    @FXML
    void handleLogin() {
        // Сброс ошибок
        emailError.setVisible(false);
        passwordError.setVisible(false);
        loginError.setVisible(false);

        // Валидация
        boolean isValid = true;

        if (!Validation.validateRequired(emailField, emailError, "Email")) {
            isValid = false;
        } else if (!Validation.validateEmail(emailField.getText(), emailError)) {
            isValid = false;
        }

        if (!Validation.validateRequired(passwordField, passwordError, "Пароль")) {
            isValid = false;
        }

        if (!isValid) return;

        // Аутентификация
        boolean isAuthenticated = authService.authenticate(emailField.getText(), passwordField.getText());

        if (isAuthenticated) {
            openMainWindow(emailField.getText());
        } else {
            loginError.setText("Неверный email или пароль");
            loginError.setVisible(true);
        }
    }

    @FXML
    void handleRegister() {
        openRegistrationWindow();
    }

    private void openMainWindow(String userEmail) {
        try {
            Stage currentStage = (Stage) emailField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/musicstore/main.fxml"));
            Parent root = loader.load();

            // Получаем контроллер главного окна и передаем email пользователя
            MainController mainController = loader.getController();
            mainController.setUserEmail(userEmail);

            Stage mainStage = new Stage();
            mainStage.setTitle("Music Store - Панель управления");
            mainStage.setScene(new Scene(root, 1200, 700));
            mainStage.setMinWidth(1000);
            mainStage.setMinHeight(600);

            mainStage.show();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка загрузки интерфейса");
        }
    }

    private void openRegistrationWindow() {
        try {
            Stage currentStage = (Stage) emailField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/musicstore/registration.fxml"));
            Parent root = loader.load();

            Stage registrationStage = new Stage();
            registrationStage.setTitle("Music Store - Регистрация");
            registrationStage.setScene(new Scene(root, 700, 600));
            registrationStage.setResizable(false);

            registrationStage.show();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка загрузки интерфейса регистрации");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(message);
        alert.showAndWait();
    }



}