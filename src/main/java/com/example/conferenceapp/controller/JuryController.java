package com.example.conferenceapp.controller;

import com.example.conferenceapp.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Просмотр жюри и модераторов с возможностью фильтрации и подсчёта.
 */
public class JuryController implements UserAware {

    /* ─ UI ─────────────────────────────────────────────────────────── */
    @FXML private Label greetingLabel;
    @FXML private TextField surnameField;
    @FXML private ComboBox<String> eventBox;
    @FXML private TableView<PersonEntry> table;
    @FXML private TableColumn<PersonEntry, ImageView> photoCol;
    @FXML private TableColumn<PersonEntry, String> nameCol;
    @FXML private TableColumn<PersonEntry, String> emailCol;
    @FXML private TableColumn<PersonEntry, String> roleCol;
    @FXML private TableColumn<PersonEntry, String> eventCol;
    @FXML private Label countLabel;
    @FXML private Button registerBtn;

    /* ─ data ───────────────────────────────────────────────────────── */
    private final ObservableList<PersonEntry> master = FXCollections.observableArrayList();
    private final FilteredList<PersonEntry> filtered = new FilteredList<>(master, p -> true);
    private Image defaultAvatar;

    /* ─ init ───────────────────────────────────────────────────────── */
    public void initialize() {
        loadDefaultAvatar();
        configureTable();
        loadSampleData();
        configureFilters();

        table.setItems(filtered);
        updateCount();

        registerBtn.setOnAction(e -> onRegister());
    }

    private void loadDefaultAvatar() {
        URL url = getClass().getResource("/com/example/conferenceapp/images/logo.png");
        if (url != null) {
            defaultAvatar = new Image(url.toExternalForm(), 48, 48, true, true);
        }
    }

    private void configureTable() {
        photoCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(createAvatar(cell.getValue().getPhotoPath())));
        nameCol .setCellValueFactory(cell -> cell.getValue().fullNameProperty());
        emailCol.setCellValueFactory(cell -> cell.getValue().emailProperty());
        roleCol .setCellValueFactory(cell -> cell.getValue().roleProperty());
        eventCol.setCellValueFactory(cell -> cell.getValue().eventProperty());
        table.setPlaceholder(new Label("Нет данных"));
    }

    private void loadSampleData() {
        master.setAll(List.of(
                new PersonEntry("Иванов Сергей Петрович", "jury", "ivanov@example.com", "Security Weekend", null),
                new PersonEntry("Петрова Елена Владимировна", "jury", "petrova@example.com", "Digital Security Day", null),
                new PersonEntry("Сидоров Андрей", "moderator", "sidorov@example.com", "Security Weekend", null),
                new PersonEntry("Антонова Марина", "moderator", "antonova@example.com", "CyberLeaders", null),
                new PersonEntry("Кузнецов Михаил", "jury", "kuznetsov@example.com", "CyberLeaders", null)
        ));
    }

    private void configureFilters() {
        eventBox.getItems().setAll(master.stream()
                .map(PersonEntry::getEvent)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList()));

        surnameField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        eventBox.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String term = surnameField.getText() == null ? "" : surnameField.getText().trim().toLowerCase(Locale.ROOT);
        String event = eventBox.getValue();

        filtered.setPredicate(entry ->
                (term.isEmpty() || entry.getSurname().startsWith(term)) &&
                        (event == null || event.equals(entry.getEvent()))
        );

        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Количество: " + filtered.size());
    }

    private void onRegister() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Регистрация жюри/модераторов будет доступна в админ-панели.");
        alert.setHeaderText(null);
        alert.initOwner(registerBtn.getScene().getWindow());
        alert.showAndWait();
    }

    private ImageView createAvatar(String photoPath) {
        Image image = null;
        if (photoPath != null && !photoPath.isBlank()) {
            image = new Image("file:" + photoPath, 48, 48, true, true);
        } else {
            image = defaultAvatar;
        }
        ImageView view = new ImageView();
        if (image != null) {
            view.setImage(image);
        }
        view.setFitWidth(48);
        view.setFitHeight(48);
        view.setPreserveRatio(true);
        return view;
    }

    /* ─ UserAware ─────────────────────────────────────────────────── */
    @Override
    public void setUser(User user) {
        if (user == null) {
            greetingLabel.setText("Жюри и модераторы");
            return;
        }
        String welcome = switch (user.getRole()) {
            case MODERATOR -> "Справочник коллег";
            case ORGANIZER -> "Управление жюри";
            default -> "Жюри и модераторы";
        };
        greetingLabel.setText("%s, %s".formatted(welcome, user.getFirstName()));
    }

    /* ─ model ─────────────────────────────────────────────────────── */
    public static class PersonEntry {
        private final StringProperty fullName = new SimpleStringProperty();
        private final StringProperty role = new SimpleStringProperty();
        private final StringProperty email = new SimpleStringProperty();
        private final StringProperty event = new SimpleStringProperty();
        private final StringProperty photoPath = new SimpleStringProperty();

        public PersonEntry(String fullName, String roleCode, String email, String event, String photoPath) {
            this.fullName.set(fullName);
            this.role.set(roleCode.equals("jury") ? "Жюри" : "Модератор");
            this.email.set(email);
            this.event.set(event);
            this.photoPath.set(photoPath);
        }

        public String getSurname() {
            String[] parts = fullName.get().split("\\s+");
            return parts.length == 0 ? "" : parts[0].toLowerCase(Locale.ROOT);
        }

        public String getEvent() { return event.get(); }
        public String getPhotoPath() { return photoPath.get(); }

        public StringProperty fullNameProperty() { return fullName; }
        public StringProperty roleProperty() { return role; }
        public StringProperty emailProperty() { return email; }
        public StringProperty eventProperty() { return event; }
    }
}
