package com.example.musicstore;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {



    @Override
    public void start(Stage primaryStage) throws IOException {
        // Загрузка формы аутентификации при запуске
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/musicstore/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Music Store - Авторизация");
        primaryStage.setScene(new Scene(root, 700, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}