package com.example.musicstore.utils;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;

public class Validation {

    public static boolean validateRequired(Control field, Label errorLabel, String fieldName) {
        if (field instanceof TextInputControl) {
            String text = ((TextInputControl) field).getText();
            if (text == null || text.trim().isEmpty()) {
                errorLabel.setText(fieldName + " обязателен для заполнения");
                errorLabel.setVisible(true);
                return false;
            }
        }
        errorLabel.setVisible(false);
        return true;
    }

    public static boolean validateEmail(String email, Label errorLabel) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorLabel.setText("Некорректный формат email");
            errorLabel.setVisible(true);
            return false;
        }
        errorLabel.setVisible(false);
        return true;
    }

    public static boolean validateNumber(Control field, Label errorLabel, String fieldName) {
        if (field instanceof TextInputControl) {
            String text = ((TextInputControl) field).getText();
            if (!text.matches("\\d+")) {
                errorLabel.setText(fieldName + " должен быть числом");
                errorLabel.setVisible(true);
                return false;
            }
        }
        errorLabel.setVisible(false);
        return true;
    }
}