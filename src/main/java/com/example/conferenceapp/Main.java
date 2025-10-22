package com.example.conferenceapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        /* ---------- главное FXML ---------- */
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/example/conferenceapp/fxml/Main.fxml"));

        Scene scene = new Scene(root);

        /* ---------- глобальный CSS ---------- */
        URL cssUrl = getClass().getResource(
                "/com/example/conferenceapp/css/styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        /* ---------- иконка приложения ---------- */
        URL iconUrl = getClass().getResource(
                "/com/example/conferenceapp/images/icon.png");   // сначала PNG
        if (iconUrl == null) {                                   // затем ICO
            iconUrl = getClass().getResource(
                    "/com/example/conferenceapp/images/icon.ico");
        }
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        } else {
            System.err.println("[WARN] icon.(png/ico) не найден — иконка не установлена");
        }

        primaryStage.setTitle("Conference Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}
