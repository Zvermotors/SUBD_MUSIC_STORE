module com.example.musicstore {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.example.musicstore to javafx.fxml;
    opens com.example.musicstore.controllers to javafx.fxml;
    opens com.example.musicstore.models to javafx.fxml;
    opens com.example.musicstore.services to javafx.fxml;
    opens com.example.musicstore.utils to javafx.fxml;

    exports com.example.musicstore;
    exports com.example.musicstore.controllers;
    exports com.example.musicstore.models;
    exports com.example.musicstore.services;
    exports com.example.musicstore.utils;
}