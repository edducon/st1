package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.dao.PersonDao;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.PersonCard;
import com.example.conferenceapp.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.Locale;

/**
 * Просмотр жюри и модераторов с фильтрацией по фамилии и мероприятию.
 */
public class JuryController implements UserAware {

    @FXML private Label greetingLabel;
    @FXML private TextField surnameField;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TableView<PersonCard> table;
    @FXML private TableColumn<PersonCard, ImageView> photoCol;
    @FXML private TableColumn<PersonCard, String> nameCol;
    @FXML private TableColumn<PersonCard, String> emailCol;
    @FXML private TableColumn<PersonCard, String> roleCol;
    @FXML private TableColumn<PersonCard, String> eventCol;
    @FXML private Label countLabel;
    @FXML private Button registerBtn;

    private final ObservableList<PersonCard> master = FXCollections.observableArrayList();
    private final FilteredList<PersonCard> filtered = new FilteredList<>(master, p -> true);
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();

    private User user;
    private Image defaultAvatar;

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

        table.setItems(filtered);

        surnameField.textProperty().addListener((obs, o, n) -> applyFilters());
        eventBox.valueProperty().addListener((obs, o, n) -> reload());

        registerBtn.setOnAction(e -> JuryRegistrationController.open(table.getScene(), user, this::reload));
    }

    private void loadDefaultAvatar() {
        var url = getClass().getResource("/com/example/conferenceapp/images/logo.png");
        if (url != null) {
            defaultAvatar = new Image(url.toExternalForm(), 48, 48, true, true);
        }
    }

    private void configureTable() {
        photoCol.setCellValueFactory(param -> {
            String photoPath = param.getValue().getPhotoPath();
            Image image = null;
            if (photoPath != null && !photoPath.isBlank()) {
                image = new Image("file:" + photoPath, 48, 48, true, true);
            } else {
                image = defaultAvatar;
            }
            ImageView view = new ImageView(image);
            view.setFitWidth(48);
            view.setFitHeight(48);
            view.setPreserveRatio(true);
            return new ReadOnlyObjectWrapper<>(view);
        });
        nameCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getFullName()));
        emailCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getEmail()));
        roleCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getRole()));
        eventCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getEventTitle()));
    }

    private void loadEvents() {
        List<Event> events = eventDao.find(null, null);
        eventBox.setItems(FXCollections.observableArrayList(events));
        eventBox.setButtonCell(new OrganizerEventsController.EventCell());
        eventBox.setCellFactory(cb -> new OrganizerEventsController.EventCell());
    }

    private void reload() {
        Integer eventId = eventBox.getValue() != null ? eventBox.getValue().getId() : null;
        master.setAll(personDao.findJuryAndModerators(null, null, eventId));
        applyFilters();
    }

    private void applyFilters() {
        String query = surnameField.getText();
        filtered.setPredicate(card -> {
            if (query != null && !query.isBlank()) {
                String normalized = query.trim().toLowerCase(Locale.ROOT);
                return card.getFullName().toLowerCase(Locale.ROOT).contains(normalized);
            }
            return true;
        });
        countLabel.setText("Количество: " + filtered.size());
    }

    @Override
    public void setUser(User user) {
        this.user = user;
        loadEvents();
        reload();

        if (user == null) {
            greetingLabel.setText("Жюри и модераторы");
            registerBtn.setDisable(true);
            return;
        }

        registerBtn.setDisable(user.getRole() != User.Role.ORGANIZER);
        String welcome = switch (user.getRole()) {
            case ORGANIZER -> "Справочник жюри";
            case MODERATOR -> "Коллеги по мероприятиям";
            default -> "Жюри и модераторы";
        };
        greetingLabel.setText(welcome + ", " + user.getFirstName() + "!");
    }
}
