package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.User;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.time.LocalTime;

public class OrganizerController implements UserAware {

    /* ─ UI ─────────────────────────────────────────────────────────── */
    @FXML private ImageView photo;
    @FXML private Label     greeting;
    @FXML private Button    btnProfile;
    @FXML private Button    btnEvents;        // пока заглушки
    @FXML private Button    btnParticipants;
    @FXML private Button    btnJury;

    /* ─ data ───────────────────────────────────────────────────────── */
    private User user;   // текущий авторизованный организатор

    /* вызывается FXMLLoader-ом */
    public void initialize() {

        /* кнопка «Мой профиль» */
        btnProfile.setOnAction(e ->
                ProfileController.open(btnProfile.getScene(), user));

        btnEvents.setOnAction(e -> OrganizerEventsController.open(btnEvents.getScene(), user));
        btnParticipants.setOnAction(e -> ParticipantsController.open(btnParticipants.getScene(), user));
        btnJury.setOnAction(e -> JuryDirectoryController.open(btnJury.getScene(), user));
    }

    /** метод вызывается LoginController-ом сразу после загрузки окна */
    @Override
    public void setUser(User u) {
        this.user = u;

        /* приветствие по времени суток */
        LocalTime now = LocalTime.now();
        String partOfDay;
        if (now.isBefore(LocalTime.of(11, 1))) {
            partOfDay = "Доброе утро";
        } else if (now.isBefore(LocalTime.of(18, 1))) {
            partOfDay = "Добрый день";
        } else {
            partOfDay = "Добрый вечер";
        }

        greeting.setText(String.format("%s, %s %s!",
                partOfDay,
                u.getFirstName(),           // имя
                u.getMiddleName()));        // отчество

        /* устанавливаем фото (если задано) */
        if (u.getPhotoPath() != null && !u.getPhotoPath().isBlank()) {
            photo.setImage(new Image("file:" + u.getPhotoPath()));
        }
    }

    /* маленькая утилита для временных сообщений */
    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
