package com.example.musicstore.controllers;

import com.example.musicstore.Database;
import com.example.musicstore.utils.UserActionLogger;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Главный контроллер приложения MusicStore
 * Управляет основным интерфейсом и всеми операциями с данными
 */
public class MainController {
    // Элементы таблиц
    @FXML private TableView<Map<String, Object>> ensemblesTable, musiciansTable, compositionsTable, recordsTable;
    @FXML private TableView<Map<String, Object>> ensembleMembersTable, performancesTable, recordTracksTable, salesLeadersTable;
    @FXML private TableView<Map<String, Object>> analyticsTable;

    // Поля ввода для основных сущностей
    @FXML private TextField ensembleName, ensembleType, ensembleDesc;
    @FXML private TextField musicianFirstName, musicianMiddleName, musicianLastName, musicianBio;
    @FXML private TextField compositionTitle, compositionYear;
    @FXML private TextField recordTitle, recordWholesalePrice, recordRetailPrice, recordDiscs;
    @FXML private TextField searchEnsembleField, salesUpdateField, memberRole, arrangementField, trackNumberField;

    // Выпадающие списки
    @FXML private ComboBox<String> recordSelector, ensembleSelector, musicianSelector;
    @FXML private ComboBox<String> performanceEnsembleSelector, performanceCompositionSelector;
    @FXML private ComboBox<String> trackRecordSelector, trackCompositionSelector;
    @FXML private ComboBox<String> analyticsSelector;

    // Прочие элементы интерфейса
    @FXML private TextArea resultArea;
    @FXML private TabPane tabPane;

    // Новые элементы для отображения email и редактирования
    @FXML private Label userEmailLabel;
    @FXML private Label mainTitleLabel;

    // Вкладка истории действий
    @FXML private Tab userActionsTab;

    // Новые элементы для отображения деталей записи
    @FXML private ImageView detailImageView;
    @FXML private TextArea detailDescriptionArea;
    @FXML
    Label detailTitleLabel;

    private String currentUserEmail;

    /**
     * Метод инициализации контроллера
     * Выполняется при загрузке FXML файла
     */
    @FXML
    public void initialize() {

        // Проверка подключения к базе данных
        if (!Database.testConnection()) {
            showAlert("Ошибка", "Нет подключения к БД");
            return;

        }

        // Настройка интерфейса
        setupTables();
        loadAllData();
        populateAllSelectors();
        setupTableSelectionListeners();


        // Скрываем контейнер с изображением при запуске

        // Обработчик смены вкладок
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                refreshCurrentTab();
            }
        });
        debugImagePaths();
    }

    /**
     * Метод для отладки - проверяет доступность путей изображений
     */
    private void debugImagePaths() {
        String[] entityTypes = {"ensembles", "musicians", "compositions", "records"};
        String[] testNames = {"Test Ensemble", "John Doe", "Test Composition", "Test Record"};

        System.out.println("=== ПРОВЕРКА ПУТЕЙ ИЗОБРАЖЕНИЙ ===");

        for (int i = 0; i < entityTypes.length; i++) {
            String imageName = generateImageFileName(testNames[i]);
            String imagePath = "/musicstore/iamges/" + entityTypes[i] + "/" + imageName;

            System.out.println("Проверка пути: " + imagePath);

            InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                System.out.println("✓ Изображение доступно: " + imagePath);
            } else {
                System.out.println("✗ Изображение не найдено: " + imagePath);

                // Проверяем существование папки
                String folderPath = "/musicstore/iamges/" + entityTypes[i] + "/";
                InputStream folderStream = getClass().getResourceAsStream(folderPath);
                System.out.println("  Папка существует: " + (folderStream != null ? "ДА" : "НЕТ"));
            }
        }

        System.out.println("=== КОНЕЦ ПРОВЕРКИ ===");
    }

    /**
     * Настройка слушателей выбора для таблиц
     */
    private void setupTableSelectionListeners() {
        // Слушатель для таблицы ансамблей
        ensemblesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showEntityDetails(newSelection, "Ансамбль");
            } else {
                clearDetails(); // Очищаем при снятии выбора
            }
        });

        // Слушатель для таблицы музыкантов
        musiciansTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showEntityDetails(newSelection, "Музыкант");
            } else {
                clearDetails(); // Очищаем при снятии выбора
            }
        });

        // Слушатель для таблицы произведений
        compositionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showEntityDetails(newSelection, "Произведение");
            } else {
                clearDetails(); // Очищаем при снятии выбора
            }
        });

        // Слушатель для таблицы пластинок
        recordsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showEntityDetails(newSelection, "Пластинка");
            } else {
                clearDetails(); // Очищаем при снятии выбора
            }
        });
    }


    /**
     * Проверяет, изменилась ли сущность для предотвращения дублирования загрузки
     */
    private boolean hasEntityChanged(String newEntityName, String newEntityType) {
        // Получаем текущее название из заголовка
        String currentTitle = detailTitleLabel.getText();

        if (currentTitle.equals("Выберите запись для просмотра деталей")) {
            return true; // Нет текущей сущности, значит изменилась
        }

        // Извлекаем имя сущности из заголовка
        String currentEntityName = "";
        if (currentTitle.startsWith("Ансамбль: ")) {
            currentEntityName = currentTitle.substring("Ансамбль: ".length());
        } else if (currentTitle.startsWith("Музыкант: ")) {
            currentEntityName = currentTitle.substring("Музыкант: ".length());
        } else if (currentTitle.startsWith("Произведение: ")) {
            currentEntityName = currentTitle.substring("Произведение: ".length());
        } else if (currentTitle.startsWith("Пластинка: ")) {
            currentEntityName = currentTitle.substring("Пластинка: ".length());
        }

        // Сравниваем с новой сущностью
        boolean changed = !currentEntityName.equals(newEntityName);
        System.out.println("Сравнение сущностей: текущая='" + currentEntityName + "', новая='" + newEntityName + "', изменилась=" + changed);

        return changed;
    }



    /**
     * Отображение деталей выбранной сущности - УПРОЩЕННАЯ ВЕРСИЯ
     */
    private void showEntityDetails(Map<String, Object> entity, String entityType) {
        if (detailTitleLabel == null || detailDescriptionArea == null || detailImageView == null) {
            return;
        }

        try {
            // ВСЕГДА очищаем перед загрузкой новых данных
            clearDetails();

            switch (entityType) {
                case "Ансамбль":
                    showEnsembleDetails(entity);
                    break;
                case "Музыкант":
                    showMusicianDetails(entity);
                    break;
                case "Произведение":
                    showCompositionDetails(entity);
                    break;
                case "Пластинка":
                    showRecordDetails(entity);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearDetails();
        }
    }

    /**
     * Получает имя сущности для отображения
     */
    private String getEntityNameForDisplay(Map<String, Object> entity, String entityType) {
        switch (entityType) {
            case "Ансамбль":
                return safeGetString(entity, "name");
            case "Музыкант":
                return safeGetString(entity, "first_name") + " " + safeGetString(entity, "last_name");
            case "Произведение":
                return safeGetString(entity, "title");
            case "Пластинка":
                return safeGetString(entity, "title");
            default:
                return "";
        }
    }

    /**
     * Отображение деталей ансамбля
     */
    private void showEnsembleDetails(Map<String, Object> ensemble) {
        String name = safeGetString(ensemble, "name");
        String type = safeGetString(ensemble, "type");
        String description = safeGetString(ensemble, "description");

        detailTitleLabel.setText("Ансамбль: " + name);

        StringBuilder details = new StringBuilder();
        details.append("Тип: ").append(type).append("\n\n");
        details.append("Описание:\n").append(description).append("\n\n");

        // Добавляем информацию о составе
        String membersQuery = "SELECT CONCAT(m.first_name, ' ', m.last_name) as musician_name, em.role " +
                "FROM ensemble_members em " +
                "JOIN musicians m ON em.musician_id = m.musician_id " +
                "WHERE em.ensemble_id = (SELECT ensemble_id FROM ensembles WHERE name = '" + sanitize(name) + "')";

        List<Map<String, Object>> members = Database.executeQuery(membersQuery);
        if (!members.isEmpty()) {
            details.append("Состав ансамбля:\n");
            for (Map<String, Object> member : members) {
                details.append("• ").append(member.get("musician_name"))
                        .append(" - ").append(member.get("role")).append("\n");
            }
        }

        detailDescriptionArea.setText(details.toString());
        loadEntityImage(name, "ensembles");
    }

    /**
     * Отображение деталей музыканта - С ОТЧЕСТВОМ
     */
    private void showMusicianDetails(Map<String, Object> musician) {
        String firstName = safeGetString(musician, "first_name");
        String middleName = safeGetString(musician, "middle_name");
        String lastName = safeGetString(musician, "last_name");
        String bio = safeGetString(musician, "bio");

        // Формируем полное имя с отчеством
        StringBuilder fullNameBuilder = new StringBuilder(firstName);
        if (!middleName.isEmpty()) {
            fullNameBuilder.append(" ").append(middleName);
        }
        fullNameBuilder.append(" ").append(lastName);

        String fullName = fullNameBuilder.toString();
        detailTitleLabel.setText("Музыкант: " + fullName);

        StringBuilder details = new StringBuilder();
        if (!bio.isEmpty()) {
            details.append("Биография:\n").append(bio).append("\n\n");
        }

        // Добавляем информацию об ансамблях с новым запросом
        String ensemblesQuery = "SELECT e.name as ensemble_name, em.role " +
                "FROM ensemble_members em " +
                "JOIN ensembles e ON em.ensemble_id = e.ensemble_id " +
                "WHERE em.musician_id = (SELECT musician_id FROM musicians WHERE first_name = '" + sanitize(firstName) +
                "' AND last_name = '" + sanitize(lastName) + "')";

        List<Map<String, Object>> ensembles = Database.executeQuery(ensemblesQuery);
        if (!ensembles.isEmpty()) {
            details.append("Участвует в ансамблях:\n");
            for (Map<String, Object> ensemble : ensembles) {
                details.append("• ").append(ensemble.get("ensemble_name"))
                        .append(" - ").append(ensemble.get("role")).append("\n");
            }
        } else {
            details.append("Не участвует в ансамблях\n");
        }

        detailDescriptionArea.setText(details.toString());
        loadEntityImage(firstName + " " + lastName, "musicians");
    }

    /**
     * Отображение деталей произведения
     */
    private void showCompositionDetails(Map<String, Object> composition) {
        String title = safeGetString(composition, "title");
        String year = safeGetString(composition, "creation_year");

        detailTitleLabel.setText("Произведение: " + title);

        StringBuilder details = new StringBuilder();
        details.append("Год создания: ").append(year).append("\n\n");

        // Добавляем информацию об исполнениях
        String performancesQuery = "SELECT e.name as ensemble_name, p.arrangement " +
                "FROM performances p " +
                "JOIN ensembles e ON p.ensemble_id = e.ensemble_id " +
                "WHERE p.composition_id = (SELECT composition_id FROM compositions WHERE title = '" + sanitize(title) + "')";

        List<Map<String, Object>> performances = Database.executeQuery(performancesQuery);
        if (!performances.isEmpty()) {
            details.append("Исполняется ансамблями:\n");
            for (Map<String, Object> performance : performances) {
                details.append("• ").append(performance.get("ensemble_name"));
                String arrangement = safeGetString(performance, "arrangement");
                if (!arrangement.isEmpty()) {
                    details.append(" (").append(arrangement).append(")");
                }
                details.append("\n");
            }
        }

        detailDescriptionArea.setText(details.toString());
        loadEntityImage(title, "compositions");
    }

    /**
     * Отображение деталей пластинки
     */
    private void showRecordDetails(Map<String, Object> record) {
        String title = safeGetString(record, "title");
        String wholesale = safeGetString(record, "wholesale_price");
        String retail = safeGetString(record, "retail_price");
        String discs = safeGetString(record, "disc_count");
        String sales = safeGetString(record, "current_year_sales");
        String stock = safeGetString(record, "remaining_stock");

        detailTitleLabel.setText("Пластинка: " + title);

        StringBuilder details = new StringBuilder();
        details.append("Цена опт: ").append(wholesale).append(" руб.\n");
        details.append("Цена розница: ").append(retail).append(" руб.\n");
        details.append("Количество дисков: ").append(discs).append("\n");
        details.append("Продажи за год: ").append(sales).append("\n");
        details.append("Остаток на складе: ").append(stock).append("\n\n");

        // Добавляем информацию о треках
        String tracksQuery = "SELECT c.title as composition_title, rt.track_number " +
                "FROM record_tracks rt " +
                "JOIN compositions c ON rt.composition_id = c.composition_id " +
                "WHERE rt.record_id = (SELECT record_id FROM records WHERE title = '" + sanitize(title) + "') " +
                "ORDER BY rt.track_number";

        List<Map<String, Object>> tracks = Database.executeQuery(tracksQuery);
        if (!tracks.isEmpty()) {
            details.append("Треки:\n");
            for (Map<String, Object> track : tracks) {
                details.append(track.get("track_number")).append(". ")
                        .append(track.get("composition_title")).append("\n");
            }
        }

        detailDescriptionArea.setText(details.toString());
        loadEntityImage(title, "records");
    }

    /**
     * Загрузка изображения для сущности - ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    private void loadEntityImage(String entityName, String entityType) {
        try {
            // Сначала очищаем текущее изображение
            detailImageView.setImage(null);

            // Показываем контейнер с изображением
            showImageContainer();

            // Формируем путь к изображению - ТОЛЬКО в папке musicians для музыкантов
            String imageName = generateImageFileName(entityName);
            String imagePath = "/musicstore/iamges/" + entityType + "/" + imageName;

            System.out.println("🔄 Загрузка изображения для " + entityType + " '" + entityName + "'");
            System.out.println("   Путь: " + imagePath);

            // Пытаемся загрузить изображение из ресурсов
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            if (imageStream != null) {
                System.out.println("✅ InputStream создан успешно");
                Image image = new Image(imageStream);
                if (!image.isError()) {
                    detailImageView.setImage(image);
                    System.out.println("✅ УСПЕХ: Изображение загружено для " + entityName);
                    return;
                } else {
                    System.out.println("❌ Ошибка загрузки изображения (Image error)");
                }
            } else {
                System.out.println("❌ InputStream = null - файл не найден по пути: " + imagePath);

                // ДОПОЛНИТЕЛЬНАЯ ОТЛАДКА: проверим существование папки
                debugResourceAccess(imagePath);
            }

            // Если изображение не найдено, просто очищаем
            loadDefaultImage(entityType);

        } catch (Exception e) {
            System.err.println("💥 Ошибка загрузки изображения для " + entityType + ": " + entityName);
            e.printStackTrace();
            loadDefaultImage(entityType);
        }
    }


    /**
     * Отладочный метод для проверки доступа к ресурсам
     */
    private void debugResourceAccess(String imagePath) {
        try {
            System.out.println("=== ОТЛАДКА ДОСТУПА К РЕСУРСАМ ===");

            // Проверяем доступ к корневой папке ресурсов
            String rootPath = "/musicstore/";
            InputStream rootStream = getClass().getResourceAsStream(rootPath);
            System.out.println("Доступ к корневой папке " + rootPath + ": " + (rootStream != null ? "ЕСТЬ" : "НЕТ"));

            // Проверяем доступ к папке images
            String imagesPath = "/musicstore/iamges/";
            InputStream imagesStream = getClass().getResourceAsStream(imagesPath);
            System.out.println("Доступ к папке images " + imagesPath + ": " + (imagesStream != null ? "ЕСТЬ" : "НЕТ"));

            // Пытаемся получить URL ресурса
            java.net.URL resourceUrl = getClass().getResource(imagePath);
            System.out.println("URL ресурса " + imagePath + ": " + resourceUrl);

            // Пытаемся получить URL папки
            java.net.URL folderUrl = getClass().getResource("/musicstore/iamges/");
            System.out.println("URL папки images: " + folderUrl);

            System.out.println("=== КОНЕЦ ОТЛАДКИ ===");

        } catch (Exception e) {
            System.err.println("Ошибка при отладке доступа к ресурсам: " + e.getMessage());
        }
    }


    /**
     * Загрузка изображения по умолчанию - ИСПРАВЛЕННЫЙ ПУТЬ
     */

    private void loadDefaultImage(String entityType) {
        try {
            System.out.println("🔄 Нет изображения для " + entityType + ", очищаем");
            detailImageView.setImage(null);
        } catch (Exception e) {
            System.err.println("💥 Ошибка при очистке изображения: " + e.getMessage());
            detailImageView.setImage(null);
        }
    }

    /**
     * Установка email пользователя
     */
    public void setUserEmail(String email) {
        this.currentUserEmail = email;
        if (userEmailLabel != null) {
            userEmailLabel.setText("Пользователь: " + email);
        }
        if (mainTitleLabel != null) {
            mainTitleLabel.setText("Музыкальный магазин - " + email);
        }

        // Логгируем вход в систему
        UserActionLogger.logAction(currentUserEmail, "Вход в систему", "Система", "Пользователь вошел в систему");

        // Инициализируем вкладку истории действий после установки email
        initializeUserActionsTab();

    }


    /**
     * Метод инициализации таблиц - настраивает все TableView в приложении
     * Связывает данные из базы данных с визуальными элементами таблиц
     */
    private void setupTables() {
        // Настройка таблиц основных сущностей
        setupTable(ensemblesTable, new String[]{"name", "type", "description"},
                new String[]{"Название", "Тип", "Описание"});

        // Исправленная настройка для музыкантов с отчеством
        setupTable(musiciansTable, new String[]{"first_name", "middle_name", "last_name", "bio"},
                new String[]{"Имя", "Отчество", "Фамилия", "Биография"});

        setupTable(compositionsTable, new String[]{"title", "creation_year"},
                new String[]{"Название", "Год создания"});
        setupTable(recordsTable, new String[]{"title", "wholesale_price", "retail_price", "disc_count", "current_year_sales", "remaining_stock"},
                new String[]{"Название", "Опт", "Розница", "Диски", "Продажи", "Остаток"});

        // Остальные таблицы без изменений
        setupTable(ensembleMembersTable, new String[]{"ensemble_name", "musician_name", "role"},
                new String[]{"Ансамбль", "Музыкант", "Роль"});
        setupTable(performancesTable, new String[]{"ensemble_name", "composition_title", "arrangement"},
                new String[]{"Ансамбль", "Произведение", "Аранжировка"});
        setupTable(recordTracksTable, new String[]{"record_title", "composition_title", "track_number"},
                new String[]{"Пластинка", "Произведение", "Трек"});
        setupTable(salesLeadersTable, new String[]{"title", "current_year_sales", "retail_price", "remaining_stock"},
                new String[]{"Название", "Продажи", "Цена", "Остаток"});

        // Настройка таблицы аналитики
        setupTable(analyticsTable, new String[]{"record_title", "ensemble_name", "compositions_count", "total_duration", "musicians_count", "current_year_sales", "total_revenue"},
                new String[]{"Пластинка", "Ансамбль", "Треков", "Длительность", "Музыкантов", "Продажи", "Выручка"});
    }

    /**
     * Настраивает отдельную таблицу с указанными колонками
     * @param table таблица для настройки
     * @param keys названия полей в данных
     * @param headers заголовки колонок для отображения
     */
    private void setupTable(TableView<Map<String, Object>> table, String[] keys, String[] headers) {
        table.getColumns().clear();
        for (int i = 0; i < keys.length; i++) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(headers[i]);
            final String key = keys[i];
            col.setCellValueFactory(cellData -> {
                Map<String, Object> row = cellData.getValue();
                if (row == null) return new SimpleStringProperty("");
                Object value = row.get(key);

                String displayValue = value != null ? value.toString() : "";

                // Форматирование даты для русской локали
                if (key.equals("creation_year")) {
                    displayValue = formatDateToRussian(displayValue);
                }

                // ИСПРАВЛЕНИЕ: скрываем ID, показываем только названия
                if (displayValue.contains(":") && (key.equals("ensemble_name") || key.equals("musician_name") ||
                        key.equals("composition_title") || key.equals("record_title"))) {
                    displayValue = extractNameFromDisplayString(displayValue);
                }

                return new SimpleStringProperty(displayValue);
            });

            // Настройка ширины колонок
            if (headers[i].equals("Название") || headers[i].equals("Ансамбль") ||
                    headers[i].equals("Произведение") || headers[i].equals("Пластинка") ||
                    headers[i].equals("Имя") || headers[i].equals("Фамилия")) {
                col.setPrefWidth(150);
            } else if (headers[i].equals("Отчество")) {
                col.setPrefWidth(120);
            } else if (headers[i].equals("Биография") || headers[i].equals("Описание")) {
                col.setPrefWidth(350);
            } else {
                col.setPrefWidth(100);
            }

            table.getColumns().add(col);
        }
        table.setSortPolicy(param -> true);
    }
    /**
     * Форматирует дату в русский стиль (DD.MM.YYYY)
     */
    private String formatDateToRussian(String dateString) {
        if (dateString == null || dateString.isEmpty() || "NULL".equalsIgnoreCase(dateString)) {
            return "";
        }

        try {
            // Убираем возможные кавычки
            dateString = dateString.replace("'", "").trim();

            // Если строка содержит только год (например, "2023")
            if (dateString.matches("\\d{4}")) {
                return dateString;
            }

            // Пытаемся парсить разные форматы дат
            java.text.SimpleDateFormat[] formats = {
                    new java.text.SimpleDateFormat("yyyy-MM-dd"),
                    new java.text.SimpleDateFormat("dd/MM/yyyy"),
                    new java.text.SimpleDateFormat("MM/dd/yyyy"),
                    new java.text.SimpleDateFormat("yyyy.MM.dd")
            };

            for (java.text.SimpleDateFormat format : formats) {
                try {
                    java.util.Date date = format.parse(dateString);
                    java.text.SimpleDateFormat russianFormat = new java.text.SimpleDateFormat("dd.MM.yyyy");
                    return russianFormat.format(date);
                } catch (Exception e) {
                    // Пробуем следующий формат
                }
            }

            // Если не удалось распарсить, возвращаем как есть
            return dateString;
        } catch (Exception e) {
            System.err.println("Ошибка форматирования даты: " + dateString + " - " + e.getMessage());
            return dateString;
        }
    }


    /**
     * Загрузка всех данных из базы данных в таблицы
     */
    private void loadAllData() {
        // Загрузка основных сущностей
        ensemblesTable.setItems(loadData("SELECT * FROM ensembles"));
        musiciansTable.setItems(loadData("SELECT * FROM musicians"));
        compositionsTable.setItems(loadData("SELECT * FROM compositions"));
        recordsTable.setItems(loadData("SELECT * FROM records"));

        // Загрузка связей и дополнительных данных
        loadRelationData();
        showSalesLeaders();
    }

    /**
     * Метод загрузки связей между данными - С ОБНОВЛЕННЫМ ЗАПРОСОМ ДЛЯ ОТЧЕСТВА
     */
    private void loadRelationData() {
        // Загрузка состава ансамблей - обновленный запрос для отчества
        String ensembleMembersQuery = "SELECT e.name as ensemble_name, " +
                "CONCAT(m.first_name, " +
                "CASE WHEN m.middle_name IS NOT NULL AND m.middle_name != '' THEN ' ' || m.middle_name ELSE '' END, " +
                "' ', m.last_name) as musician_name, em.role " +
                "FROM ensemble_members em " +
                "JOIN ensembles e ON em.ensemble_id = e.ensemble_id " +
                "JOIN musicians m ON em.musician_id = m.musician_id " +
                "ORDER BY e.name, em.role";

        ensembleMembersTable.setItems(loadData(ensembleMembersQuery));

        // Загрузка исполнений
        String performancesQuery = "SELECT e.name as ensemble_name, c.title as composition_title, p.arrangement " +
                "FROM performances p " +
                "JOIN ensembles e ON p.ensemble_id = e.ensemble_id " +
                "JOIN compositions c ON p.composition_id = c.composition_id " +
                "ORDER BY e.name, c.title";
        performancesTable.setItems(loadData(performancesQuery));

        // Загрузка треков на пластинках
        String recordTracksQuery = "SELECT r.title as record_title, c.title as composition_title, rt.track_number " +
                "FROM record_tracks rt " +
                "JOIN records r ON rt.record_id = r.record_id " +
                "JOIN compositions c ON rt.composition_id = c.composition_id " +
                "ORDER BY r.title, rt.track_number";
        recordTracksTable.setItems(loadData(recordTracksQuery));

        // Принудительно обновляем отображение таблиц
        ensembleMembersTable.refresh();
        performancesTable.refresh();
        recordTracksTable.refresh();
    }

    /**
     * Выполняет SQL запрос и возвращает данные в формате для JavaFX TableView
     * @param query SQL запрос для выполнения
     * @return ObservableList с данными для отображения в таблице
     */
    private javafx.collections.ObservableList<Map<String, Object>> loadData(String query) {
        List<Map<String, Object>> data = Database.executeQuery(query);

        // ИСПРАВЛЕНИЕ: очищаем данные от ID для отображения
        if (query.contains("ensemble_name") || query.contains("musician_name") ||
                query.contains("composition_title") || query.contains("record_title")) {
            for (Map<String, Object> row : data) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().toString().contains(":")) {
                        String cleanValue = extractNameFromDisplayString(entry.getValue().toString());
                        row.put(entry.getKey(), cleanValue);
                    }
                }
            }
        }

        return FXCollections.observableArrayList(data);
    }

    /**
     * Загружает данные для всех комбобоксов из базы данных
     */
    private void populateAllSelectors() {
        // Заполнение выпадающих списков основными сущностями - ИСПРАВЛЕНО: убраны ID
        populateComboBox(ensembleSelector, "SELECT ensemble_id, name FROM ensembles");
        populateComboBox(musicianSelector, "SELECT musician_id, first_name, last_name FROM musicians");
        populateComboBox(performanceEnsembleSelector, "SELECT ensemble_id, name FROM ensembles");
        populateComboBox(performanceCompositionSelector, "SELECT composition_id, title FROM compositions");
        populateComboBox(trackRecordSelector, "SELECT record_id, title FROM records");
        populateComboBox(trackCompositionSelector, "SELECT composition_id, title FROM compositions");
        populateComboBox(recordSelector, "SELECT record_id, title FROM records");

        // Настройка списка аналитики
        analyticsSelector.getItems().clear();
        analyticsSelector.getItems().addAll("Полная информация о пластинках", "Ансамбли по количеству произведений", "Музыканты по количеству ансамблей", "Произведения по популярности", "Финансовая аналитика");
        analyticsSelector.getSelectionModel().selectFirst();
    }

    /**
     * Метод заполнения выпадающих списков данными из базы - С ОБНОВЛЕНИЕМ ДЛЯ ОТЧЕСТВА
     */
    private void populateComboBox(ComboBox<String> combo, String query) {
        combo.getItems().clear();
        List<Map<String, Object>> data = Database.executeQuery(query);

        for (Map<String, Object> item : data) {
            String display = null;
            if (item.containsKey("ensemble_id") && item.containsKey("name")) {
                display = item.get("name").toString(); // только название
            }
            else if (item.containsKey("musician_id") && item.containsKey("first_name") && item.containsKey("last_name")) {
                // Формируем полное имя с отчеством
                String firstName = item.get("first_name").toString();
                String lastName = item.get("last_name").toString();
                String middleName = item.containsKey("middle_name") && item.get("middle_name") != null
                        ? item.get("middle_name").toString()
                        : "";

                display = firstName;
                if (!middleName.isEmpty()) {
                    display += " " + middleName;
                }
                display += " " + lastName;
            }
            else if (item.containsKey("composition_id") && item.containsKey("title")) {
                display = item.get("title").toString();
            }
            else if (item.containsKey("record_id") && item.containsKey("title")) {
                display = item.get("title").toString();
            }

            if (display != null && !display.trim().isEmpty()) {
                combo.getItems().add(display.trim());
            }
        }

        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    /**
     * Метод заполнения ComboBox для редактирования связей - ИСПРАВЛЕНО: убраны ID
     */
    private void populateComboBoxForEdit(ComboBox<String> combo, String query) {
        combo.getItems().clear();
        List<Map<String, Object>> data = Database.executeQuery(query);
        for (Map<String, Object> item : data) {
            // Создаем строку отображения БЕЗ ID - только название
            String display = "";
            if (item.containsKey("ensemble_id") && item.containsKey("name")) {
                display = item.get("name").toString(); // ИСПРАВЛЕНИЕ: только название
            }
            else if (item.containsKey("musician_id") && item.containsKey("first_name") && item.containsKey("last_name")) {
                display = item.get("first_name") + " " + item.get("last_name"); // ИСПРАВЛЕНИЕ: только имя и фамилия
            }
            else if (item.containsKey("composition_id") && item.containsKey("title")) {
                display = item.get("title").toString(); // ИСПРАВЛЕНИЕ: только название
            }
            else if (item.containsKey("record_id") && item.containsKey("title")) {
                display = item.get("title").toString(); // ИСПРАВЛЕНИЕ: только название
            }
            // Добавляем обработку для запроса с CONCAT
            else if (item.containsKey("musician_id") && item.containsKey("name")) {
                display = item.get("name").toString(); // ИСПРАВЛЕНИЕ: только название
            }

            if (!display.isEmpty()) {
                combo.getItems().add(display);
            }
        }

        // Устанавливаем первый элемент по умолчанию, если список не пуст
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
    }

    /**
     * Загрузка данных аналитики в зависимости от выбранного типа
     */
    @FXML
    private void loadAnalyticsData() {
        String selectedAnalysis = analyticsSelector.getValue();
        if (selectedAnalysis == null) return;

        // Очистка таблицы перед загрузкой новых данных
        analyticsTable.getItems().clear();
        analyticsTable.getColumns().clear();

        switch (selectedAnalysis) {
            case "Полная информация о пластинках":
                loadCompleteRecordInfo();
                break;
            case "Ансамбли по количеству произведений":
                loadEnsemblesByCompositions();
                break;
            case "Музыканты по количеству ансамблей":
                loadMusiciansByEnsembles();
                break;
            case "Произведения по популярности":
                loadCompositionsByPopularity();
                break;
            case "Финансовая аналитика":
                loadFinancialAnalytics();
                break;
        }
    }

    /**
     * Обработчик изменения выбора в комбобоксе аналитики
     */
    @FXML
    private void onAnalyticsSelectionChanged() {
        loadAnalyticsData();
    }

    /**
     * Загрузка полной информации о пластинках
     */
    private void loadCompleteRecordInfo() {
        String query = "SELECT r.title as record_title, e.name as ensemble_name, COUNT(DISTINCT rt.composition_id) as compositions_count, ROUND(COUNT(DISTINCT rt.composition_id) * 3.5, 1) as total_duration, COUNT(DISTINCT em.musician_id) as musicians_count, r.current_year_sales, ROUND(r.current_year_sales * r.retail_price, 2) as total_revenue FROM records r LEFT JOIN record_tracks rt ON r.record_id = rt.record_id LEFT JOIN performances p ON rt.composition_id = p.composition_id LEFT JOIN ensembles e ON p.ensemble_id = e.ensemble_id LEFT JOIN ensemble_members em ON e.ensemble_id = em.ensemble_id GROUP BY r.record_id, e.ensemble_id ORDER BY r.current_year_sales DESC, total_revenue DESC";

        setupTable(analyticsTable,
                new String[]{"record_title", "ensemble_name", "compositions_count", "total_duration", "musicians_count", "current_year_sales", "total_revenue"},
                new String[]{"Пластинка", "Ансамбль", "Треков", "Длительность", "Музыкантов", "Продажи", "Выручка"});

        analyticsTable.setItems(loadData(query));
        resultArea.setText("Полная информация о пластинках с ансамблями, количеством треков и финансовыми показателями\nЗагружено записей: " + analyticsTable.getItems().size());
    }

    /**
     * Загрузка рейтинга ансамблей по количеству произведений
     */
    private void loadEnsemblesByCompositions() {
        String query = "SELECT e.name as ensemble_name, COUNT(DISTINCT p.composition_id) as compositions_count, COUNT(DISTINCT em.musician_id) as musicians_count, COUNT(DISTINCT r.record_id) as records_count FROM ensembles e LEFT JOIN performances p ON e.ensemble_id = p.ensemble_id LEFT JOIN ensemble_members em ON e.ensemble_id = em.ensemble_id LEFT JOIN record_tracks rt ON p.composition_id = rt.composition_id LEFT JOIN records r ON rt.record_id = r.record_id GROUP BY e.ensemble_id ORDER BY compositions_count DESC, musicians_count DESC";

        setupTable(analyticsTable,
                new String[]{"ensemble_name", "compositions_count", "musicians_count", "records_count"},
                new String[]{"Ансамбль", "Произведений", "Музыкантов", "Пластинок"});

        analyticsTable.setItems(loadData(query));
        resultArea.setText("Рейтинг ансамблей по количеству произведений в репертуаре\nЗагружено ансамблей: " + analyticsTable.getItems().size());
    }

    /**
     * Загрузка рейтинга музыкантов по количеству ансамблей
     */
    private void loadMusiciansByEnsembles() {
        String query = "SELECT CONCAT(m.first_name, ' ', m.last_name) as musician_name, COUNT(DISTINCT em.ensemble_id) as ensembles_count, GROUP_CONCAT(DISTINCT e.name SEPARATOR ', ') as ensemble_names, COUNT(DISTINCT p.composition_id) as compositions_count FROM musicians m LEFT JOIN ensemble_members em ON m.musician_id = em.musician_id LEFT JOIN ensembles e ON em.ensemble_id = e.ensemble_id LEFT JOIN performances p ON e.ensemble_id = p.ensemble_id GROUP BY m.musician_id ORDER BY ensembles_count DESC, compositions_count DESC";

        setupTable(analyticsTable,
                new String[]{"musician_name", "ensembles_count", "ensemble_names", "compositions_count"},
                new String[]{"Музыкант", "Ансамблей", "Состав ансамблей", "Произведений"});

        analyticsTable.setItems(loadData(query));
        resultArea.setText("Рейтинг музыкантов по количеству ансамблей и произведений\nЗагружено музыкантов: " + analyticsTable.getItems().size());
    }

    /**
     * Загрузка популярности произведений
     */
    private void loadCompositionsByPopularity() {
        String query = "SELECT c.title as composition_title, c.creation_year, COUNT(DISTINCT p.ensemble_id) as ensembles_count, COUNT(DISTINCT rt.record_id) as records_count, GROUP_CONCAT(DISTINCT e.name SEPARATOR ', ') as performing_ensembles FROM compositions c LEFT JOIN performances p ON c.composition_id = p.composition_id LEFT JOIN ensembles e ON p.ensemble_id = e.ensemble_id LEFT JOIN record_tracks rt ON c.composition_id = rt.composition_id GROUP BY c.composition_id ORDER BY records_count DESC, ensembles_count DESC";

        setupTable(analyticsTable,
                new String[]{"composition_title", "creation_year", "ensembles_count", "records_count", "performing_ensembles"},
                new String[]{"Произведение", "Год", "Ансамблей", "Пластинок", "Исполняющие ансамбли"});

        analyticsTable.setItems(loadData(query));
        resultArea.setText("Популярность произведений по количеству записей и исполняющих ансамблей\nЗагружено произведений: " + analyticsTable.getItems().size());
    }

    /**
     * Загрузка финансовой аналитики
     */
    private void loadFinancialAnalytics() {
        String query = "SELECT r.title as record_title, r.current_year_sales, r.retail_price, r.wholesale_price, ROUND(r.current_year_sales * r.retail_price, 2) as total_revenue, ROUND(r.current_year_sales * (r.retail_price - r.wholesale_price), 2) as total_profit, r.remaining_stock, ROUND((r.current_year_sales * 100.0) / (r.current_year_sales + r.remaining_stock), 2) as sales_percentage FROM records r ORDER BY total_revenue DESC, total_profit DESC";

        setupTable(analyticsTable,
                new String[]{"record_title", "current_year_sales", "retail_price", "wholesale_price", "total_revenue", "total_profit", "remaining_stock", "sales_percentage"},
                new String[]{"Пластинка", "Продажи", "Розница", "Опт", "Выручка", "Прибыль", "Остаток", "% продаж"});

        analyticsTable.setItems(loadData(query));
        resultArea.setText("Финансовая аналитика: выручка, прибыль и эффективность продаж\nЗагружено пластинок: " + analyticsTable.getItems().size());
    }

    /**
     * Поиск произведений ансамбля
     */
    @FXML
    private void searchEnsembleCompositions() {
        String name = searchEnsembleField.getText();
        if (!name.isEmpty()) {
            // ПРОВЕРКА: поле не должно быть пустым
            if (name.trim().isEmpty()) {
                showAlert("Ошибка", "Введите название ансамбля");
                return;
            }

            int count = Database.getEnsembleCompositionsCount(name);
            List<Map<String, Object>> records = Database.getEnsembleRecords(name);

            StringBuilder result = new StringBuilder();
            result.append("Ансамбль '").append(name).append("'\n");
            result.append("Исполняет ").append(count).append(" произведений\n");
            result.append("Выпущено пластинок: ").append(records.size()).append("\n\n");

            if (!records.isEmpty()) {
                result.append("Пластинки с участием ансамбля:\n");
                for (Map<String, Object> record : records) {
                    result.append("• ").append(record.get("title")).append(" (продажи: ").append(record.get("current_year_sales")).append(")\n");
                }
            }

            resultArea.setText(result.toString());
        } else {
            resultArea.setText("Введите название ансамбля");
        }
    }

    /**
     * Отображение лидеров продаж
     */
    @FXML
    private void showSalesLeaders() {
        salesLeadersTable.setItems(loadData("SELECT title, current_year_sales, retail_price, remaining_stock FROM records ORDER BY current_year_sales DESC LIMIT 10"));
        resultArea.setText("Отображены лидеры продаж за текущий год\nТоп-10 пластинок по продажам");
    }

    /**
     * Обновление данных о продажах
     */
    @FXML
    private void updateSales() {
        String record = recordSelector.getValue();
        String salesText = salesUpdateField.getText();

        if (record != null && !salesText.isEmpty()) {
            // ПРОВЕРКА: поле продаж не должно быть пустым
            if (salesText.trim().isEmpty()) {
                showAlert("Ошибка", "Введите количество продаж");
                return;
            }

            try {
                int sales = Integer.parseInt(salesText);
                if (sales > 0) {
                    // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед обновлением
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Подтверждение обновления");
                    confirmAlert.setHeaderText("Обновление продаж");
                    confirmAlert.setContentText("Вы действительно хотите обновить продажи для пластинки '" + record + "' на " + sales + " единиц?");

                    Optional<ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // ИСПРАВЛЕНИЕ: получаем ID по названию записи
                        String recordTitle = record; // Теперь record содержит только название
                        String recordId = getEntityIdFromName("records", "title", recordTitle);

                        if (!recordId.equals("0") && Database.updateRecordSales(Integer.parseInt(recordId), sales)) {
                            UserActionLogger.logAction(currentUserEmail, "Обновление продаж", "Пластинка",
                                    "Обновлены продажи для пластинки: " + record + " на +" + sales + " единиц");
                            loadAllData();
                            showSalesLeaders();
                            loadAnalyticsData();
                            resultArea.setText("Продажи обновлены для: " + record);
                            salesUpdateField.clear();
                        } else {
                            resultArea.setText("Ошибка обновления продаж");
                        }
                    }
                } else {
                    resultArea.setText("Введите положительное число");
                }
            } catch (NumberFormatException e) {
                resultArea.setText("Ошибка: введите корректное число");
            }
        } else {
            resultArea.setText("Выберите пластинку и введите количество продаж");
        }
    }

    // Методы добавления основных сущностей

    /**
     * Добавление нового ансамбля
     */
    @FXML
    private void addEnsemble() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (ensembleName.getText().isEmpty()) {
            showAlert("Ошибка", "Введите название ансамбля");
            return;
        }

        if (ensembleType.getText().isEmpty()) {
            showAlert("Ошибка", "Введите тип ансамбля");
            return;
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление ансамбля");
        confirmAlert.setContentText("Вы действительно хотите добавить ансамбль '" + ensembleName.getText() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = String.format("INSERT INTO ensembles (name, type, description) VALUES ('%s', '%s', '%s')", sanitize(ensembleName.getText()), sanitize(ensembleType.getText()), sanitize(ensembleDesc.getText()));
            if (Database.executeUpdate(query)) {
                UserActionLogger.logAction(currentUserEmail, "Добавление", "Ансамбль",
                        "Добавлен ансамбль: " + ensembleName.getText());
                loadAllData();
                populateAllSelectors();
                clearFields();
                showAlert("Успех", "Ансамбль добавлен");
            } else {
                showAlert("Ошибка", "Не удалось добавить ансамбль");
            }
        }
    }

    /**
     * Добавление нового музыканта
     */
    @FXML
    private void addMusician() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (musicianFirstName.getText().isEmpty() || musicianLastName.getText().isEmpty()) {
            showAlert("Ошибка", "Введите имя и фамилию музыканта");
            return;
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление музыканта");
        String fullName = musicianFirstName.getText().trim() + " " +
                (musicianMiddleName.getText().trim().isEmpty() ? "" : musicianMiddleName.getText().trim() + " ") +
                musicianLastName.getText().trim();
        confirmAlert.setContentText("Вы действительно хотите добавить музыканта '" + fullName + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Формируем запрос с учетом отчества (может быть null)
            String middleNameValue = musicianMiddleName.getText().trim();
            String middleNameSql = middleNameValue.isEmpty() ? "NULL" : "'" + sanitize(middleNameValue) + "'";

            String query = String.format(
                    "INSERT INTO musicians (first_name, middle_name, last_name, bio) VALUES ('%s', %s, '%s', '%s')",
                    sanitize(musicianFirstName.getText().trim()),
                    middleNameSql,
                    sanitize(musicianLastName.getText().trim()),
                    sanitize(musicianBio.getText().trim())
            );

            System.out.println("Executing query: " + query);

            if (Database.executeUpdate(query)) {
                // Формируем полное имя для лога
                String fullNameForLog = musicianFirstName.getText().trim() + " " +
                        (middleNameValue.isEmpty() ? "" : musicianMiddleName.getText().trim() + " ") +
                        musicianLastName.getText().trim();

                UserActionLogger.logAction(currentUserEmail, "Добавление", "Музыкант",
                        "Добавлен музыкант: " + fullNameForLog);

                loadAllData();
                populateAllSelectors();
                clearFields();
                showAlert("Успех", "Музыкант " + fullNameForLog + " успешно добавлен");
            } else {
                showAlert("Ошибка", "Не удалось добавить музыканта. Возможно, такой музыкант уже существует.");
            }
        }
    }

    /**
     * Добавление нового произведения
     */
    @FXML
    private void addComposition() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (compositionTitle.getText().isEmpty()) {
            showAlert("Ошибка", "Введите название произведения");
            return;
        }

        // ПРОВЕРКА: проверяем год, если он указан
        String yearText = compositionYear.getText().trim();
        if (!yearText.isEmpty()) {
            try {
                int year = Integer.parseInt(yearText);
                if (year < 1000 || year > java.time.Year.now().getValue()) {
                    showAlert("Ошибка", "Введите корректный год (1000-" + java.time.Year.now().getValue() + ")");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректный год");
                return;
            }
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление произведения");
        confirmAlert.setContentText("Вы действительно хотите добавить произведение '" + compositionTitle.getText() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String year = compositionYear.getText().isEmpty() ? "NULL" : compositionYear.getText();
            String query = String.format("INSERT INTO compositions (title, creation_year) VALUES ('%s', %s)", sanitize(compositionTitle.getText()), year);
            if (Database.executeUpdate(query)) {
                UserActionLogger.logAction(currentUserEmail, "Добавление", "Произведение",
                        "Добавлено произведение: " + compositionTitle.getText());
                loadAllData();
                populateAllSelectors();
                clearFields();
                showAlert("Успех", "Произведение добавлено");
            } else {
                showAlert("Ошибка", "Не удалось добавить произведение");
            }
        }
    }

    /**
     * Добавление новой пластинки
     */
    @FXML
    private void addRecord() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (recordTitle.getText().isEmpty()) {
            showAlert("Ошибка", "Введите название пластинки");
            return;
        }

        // ПРОВЕРКА: проверяем числовые поля
        String wholesaleText = recordWholesalePrice.getText().trim();
        String retailText = recordRetailPrice.getText().trim();
        String discsText = recordDiscs.getText().trim();

        if (!wholesaleText.isEmpty()) {
            try {
                double wholesale = Double.parseDouble(wholesaleText);
                if (wholesale < 0) {
                    showAlert("Ошибка", "Оптовая цена не может быть отрицательной");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректную оптовую цену");
                return;
            }
        }

        if (!retailText.isEmpty()) {
            try {
                double retail = Double.parseDouble(retailText);
                if (retail < 0) {
                    showAlert("Ошибка", "Розничная цена не может быть отрицательной");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректную розничную цену");
                return;
            }
        }

        if (!discsText.isEmpty()) {
            try {
                int discs = Integer.parseInt(discsText);
                if (discs <= 0) {
                    showAlert("Ошибка", "Количество дисков должно быть положительным числом");
                    return;
                }
                if (discs > 100) {
                    showAlert("Ошибка", "Количество дисков не может превышать 100");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Введите корректное количество дисков");
                return;
            }
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление пластинки");
        confirmAlert.setContentText("Вы действительно хотите добавить пластинку '" + recordTitle.getText() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String wholesale = recordWholesalePrice.getText().isEmpty() ? "0" : recordWholesalePrice.getText();
            String retail = recordRetailPrice.getText().isEmpty() ? "0" : recordRetailPrice.getText();
            String discs = recordDiscs.getText().isEmpty() ? "1" : recordDiscs.getText();

            String query = String.format("INSERT INTO records (title, wholesale_price, retail_price, disc_count) VALUES ('%s', %s, %s, %s)", sanitize(recordTitle.getText()), wholesale, retail, discs);
            if (Database.executeUpdate(query)) {
                UserActionLogger.logAction(currentUserEmail, "Добавление", "Пластинка",
                        "Добавлена пластинка: " + recordTitle.getText());
                loadAllData();
                populateAllSelectors();
                clearFields();
                showAlert("Успех", "Пластинка добавлена");
            } else {
                showAlert("Ошибка", "Не удалось добавить пластинку");
            }
        }
    }

    // Методы добавления связей

    /**
     * Добавление музыканта в ансамбль
     */
    @FXML
    private void addEnsembleMember() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (ensembleSelector.getValue() == null || musicianSelector.getValue() == null || memberRole.getText().isEmpty()) {
            showAlert("Ошибка", "Выберите ансамбль, музыкант и укажите роль");
            return;
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление участника в ансамбль");
        confirmAlert.setContentText("Вы действительно хотите добавить музыканта '" + musicianSelector.getValue() +
                "' в ансамбль '" + ensembleSelector.getValue() + "' с ролью '" + memberRole.getText() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String ensembleName = ensembleSelector.getValue();
                String musicianName = musicianSelector.getValue();

                String ensembleId = getEntityIdFromName("ensembles", "name", ensembleName);
                String musicianId = getMusicianIdFromName(musicianName);

                if (!ensembleId.equals("0") && !musicianId.equals("0")) {
                    String query = String.format("INSERT INTO ensemble_members (ensemble_id, musician_id, role) VALUES (%s, %s, '%s')", ensembleId, musicianId, sanitize(memberRole.getText()));
                    if (Database.executeUpdate(query)) {
                        UserActionLogger.logAction(currentUserEmail, "Добавление", "Состав ансамбля",
                                "Добавлен музыкант " + musicianName + " в ансамбль " + ensembleName + " с ролью: " + memberRole.getText());
                        loadRelationData();
                        memberRole.clear();
                        showAlert("Успех", "Музыкант добавлен в ансамбль");
                    } else {
                        showAlert("Ошибка", "Не удалось добавить музыканта в ансамбль");
                    }
                } else {
                    showAlert("Ошибка", "Не удалось найти выбранные сущности");
                }
            } catch (Exception e) {
                showAlert("Ошибка", "Неверный формат данных");
            }
        }
    }

    /**
     * Добавление исполнения произведения ансамблем
     */
    @FXML
    private void addPerformance() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (performanceEnsembleSelector.getValue() == null || performanceCompositionSelector.getValue() == null) {
            showAlert("Ошибка", "Выберите ансамбль и произведение");
            return;
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление исполнения");
        confirmAlert.setContentText("Вы действительно хотите добавить исполнение произведения '" +
                performanceCompositionSelector.getValue() + "' ансамблем '" +
                performanceEnsembleSelector.getValue() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String ensembleName = performanceEnsembleSelector.getValue();
                String compositionTitle = performanceCompositionSelector.getValue();

                String ensembleId = getEntityIdFromName("ensembles", "name", ensembleName);
                String compositionId = getEntityIdFromName("compositions", "title", compositionTitle);

                if (!ensembleId.equals("0") && !compositionId.equals("0")) {
                    String query = String.format("INSERT INTO performances (ensemble_id, composition_id, arrangement) VALUES (%s, %s, '%s')", ensembleId, compositionId, sanitize(arrangementField.getText()));
                    if (Database.executeUpdate(query)) {
                        UserActionLogger.logAction(currentUserEmail, "Добавление", "Исполнение",
                                "Добавлено исполнение произведения " + compositionTitle + " ансамблем " + ensembleName);
                        loadRelationData();
                        arrangementField.clear();
                        showAlert("Успех", "Исполнение добавлено");
                    } else {
                        showAlert("Ошибка", "Не удалось добавить исполнение");
                    }
                } else {
                    showAlert("Ошибка", "Не удалось найти выбранные сущности");
                }
            } catch (Exception e) {
                showAlert("Ошибка", "Неверный формат данных");
            }
        }
    }

    /**
     * Добавление трека на пластинку
     */
    @FXML
    private void addRecordTrack() {
        // ПРОВЕРКА: проверяем обязательные поля
        if (trackRecordSelector.getValue() == null || trackCompositionSelector.getValue() == null || trackNumberField.getText().isEmpty()) {
            showAlert("Ошибка", "Выберите пластинку, произведение и укажите номер трека");
            return;
        }

        // ПРОВЕРКА: проверяем номер трека
        try {
            int trackNumber = Integer.parseInt(trackNumberField.getText());
            if (trackNumber <= 0) {
                showAlert("Ошибка", "Номер трека должен быть положительным числом");
                return;
            }
            if (trackNumber > 100) {
                showAlert("Ошибка", "Номер трека не может превышать 100");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите корректный номер трека");
            return;
        }

        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение добавления");
        confirmAlert.setHeaderText("Добавление трека");
        confirmAlert.setContentText("Вы действительно хотите добавить трек №" + trackNumberField.getText() +
                " '" + trackCompositionSelector.getValue() + "' на пластинку '" +
                trackRecordSelector.getValue() + "'?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String recordTitle = trackRecordSelector.getValue();
                String compositionTitle = trackCompositionSelector.getValue();

                String recordId = getEntityIdFromName("records", "title", recordTitle);
                String compositionId = getEntityIdFromName("compositions", "title", compositionTitle);
                int trackNumber = Integer.parseInt(trackNumberField.getText());

                if (!recordId.equals("0") && !compositionId.equals("0")) {
                    String query = String.format("INSERT INTO record_tracks (record_id, composition_id, track_number) VALUES (%s, %s, %d)", recordId, compositionId, trackNumber);
                    if (Database.executeUpdate(query)) {
                        UserActionLogger.logAction(currentUserEmail, "Добавление", "Трек на пластинке",
                                "Добавлен трек " + trackNumber + " на пластинку " + recordTitle + ": " + compositionTitle);
                        loadRelationData();
                        trackNumberField.clear();
                        showAlert("Успех", "Трек добавлен на пластинку");
                    } else {
                        showAlert("Ошибка", "Не удалось добавить трек");
                    }
                } else {
                    showAlert("Ошибка", "Не удалось найти выбранные сущности");
                }
            } catch (Exception e) {
                showAlert("Ошибка", "Неверный формат данных");
            }
        }
    }

    // Методы удаления основных сущностей

    /**
     * Удаление ансамбля
     */
    @FXML
    private void deleteEnsemble() {
        Map<String, Object> selected = ensemblesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление ансамбля");
            confirmAlert.setContentText("Вы действительно хотите удалить ансамбль '" + safeGetString(selected, "name") + "'?\nВсе связанные данные также будут удалены.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteEntity(ensemblesTable, "ensembles", "ensemble_id", "Ансамбль");
            }
        } else {
            showAlert("Ошибка", "Выберите ансамбль для удаления");
        }
    }

    /**
     * Удаление музыканта
     */
    @FXML
    private void deleteMusician() {
        Map<String, Object> selected = musiciansTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление музыканта");
            String fullName = safeGetString(selected, "first_name") + " " + safeGetString(selected, "last_name");
            confirmAlert.setContentText("Вы действительно хотите удалить музыканта '" + fullName + "'?\nВсе связанные данные также будут удалены.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteEntity(musiciansTable, "musicians", "musician_id", "Музыкант");
            }
        } else {
            showAlert("Ошибка", "Выберите музыканта для удаления");
        }
    }

    /**
     * Удаление произведения
     */
    @FXML
    private void deleteComposition() {
        Map<String, Object> selected = compositionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление произведения");
            confirmAlert.setContentText("Вы действительно хотите удалить произведение '" + safeGetString(selected, "title") + "'?\nВсе связанные данные также будут удалены.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteEntity(compositionsTable, "compositions", "composition_id", "Произведение");
            }
        } else {
            showAlert("Ошибка", "Выберите произведение для удаления");
        }
    }

    /**
     * Удаление пластинки
     */
    @FXML
    private void deleteRecord() {
        Map<String, Object> selected = recordsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление пластинки");
            confirmAlert.setContentText("Вы действительно хотите удалить пластинку '" + safeGetString(selected, "title") + "'?\nВсе связанные данные также будут удалены.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteEntity(recordsTable, "records", "record_id", "Пластинка");
            }
        } else {
            showAlert("Ошибка", "Выберите пластинку для удаления");
        }
    }

    // Методы удаления связей

    /**
     * Удаление участника ансамбля
     */
    @FXML
    private void deleteEnsembleMember() {
        Map<String, Object> selected = ensembleMembersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление участника из ансамбля");
            confirmAlert.setContentText("Вы действительно хотите удалить музыканта '" +
                    safeGetString(selected, "musician_name") + "' из ансамбля '" +
                    safeGetString(selected, "ensemble_name") + "'?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteRelationEntity(ensembleMembersTable, "ensemble_members", "Состав ансамбля");
            }
        } else {
            showAlert("Ошибка", "Выберите участника для удаления");
        }
    }

    /**
     * Удаление исполнения
     */
    @FXML
    private void deletePerformance() {
        Map<String, Object> selected = performancesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление исполнения");
            confirmAlert.setContentText("Вы действительно хотите удалить исполнение произведения '" +
                    safeGetString(selected, "composition_title") + "' ансамблем '" +
                    safeGetString(selected, "ensemble_name") + "'?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteRelationEntity(performancesTable, "performances", "Исполнение");
            }
        } else {
            showAlert("Ошибка", "Выберите исполнение для удаления");
        }
    }

    /**
     * Удаление трека с пластинки
     */
    @FXML
    private void deleteRecordTrack() {
        Map<String, Object> selected = recordTracksTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление трека с пластинки");
            confirmAlert.setContentText("Вы действительно хотите удалить трек '" +
                    safeGetString(selected, "composition_title") + "' с пластинки '" +
                    safeGetString(selected, "record_title") + "'?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteRelationEntity(recordTracksTable, "record_tracks", "Трек на пластинке");
            }
        } else {
            showAlert("Ошибка", "Выберите трек для удаления");
        }
    }

    // Методы редактирования основных сущностей

    /**
     * Редактирование выбранной записи в таблице ансамблей
     */
    @FXML
    private void editEnsemble() {
        Map<String, Object> selected = ensemblesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editEntity(ensemblesTable, "ensembles", "ensemble_id", "Ансамбль");
        } else {
            showAlert("Ошибка", "Выберите ансамбль для редактирования");
        }
    }

    /**
     * Редактирование выбранной записи в таблице музыкантов - С ОТЧЕСТВОМ
     */
    @FXML
    private void editMusician() {
        Map<String, Object> selected = musiciansTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Создаем диалоговое окно для редактирования
            Dialog<Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("Редактирование музыканта");
            dialog.setHeaderText("Редактирование данных музыканта");

            // Создаем поля для редактирования
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Поля для редактирования
            TextField firstNameField = new TextField(safeGetString(selected, "first_name"));
            TextField middleNameField = new TextField(safeGetString(selected, "middle_name"));
            TextField lastNameField = new TextField(safeGetString(selected, "last_name"));
            TextArea bioArea = new TextArea(safeGetString(selected, "bio"));
            bioArea.setPrefRowCount(5);
            bioArea.setPrefColumnCount(40);

            grid.add(new Label("Имя:"), 0, 0);
            grid.add(firstNameField, 1, 0);
            grid.add(new Label("Отчество:"), 0, 1);
            grid.add(middleNameField, 1, 1);
            grid.add(new Label("Фамилия:"), 0, 2);
            grid.add(lastNameField, 1, 2);
            grid.add(new Label("Биография:"), 0, 3);
            grid.add(bioArea, 1, 3);

            dialog.getDialogPane().setContent(grid);

            // Добавляем кнопки
            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Обработка результата
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Валидация
                    if (firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty()) {
                        showAlert("Ошибка", "Имя и фамилия обязательны для заполнения");
                        return null;
                    }

                    // Обновляем данные
                    selected.put("first_name", firstNameField.getText().trim());
                    selected.put("middle_name", middleNameField.getText().trim());
                    selected.put("last_name", lastNameField.getText().trim());
                    selected.put("bio", bioArea.getText().trim());
                    return selected;
                }
                return null;
            });

            Optional<Map<String, Object>> result = dialog.showAndWait();
            result.ifPresent(updatedData -> {
                // Формируем SQL запрос для обновления
                String middleNameValue = safeGetString(updatedData, "middle_name");
                String middleNameSql = middleNameValue.isEmpty() ? "NULL" : "'" + sanitize(middleNameValue) + "'";

                String query = String.format(
                        "UPDATE musicians SET first_name = '%s', middle_name = %s, last_name = '%s', bio = '%s' " +
                                "WHERE musician_id = %s",
                        sanitize(safeGetString(updatedData, "first_name")),
                        middleNameSql,
                        sanitize(safeGetString(updatedData, "last_name")),
                        sanitize(safeGetString(updatedData, "bio")),
                        selected.get("musician_id")
                );

                System.out.println("Executing update query: " + query);

                if (Database.executeUpdate(query)) {
                    // Формируем полное имя для лога
                    String fullName = safeGetString(updatedData, "first_name") + " " +
                            (middleNameValue.isEmpty() ? "" : safeGetString(updatedData, "middle_name") + " ") +
                            safeGetString(updatedData, "last_name");

                    UserActionLogger.logAction(currentUserEmail, "Редактирование", "Музыкант",
                            "Обновлен музыкант: " + fullName);

                    loadAllData();
                    populateAllSelectors();
                    showAlert("Успех", "Музыкант " + fullName + " успешно обновлен");
                } else {
                    showAlert("Ошибка", "Не удалось обновить данные музыканта");
                }
            });
        } else {
            showAlert("Ошибка", "Выберите музыканта для редактирования");
        }
    }

    /**
     * Редактирование выбранной записи в таблице произведений
     */
    @FXML
    private void editComposition() {
        Map<String, Object> selected = compositionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editEntity(compositionsTable, "compositions", "composition_id", "Произведение");
        } else {
            showAlert("Ошибка", "Выберите произведение для редактирования");
        }
    }

    /**
     * Редактирование выбранной записи в таблице пластинок
     */
    @FXML
    private void editRecord() {
        Map<String, Object> selected = recordsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editEntity(recordsTable, "records", "record_id", "Пластинка");
        } else {
            showAlert("Ошибка", "Выберите пластинку для редактирования");
        }
    }

    // Новые методы редактирования для вкладок связей

    /**
     * Редактирование состава ансамбля
     */
    @FXML
    private void editEnsembleMember() {
        Map<String, Object> selected = ensembleMembersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editRelationEntity(ensembleMembersTable, "ensemble_members", "Состав ансамбля");
        } else {
            showAlert("Ошибка", "Выберите участника для редактирования");
        }
    }

    /**
     * Редактирование исполнения
     */
    @FXML
    private void editPerformance() {
        Map<String, Object> selected = performancesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editRelationEntity(performancesTable, "performances", "Исполнение");
        } else {
            showAlert("Ошибка", "Выберите исполнение для редактирования");
        }
    }

    /**
     * Редактирование трека на пластинке
     */
    @FXML
    private void editRecordTrack() {
        Map<String, Object> selected = recordTracksTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editRelationEntity(recordTracksTable, "record_tracks", "Трек на пластинке");
        } else {
            showAlert("Ошибка", "Выберите трек для редактирования");
        }
    }

    /**
     * Универсальный метод редактирования сущности - БЕЗ image и created_by
     */
    private void editEntity(TableView<Map<String, Object>> table, String tableName, String idColumn, String entityName) {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Создаем диалоговое окно для редактирования
            Dialog<Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("Редактирование " + entityName.toLowerCase());
            dialog.setHeaderText("Редактирование данных " + entityName.toLowerCase());

            // Создаем поля для редактирования
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Динамически создаем поля на основе выбранной записи
            int row = 0;
            for (Map.Entry<String, Object> entry : selected.entrySet()) {
                String key = entry.getKey();

                // ИСКЛЮЧАЕМ только те поля, которые не нужно редактировать
                // image и created_by - убираем из окна редактирования, но они остаются в базе
                if (!key.equals(idColumn) &&
                        !key.equals("image") &&
                        !key.equals("created_by") &&
                        !key.equals("created_at") &&
                        !key.equals("updated_at")) {

                    Label label = new Label(getRussianFieldName(key) + ":");
                    Object value = entry.getValue();
                    String valueStr = value != null ? value.toString() : "";

                    // Для больших текстовых полей используем TextArea
                    if (key.equals("description") || key.equals("bio")) {
                        TextArea textArea = new TextArea(valueStr);
                        textArea.setPrefRowCount(4);
                        textArea.setPrefColumnCount(30);
                        textArea.setId(key);
                        grid.add(label, 0, row);
                        grid.add(textArea, 1, row);
                    } else {
                        TextField textField = new TextField(valueStr);
                        textField.setId(key);
                        grid.add(label, 0, row);
                        grid.add(textField, 1, row);
                    }
                    row++;
                }
            }

            dialog.getDialogPane().setContent(grid);

            // Добавляем кнопки
            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Обработка результата
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Собираем обновленные данные
                    for (Map.Entry<String, Object> entry : selected.entrySet()) {
                        String key = entry.getKey();

                        // Пропускаем поля, которые не редактировали
                        if (key.equals(idColumn) ||
                                key.equals("image") ||
                                key.equals("created_by") ||
                                key.equals("created_at") ||
                                key.equals("updated_at")) {
                            continue;
                        }

                        if (key.equals("description") || key.equals("bio")) {
                            TextArea field = (TextArea) grid.lookup("#" + key);
                            if (field != null) {
                                selected.put(key, field.getText());
                            }
                        } else {
                            TextField field = (TextField) grid.lookup("#" + key);
                            if (field != null) {
                                selected.put(key, field.getText());
                            }
                        }
                    }
                    return selected;
                }
                return null;
            });

            Optional<Map<String, Object>> result = dialog.showAndWait();
            result.ifPresent(updatedData -> {
                // Формируем SQL запрос для обновления
                StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : updatedData.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // Пропускаем системные поля и поля, которые не редактировали
                    if (!key.equals(idColumn) &&
                            !key.equals("image") &&
                            !key.equals("created_by") &&
                            !key.equals("created_at") &&
                            !key.equals("updated_at") &&
                            value != null) {

                        if (!first) {
                            query.append(", ");
                        }
                        query.append(key).append(" = '").append(sanitize(value.toString())).append("'");
                        first = false;
                    }
                }

                String finalQuery = query.append(" WHERE ").append(idColumn).append(" = ").append(selected.get(idColumn)).toString();
                System.out.println("Executing edit query: " + finalQuery);

                if (Database.executeUpdate(finalQuery)) {
                    UserActionLogger.logAction(currentUserEmail, "Редактирование", entityName,
                            "Обновлена запись ID: " + selected.get(idColumn));
                    loadAllData();
                    showAlert("Успех", entityName + " успешно обновлен");
                } else {
                    showAlert("Ошибка", "Не удалось обновить " + entityName);
                }
            });
        } else {
            showAlert("Ошибка", "Выберите " + entityName + " для редактирования");
        }
    }
    /**
     * Универсальный метод редактирования связей - БЕЗ image и created_by
     */
    private void editRelationEntity(TableView<Map<String, Object>> table, String tableName, String entityName) {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // СОХРАНЯЕМ СТАРЫЕ ДАННЫЕ ДЛЯ ОБНОВЛЕНИЯ ТАБЛИЦЫ
            Map<String, Object> oldSelectedData = new HashMap<>(selected);

            Dialog<Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("Редактирование " + entityName.toLowerCase());

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            int row = 0;
            Map<String, ComboBox<String>> comboBoxes = new HashMap<>();

            for (Map.Entry<String, Object> entry : selected.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // ИСКЛЮЧАЕМ ненужные поля из окна редактирования
                if (key.equals("image") || key.equals("created_by") ||
                        key.equals("created_at") || key.equals("updated_at")) {
                    continue;
                }

                Label label = new Label(getRussianFieldName(key) + ":");

                if (key.equals("ensemble_name") || key.equals("musician_name") ||
                        key.equals("composition_title") || key.equals("record_title")) {
                    // Используем ComboBox для связанных сущностей
                    ComboBox<String> comboBox = new ComboBox<>();
                    comboBox.setPrefWidth(200);

                    // Заполняем данными
                    if (key.equals("ensemble_name")) {
                        populateComboBoxForEdit(comboBox, "SELECT ensemble_id, name FROM ensembles");
                    } else if (key.equals("musician_name")) {
                        populateComboBoxForEdit(comboBox, "SELECT musician_id, CONCAT(first_name, ' ', last_name) as name FROM musicians");
                    } else if (key.equals("composition_title")) {
                        populateComboBoxForEdit(comboBox, "SELECT composition_id, title FROM compositions");
                    } else if (key.equals("record_title")) {
                        populateComboBoxForEdit(comboBox, "SELECT record_id, title FROM records");
                    }

                    // Устанавливаем текущее значение
                    String currentValue = value != null ? value.toString() : "";
                    setCurrentValueInComboBox(comboBox, currentValue);
                    comboBoxes.put(key, comboBox);
                    grid.add(label, 0, row);
                    grid.add(comboBox, 1, row);
                } else {
                    // Обычное текстовое поле для остальных данных
                    String currentValue = value != null ? value.toString() : "";
                    TextField textField = new TextField(currentValue);
                    textField.setId(key);
                    grid.add(label, 0, row);
                    grid.add(textField, 1, row);
                }
                row++;
            }

            dialog.getDialogPane().setContent(grid);
            ButtonType saveButtonType = new ButtonType("Сохранить", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Обновляем данные из ComboBox
                    for (Map.Entry<String, ComboBox<String>> comboEntry : comboBoxes.entrySet()) {
                        String value = comboEntry.getValue().getValue();
                        if (value != null) {
                            selected.put(comboEntry.getKey(), value);
                        }
                    }

                    // Обновляем данные из текстовых полей
                    for (Map.Entry<String, Object> entry : selected.entrySet()) {
                        String key = entry.getKey();

                        // Пропускаем ненужные поля
                        if (key.equals("image") || key.equals("created_by") ||
                                key.equals("created_at") || key.equals("updated_at")) {
                            continue;
                        }

                        if (!comboBoxes.containsKey(key)) {
                            TextField field = (TextField) grid.lookup("#" + key);
                            if (field != null) {
                                selected.put(key, field.getText());
                            }
                        }
                    }
                    return selected;
                }
                return null;
            });

            Optional<Map<String, Object>> result = dialog.showAndWait();
            result.ifPresent(updatedData -> {
                // Обновленная логика UPDATE с использованием ID из ComboBox
                boolean success = updateRelationData(tableName, oldSelectedData, updatedData, comboBoxes);
                if (success) {
                    UserActionLogger.logAction(currentUserEmail, "Редактирование", entityName,
                            "Обновлена связь: " + entityName);
                    // Принудительно обновляем данные и интерфейс
                    loadRelationData();
                    table.refresh();
                    showAlert("Успех", entityName + " успешно обновлен");
                } else {
                    showAlert("Ошибка", "Не удалось обновить " + entityName);
                }
            });
        } else {
            showAlert("Ошибка", "Выберите " + entityName + " для редактирования");
        }
    }

    /**
     * Получает русское название для поля
     */
    private String getRussianFieldName(String fieldName) {
        if (fieldName == null) return "";

        switch (fieldName) {
            // Основные поля
            case "first_name": return "Имя";
            case "middle_name": return "Отчество";
            case "last_name": return "Фамилия";
            case "bio": return "Биография";
            case "name": return "Название";
            case "type": return "Тип";
            case "description": return "Описание";
            case "title": return "Название";
            case "creation_year": return "Год создания";
            case "year": return "Год";
            case "disc_count": return "Количество дисков";
            case "current_year_sales": return "Продажи за год";
            case "remaining_stock": return "Остаток на складе";
            case "role": return "Роль";
            case "arrangement": return "Аранжировка";
            case "track_number": return "Номер трека";

            // Поля связей
            case "ensemble_name": return "Ансамбль";
            case "musician_name": return "Музыкант";
            case "composition_title": return "Произведение";
            case "record_title": return "Пластинка";
            case "compositions_count": return "Количество произведений";
            case "total_duration": return "Общая длительность";
            case "musicians_count": return "Количество музыкантов";
            case "total_revenue": return "Общая выручка";
            case "ensembles_count": return "Количество ансамблей";
            case "records_count": return "Количество пластинок";
            case "ensemble_names": return "Ансамбли";
            case "performing_ensembles": return "Исполняющие ансамбли";
            case "total_profit": return "Общая прибыль";
            case "sales_percentage": return "Процент продаж";

            // Системные поля (на всякий случай)
            case "image": return "Изображение";
            case "created_by": return "Создано";
            case "created_at": return "Дата создания";
            case "updated_at": return "Дата обновления";

            // ID поля
            case "ensemble_id": return "ID ансамбля";
            case "musician_id": return "ID музыканта";
            case "composition_id": return "ID произведения";
            case "record_id": return "ID пластинки";

            // Для аналитики
            case "wholesale_price": return "Цена опт";
            case "retail_price": return "Цена розница";

            default:
                // Если поле содержит underscore, заменяем на пробелы
                return fieldName.replace("_", " ");
        }
    }

    /**
     * Устанавливает текущее значение в ComboBox, находя соответствующую запись
     */
    private void setCurrentValueInComboBox(ComboBox<String> comboBox, String currentValue) {
        if (currentValue == null || currentValue.isEmpty()) {
            return;
        }

        // Ищем запись, которая содержит текущее значение
        for (String item : comboBox.getItems()) {
            if (item.equals(currentValue)) {
                comboBox.setValue(item);
                return;
            }
        }

        // Если не нашли, устанавливаем первую запись
        if (!comboBox.getItems().isEmpty()) {
            comboBox.setValue(comboBox.getItems().get(0));
        }
    }

    /**
     * Извлекает только название из строки формата "ID: Название"
     */
    private String extractNameFromDisplayString(String displayString) {
        if (displayString == null || displayString.isEmpty()) {
            return "";
        }

        // Если строка содержит двоеточие, берем часть после него
        if (displayString.contains(":")) {
            String[] parts = displayString.split(":", 2); // Разделяем только на 2 части
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        // Если двоеточия нет, возвращаем исходную строку
        return displayString.trim();
    }

    /**
     * Обновляет данные связей в базе данных - ИСПРАВЛЕНО: правильная проверка успешности операции
     */
    private boolean updateRelationData(String tableName, Map<String, Object> oldData,
                                       Map<String, Object> newData, Map<String, ComboBox<String>> comboBoxes) {
        try {
            if (tableName.equals("ensemble_members")) {
                // Получаем ID из старых данных для WHERE условия
                String oldEnsembleName = safeGetString(oldData, "ensemble_name");
                String oldMusicianName = safeGetString(oldData, "musician_name");

                // ИЗМЕНЕНИЕ: Извлекаем только названия
                String oldEnsembleNameOnly = extractNameFromDisplayString(oldEnsembleName);
                String oldMusicianNameOnly = extractNameFromDisplayString(oldMusicianName);

                String oldEnsembleId = getEntityIdFromName("ensembles", "name", oldEnsembleNameOnly);
                String oldMusicianId = getMusicianIdFromName(oldMusicianNameOnly);

                // Извлекаем новые значения из ComboBox
                String newEnsembleValue = comboBoxes.get("ensemble_name").getValue();
                String newMusicianValue = comboBoxes.get("musician_name").getValue();

                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String newEnsembleId = getEntityIdFromName("ensembles", "name", newEnsembleValue);
                String newMusicianId = getMusicianIdFromName(newMusicianValue);
                String newRole = safeGetString(newData, "role");

                // Проверяем, что мы нашли правильные ID
                if (oldEnsembleId.equals("0") || oldMusicianId.equals("0") || newEnsembleId.equals("0") || newMusicianId.equals("0")) {
                    System.err.println("Не удалось найти ID для данных: " + oldEnsembleNameOnly + ", " + oldMusicianNameOnly + ", " + newEnsembleValue + ", " + newMusicianValue);
                    return false;
                }

                String query = "UPDATE ensemble_members SET ensemble_id = " + newEnsembleId +
                        ", musician_id = " + newMusicianId + ", role = '" + sanitize(newRole) +
                        "' WHERE ensemble_id = " + oldEnsembleId + " AND musician_id = " + oldMusicianId;

                System.out.println("Executing query: " + query);
                boolean success = Database.executeUpdate(query);
                System.out.println("Update result: " + success);
                return success;
            }
            else if (tableName.equals("performances")) {
                // Получаем ID из старых данных для WHERE условия
                String oldEnsembleName = safeGetString(oldData, "ensemble_name");
                String oldCompositionTitle = safeGetString(oldData, "composition_title");

                // ИЗМЕНЕНИЕ: Извлекаем только названия
                String oldEnsembleNameOnly = extractNameFromDisplayString(oldEnsembleName);
                String oldCompositionTitleOnly = extractNameFromDisplayString(oldCompositionTitle);

                String oldEnsembleId = getEntityIdFromName("ensembles", "name", oldEnsembleNameOnly);
                String oldCompositionId = getEntityIdFromName("compositions", "title", oldCompositionTitleOnly);

                // Извлекаем новые значения из ComboBox
                String newEnsembleValue = comboBoxes.get("ensemble_name").getValue();
                String newCompositionValue = comboBoxes.get("composition_title").getValue();

                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String newEnsembleId = getEntityIdFromName("ensembles", "name", newEnsembleValue);
                String newCompositionId = getEntityIdFromName("compositions", "title", newCompositionValue);
                String newArrangement = safeGetString(newData, "arrangement");

                // Проверяем, что мы нашли правильные ID
                if (oldEnsembleId.equals("0") || oldCompositionId.equals("0") || newEnsembleId.equals("0") || newCompositionId.equals("0")) {
                    System.err.println("Не удалось найти ID для данных: " + oldEnsembleNameOnly + ", " + oldCompositionTitleOnly + ", " + newEnsembleValue + ", " + newCompositionValue);
                    return false;
                }

                String query = "UPDATE performances SET ensemble_id = " + newEnsembleId +
                        ", composition_id = " + newCompositionId + ", arrangement = '" + sanitize(newArrangement) +
                        "' WHERE ensemble_id = " + oldEnsembleId + " AND composition_id = " + oldCompositionId;

                System.out.println("Executing query: " + query);
                boolean success = Database.executeUpdate(query);
                System.out.println("Update result: " + success);
                return success;
            }
            else if (tableName.equals("record_tracks")) {
                // Получаем ID из старых данных для WHERE условия
                String oldRecordTitle = safeGetString(oldData, "record_title");
                String oldCompositionTitle = safeGetString(oldData, "composition_title");

                // ИЗМЕНЕНИЕ: Извлекаем только названия
                String oldRecordTitleOnly = extractNameFromDisplayString(oldRecordTitle);
                String oldCompositionTitleOnly = extractNameFromDisplayString(oldCompositionTitle);

                String oldRecordId = getEntityIdFromName("records", "title", oldRecordTitleOnly);
                String oldCompositionId = getEntityIdFromName("compositions", "title", oldCompositionTitleOnly);

                // Извлекаем новые значения из ComboBox
                String newRecordValue = comboBoxes.get("record_title").getValue();
                String newCompositionValue = comboBoxes.get("composition_title").getValue();

                // ИСПРАВЛЕНИЕ: получаем ID по названиям
                String newRecordId = getEntityIdFromName("records", "title", newRecordValue);
                String newCompositionId = getEntityIdFromName("compositions", "title", newCompositionValue);
                String newTrackNumber = safeGetString(newData, "track_number");

                // Проверяем, что trackNumber - число
                try {
                    Integer.parseInt(newTrackNumber);
                } catch (NumberFormatException e) {
                    newTrackNumber = "1";
                }

                // Проверяем, что мы нашли правильные ID
                if (oldRecordId.equals("0") || oldCompositionId.equals("0") || newRecordId.equals("0") || newCompositionId.equals("0")) {
                    System.err.println("Не удалось найти ID для данных: " + oldRecordTitleOnly + ", " + oldCompositionTitleOnly + ", " + newRecordValue + ", " + newCompositionValue);
                    return false;
                }

                String query = "UPDATE record_tracks SET record_id = " + newRecordId +
                        ", composition_id = " + newCompositionId + ", track_number = " + newTrackNumber +
                        " WHERE record_id = " + oldRecordId + " AND composition_id = " + oldCompositionId;

                System.out.println("Executing query: " + query);
                boolean success = Database.executeUpdate(query);
                System.out.println("Update result: " + success);
                return success;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in updateRelationData: " + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Безопасно получает строку из Map с проверкой на null
     */
    private String safeGetString(Map<String, Object> map, String key) {
        if (map == null || key == null) return "";
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Получает ID сущности по имени из базы данных
     */
    private String getEntityIdFromName(String tableName, String nameColumn, String nameValue) {
        if (nameValue == null || nameValue.isEmpty()) {
            System.err.println("Пустое значение для поиска в таблице " + tableName);
            return "0";
        }

        try {
            String idColumn = tableName.substring(0, tableName.length() - 1) + "_id";
            String query = "SELECT " + idColumn + " FROM " + tableName +
                    " WHERE " + nameColumn + " = '" + sanitize(nameValue) + "'";

            System.out.println("Поиск ID для " + tableName + ": " + query);

            List<Map<String, Object>> result = Database.executeQuery(query);
            if (!result.isEmpty() && result.get(0).get(idColumn) != null) {
                String foundId = result.get(0).get(idColumn).toString();
                System.out.println("Найден ID: " + foundId + " для " + nameValue);
                return foundId;
            } else {
                System.err.println("Не найден ID для " + tableName + " с именем: " + nameValue);
                // Попробуем найти по частичному совпадению
                String likeQuery = "SELECT " + idColumn + " FROM " + tableName +
                        " WHERE " + nameColumn + " LIKE '%" + sanitize(nameValue) + "%'";
                List<Map<String, Object>> likeResult = Database.executeQuery(likeQuery);
                if (!likeResult.isEmpty() && likeResult.get(0).get(idColumn) != null) {
                    String foundId = likeResult.get(0).get(idColumn).toString();
                    System.out.println("Найден ID по частичному совпадению: " + foundId + " для " + nameValue);
                    return foundId;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting ID for " + tableName + " with name: " + nameValue);
            e.printStackTrace();
        }
        return "0";
    }

    /**
     * Получает ID музыканта по имени и фамилии
     */
    private String getMusicianIdFromName(String musicianName) {
        if (musicianName == null || musicianName.isEmpty()) {
            System.err.println("Пустое имя музыканта");
            return "0";
        }

        // ИЗМЕНЕНИЕ: Извлекаем только имя из строки формата "ID: Имя Фамилия"
        String musicianNameOnly = extractNameFromDisplayString(musicianName);

        try {
            // Сначала попробуем точное совпадение по полному имени
            String exactQuery = "SELECT musician_id FROM musicians WHERE CONCAT(first_name, ' ', last_name) = '" + sanitize(musicianNameOnly) + "'";
            List<Map<String, Object>> exactResult = Database.executeQuery(exactQuery);
            if (!exactResult.isEmpty() && exactResult.get(0).get("musician_id") != null) {
                String foundId = exactResult.get(0).get("musician_id").toString();
                System.out.println("Найден ID музыканта по точному совпадению: " + foundId + " для " + musicianNameOnly);
                return foundId;
            }

            // Если точное не сработало, попробуем разделить имя и фамилию
            String[] names = musicianNameOnly.split(" ");
            if (names.length >= 2) {
                String firstName = names[0];
                String lastName = names[1];
                String query = "SELECT musician_id FROM musicians WHERE first_name = '" + sanitize(firstName) +
                        "' AND last_name = '" + sanitize(lastName) + "'";
                List<Map<String, Object>> result = Database.executeQuery(query);
                if (!result.isEmpty() && result.get(0).get("musician_id") != null) {
                    String foundId = result.get(0).get("musician_id").toString();
                    System.out.println("Найден ID музыканта: " + foundId + " для " + musicianNameOnly);
                    return foundId;
                }
            }

            System.err.println("Не найден ID для музыканта: " + musicianNameOnly);

        } catch (Exception e) {
            System.err.println("Error getting musician ID for: " + musicianNameOnly);
            e.printStackTrace();
        }
        return "0";
    }

    /**
     * Удаление основной сущности из базы данных
     * @param table таблица с данными
     * @param tableName название таблицы в БД
     * @param idColumn название колонки с идентификатором
     * @param entityName название сущности для сообщений
     */
    private void deleteEntity(TableView<Map<String, Object>> table, String tableName, String idColumn, String entityName) {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String query = "DELETE FROM " + tableName + " WHERE " + idColumn + " = " + selected.get(idColumn);
            if (Database.executeUpdate(query)) {
                UserActionLogger.logAction(currentUserEmail, "Удаление", entityName,
                        "Удалена запись ID: " + selected.get(idColumn));
                loadAllData();
                populateAllSelectors();
                showAlert("Успех", entityName + " удален");
            } else {
                showAlert("Ошибка", "Не удалось удалить " + entityName);
            }
        } else {
            showAlert("Ошибка", "Выберите " + entityName + " для удаления");
        }
    }

    /**
     * Удаление связи между сущностями
     * @param table таблица с данными
     * @param tableName название таблицы в БД
     * @param entityName название сущности для сообщений
     */
    private void deleteRelationEntity(TableView<Map<String, Object>> table, String tableName, String entityName) {
        Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String query = "";
            if (tableName.equals("ensemble_members")) {
                String ensembleName = selected.get("ensemble_name").toString();
                String musicianName = selected.get("musician_name").toString();
                query = "DELETE FROM ensemble_members WHERE ensemble_id = (SELECT ensemble_id FROM ensembles WHERE name = '" + sanitize(ensembleName) + "') AND musician_id = (SELECT musician_id FROM musicians WHERE CONCAT(first_name, ' ', last_name) = '" + sanitize(musicianName) + "')";
            } else if (tableName.equals("performances")) {
                String ensembleName = selected.get("ensemble_name").toString();
                String compositionTitle = selected.get("composition_title").toString();
                query = "DELETE FROM performances WHERE ensemble_id = (SELECT ensemble_id FROM ensembles WHERE name = '" + sanitize(ensembleName) + "') AND composition_id = (SELECT composition_id FROM compositions WHERE title = '" + sanitize(compositionTitle) + "')";
            } else if (tableName.equals("record_tracks")) {
                String recordTitle = selected.get("record_title").toString();
                String compositionTitle = selected.get("composition_title").toString();
                query = "DELETE FROM record_tracks WHERE record_id = (SELECT record_id FROM records WHERE title = '" + sanitize(recordTitle) + "') AND composition_id = (SELECT composition_id FROM compositions WHERE title = '" + sanitize(compositionTitle) + "')";
            }

            if (Database.executeUpdate(query)) {
                UserActionLogger.logAction(currentUserEmail, "Удаление", entityName,
                        "Удалена связь: " + entityName);
                loadRelationData();
                showAlert("Успех", entityName + " удален");
            } else {
                showAlert("Ошибка", "Не удалось удалить " + entityName);
            }
        } else {
            showAlert("Ошибка", "Выберите " + entityName + " для удаления");
        }
    }

    /**
     * Показать информационное сообщение
     * @param title заголовок сообщения
     * @param message текст сообщения
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Очистка полей ввода основных сущностей
     */
    private void clearFields() {
        ensembleName.clear(); ensembleType.clear(); ensembleDesc.clear();
        musicianFirstName.clear(); musicianMiddleName.clear(); musicianLastName.clear(); musicianBio.clear();
        compositionTitle.clear(); compositionYear.clear();
        recordTitle.clear(); recordWholesalePrice.clear(); recordRetailPrice.clear(); recordDiscs.clear();
    }

    /**
     * Очистка полей ввода связей
     */
    private void clearRelationFields() {
        memberRole.clear(); arrangementField.clear(); trackNumberField.clear();
    }

    /**
     * Обновление всех данных - ИСПРАВЛЕНО: полное обновление всех данных
     */
    @FXML
    private void refreshAllData() {
        loadAllData();
        populateAllSelectors();
        loadRelationData();
        loadAnalyticsData();
        showSalesLeaders();
        resultArea.setText("Все данные обновлены");
    }

    /**
     * Очистка всех полей ввода
     */
    @FXML
    private void clearAllFields() {
        clearFields();
        clearRelationFields();
        resultArea.clear();
    }

    /**
     * Санитизация ввода для защиты от SQL-инъекций
     * @param input входная строка
     * @return безопасная строка
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }

    @FXML
    private void handleLogout() {
        // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед выходом
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение выхода");
        confirmAlert.setHeaderText("Выход из системы");
        confirmAlert.setContentText("Вы действительно хотите выйти из системы?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            UserActionLogger.logAction(currentUserEmail, "Выход из системы", "Система",
                    "Пользователь вышел из системы");
            try {
                Stage currentStage = (Stage) tabPane.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/musicstore/login.fxml"));
                Parent root = loader.load();

                Stage loginStage = new Stage();
                loginStage.setTitle("Music Store - Авторизация");
                loginStage.setScene(new Scene(root, 700, 600));
                loginStage.setResizable(false);

                loginStage.show();
                currentStage.close();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка при выходе из системы");
            }
        }
    }

    /**
     * Метод обновления данных активной вкладки - ИСПРАВЛЕНО: принудительное обновление для исполнений
     */
    private void refreshCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabName = selectedTab.getText();
            switch (tabName) {
                case "Ансамбли":
                    ensemblesTable.setItems(loadData("SELECT * FROM ensembles"));
                    break;
                case "Музыканты":
                    musiciansTable.setItems(loadData("SELECT * FROM musicians"));
                    break;
                case "Произведения":
                    compositionsTable.setItems(loadData("SELECT * FROM compositions"));
                    break;
                case "Пластинки":
                    recordsTable.setItems(loadData("SELECT * FROM records"));
                    break;
                case "Состав ансамблей":
                    // ИСПРАВЛЕНИЕ: принудительно обновляем данные
                    ensembleMembersTable.setItems(loadData("SELECT e.name as ensemble_name, CONCAT(m.first_name, ' ', m.last_name) as musician_name, em.role FROM ensemble_members em JOIN ensembles e ON em.ensemble_id = e.ensemble_id JOIN musicians m ON em.musician_id = m.musician_id ORDER BY e.name, em.role"));
                    populateComboBox(ensembleSelector, "SELECT ensemble_id, name FROM ensembles");
                    populateComboBox(musicianSelector, "SELECT musician_id, first_name, last_name FROM musicians");
                    break;
                case "Исполнения":
                    // ИСПРАВЛЕНИЕ: принудительно обновляем данные для исполнений
                    performancesTable.setItems(loadData("SELECT e.name as ensemble_name, c.title as composition_title, p.arrangement FROM performances p JOIN ensembles e ON p.ensemble_id = e.ensemble_id JOIN compositions c ON p.composition_id = c.composition_id ORDER BY e.name, c.title"));
                    populateComboBox(performanceEnsembleSelector, "SELECT ensemble_id, name FROM ensembles");
                    populateComboBox(performanceCompositionSelector, "SELECT composition_id, title FROM compositions");
                    break;
                case "Треки на пластинках":
                    // ИСПРАВЛЕНИЕ: принудительно обновляем данные
                    recordTracksTable.setItems(loadData("SELECT r.title as record_title, c.title as composition_title, rt.track_number FROM record_tracks rt JOIN records r ON rt.record_id = r.record_id JOIN compositions c ON rt.composition_id = c.composition_id ORDER BY r.title, rt.track_number"));
                    populateComboBox(trackRecordSelector, "SELECT record_id, title FROM records");
                    populateComboBox(trackCompositionSelector, "SELECT composition_id, title FROM compositions");
                    break;
                case "Аналитика":
                    loadAnalyticsData();
                    populateComboBox(recordSelector, "SELECT record_id, title FROM records");
                    break;
                case "Лидеры продаж":
                    showSalesLeaders();
                    break;
                case "История действий":
                    // Обновление данных вкладки истории действий
                    updateUserActionsTab();
                    break;
            }
        }
    }

    private void updateUserActionsTab() {
        if (userActionsTab != null && userActionsTab.getContent() != null) {
            // Получаем контроллер вкладки истории действий
            UserActionsController userActionsController = (UserActionsController) userActionsTab.getProperties().get("controller");
            if (userActionsController != null) {
                userActionsController.loadUserActions();
            }
        }
    }

    /**
     * Инициализация вкладки истории действий
     */
    private void initializeUserActionsTab() {
        if (userActionsTab != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/musicstore/user_actions.fxml"));
                Parent userActionsContent = loader.load();

                UserActionsController userActionsController = loader.getController();
                userActionsController.setUserEmail(currentUserEmail);

                // Сохраняем контроллер в свойствах вкладки для последующего доступа
                userActionsTab.setContent(userActionsContent);
                userActionsTab.getProperties().put("controller", userActionsController);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Ошибка", "Не удалось загрузить вкладку истории действий");
            }
        }
    }

    /**
     * Обработчик добавления изображения
     */
    @FXML
    private void addImage() {
        System.out.println("=== НАЧАЛО addImage() ===");

        // Способ 1: Попробуем получить выбранную запись из активной таблицы
        Map<String, Object> selectedEntity = getSimpleSelectedEntity();

        if (selectedEntity == null) {
            // Способ 2: Если не нашли в таблице, покажем диалог выбора
            showEntitySelectionDialog();
            return;
        }

        // Если нашли сущность, продолжаем
        processEntityForImage(selectedEntity);
    }

    /**
     * Простой метод получения выбранной сущности - ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    private Map<String, Object> getSimpleSelectedEntity() {
        // ОПРЕДЕЛЯЕМ ПО АКТИВНОЙ ВКЛАДКЕ
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        String activeTab = selectedTab != null ? selectedTab.getText() : "";

        System.out.println("🔍 Поиск выбранной сущности. Активная вкладка: " + activeTab);

        Map<String, Object> selected = null;

        switch (activeTab) {
            case "Музыканты":
                selected = musiciansTable.getSelectionModel().getSelectedItem();
                if (selected != null) System.out.println("🎵 Найден МУЗЫКАНТ: " + safeGetString(selected, "first_name") + " " + safeGetString(selected, "last_name"));
                break;
            case "Ансамбли":
                selected = ensemblesTable.getSelectionModel().getSelectedItem();
                if (selected != null) System.out.println("🎵 Найден АНСАМБЛЬ: " + safeGetString(selected, "name"));
                break;
            case "Произведения":
                selected = compositionsTable.getSelectionModel().getSelectedItem();
                if (selected != null) System.out.println("🎵 Найден ПРОИЗВЕДЕНИЕ: " + safeGetString(selected, "title"));
                break;
            case "Пластинки":
                selected = recordsTable.getSelectionModel().getSelectedItem();
                if (selected != null) System.out.println("🎵 Найден ПЛАСТИНКА: " + safeGetString(selected, "title"));
                break;
            default:
                System.out.println("❌ Неизвестная вкладка: " + activeTab);
        }

        if (selected == null) {
            System.out.println("❌ Ни одна запись не выбрана в таблице: " + activeTab);
        }

        return selected;
    }


    private void showEntitySelectionDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Ансамбль", "Ансамбль", "Музыкант", "Произведение", "Пластинка");
        dialog.setTitle("Выбор типа сущности");
        dialog.setHeaderText("Вы действительно хотите добавить изображение?");
        dialog.setContentText("Выберите тип сущности для добавления изображения:");

        Optional<String> typeResult = dialog.showAndWait();
        if (typeResult.isPresent()) {
            String entityType = typeResult.get();
            showNameInputDialog(entityType);
        }
    }
    /**
     * Диалог ввода названия сущности
     */
    private void showNameInputDialog(String entityType) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ввод названия");
        dialog.setHeaderText("Добавление изображения для " + entityType);
        dialog.setContentText("Введите название:");

        Optional<String> nameResult = dialog.showAndWait();
        if (nameResult.isPresent() && !nameResult.get().trim().isEmpty()) {
            String entityName = nameResult.get().trim();
            String englishType = convertRussianTypeToEnglish(entityType);

            openImageSelectionDialog(englishType, entityName);
        }
    }

    /**
     * Обработка найденной сущности - ПРОСТАЯ ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    private void processEntityForImage(Map<String, Object> entity) {
        String entityType = "";
        String entityName = "";

        // ОПРЕДЕЛЯЕМ ПО АКТИВНОЙ ВКЛАДКЕ - САМЫЙ ПРОСТОЙ СПОСОБ
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabName = selectedTab.getText();
            System.out.println("📑 Активная вкладка: " + tabName);

            switch (tabName) {
                case "Музыканты":
                    entityType = "musicians";
                    entityName = safeGetString(entity, "first_name") + " " + safeGetString(entity, "last_name");
                    break;
                case "Ансамбли":
                    entityType = "ensembles";
                    entityName = safeGetString(entity, "name");
                    break;
                case "Произведения":
                    entityType = "compositions";
                    entityName = safeGetString(entity, "title");
                    break;
                case "Пластинки":
                    entityType = "records";
                    entityName = safeGetString(entity, "title");
                    break;
            }
        }

        if (!entityName.isEmpty() && !entityType.isEmpty()) {
            System.out.println("🎯 ОПРЕДЕЛЕНО: " + entityType + " - " + entityName);
            openImageSelectionDialog(entityType, entityName);
        } else {
            System.err.println("❌ Не удалось определить тип сущности");
            showAlert("Ошибка", "Не удалось определить тип сущности. Выберите сущность вручную.");
            showEntitySelectionDialog();
        }
    }

    /**
     * Конвертирует русский тип в английский
     */
    private String convertRussianTypeToEnglish(String russianType) {
        switch (russianType) {
            case "Ансамбль": return "ensembles";
            case "Музыкант": return "musicians";
            case "Произведение": return "compositions";
            case "Пластинка": return "records";
            default: return "ensembles";
        }
    }

    /**
     * Получает выбранную сущность из активной таблицы
     */
    private Map<String, Object> getSelectedEntityFromCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return null;

        String tabName = selectedTab.getText();
        switch (tabName) {
            case "Ансамбли":
                return ensemblesTable.getSelectionModel().getSelectedItem();
            case "Музыканты":
                return musiciansTable.getSelectionModel().getSelectedItem();
            case "Произведения":
                return compositionsTable.getSelectionModel().getSelectedItem();
            case "Пластинки":
                return recordsTable.getSelectionModel().getSelectedItem();
            default:
                return null;
        }
    }

    /**
     * Определяет тип сущности по активной вкладке
     */
    private String getCurrentEntityType() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return "";

        String tabName = selectedTab.getText();
        switch (tabName) {
            case "Ансамбли": return "ensembles";
            case "Музыканты": return "musicians";
            case "Произведения": return "compositions";
            case "Пластинки": return "records";
            default: return "";
        }
    }

    /**
     * Извлекает название сущности из данных
     */
    private String getEntityName(Map<String, Object> entity, String entityType) {
        switch (entityType) {
            case "ensembles":
                return safeGetString(entity, "name");
            case "musicians":
                return safeGetString(entity, "first_name") + " " + safeGetString(entity, "last_name");
            case "compositions":
                return safeGetString(entity, "title");
            case "records":
                return safeGetString(entity, "title");
            default:
                return "";
        }
    }
    /**
     * Открывает диалог выбора изображения - С ДОПОЛНИТЕЛЬНОЙ ОТЛАДКОЙ
     */
    private void openImageSelectionDialog(String entityType, String entityName) {
        System.out.println("🎯 ОТКРЫВАЕМ ДИАЛОГ ДЛЯ: " + entityType + " - " + entityName);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение для: " + entityName + " (" + getRussianEntityType(entityType) + ")");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        if (selectedFile != null) {
            // ПРОВЕРКА: проверяем размер файла (максимум 5MB)
            long fileSize = selectedFile.length();
            if (fileSize > 5 * 1024 * 1024) {
                showAlert("Ошибка", "Размер файла не должен превышать 5MB");
                return;
            }

            System.out.println("📁 ВЫБРАН ФАЙЛ: " + selectedFile.getName() + " для " + entityType + ": " + entityName);
            processSelectedImage(selectedFile, entityType, entityName);
        } else {
            System.out.println("❌ Файл не выбран");
        }
    }


    /**
     * Обрабатывает выбранное изображение - ОПТИМИЗИРОВАННАЯ ВЕРСИЯ
     */
    private void processSelectedImage(File imageFile, String entityType, String entityName) {
        try {
            String fileName = generateImageFileName(entityName);

            System.out.println("💾 Сохранение для: " + entityType + " - " + entityName);

            // КОПИРУЕМ В ОБЕ ПАПКИ
            String[] paths = {
                    "src/main/resources/musicstore/iamges/" + entityType,
                    "target/classes/musicstore/iamges/" + entityType
            };

            for (String path : paths) {
                File dir = new File(path);
                if (!dir.exists()) dir.mkdirs();

                File destination = new File(dir, fileName);
                Files.copy(imageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ " + destination.getAbsolutePath());
            }

            // СРАЗУ показываем изображение
            javafx.application.Platform.runLater(() -> {
                loadEntityImage(entityName, entityType);
            });

            showAlert("Успех", "Изображение добавлено для: " + entityName);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось сохранить изображение");
        }
    }
    /**
     * Тестирует загрузку изображения сразу после сохранения
     */
    private void testImageLoading(String entityType, String entityName) {
        try {
            System.out.println("=== ТЕСТИРОВАНИЕ ЗАГРУЗКИ ===");

            String imageName = generateImageFileName(entityName);
            String imagePath = "/musicstore/iamges/" + entityType + "/" + imageName;

            System.out.println("🔍 Тестовый путь: " + imagePath);

            // Пробуем загрузить разными способами

            // Способ 1: через getResourceAsStream
            InputStream stream1 = getClass().getResourceAsStream(imagePath);
            System.out.println("   InputStream: " + (stream1 != null ? "УСПЕХ" : "НЕ УДАЛОСЬ"));
            if (stream1 != null) stream1.close();

            // Способ 2: через getResource
            java.net.URL url = getClass().getResource(imagePath);
            System.out.println("   URL: " + (url != null ? url.toString() : "НЕ НАЙДЕН"));

            // Способ 3: прямое чтение файла
            File testFile = new File("src/main/resources/musicstore/iamges/" + entityType + "/" + imageName);
            System.out.println("   Прямой файл: " + testFile.exists() + " (" + testFile.length() + " bytes)");

        } catch (Exception e) {
            System.err.println("Ошибка при тестировании: " + e.getMessage());
        }
    }


    /**
     * Генерирует имя файла для изображения - ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    private String generateImageFileName(String entityName) {
        if (entityName == null || entityName.isEmpty()) {
            return "default.jpg";
        }

        // Сохраняем русские буквы, заменяем только пробелы и запрещенные символы
        String fileName = entityName
                .toLowerCase()
                .replace(" ", "_")
                .replaceAll("[^a-zA-Zа-яА-Я0-9_-]", "") // Разрешаем русские буквы
                .trim();

        // Если после фильтрации имя пустое, используем хэш
        if (fileName.isEmpty()) {
            fileName = "entity_" + Math.abs(entityName.hashCode());
        }

        // Добавляем расширение
        fileName += ".jpg";

        System.out.println("Сгенерировано имя файла для '" + entityName + "': " + fileName);
        return fileName;
    }

    /**
     * Обновляет изображение сущности в интерфейсе
     */
    private void updateEntityImage(String entityType, String entityName) {
        // Перезагружаем изображение для текущей выбранной сущности
        loadEntityImage(entityName, entityType);
    }

    /**
     * Получает русское название типа сущности
     */
    private String getRussianEntityType(String entityType) {
        switch (entityType) {
            case "ensembles": return "Ансамбль";
            case "musicians": return "Музыкант";
            case "compositions": return "Произведение";
            case "records": return "Пластинка";
            default: return "Сущность";
        }
    }


    /**
     * Показывает контейнер с изображением
     */
    private void showImageContainer() {
        if (detailImageView.getParent() instanceof VBox) {
            VBox imageContainer = (VBox) detailImageView.getParent();
            imageContainer.setVisible(true);
            imageContainer.setManaged(true);
        }
    }

    /**
     * Скрывает контейнер с изображением
     */
    private void hideImageContainer() {
        if (detailImageView.getParent() instanceof VBox) {
            VBox imageContainer = (VBox) detailImageView.getParent();
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }
        detailImageView.setImage(null);
    }

    /**
     * Обновленный метод очистки деталей
     */
    private void clearDetails() {
        if (detailTitleLabel != null) detailTitleLabel.setText("Выберите запись для просмотра деталей");
        if (detailDescriptionArea != null) detailDescriptionArea.clear();
        hideImageContainer();
    }

    @FXML
    private void removeImage() {
        if (detailImageView.getImage() != null) {
            // ПОДТВЕРЖДЕНИЕ: запрос подтверждения перед удалением изображения
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Подтверждение удаления");
            confirmAlert.setHeaderText("Удаление изображения");
            confirmAlert.setContentText("Вы действительно хотите удалить изображение?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                detailImageView.setImage(null);
                showAlert("Информация", "Изображение удалено");
            }
        } else {
            showAlert("Информация", "Нет изображения для удаления");
        }
    }
    /**
     * Обновляет изображение с задержкой (чтобы файловая система успела обновиться)
     */
    private void refreshCurrentImageWithDelay(String entityType, String entityName) {
        new Thread(() -> {
            try {
                // Ждем 500ms чтобы файловая система точно обновилась
                Thread.sleep(500);

                // Обновляем в UI потоке
                javafx.application.Platform.runLater(() -> {
                    try {
                        System.out.println("Принудительная перезагрузка изображения...");
                        loadEntityImage(entityName, entityType);

                        // Дополнительно: перезагружаем текущую вкладку
                        refreshCurrentTab();

                    } catch (Exception e) {
                        System.err.println("Ошибка при обновлении изображения: " + e.getMessage());
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Перезагружает изображение для текущей выбранной сущности - ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    @FXML
    private void reloadImages() {
        System.out.println("🔄 ПЕРЕЗАГРУЗКА ИЗОБРАЖЕНИЯ");

        try {
            // Определяем что сейчас показывается в деталях
            String currentTitle = detailTitleLabel.getText();

            if (currentTitle.startsWith("Музыкант: ")) {
                String name = currentTitle.substring("Музыкант: ".length());
                System.out.println("🎯 Перезагружаем изображение музыканта: " + name);
                forceLoadImage(name, "musicians");
            }
            else if (currentTitle.startsWith("Ансамбль: ")) {
                String name = currentTitle.substring("Ансамбль: ".length());
                System.out.println("🎯 Перезагружаем изображение ансамбля: " + name);
                forceLoadImage(name, "ensembles");
            }
            else if (currentTitle.startsWith("Произведение: ")) {
                String name = currentTitle.substring("Произведение: ".length());
                System.out.println("🎯 Перезагружаем изображение произведения: " + name);
                forceLoadImage(name, "compositions");
            }
            else if (currentTitle.startsWith("Пластинка: ")) {
                String name = currentTitle.substring("Пластинка: ".length());
                System.out.println("🎯 Перезагружаем изображение пластинки: " + name);
                forceLoadImage(name, "records");
            }
            else {
                showAlert("Информация", "Сначала выберите запись для просмотра");
            }
        } catch (Exception e) {
            System.err.println("💥 Ошибка при перезагрузке изображения: " + e.getMessage());
            showAlert("Ошибка", "Не удалось перезагрузить изображение");
        }
    }

    /**
     * Принудительная загрузка изображения с вашим путем
     */
    private void forceLoadImage(String entityName, String entityType) {
        try {
            detailImageView.setImage(null);

            String imageName = generateImageFileName(entityName);
            // ВАШ ПУТЬ
            String imagePath = "/musicstore/iamges/" + entityType + "/" + imageName;

            System.out.println("🔍 Принудительная загрузка по пути: " + imagePath);

            // Пробуем загрузить через ресурсы
            InputStream imageStream = getClass().getResourceAsStream(imagePath);
            if (imageStream != null) {
                System.out.println("✅ InputStream создан успешно");
                Image image = new Image(imageStream);
                if (!image.isError()) {
                    detailImageView.setImage(image);
                    System.out.println("✅ УСПЕХ: Изображение перезагружено");
                    showAlert("Успех", "Изображение обновлено!");
                    return;
                }
                imageStream.close();
            }

            // Если не нашли, пробуем через файловую систему
            String filePath = "src/main/resources/musicstore/iamges/" + entityType + "/" + imageName;
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                System.out.println("✅ Найден файл: " + filePath);
                Image image = new Image(imageFile.toURI().toString());
                detailImageView.setImage(image);
                System.out.println("✅ УСПЕХ: Изображение загружено из файла");
                showAlert("Успех", "Изображение обновлено!");
                return;
            }

            System.out.println("❌ Изображение не найдено");
            showAlert("Информация", "Изображение не найдено для: " + entityName);

        } catch (Exception e) {
            System.err.println("💥 Ошибка принудительной загрузки: " + e.getMessage());
            showAlert("Ошибка", "Ошибка загрузки изображения");
        }
    }


}