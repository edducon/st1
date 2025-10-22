package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.dao.PersonDao;
import com.example.conferenceapp.dao.ReferenceDao;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ParticipantRegistrationController {

    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> genderBox;
    @FXML private DatePicker birthDatePicker;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> countryBox;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TextField photoField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Button choosePhotoBtn;

    private final ReferenceDao referenceDao = new ReferenceDao();
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();

    private Consumer<Void> onRegistered;
    private User organizer;
    private String generatedId;

    public static void open(Scene parent, User organizer, Runnable onRegistered) {
        try {
            FXMLLoader loader = new FXMLLoader(ParticipantRegistrationController.class.getResource("/com/example/conferenceapp/fxml/ParticipantRegistration.fxml"));
            DialogPane pane = loader.load();
            ParticipantRegistrationController controller = loader.getController();
            controller.organizer = organizer;
            controller.onRegistered = v -> onRegistered.run();
            controller.initData();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Регистрация участника");
            dialog.setDialogPane(pane);
            dialog.initOwner(parent.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);

            Button ok = (Button) pane.lookupButton(ButtonType.OK);
            ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!controller.save()) {
                    ev.consume();
                }
            });

            dialog.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть регистрацию участника", e);
        }
    }

    public void initialize() {
        genderBox.getItems().setAll("Мужской", "Женский");
        directionBox.setEditable(true);
        choosePhotoBtn.setOnAction(e -> choosePhoto());
    }

    private void initData() {
        generatedId = personDao.nextIdNumber("PT-");
        idLabel.setText(generatedId);
        countryBox.getItems().setAll(referenceDao.findAllCountries());
        directionBox.getItems().setAll(referenceDao.findAllDirections());
        if (organizer != null) {
            List<Event> events = eventDao.findByOrganizer(organizer.getId(), null, null);
            eventBox.getItems().setAll(events);
        }
    }

    private void choosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выбор фото");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(photoField.getScene().getWindow());
        if (file != null) {
            photoField.setText(file.getAbsolutePath());
        }
    }

    private boolean save() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showError("Введите имя");
            return false;
        }
        if (genderBox.getValue() == null) {
            showError("Выберите пол");
            return false;
        }
        LocalDate birthDate = birthDatePicker.getValue();
        if (birthDate == null) {
            showError("Укажите дату рождения");
            return false;
        }
        if (emailField.getText() == null || emailField.getText().isBlank()) {
            showError("Введите email");
            return false;
        }
        if (phoneField.getText() == null || phoneField.getText().isBlank()) {
            showError("Введите телефон");
            return false;
        }
        if (countryBox.getValue() == null) {
            showError("Выберите страну");
            return false;
        }
        if (!validatePassword(passwordField.getText())) {
            showError("Пароль не соответствует требованиям");
            return false;
        }
        if (!passwordField.getText().equals(confirmField.getText())) {
            showError("Пароли не совпадают");
            return false;
        }

        Integer directionId = null;
        if (directionBox.getEditor().getText() != null && !directionBox.getEditor().getText().isBlank()) {
            directionId = referenceDao.ensureDirection(directionBox.getEditor().getText().trim());
        }

        Integer countryId = null;
        if (countryBox.getValue() != null) {
            String country = countryBox.getValue();
            String sql = "SELECT id FROM country WHERE name_ru = ?";
            try (var c = com.example.conferenceapp.util.DBUtil.getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setString(1, country);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        countryId = rs.getInt(1);
                    }
                }
            } catch (Exception ex) {
                showError("Не удалось определить страну: " + ex.getMessage());
            }
        }

        int userId = personDao.register(
                "participant",
                generatedId,
                nameField.getText().trim(),
                genderBox.getValue().equals("Мужской") ? "male" : "female",
                birthDate,
                directionId,
                countryId,
                emailField.getText(),
                phoneField.getText(),
                passwordField.getText(),
                photoField.getText()
        );

        if (userId == 0) {
            showError("Не удалось зарегистрировать участника");
            return false;
        }

        if (eventBox.getValue() != null) {
            attachParticipant(userId, eventBox.getValue());
        }

        if (onRegistered != null) {
            onRegistered.accept(null);
        }

        return true;
    }

    private void attachParticipant(int userId, Event event) {
        String sql = "INSERT INTO participant_event(participant_id, event_id) VALUES(?, ?)";
        try (var c = com.example.conferenceapp.util.DBUtil.getConnection();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, event.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            showError("Не удалось прикрепить участника: " + ex.getMessage());
        }
    }

    private boolean validatePassword(String password) {
        if (password == null || password.length() < 6) return false;
        boolean hasUpper = Pattern.compile("[A-ZА-Я]").matcher(password).find();
        boolean hasLower = Pattern.compile("[a-zа-я]").matcher(password).find();
        boolean hasDigit = Pattern.compile("\\d").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[^a-zA-Zа-яА-Я0-9]").matcher(password).find();
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
