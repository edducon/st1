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

public class ParticipantsController implements UserAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TableView<PersonCard> table;
    @FXML private TableColumn<PersonCard, ImageView> photoCol;
    @FXML private TableColumn<PersonCard, String> nameCol;
    @FXML private TableColumn<PersonCard, String> emailCol;
    @FXML private TableColumn<PersonCard, String> phoneCol;
    @FXML private Label countLabel;
    @FXML private Button registerBtn;

    private final ObservableList<PersonCard> master = FXCollections.observableArrayList();
    private final FilteredList<PersonCard> filtered = new FilteredList<>(master, p -> true);
    private final PersonDao personDao = new PersonDao();
    private final EventDao eventDao = new EventDao();

    private User organizer;

    public static void open(Scene parent, User organizer) {
        try {
            FXMLLoader loader = new FXMLLoader(ParticipantsController.class.getResource("/com/example/conferenceapp/fxml/Participants.fxml"));
            DialogPane pane = loader.load();
            ParticipantsController controller = loader.getController();
            controller.setUser(organizer);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Участники мероприятий");
            dialog.setDialogPane(pane);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(parent.getWindow());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.show();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть список участников", e);
        }
    }

    public void initialize() {
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
        phoneCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getPhone()));

        table.setItems(filtered);

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        eventBox.valueProperty().addListener((obs, oldV, newV) -> reload());

        registerBtn.setOnAction(e -> ParticipantRegistrationController.open(table.getScene(), organizer, this::reload));
    }

    private void loadEvents() {
        if (organizer == null) return;
        List<Event> events = eventDao.findByOrganizer(organizer.getId(), null, null);
        eventBox.getItems().setAll(events);
    }

    private void reload() {
        String lastName = searchField.getText();
        Integer eventId = eventBox.getValue() != null ? eventBox.getValue().getId() : null;
        master.setAll(personDao.findParticipants(lastName, eventId));
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText();
        filtered.setPredicate(card -> query == null || query.isBlank() || card.getFullName().toLowerCase().contains(query.toLowerCase()));
        countLabel.setText("Всего: " + filtered.size());
    }

    @Override
    public void setUser(User user) {
        this.organizer = user;
        loadEvents();
        reload();
    }
}
