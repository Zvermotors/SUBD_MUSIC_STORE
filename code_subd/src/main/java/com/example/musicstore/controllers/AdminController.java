package com.example.musicstore.controllers;

import com.example.musicstore.services.AuthService;
import com.example.musicstore.utils.Validation;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label fullNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    @FXML private Button registerButton;

    private AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        // Сброс ошибок
        clearErrors();

        // Валидация
        boolean isValid = true;

        if (!Validation.validateRequired(fullNameField, fullNameError, "ФИО")) isValid = false;

        if (!Validation.validateRequired(emailField, emailError, "Email")) {
            isValid = false;
        } else if (!Validation.validateEmail(emailField.getText(), emailError)) {
            isValid = false;
        }

        if (!Validation.validateRequired(phoneField, phoneError, "Телефон")) isValid = false;

        if (!Validation.validateRequired(passwordField, passwordError, "Пароль")) {
            isValid = false;
        } else if (passwordField.getText().length() < 6) {
            passwordError.setText("Пароль должен содержать минимум 6 символов");
            passwordError.setVisible(true);
            isValid = false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            confirmPasswordError.setText("Пароли не совпадают");
            confirmPasswordError.setVisible(true);
            isValid = false;
        }

        if (!isValid) return;

        // Регистрация
        boolean success = authService.registerAdmin(
                emailField.getText(),
                passwordField.getText(),
                fullNameField.getText(),
                phoneField.getText()
        );

        if (success) {
            showSuccess("Администратор успешно зарегистрирован");
            clearForm();
        } else {
            showError("Ошибка регистрации администратора");
        }
    }

    private void clearErrors() {
        fullNameError.setVisible(false);
        emailError.setVisible(false);
        phoneError.setVisible(false);
        passwordError.setVisible(false);
        confirmPasswordError.setVisible(false);
    }

    private void clearForm() {
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(message);
        alert.showAndWait();
    }
}