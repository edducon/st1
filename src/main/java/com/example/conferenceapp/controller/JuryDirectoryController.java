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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.DialogPane;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.List;

public class JuryDirectoryController implements UserAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleBox;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TableView<PersonCard> table;
    @FXML private TableColumn<PersonCard, ImageView> photoCol;
    @FXML private TableColumn<PersonCard, String> nameCol;
    @FXML private TableColumn<PersonCard, String> emailCol;
    @FXML private TableColumn<PersonCard, String> roleCol;
    @FXML private Label countLabel;
    @FXML private Button registerBtn;

    private final ObservableList<PersonCard> master = FXCollections.observableArrayList();
    private final FilteredList<PersonCard> filtered = new FilteredList<>(master, p -> true);
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();

    private User organizer;

    public static void open(Scene parent, User organizer) {
        try {
            FXMLLoader loader = new FXMLLoader(JuryDirectoryController.class.getResource("/com/example/conferenceapp/fxml/JuryDirectory.fxml"));
            DialogPane pane = loader.load();
            JuryDirectoryController controller = loader.getController();
            controller.setUser(organizer);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Жюри и модераторы");
            dialog.setDialogPane(pane);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(parent.getWindow());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.show();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть справочник жюри", e);
        }
    }

    public void initialize() {
        roleBox.getItems().setAll("Все", "Жюри", "Модераторы");
        roleBox.getSelectionModel().selectFirst();

        eventBox.setButtonCell(new OrganizerEventsController.EventCell());
        eventBox.setCellFactory(cb -> new OrganizerEventsController.EventCell());

        photoCol.setCellValueFactory(param -> {
            String photo = param.getValue().getPhotoPath();
            ImageView view = new ImageView();
            if (photo != null && !photo.isBlank()) {
                view.setImage(new Image("file:" + photo, 48, 48, true, true));
            }
            view.setFitWidth(48);
            view.setFitHeight(48);
            return new ReadOnlyObjectWrapper<>(view);
        });
        nameCol .setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getFullName()));
        emailCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getEmail()));
        roleCol .setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getRole()));

        table.setItems(filtered);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        eventBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        registerBtn.setOnAction(e -> JuryRegistrationController.open(table.getScene(), organizer, this::reload));
    }

    private void loadEvents() {
        if (organizer == null) return;
        List<Event> events = eventDao.findByOrganizer(organizer.getId(), null, null);
        eventBox.getItems().setAll(events);
    }

    private void reload() {
        String role = switch (roleBox.getSelectionModel().getSelectedIndex()) {
            case 1 -> "jury";
            case 2 -> "moderator";
            default -> null;
        };
        String lastName = searchField.getText();
        Integer eventId = eventBox.getValue() != null ? eventBox.getValue().getId() : null;
        master.setAll(personDao.findJuryAndModerators(role, lastName, eventId));
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText();
        filtered.setPredicate(card -> {
            if (query != null && !query.isBlank() && !card.getFullName().toLowerCase().contains(query.toLowerCase())) {
                return false;
            }
            return true;
        });
        countLabel.setText("Всего: " + filtered.size());
    }

    @Override
    public void setUser(User user) {
        this.organizer = user;
        loadEvents();
        reload();
    }
}
