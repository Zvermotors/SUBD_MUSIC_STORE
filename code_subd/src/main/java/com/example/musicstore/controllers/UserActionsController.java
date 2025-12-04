package com.example.musicstore.controllers;

import com.example.musicstore.Database;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class UserActionsController {

    @FXML private Label currentUserEmailLabel;
    @FXML private Label totalActionsLabel;
    @FXML private TableView<Map<String, Object>> actionsTable;
    @FXML private ComboBox<String> actionTypeFilter;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    private String currentUserEmail;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();

        // Добавляем слушатель для обновления данных при показе вкладки
        setupTabListener();
    }

    public void setUserEmail(String email) {
        this.currentUserEmail = email;
        if (currentUserEmailLabel != null) {
            currentUserEmailLabel.setText(email);
        }
        loadUserActions();
    }

    private void setupTabListener() {
        // Получаем родительскую вкладку и добавляем слушатель
        if (actionsTable != null && actionsTable.getScene() != null) {
            TabPane tabPane = (TabPane) actionsTable.getScene().lookup("#tabPane");
            if (tabPane != null) {
                tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                    if (newTab != null && newTab.getText().equals("История действий")) {
                        // Обновляем данные при активации вкладки
                        loadUserActions();
                    }
                });
            }
        }
    }

    private void setupTable() {
        // Настройка колонок таблицы
        TableColumn<Map<String, Object>, String> dateColumn = new TableColumn<>("Дата и время");
        dateColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("action_date");
            return new SimpleStringProperty(value != null ? value.toString() : "");
        });
        dateColumn.setPrefWidth(150);

        TableColumn<Map<String, Object>, String> actionColumn = new TableColumn<>("Действие");
        actionColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("action_type");
            return new SimpleStringProperty(value != null ? value.toString() : "");
        });
        actionColumn.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> entityColumn = new TableColumn<>("Тип сущности");
        entityColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("entity_type");
            return new SimpleStringProperty(value != null ? value.toString() : "");
        });
        entityColumn.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> detailsColumn = new TableColumn<>("Детали");
        detailsColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("action_details");
            return new SimpleStringProperty(value != null ? value.toString() : "");
        });
        detailsColumn.setPrefWidth(250);

        TableColumn<Map<String, Object>, String> ipColumn = new TableColumn<>("IP адрес");
        ipColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("ip_address");
            return new SimpleStringProperty(value != null ? value.toString() : "");
        });
        ipColumn.setPrefWidth(120);

        actionsTable.getColumns().setAll(dateColumn, actionColumn, entityColumn, detailsColumn, ipColumn);
    }

    private void setupFilters() {
        // Заполнение фильтров
        actionTypeFilter.getItems().addAll("Все действия", "Добавление", "Редактирование", "Удаление", "Вход в систему", "Выход из системы");
        actionTypeFilter.setValue("Все действия");

        // Установка дат по умолчанию (последние 30 дней)
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
    }

    @FXML
    public void loadUserActions() {
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            System.out.println("Email пользователя не установлен");
            return;
        }

        String query = buildQuery();
        System.out.println("Выполняем запрос: " + query);

        List<Map<String, Object>> actions = Database.executeQuery(query);

        actionsTable.setItems(FXCollections.observableArrayList(actions));
        totalActionsLabel.setText("Всего действий: " + actions.size());

        System.out.println("Загружено действий: " + actions.size());
    }

    private String buildQuery() {
        StringBuilder query = new StringBuilder(
                "SELECT action_date, action_type, entity_type, action_details, ip_address " +
                        "FROM user_actions WHERE user_email = '" + currentUserEmail + "'"
        );

        // Добавляем фильтр по типу действия
        String actionType = actionTypeFilter.getValue();
        if (actionType != null && !actionType.equals("Все действия")) {
            query.append(" AND action_type = '").append(actionType).append("'");
        }

        // Добавляем фильтр по дате
        if (startDatePicker.getValue() != null) {
            query.append(" AND DATE(action_date) >= '").append(startDatePicker.getValue()).append("'");
        }
        if (endDatePicker.getValue() != null) {
            query.append(" AND DATE(action_date) <= '").append(endDatePicker.getValue()).append("'");
        }

        query.append(" ORDER BY action_date DESC");
        return query.toString();
    }

    @FXML
    private void applyFilter() {
        loadUserActions();
    }

    @FXML
    private void resetFilter() {
        actionTypeFilter.setValue("Все действия");
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        loadUserActions();
    }

    @FXML
    private void refreshData() {
        loadUserActions();
    }

    @FXML
    private void exportToCsv() {
        try {
            String fileName = "user_actions_" + currentUserEmail + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

            FileWriter writer = new FileWriter(fileName);

            // Заголовок CSV
            writer.write("Дата и время;Действие;Тип сущности;Детали;IP адрес\n");

            // Данные
            for (Map<String, Object> row : actionsTable.getItems()) {
                writer.write(
                        safeGetString(row, "action_date") + ";" +
                                safeGetString(row, "action_type") + ";" +
                                safeGetString(row, "entity_type") + ";" +
                                safeGetString(row, "action_details") + ";" +
                                safeGetString(row, "ip_address") + "\n"
                );
            }

            writer.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Экспорт завершен");
            alert.setHeaderText("Данные успешно экспортированы в файл: " + fileName);
            alert.showAndWait();

        } catch (IOException e) {
            showError("Ошибка при экспорте данных: " + e.getMessage());
        }
    }

    @FXML
    private void clearHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Очистка истории");
        alert.setHeaderText("Вы уверены, что хотите очистить историю действий?");
        alert.setContentText("Это действие нельзя отменить.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String query = "DELETE FROM user_actions WHERE user_email = '" + currentUserEmail + "'";
                if (Database.executeUpdate(query)) {
                    loadUserActions();
                    showAlert("Успех", "История действий очищена");
                } else {
                    showError("Ошибка при очистке истории");
                }
            }
        });
    }

    private String safeGetString(Map<String, Object> map, String key) {
        if (map == null || key == null) return "";
        Object value = map.get(key);
        return value != null ? value.toString().replace(";", ",") : "";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}