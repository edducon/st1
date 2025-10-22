package com.example.conferenceapp.controller;

import com.example.conferenceapp.dao.ActivityDao;
import com.example.conferenceapp.dao.EventDao;
import com.example.conferenceapp.dao.ModeratorDao;
import com.example.conferenceapp.dao.ReferenceDao;
import com.example.conferenceapp.model.Event;
import com.example.conferenceapp.model.ModeratorSlot;
import com.example.conferenceapp.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ModeratorController implements UserAware {

    @FXML private Label greetingLabel;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TableView<ModeratorSlot> activityTable;
    @FXML private TableColumn<ModeratorSlot, String> activityCol;
    @FXML private TableColumn<ModeratorSlot, String> eventCol;
    @FXML private TableColumn<ModeratorSlot, String> dateCol;
    @FXML private TableColumn<ModeratorSlot, String> timeCol;
    @FXML private TableColumn<ModeratorSlot, String> statusCol;
    @FXML private Button applyBtn;
    @FXML private Button myActivitiesBtn;
    @FXML private Label infoLabel;

    private final ObservableList<ModeratorSlot> master = FXCollections.observableArrayList();
    private final FilteredList<ModeratorSlot> filtered = new FilteredList<>(master, s -> true);

    private final ModeratorDao moderatorDao = new ModeratorDao();
    private final EventDao eventDao = new EventDao();
    private final ReferenceDao referenceDao = new ReferenceDao();
    private final ActivityDao activityDao = new ActivityDao();

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private User moderator;

    @FXML private Label greetingLabel;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<Event> eventBox;
    @FXML private TableView<ModeratorSlot> activityTable;
    @FXML private TableColumn<ModeratorSlot, String> activityCol;
    @FXML private TableColumn<ModeratorSlot, String> eventCol;
    @FXML private TableColumn<ModeratorSlot, String> dateCol;
    @FXML private TableColumn<ModeratorSlot, String> timeCol;
    @FXML private TableColumn<ModeratorSlot, String> statusCol;
    @FXML private Button applyBtn;
    @FXML private Button myActivitiesBtn;
    @FXML private Label infoLabel;

    private final ObservableList<ModeratorSlot> master = FXCollections.observableArrayList();
    private final FilteredList<ModeratorSlot> filtered = new FilteredList<>(master, s -> true);

    private final ModeratorDao moderatorDao = new ModeratorDao();
    private final EventDao eventDao = new EventDao();
    private final ReferenceDao referenceDao = new ReferenceDao();
    private final ActivityDao activityDao = new ActivityDao();

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private User moderator;

    /* ─ UI ─────────────────────────────────────────────────────────── */
    @FXML private Label greetingLabel;
    @FXML private ComboBox<String> directionBox;
    @FXML private ComboBox<String> eventBox;
    @FXML private TableView<ActivityEntry> activityTable;
    @FXML private TableColumn<ActivityEntry, String> activityCol;
    @FXML private TableColumn<ActivityEntry, String> eventCol;
    @FXML private TableColumn<ActivityEntry, String> dateCol;
    @FXML private TableColumn<ActivityEntry, String> timeCol;
    @FXML private TableColumn<ActivityEntry, String> statusCol;
    @FXML private Button applyBtn;
    @FXML private Button myActivitiesBtn;
    @FXML private Label infoLabel;

    /* ─ data ───────────────────────────────────────────────────────── */
    private final ObservableList<ActivityEntry> master = FXCollections.observableArrayList();
    private final FilteredList<ActivityEntry> filtered = new FilteredList<>(master, e -> true);
    private User user;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /* ─ init ───────────────────────────────────────────────────────── */
    public void initialize() {
        activityCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getActivityTitle()));
        eventCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getEventTitle()));
        dateCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(DATE_FMT.format(p.getValue().getDate())));
        timeCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(TIME_FMT.format(p.getValue().getStart()) + " – " + TIME_FMT.format(p.getValue().getEnd())));
        statusCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(translateStatus(p.getValue().getStatus())));

        activityTable.setItems(filtered);
        activityTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        directionBox.getItems().setAll(referenceDao.findAllDirections());
        directionBox.setOnAction(e -> reload());

        eventBox.setButtonCell(new OrganizerEventsController.EventCell());
        eventBox.setCellFactory(cb -> new OrganizerEventsController.EventCell());
        eventBox.setOnAction(e -> reload());

        applyBtn.disableProperty().bind(activityTable.getSelectionModel().selectedItemProperty().isNull());
        applyBtn.setOnAction(e -> onApply());
        myActivitiesBtn.setOnAction(e -> showMyActivities());

        searchInfo();
    }

    private void searchInfo() {
        infoLabel.setText("Доступных активностей: 0");
    }

    private void reload() {
        if (moderator == null) return;
        String direction = directionBox.getValue();
        Integer eventId = eventBox.getValue() != null ? eventBox.getValue().getId() : null;
        master.setAll(moderatorDao.loadSlots(moderator.getId(), direction, eventId));
        filtered.setPredicate(slot -> true);
        infoLabel.setText("Доступных активностей: " + filtered.size());
    }

    private void onApply() {
        ModeratorSlot slot = activityTable.getSelectionModel().getSelectedItem();
        if (slot == null) return;

        if (slot.getStatus() == ModeratorSlot.Status.APPROVED) {
            showAlert(Alert.AlertType.INFORMATION, "Вы уже модерируете эту активность");
            return;
        }
        if (slot.getStatus() == ModeratorSlot.Status.SENT) {
            showAlert(Alert.AlertType.INFORMATION, "Заявка уже отправлена и ожидает подтверждения");
            return;
        }

        if (activityDao.hasCollision(moderator.getId(), slot.getDate(), slot.getStart(), slot.getEnd())) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Конфликт расписания");
            alert.setHeaderText("Обнаружено пересечение по времени");
            ButtonType cancelPast = new ButtonType("Отменить прошлые заявки", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelCurrent = new ButtonType("Отменить текущую", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(cancelPast, cancelCurrent);
            alert.setContentText("Выберите действие");
            ButtonType result = alert.showAndWait().orElse(cancelCurrent);
            if (result == cancelPast) {
                moderatorDao.cancelApplications(moderator.getId());
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Заявка не отправлена");
                return;
            }
        }

        moderatorDao.submitApplication(slot.getActivityId(), moderator.getId());
        showAlert(Alert.AlertType.INFORMATION, "Заявка отправлена и ожидает подтверждения организатора");
        reload();
    }

    private void showMyActivities() {
        List<ModeratorSlot> mine = moderatorDao.myActivities(moderator.getId());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Мои активности");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> listView = new ListView<>();
        listView.getItems().setAll(mine.stream()
                .map(slot -> String.format("%s (%s %s–%s) — %s",
                        slot.getActivityTitle(),
                        DATE_FMT.format(slot.getDate()),
                        TIME_FMT.format(slot.getStart()),
                        TIME_FMT.format(slot.getEnd()),
                        slot.getEventTitle()))
                .collect(Collectors.toList()));

        dialog.getDialogPane().setContent(listView);
        dialog.showAndWait();
    }

    private String translateStatus(ModeratorSlot.Status status) {
        return switch (status) {
            case AVAILABLE -> "Доступно";
            case SENT -> "Заявка отправлена";
            case APPROVED -> "Одобрено";
        };
    }

    @Override
    public void setUser(User user) {
        this.moderator = user;
        greetingLabel.setText(buildGreeting(user));
        loadEvents();
        reload();
    }

    private void loadEvents() {
        if (moderator == null) return;
        eventBox.getItems().setAll(eventDao.find(null, null));
    }

    private String buildGreeting(User user) {
        LocalTime now = LocalTime.now();
        String part = now.isBefore(LocalTime.of(11, 1)) ? "Доброе утро" :
                now.isBefore(LocalTime.of(18, 1)) ? "Добрый день" : "Добрый вечер";
        return part + ", " + user.getFirstName() + "!";
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
