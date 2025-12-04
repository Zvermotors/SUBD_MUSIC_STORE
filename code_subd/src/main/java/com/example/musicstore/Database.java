package com.example.musicstore;

import java.sql.*;
import java.util.*;

/**
 * Класс для работы с базой данных музыкального магазина
 * Обеспечивает подключение к БД и выполнение SQL-запросов
 */
public class Database {
    // Параметры подключения к базе данных
    private static final String URL = "jdbc:mysql://localhost:3306/music_store";
    private static final String USER = "root";
    private static final String PASSWORD = "mysql";

    /**
     * Тестирование подключения к базе данных
     * @return true если подключение успешно, false в случае ошибки
     */
    public static boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            return true;
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Выполнение SQL запроса с возвратом результата
     * @param query SQL запрос для выполнения
     * @return список карт (Map) с результатами запроса
     */
    public static List<Map<String, Object>> executeQuery(String query) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value != null ? value : "");
                }
                resultList.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
            System.err.println("Query: " + query);
        }

        return resultList;
    }

    /**
     * Выполнение SQL запроса на обновление данных (INSERT, UPDATE, DELETE)
     * @param query SQL запрос для выполнения
     * @return true если запрос выполнен успешно, false в случае ошибки
     */
    public static boolean executeUpdate(String query) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            int rowsAffected = stmt.executeUpdate(query);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
            System.err.println("Query: " + query);
            return false;
        }
    }

    /**
     * Получение количества произведений, исполняемых ансамблем
     * @param ensembleName название ансамбля
     * @return количество произведений
     */
    public static int getEnsembleCompositionsCount(String ensembleName) {
        String query = "SELECT COUNT(DISTINCT p.composition_id) as count FROM ensembles e JOIN performances p ON e.ensemble_id = p.ensemble_id WHERE e.name = '" + ensembleName + "'";
        List<Map<String, Object>> result = executeQuery(query);
        if (!result.isEmpty() && result.get(0).get("count") != null) {
            Object count = result.get(0).get("count");
            return convertToInt(count);
        }
        return 0;
    }

    /**
     * Получение списка пластинок с участием указанного ансамбля
     * @param ensembleName название ансамбля
     * @return список пластинок
     */
    public static List<Map<String, Object>> getEnsembleRecords(String ensembleName) {
        String query = "SELECT DISTINCT r.* FROM records r JOIN record_tracks rt ON r.record_id = rt.record_id JOIN performances p ON rt.composition_id = p.composition_id JOIN ensembles e ON p.ensemble_id = e.ensemble_id WHERE e.name = '" + ensembleName + "'";
        return executeQuery(query);
    }

    /**
     * Обновление данных о продажах пластинки
     * @param recordId идентификатор пластинки
     * @param additionalSales количество дополнительных продаж
     * @return true если обновление успешно, false в случае ошибки
     */
    public static boolean updateRecordSales(int recordId, int additionalSales) {
        // Получение текущего количества продаж
        String selectQuery = "SELECT current_year_sales FROM records WHERE record_id = " + recordId;
        List<Map<String, Object>> currentData = executeQuery(selectQuery);

        if (currentData.isEmpty()) return false;

        Object salesObj = currentData.get(0).get("current_year_sales");
        int currentSales = convertToInt(salesObj);
        int newSales = currentSales + additionalSales;

        // Обновление данных о продажах
        String updateQuery = "UPDATE records SET current_year_sales = " + newSales + " WHERE record_id = " + recordId;
        return executeUpdate(updateQuery);
    }

    /**
     * Универсальный метод преобразования объекта в целое число
     * @param obj объект для преобразования
     * @return целочисленное значение
     */
    private static int convertToInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}