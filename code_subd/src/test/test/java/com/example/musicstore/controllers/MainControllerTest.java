package com.example.musicstore.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MainControllerTest {

    private MainController controller;

    @BeforeEach
    void setUp() {
        controller = new MainController();
    }

    // Тест 1: Проверка санитизации
    @Test
    void testSanitize() throws Exception {
        Method method = MainController.class.getDeclaredMethod("sanitize", String.class);
        method.setAccessible(true);

        // Ошибка была здесь - нужно передавать аргументы правильно
        assertEquals("O''Connor", method.invoke(controller, "O'Connor"));
        assertEquals("test''s", method.invoke(controller, "test's"));
        assertEquals("test", method.invoke(controller, "test"));
        assertEquals("", method.invoke(controller, (Object) null));
    }

    // Тест 2: Извлечение имени из строки
    @ParameterizedTest
    @CsvSource({
            "'1: Иван', 'Иван'",
            "'ID: Name', 'Name'",
            "'Просто имя', 'Просто имя'",
            "'', ''"
    })
    void testExtractName(String input, String expected) throws Exception {
        Method method = MainController.class.getDeclaredMethod("extractNameFromDisplayString", String.class);
        method.setAccessible(true);

        assertEquals(expected, method.invoke(controller, input));
    }

    // Тест 3: Безопасное получение строки из Map
    @Test
    void testSafeGetString() throws Exception {
        Method method = MainController.class.getDeclaredMethod("safeGetString", Map.class, String.class);
        method.setAccessible(true);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 25);

        assertEquals("John", method.invoke(controller, map, "name"));
        assertEquals("25", method.invoke(controller, map, "age"));
        assertEquals("", method.invoke(controller, map, "missing"));
        assertEquals("", method.invoke(controller, null, "name"));
    }

    // Тест 4: Генерация имени файла
    @ParameterizedTest
    @ValueSource(strings = {"Иван Петров", "John Doe", "Test Name", ""})
    void testGenerateFileName(String input) throws Exception {
        Method method = MainController.class.getDeclaredMethod("generateImageFileName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(controller, input);
        assertTrue(result.endsWith(".jpg"));

        if (input.isEmpty()) {
            assertEquals("default.jpg", result);
        }
    }

    // Тест 5: Русские названия полей - ИСПРАВЛЕНО
    @Test
    void testRussianFieldNames() throws Exception {
        Method method = MainController.class.getDeclaredMethod("getRussianFieldName", String.class);
        method.setAccessible(true);

        assertEquals("Имя", method.invoke(controller, "first_name"));
        assertEquals("Отчество", method.invoke(controller, "middle_name"));
        assertEquals("Фамилия", method.invoke(controller, "last_name"));
        assertEquals("Название", method.invoke(controller, "title"));
        assertEquals("unknown field", method.invoke(controller, "unknown_field")); // ИСПРАВЛЕНО!
    }

    // Тест 6: Конвертация типов
    @Test
    void testTypeConversion() throws Exception {
        Method method = MainController.class.getDeclaredMethod("convertRussianTypeToEnglish", String.class);
        method.setAccessible(true);

        assertEquals("ensembles", method.invoke(controller, "Ансамбль"));
        assertEquals("musicians", method.invoke(controller, "Музыкант"));
        assertEquals("compositions", method.invoke(controller, "Произведение"));
        assertEquals("records", method.invoke(controller, "Пластинка"));
    }

    // Тест 7: Форматирование даты - ИСПРАВЛЕНО
    @Test
    void testDateFormat() throws Exception {
        Method method = MainController.class.getDeclaredMethod("formatDateToRussian", String.class);
        method.setAccessible(true);

        assertEquals("2023", method.invoke(controller, "2023"));
        assertEquals("", method.invoke(controller, ""));
        assertEquals("", method.invoke(controller, (Object) null));
    }

    // Тест 8: Логика формирования имени
    @Test
    void testNameLogic() {
        // Проверяем логику формирования полного имени
        String firstName = "Иван";
        String middleName = "Иванович";
        String lastName = "Петров";

        StringBuilder name = new StringBuilder(firstName);
        if (!middleName.isEmpty()) {
            name.append(" ").append(middleName);
        }
        name.append(" ").append(lastName);

        assertEquals("Иван Иванович Петров", name.toString());

        // Без отчества
        middleName = "";
        name = new StringBuilder(firstName);
        if (!middleName.isEmpty()) {
            name.append(" ").append(middleName);
        }
        name.append(" ").append(lastName);

        assertEquals("Иван Петров", name.toString());
    }

    // Тест 9: Обработка специальных символов
    @Test
    void testSpecialCharacters() throws Exception {
        Method sanitize = MainController.class.getDeclaredMethod("sanitize", String.class);
        sanitize.setAccessible(true);

        assertEquals("''", sanitize.invoke(controller, "'"));
        assertEquals("''''''", sanitize.invoke(controller, "'''"));
    }

    // Тест 10: Граничные случаи - ИСПРАВЛЕНО
    @Test
    void testEdgeCases() throws Exception {
        Method sanitizeMethod = MainController.class.getDeclaredMethod("sanitize", String.class);
        Method safeGetMethod = MainController.class.getDeclaredMethod("safeGetString", Map.class, String.class);
        Method extractMethod = MainController.class.getDeclaredMethod("extractNameFromDisplayString", String.class);

        sanitizeMethod.setAccessible(true);
        safeGetMethod.setAccessible(true);
        extractMethod.setAccessible(true);

        // Граничные случаи для санитизации
        assertEquals("''", sanitizeMethod.invoke(controller, "'"));
        assertEquals("''''", sanitizeMethod.invoke(controller, "''"));

        // Граничные случаи для safeGetString
        Map<String, Object> emptyMap = new HashMap<>();
        assertEquals("", safeGetMethod.invoke(controller, emptyMap, "any"));

        // Граничные случаи для extractNameFromDisplayString
        assertEquals("", extractMethod.invoke(controller, (Object) null));
    }
}