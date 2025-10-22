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

public class JuryRegistrationController {

    @FXML private Label idNumberLabel;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> genderBox;
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> directionBox;
    @FXML private CheckBox attachEventCheck;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TextField photoField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private DatePicker birthDatePicker;
    @FXML private Button choosePhotoBtn;

    private final ReferenceDao referenceDao = new ReferenceDao();
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();

    private Consumer<Void> onRegistered;
    private User organizer;
    private String generatedId;

    public static void open(Scene parent, User organizer, Runnable onRegistered) {
        try {
            FXMLLoader loader = new FXMLLoader(JuryRegistrationController.class.getResource("/com/example/conferenceapp/fxml/JuryRegistration.fxml"));
            DialogPane pane = loader.load();
            JuryRegistrationController controller = loader.getController();
            controller.organizer = organizer;
            controller.onRegistered = v -> onRegistered.run();
            controller.initData();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Регистрация жюри / модератора");
            dialog.setDialogPane(pane);
            dialog.initOwner(parent.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);

            Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                if (!controller.save()) {
                    ev.consume();
                }
            });

            dialog.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть форму регистрации", e);
        }
    }

    public void initialize() {
        directionBox.setEditable(true);
        genderBox.getItems().setAll("Мужской", "Женский");
        roleBox.getItems().setAll("Жюри", "Модератор");
        attachEventCheck.selectedProperty().addListener((obs, oldV, newV) -> eventBox.setDisable(!newV));
        choosePhotoBtn.setOnAction(e -> choosePhoto());
    }

    private void initData() {
        generatedId = personDao.nextIdNumber("JR-");
        idNumberLabel.setText(generatedId);
        directionBox.getItems().setAll(referenceDao.findAllDirections());
        if (organizer != null) {
            List<Event> events = eventDao.findByOrganizer(organizer.getId(), null, null);
            eventBox.getItems().setAll(events);
        }
    }

    private void choosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выбор фото");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(photoField.getScene().getWindow());
        if (file != null) {
            photoField.setText(file.getAbsolutePath());
        }
    }

    private boolean save() {
        if (fullNameField.getText() == null || fullNameField.getText().isBlank()) {
            showError("Введите ФИО");
            return false;
        }
        if (roleBox.getValue() == null) {
            showError("Выберите роль");
            return false;
        }
        if (genderBox.getValue() == null) {
            showError("Выберите пол");
            return false;
        }
        if (emailField.getText() == null || emailField.getText().isBlank()) {
            showError("Введите email");
            return false;
        }
        if (phoneField.getText() == null || phoneField.getText().isBlank()) {
            showError("Введите телефон в формате +7(999)-999-99-99");
            return false;
        }
        if (directionBox.getEditor().getText() == null || directionBox.getEditor().getText().isBlank()) {
            showError("Укажите направление");
            return false;
        }
        if (!validatePassword(passwordField.getText())) {
            showError("Пароль должен содержать минимум 6 символов, строчные и заглавные буквы, цифру и спецсимвол");
            return false;
        }
        if (!passwordField.getText().equals(confirmField.getText())) {
            showError("Пароли не совпадают");
            return false;
        }
        if (attachEventCheck.isSelected() && eventBox.getValue() == null) {
            showError("Выберите мероприятие для привязки");
            return false;
        }

        Integer directionId = null;
        if (directionBox.getEditor().getText() != null && !directionBox.getEditor().getText().isBlank()) {
            directionId = referenceDao.ensureDirection(directionBox.getEditor().getText().trim());
        }

        int userId = personDao.register(
                roleBox.getValue().equals("Жюри") ? "jury" : "moderator",
                generatedId,
                fullNameField.getText().trim(),
                genderBox.getValue() != null && genderBox.getValue().equals("Мужской") ? "male" : "female",
                birthDatePicker.getValue(),
                directionId,
                null,
                emailField.getText().trim(),
                phoneField.getText().trim(),
                passwordField.getText(),
                photoField.getText()
        );

        if (userId == 0) {
            showError("Не удалось зарегистрировать пользователя");
            return false;
        }

        if (attachEventCheck.isSelected() && eventBox.getValue() != null) {
            attachToEvent(userId, eventBox.getValue());
        }

        if (onRegistered != null) {
            onRegistered.accept(null);
        }

        return true;
    }

    private void attachToEvent(int userId, Event event) {
        String sql = "INSERT INTO moderator_assignment(user_id, event_id) VALUES(?, ?)";
        try (var c = com.example.conferenceapp.util.DBUtil.getConnection();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, event.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            showError("Не удалось привязать пользователя к мероприятию: " + ex.getMessage());
        }
    }

    private boolean validatePassword(String password) {
        if (password == null) return false;
        if (password.length() < 6) return false;
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
